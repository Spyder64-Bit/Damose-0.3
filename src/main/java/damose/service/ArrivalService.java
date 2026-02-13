package damose.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import damose.config.AppConstants;
import damose.data.mapper.StopTripMapper;
import damose.data.mapper.TripIdUtils;
import damose.data.mapper.TripMatcher;
import damose.model.StopTime;
import damose.model.Trip;
import damose.model.TripServiceCalendar;
import damose.model.TripUpdateRecord;
import damose.model.ConnectionMode;

public class ArrivalService {

    private final Map<String, Map<String, Long>> realtimeArrivals = new HashMap<>();
    private final Map<String, Map<String, NavigableSet<Long>>> realtimeArrivalsByRoute = new HashMap<>();

    private static final long ROUTE_FALLBACK_MAX_DIFF_SECONDS = 30 * 60;

    private final TripMatcher matcher;
    private final StopTripMapper stopTripMapper;
    private final TripServiceCalendar tripServiceCalendar;

    public ArrivalService(TripMatcher matcher, StopTripMapper stopTripMapper, 
                         TripServiceCalendar tripServiceCalendar) {
        this.matcher = matcher;
        this.stopTripMapper = stopTripMapper;
        this.tripServiceCalendar = tripServiceCalendar;
    }

    public void updateRealtimeArrivals(List<TripUpdateRecord> updates) {
        synchronized (realtimeArrivals) {
            realtimeArrivals.clear();
            realtimeArrivalsByRoute.clear();
            for (TripUpdateRecord u : updates) {
                String rawFeedTrip = u.getTripId();
                Set<String> variants = TripIdUtils.generateVariants(rawFeedTrip);
                if (variants.isEmpty()) {
                    String fallback = normalizeTripKey(rawFeedTrip);
                    if (fallback != null && !fallback.isBlank()) {
                        variants = Set.of(fallback);
                    }
                }
                if (variants.isEmpty()) {
                    continue;
                }

                Set<String> stopVariants = generateStopIdVariants(u.getStopId());
                if (stopVariants.isEmpty()) {
                    continue;
                }

                for (String key : variants) {
                    Map<String, Long> byStop = realtimeArrivals.computeIfAbsent(key, k -> new HashMap<>());
                    for (String stopKey : stopVariants) {
                        byStop.merge(stopKey, u.getArrivalEpochSeconds(), Math::min);
                    }
                }

                Set<String> routeVariants = generateRouteIdVariants(u.getRouteId());
                for (String routeKey : routeVariants) {
                    Map<String, NavigableSet<Long>> byStop =
                            realtimeArrivalsByRoute.computeIfAbsent(routeKey, k -> new HashMap<>());
                    for (String stopKey : stopVariants) {
                        byStop.computeIfAbsent(stopKey, k -> new TreeSet<>()).add(u.getArrivalEpochSeconds());
                    }
                }
            }
        }
    }

    public List<String> computeArrivalsForStop(String stopId, ConnectionMode mode, long currentFeedTs) {
        List<StopTime> times = stopTripMapper.getStopTimesForStop(stopId);
        if (times == null || times.isEmpty()) {
            return List.of("Nessun arrivo imminente");
        }

        final long nowEpoch = Instant.now().getEpochSecond();
        final LocalDate feedDate = Instant.ofEpochSecond(currentFeedTs)
                .atZone(ZoneId.systemDefault()).toLocalDate();

        Map<String, RouteArrivalInfo> perRoute = new HashMap<>();

        for (StopTime st : times) {
            Trip trip = matcher.matchByTripId(st.getTripId());
            if (trip == null) continue;
            String routeId = trip.getRouteId();

            String serviceId = trip.getServiceId();
            if (serviceId != null && !serviceId.isEmpty()) {
                if (!tripServiceCalendar.serviceRunsOnDate(serviceId, feedDate)) {
                    continue;
                }
            }

            LocalTime arr = st.getArrivalTime();
            if (arr == null) continue;

            long scheduledEpoch = computeScheduledEpochForFeed(st, currentFeedTs);
            if (scheduledEpoch <= 0) continue;

            long staticDiffMin = (scheduledEpoch - nowEpoch) / 60;
            if (staticDiffMin < -2 || staticDiffMin > AppConstants.STATIC_WINDOW_MIN) continue;

            Long predictedEpoch = (mode == ConnectionMode.ONLINE) 
                ? lookupRealtimeArrivalEpochStrictByStop(st, stopId, routeId, scheduledEpoch) 
                : null;

            if (predictedEpoch != null) {
                long rtDiffMin = (predictedEpoch - nowEpoch) / 60;
                if (rtDiffMin < -2 || rtDiffMin > AppConstants.RT_WINDOW_MIN) {
                    predictedEpoch = null;
                }
            }

            RouteArrivalInfo candidate = new RouteArrivalInfo(routeId, scheduledEpoch, predictedEpoch);
            RouteArrivalInfo current = perRoute.get(routeId);

            if (current == null) {
                perRoute.put(routeId, candidate);
            } else {
                long curKey = current.sortKey();
                long candKey = candidate.sortKey();

                if (candKey < curKey) {
                    perRoute.put(routeId, candidate);
                } else if (candidate.predictedEpoch != null && current.predictedEpoch == null) {
                    if (candKey - curKey < 30 * 60) {
                        perRoute.put(routeId, candidate);
                    }
                }
            }
        }

        List<String> arrivi = perRoute.values().stream()
            .sorted(Comparator.comparingLong(RouteArrivalInfo::sortKey))
            .map(this::formatArrivalInfo)
            .collect(Collectors.toList());

        if (arrivi.isEmpty()) {
            arrivi = new ArrayList<>();
            arrivi.add("Nessun arrivo imminente");
        }
        return arrivi;
    }
    
    public List<String> getAllTripsForStopToday(String stopId, ConnectionMode mode, long currentFeedTs) {
        List<StopTime> times = stopTripMapper.getStopTimesForStop(stopId);
        if (times == null || times.isEmpty()) {
            return List.of("Nessun passaggio programmato per oggi");
        }

        final LocalDate feedDate = Instant.ofEpochSecond(currentFeedTs)
                .atZone(ZoneId.systemDefault()).toLocalDate();

        List<TripArrivalInfo> allTrips = new ArrayList<>();

        for (StopTime st : times) {
            Trip trip = matcher.matchByTripId(st.getTripId());
            if (trip == null) continue;
            
            String routeId = trip.getRouteId();

            String serviceId = trip.getServiceId();
            if (serviceId != null && !serviceId.isEmpty()) {
                if (!tripServiceCalendar.serviceRunsOnDate(serviceId, feedDate)) {
                    continue;
                }
            }

            LocalTime arr = st.getArrivalTime();
            if (arr == null) continue;

            long scheduledEpoch = computeScheduledEpochForFeed(st, currentFeedTs);
            if (scheduledEpoch <= 0) continue;

            Long predictedEpoch = (mode == ConnectionMode.ONLINE) 
                ? lookupRealtimeArrivalEpochStrictByStop(st, stopId, routeId, scheduledEpoch) 
                : null;

            allTrips.add(new TripArrivalInfo(routeId, trip.getTripHeadsign(), 
                arr, scheduledEpoch, predictedEpoch));
        }

        allTrips.sort(Comparator.comparing(t -> t.arrivalTime));

        List<String> result = new ArrayList<>();
        for (TripArrivalInfo info : allTrips) {
            result.add(formatTripInfo(info));
        }

        if (result.isEmpty()) {
            result.add("Nessun passaggio programmato per oggi");
        }
        return result;
    }
    
    private String formatTripInfo(TripArrivalInfo info) {
        String timeStr = String.format("%02d:%02d", info.arrivalTime.getHour(), info.arrivalTime.getMinute());
        String headsign = (info.headsign != null && !info.headsign.isEmpty()) ? info.headsign : "";
        
        if (headsign.length() > 30) {
            headsign = headsign.substring(0, 27) + "...";
        }
        
        if (info.predictedEpoch != null) {
            long delayMin = (info.predictedEpoch - info.scheduledEpoch) / 60;
            String rtStatus;
            if (delayMin > 1) rtStatus = "+" + delayMin + " min";
            else if (delayMin < -1) rtStatus = "-" + Math.abs(delayMin) + " min";
            else rtStatus = "OK";
            
            return timeStr + " | " + info.routeId + " " + headsign + " [" + rtStatus + "]";
        } else {
            return timeStr + " | " + info.routeId + " " + headsign;
        }
    }
    
    private static class TripArrivalInfo {
        final String routeId;
        final String headsign;
        final LocalTime arrivalTime;
        final long scheduledEpoch;
        final Long predictedEpoch;

        TripArrivalInfo(String routeId, String headsign, 
                       LocalTime arrivalTime, long scheduledEpoch, Long predictedEpoch) {
            this.routeId = routeId;
            this.headsign = headsign;
            this.arrivalTime = arrivalTime;
            this.scheduledEpoch = scheduledEpoch;
            this.predictedEpoch = predictedEpoch;
        }
    }

    private String formatArrivalInfo(RouteArrivalInfo info) {
        long now = Instant.now().getEpochSecond();
        
        if (info.predictedEpoch != null) {
            long diffFromNowMin = Math.max(0, (info.predictedEpoch - now) / 60);
            long delayMin = (info.predictedEpoch - info.scheduledEpoch) / 60;
            String status;
            if (delayMin > 1) status = "ritardo di " + delayMin + " min";
            else if (delayMin < -1) status = "anticipo di " + Math.abs(delayMin) + " min";
            else status = "in orario";

            if (diffFromNowMin <= AppConstants.IN_ARRIVO_THRESHOLD_MIN) {
                return info.routeId + " - In arrivo (" + status + ")";
            } else {
                return info.routeId + " - " + diffFromNowMin + " min (" + status + ")";
            }
        } else {
            long diffStaticMin = Math.max(0, (info.scheduledEpoch - now) / 60);
            
            if (diffStaticMin <= AppConstants.IN_ARRIVO_THRESHOLD_MIN) {
                return info.routeId + " - In arrivo";
            } else {
                return info.routeId + " - " + diffStaticMin + " min";
            }
        }
    }

    private Long lookupRealtimeArrivalEpochStrictByStop(StopTime st, String stopId,
                                                        String routeId, long scheduledEpoch) {
        String rawStaticTrip = st.getTripId();
        String normalizedStaticKey = normalizeTripKey(rawStaticTrip);
        Set<String> staticVariants = TripIdUtils.generateVariants(normalizedStaticKey);
        Set<String> stopVariants = generateStopIdVariants(stopId);

        synchronized (realtimeArrivals) {
            for (String v : staticVariants) {
                Map<String, Long> byStop = realtimeArrivals.get(v);
                if (byStop == null) continue;
                for (String stopVariant : stopVariants) {
                    Long direct = byStop.get(stopVariant);
                    if (direct != null) {
                        return direct;
                    }
                }
            }

            for (String key : realtimeArrivals.keySet()) {
                for (String v : staticVariants) {
                    if (key == null || v == null) continue;
                    if (key.contains(v) || key.endsWith(v)) {
                        Map<String, Long> candidate = realtimeArrivals.get(key);
                        if (candidate == null) continue;
                        for (String stopVariant : stopVariants) {
                            Long cand = candidate.get(stopVariant);
                            if (cand != null) {
                                return cand;
                            }
                        }
                    }
                }
            }

            Set<String> routeVariants = generateRouteIdVariants(routeId);
            Long bestRouteFallback = null;
            long bestDiff = Long.MAX_VALUE;
            for (String routeVariant : routeVariants) {
                Map<String, NavigableSet<Long>> byStop = realtimeArrivalsByRoute.get(routeVariant);
                if (byStop == null) continue;
                for (String stopVariant : stopVariants) {
                    NavigableSet<Long> epochs = byStop.get(stopVariant);
                    Long candidate = pickClosestEpoch(epochs, scheduledEpoch);
                    if (candidate == null) {
                        continue;
                    }
                    long diff = Math.abs(candidate - scheduledEpoch);
                    if (diff < bestDiff) {
                        bestDiff = diff;
                        bestRouteFallback = candidate;
                    }
                }
            }
            if (bestRouteFallback != null && bestDiff <= ROUTE_FALLBACK_MAX_DIFF_SECONDS) {
                return bestRouteFallback;
            }

            return null;
        }
    }

    private Long pickClosestEpoch(NavigableSet<Long> epochs, long targetEpoch) {
        if (epochs == null || epochs.isEmpty()) return null;
        Long floor = epochs.floor(targetEpoch);
        Long ceil = epochs.ceiling(targetEpoch);
        if (floor == null) return ceil;
        if (ceil == null) return floor;
        return Math.abs(targetEpoch - floor) <= Math.abs(ceil - targetEpoch) ? floor : ceil;
    }

    private String normalizeTripKey(String rawTripId) {
        if (rawTripId == null) return null;
        String simple = TripIdUtils.normalizeSimple(rawTripId);
        if (simple != null) {
            simple = simple.trim();
        }
        return simple;
    }

    private Set<String> generateStopIdVariants(String rawStopId) {
        Set<String> out = new HashSet<>();
        if (rawStopId == null) return out;

        String trimmed = rawStopId.trim();
        if (trimmed.isEmpty()) return out;

        out.add(trimmed);

        String normalized = normalizeStopIdForMatch(trimmed);
        if (normalized != null && !normalized.isEmpty()) {
            out.add(normalized);
        }

        String digitsOnly = trimmed.replaceAll("\\D", "");
        if (digitsOnly.length() >= 4) {
            out.add(digitsOnly);
        }

        if (normalized != null) {
            String normDigits = normalized.replaceAll("\\D", "");
            if (normDigits.length() >= 4) {
                out.add(normDigits);
            }
        }

        return out;
    }

    private String normalizeStopIdForMatch(String rawStopId) {
        if (rawStopId == null) return null;
        String s = rawStopId.trim();
        if (s.isEmpty()) return null;

        while (true) {
            String lower = s.toLowerCase();
            if (lower.startsWith("stop:")) {
                s = s.substring("stop:".length()).trim();
                continue;
            }
            int colon = s.indexOf(':');
            if (colon > 0 && colon < 6) {
                s = s.substring(colon + 1).trim();
                continue;
            }
            break;
        }

        s = s.replaceFirst("^\\d+#", "");
        s = s.replaceFirst("[_:]\\d+$", "");
        return s.trim();
    }

    private Set<String> generateRouteIdVariants(String rawRouteId) {
        Set<String> out = new HashSet<>();
        if (rawRouteId == null) return out;

        String trimmed = rawRouteId.trim();
        if (trimmed.isEmpty()) return out;
        out.add(trimmed);

        String upper = trimmed.toUpperCase();
        out.add(upper);

        String lower = trimmed.toLowerCase();
        if (lower.startsWith("route:")) {
            String bare = trimmed.substring("route:".length()).trim();
            if (!bare.isEmpty()) {
                out.add(bare);
                out.add(bare.toUpperCase());
            }
        }

        return out;
    }

    private long computeScheduledEpochForFeed(StopTime st, long feedEpochSeconds) {
        LocalTime scheduled = st.getArrivalTime();
        if (scheduled == null) return -1;
        ZoneId zone = ZoneId.systemDefault();
        Instant feedInstant = Instant.ofEpochSecond(feedEpochSeconds);
        LocalDate feedDate = feedInstant.atZone(zone).toLocalDate();

        long best = -1;
        long bestDiff = Long.MAX_VALUE;
        for (int delta = -1; delta <= 1; delta++) {
            LocalDate candidateDate = feedDate.plusDays(delta);
            Instant candInstant = scheduled.atDate(candidateDate).atZone(zone).toInstant();
            long diff = Math.abs(candInstant.getEpochSecond() - feedEpochSeconds);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = candInstant.getEpochSecond();
            }
        }
        return best;
    }

    private static class RouteArrivalInfo {
        final String routeId;
        final long scheduledEpoch;
        final Long predictedEpoch;

        RouteArrivalInfo(String routeId, long scheduledEpoch, Long predictedEpoch) {
            this.routeId = routeId;
            this.scheduledEpoch = scheduledEpoch;
            this.predictedEpoch = predictedEpoch;
        }

        long sortKey() {
            if (predictedEpoch == null) {
                return scheduledEpoch;
            }
            return predictedEpoch;
        }
    }
}


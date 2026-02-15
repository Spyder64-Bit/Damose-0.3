package damose.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;

import com.google.transit.realtime.GtfsRealtime;

import damose.config.AppConstants;
import damose.data.loader.RoutesLoader;
import damose.data.mapper.TripIdUtils;
import damose.model.Route;
import damose.model.Stop;
import damose.model.StopTime;
import damose.model.Trip;
import damose.model.TripUpdateRecord;
import damose.model.VehiclePosition;
import damose.model.VehicleType;
import damose.service.GtfsParser;
import damose.service.RealtimeService;

/**
 * Builds floating panel information for a selected vehicle.
 */
public final class VehiclePanelInfoBuilder {

    private static final String DOT_RED_TAG = "[DOT_RED] ";
    private static final String DOT_GREEN_TAG = "[DOT_GREEN] ";
    private static final String DOT_GRAY_TAG = "[DOT_GRAY] ";

    private final ControllerDataContext dataContext;
    private final LongSupplier feedTimestampSupplier;

    public VehiclePanelInfoBuilder(ControllerDataContext dataContext, LongSupplier feedTimestampSupplier) {
        this.dataContext = dataContext;
        this.feedTimestampSupplier = feedTimestampSupplier;
    }

    public VehiclePanelInfo build(VehiclePosition vp) {
        if (vp == null || dataContext == null) {
            return new VehiclePanelInfo("Veicolo", List.of("Prossimo arrivo: non disponibile"));
        }

        Integer vpDirection = vp.getDirectionId() >= 0 ? vp.getDirectionId() : null;
        String vpRouteId = trimToNull(vp.getRouteId());
        Trip trip = dataContext.getTripMatcher().matchByTripIdAndRoute(vp.getTripId(), vpRouteId, vpDirection);

        String effectiveRouteId = vpRouteId != null
                ? vpRouteId
                : trimToNull(trip != null ? trip.getRouteId() : null);
        Route route = RoutesLoader.getRouteById(effectiveRouteId);

        VehicleType vehicleType = route != null ? route.getVehicleType() : VehicleType.BUS;
        String vehicleName = vehicleType == VehicleType.TRAM ? "Tram" : "Bus";
        String routeCode = resolveRouteCode(route, effectiveRouteId);
        String routeLongName = safe(route != null ? route.getRouteLongName() : null);
        String directionName = resolveDirectionName(effectiveRouteId, trip, vp);
        NextArrivalInfo nextArrival = resolveNextArrivalInfo(vp, trip, effectiveRouteId);

        List<String> rows = new ArrayList<>();
        if (!routeCode.isEmpty()) {
            String lineText = "Linea " + routeCode;
            if (!routeLongName.isEmpty() && !routeLongName.equalsIgnoreCase(routeCode)) {
                lineText = lineText + " - " + routeLongName;
            }
            rows.add(lineText);
        }
        rows.add("Direzione: " + directionName);
        rows.add(formatNextArrivalRow(nextArrival));
        rows.add("Posti a bordo: " + formatOccupancy(vp));

        String title = !routeCode.isEmpty()
                ? vehicleName + " linea " + routeCode
                : vehicleName + " in servizio";

        return new VehiclePanelInfo(title, rows);
    }

    private String resolveDirectionName(String routeId, Trip trip, VehiclePosition vp) {
        String headsign = safe(trip != null ? trip.getTripHeadsign() : null);
        if (!headsign.isEmpty()) {
            return headsign;
        }

        int directionId = vp.getDirectionId() >= 0
                ? vp.getDirectionId()
                : (trip != null ? trip.getDirectionId() : -1);
        if (directionId >= 0 && dataContext.getRouteService() != null) {
            String routeHeadsign = safe(dataContext.getRouteService()
                    .getRepresentativeHeadsignForRouteAndDirection(routeId, directionId));
            if (!routeHeadsign.isEmpty()) {
                return routeHeadsign;
            }
        }
        return "non disponibile";
    }

    private NextArrivalInfo resolveNextArrivalInfo(VehiclePosition vp, Trip trip, String routeId) {
        NextArrivalInfo realtime = findRealtimeNextArrival(vp, trip, routeId);
        if (realtime != null) {
            return realtime;
        }
        return findStaticNextArrival(vp, trip);
    }

    private NextArrivalInfo findRealtimeNextArrival(VehiclePosition vp, Trip trip, String routeId) {
        if (dataContext.getStopTripMapper() == null) {
            return null;
        }

        GtfsRealtime.FeedMessage tuFeed = RealtimeService.getLatestTripUpdates();
        if (tuFeed == null) {
            return null;
        }

        long currentFeedTs = feedTimestampSupplier.getAsLong();
        List<TripUpdateRecord> updates = GtfsParser.parseTripUpdates(
                tuFeed,
                dataContext.getStopTripMapper(),
                currentFeedTs
        );
        if (updates.isEmpty()) {
            return null;
        }

        Set<String> candidateTripVariants = TripIdUtils.generateVariants(
                trip != null ? trip.getTripId() : vp.getTripId()
        );
        if (candidateTripVariants.isEmpty()) {
            return null;
        }

        long nowEpoch = Instant.now().getEpochSecond();
        TripUpdateRecord best = null;
        for (TripUpdateRecord update : updates) {
            if (update == null) continue;
            if (!tripVariantMatches(candidateTripVariants, update.getTripId())) continue;

            String updateRoute = trimToNull(update.getRouteId());
            if (!matchesRouteFilter(routeId, updateRoute)) continue;
            if (update.getArrivalEpochSeconds() < nowEpoch - 60) continue;

            if (best == null || update.getArrivalEpochSeconds() < best.getArrivalEpochSeconds()) {
                best = update;
            }
        }

        if (best == null) {
            return null;
        }

        String stopName = resolveStopName(best.getStopId());
        Long scheduledEpoch = findScheduledEpochForTripStop(
                trip != null ? trip.getTripId() : vp.getTripId(),
                best.getStopId(),
                currentFeedTs
        );
        return new NextArrivalInfo(stopName, best.getArrivalEpochSeconds(), scheduledEpoch, true);
    }

    private NextArrivalInfo findStaticNextArrival(VehiclePosition vp, Trip trip) {
        if (dataContext.getStopTripMapper() == null) {
            return null;
        }

        String tripId = trimToNull(trip != null ? trip.getTripId() : vp.getTripId());
        if (tripId == null) {
            return null;
        }

        List<StopTime> stopTimes = dataContext.getStopTripMapper().getStopTimesForTrip(tripId);
        if (stopTimes.isEmpty()) {
            return null;
        }

        int currentSeq = vp.getStopSequence();
        long nowEpoch = Instant.now().getEpochSecond();
        long currentFeedTs = feedTimestampSupplier.getAsLong();
        StopTime best = null;
        long bestEpoch = Long.MAX_VALUE;

        for (StopTime stopTime : stopTimes) {
            if (stopTime == null) continue;
            if (currentSeq >= 0 && stopTime.getStopSequence() <= currentSeq) continue;

            LocalTime arr = stopTime.getArrivalTime();
            if (arr == null) continue;

            long scheduledEpoch = computeScheduledEpochForFeed(arr, currentFeedTs);
            if (scheduledEpoch < nowEpoch - 60) continue;

            if (scheduledEpoch < bestEpoch) {
                bestEpoch = scheduledEpoch;
                best = stopTime;
            }
        }

        if (best == null) {
            return null;
        }
        return new NextArrivalInfo(resolveStopName(best.getStopId()), bestEpoch, bestEpoch, false);
    }

    private Long findScheduledEpochForTripStop(String tripId, String stopId, long feedTs) {
        if (dataContext.getStopTripMapper() == null) {
            return null;
        }
        String normalizedStop = normalizeStopIdForMatch(stopId);
        if (tripId == null || normalizedStop == null) {
            return null;
        }

        List<StopTime> stopTimes = dataContext.getStopTripMapper().getStopTimesForTrip(tripId);
        if (stopTimes == null || stopTimes.isEmpty()) {
            return null;
        }

        Long bestEpoch = null;
        for (StopTime stopTime : stopTimes) {
            if (stopTime == null || stopTime.getArrivalTime() == null) continue;
            String candidateStop = normalizeStopIdForMatch(stopTime.getStopId());
            if (candidateStop == null || !candidateStop.equalsIgnoreCase(normalizedStop)) continue;

            long candidateEpoch = computeScheduledEpochForFeed(stopTime.getArrivalTime(), feedTs);
            if (candidateEpoch <= 0) continue;
            if (bestEpoch == null || candidateEpoch < bestEpoch) {
                bestEpoch = candidateEpoch;
            }
        }
        return bestEpoch;
    }

    private String formatNextArrivalRow(NextArrivalInfo info) {
        return resolveNextArrivalDotTag(info) + "Prossimo arrivo: " + formatNextArrival(info);
    }

    private String resolveNextArrivalDotTag(NextArrivalInfo info) {
        if (info == null || info.arrivalEpoch() <= 0) {
            return DOT_GRAY_TAG;
        }

        if (info.realtime() && info.scheduledEpoch() != null && info.scheduledEpoch() > 0) {
            long delayMin = (info.arrivalEpoch() - info.scheduledEpoch()) / 60;
            if (delayMin > 1) {
                return DOT_RED_TAG;
            }
            return DOT_GREEN_TAG;
        }

        long diffMin = Math.max(0, (info.arrivalEpoch() - Instant.now().getEpochSecond()) / 60);
        if (diffMin <= AppConstants.IN_ARRIVO_THRESHOLD_MIN) {
            return DOT_GREEN_TAG;
        }
        return DOT_GRAY_TAG;
    }

    private String formatNextArrival(NextArrivalInfo info) {
        if (info == null || info.arrivalEpoch() <= 0) {
            return "non disponibile";
        }

        Instant arrivalInstant = Instant.ofEpochSecond(info.arrivalEpoch());
        LocalTime time = arrivalInstant.atZone(ZoneId.systemDefault()).toLocalTime();
        String timeText = time.format(DateTimeFormatter.ofPattern("HH:mm"));
        String stopName = safe(info.stopName());
        if (stopName.isEmpty()) {
            stopName = "fermata non disponibile";
        }
        return stopName + ", " + timeText;
    }

    private static String formatOccupancy(VehiclePosition vp) {
        if (vp == null) {
            return "non disponibile";
        }
        if (vp.getOccupancyPercentage() >= 0) {
            return "occupazione " + vp.getOccupancyPercentage() + "%";
        }
        String info = safe(vp.getOccupancyInfo());
        if (!info.isEmpty() && !"dato non disponibile".equalsIgnoreCase(info)) {
            return info;
        }
        return "non disponibile";
    }

    private String resolveStopName(String stopId) {
        String normalized = trimToNull(stopId);
        if (normalized == null || dataContext.getStops() == null) {
            return "fermata non disponibile";
        }

        for (Stop stop : dataContext.getStops()) {
            if (stop == null) continue;
            String candidateId = trimToNull(stop.getStopId());
            if (candidateId != null && candidateId.equalsIgnoreCase(normalized)) {
                String name = safe(stop.getStopName());
                return name.isEmpty() ? "fermata " + normalized : name;
            }
        }
        return "fermata " + normalized;
    }

    private long computeScheduledEpochForFeed(LocalTime scheduled, long feedEpochSeconds) {
        if (scheduled == null || feedEpochSeconds <= 0) {
            return -1;
        }

        ZoneId zone = ZoneId.systemDefault();
        LocalDate feedDate = Instant.ofEpochSecond(feedEpochSeconds).atZone(zone).toLocalDate();

        long best = -1;
        long bestDiff = Long.MAX_VALUE;
        for (int delta = -1; delta <= 1; delta++) {
            long candidateEpoch = scheduled
                    .atDate(feedDate.plusDays(delta))
                    .atZone(zone)
                    .toEpochSecond();
            long diff = Math.abs(candidateEpoch - feedEpochSeconds);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = candidateEpoch;
            }
        }
        return best;
    }

    private static boolean tripVariantMatches(Set<String> variants, String rawTripId) {
        if (variants == null || variants.isEmpty() || rawTripId == null) {
            return false;
        }
        Set<String> updateVariants = TripIdUtils.generateVariants(rawTripId);
        if (updateVariants.isEmpty()) {
            return false;
        }
        for (String candidate : updateVariants) {
            if (variants.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeStopIdForMatch(String rawStopId) {
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
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private static String resolveRouteCode(Route route, String fallbackRouteId) {
        if (route == null) {
            return safe(fallbackRouteId);
        }
        String shortName = safe(route.getRouteShortName());
        if (!shortName.isEmpty()) {
            return shortName;
        }
        return safe(route.getRouteId());
    }

    private static boolean matchesRouteFilter(String filterRouteId, String candidateRouteId) {
        if (filterRouteId == null) return true;
        if (candidateRouteId == null) return false;

        String filter = filterRouteId.trim();
        String candidate = candidateRouteId.trim();
        if (filter.isEmpty() || candidate.isEmpty()) return false;
        return filter.equalsIgnoreCase(candidate);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record NextArrivalInfo(String stopName, long arrivalEpoch, Long scheduledEpoch, boolean realtime) {
    }

    public record VehiclePanelInfo(String title, List<String> rows) {
    }
}


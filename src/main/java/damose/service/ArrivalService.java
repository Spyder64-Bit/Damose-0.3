package damose.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
import damose.model.ConnectionMode;
import damose.model.StopTime;
import damose.model.Trip;
import damose.model.TripServiceCalendar;
import damose.model.TripUpdateRecord;

/**
 * Provides service logic for arrival service.
 */
public class ArrivalService {

    private final Map<String, Map<String, Long>> realtimeArrivals = new HashMap<>();
    private final Map<String, Map<String, NavigableSet<Long>>> realtimeArrivalsByRoute = new HashMap<>();

    private static final long ROUTE_FALLBACK_MAX_DIFF_SECONDS = 30 * 60;
    private static final long RT_HISTORY_SECONDS = 5 * 60;
    private static final long RT_LOOKAHEAD_SECONDS = (AppConstants.RT_WINDOW_MIN + 30L) * 60L;
    private static final int MAX_ROUTE_EPOCHS_PER_STOP = 12;

    private final TripMatcher matcher;
    private final StopTripMapper stopTripMapper;
    private final TripServiceCalendar tripServiceCalendar;
    private final RouteFallbackPredictionAssigner routeFallbackPredictionAssigner;

    public ArrivalService(TripMatcher matcher,
                          StopTripMapper stopTripMapper,
                          TripServiceCalendar tripServiceCalendar) {
        this.matcher = matcher;
        this.stopTripMapper = stopTripMapper;
        this.tripServiceCalendar = tripServiceCalendar;
        this.routeFallbackPredictionAssigner = new RouteFallbackPredictionAssigner(
                realtimeArrivalsByRoute,
                realtimeArrivals,
                ROUTE_FALLBACK_MAX_DIFF_SECONDS
        );
    }

    /**
     * Handles updateRealtimeArrivals.
     */
    public void updateRealtimeArrivals(List<TripUpdateRecord> updates) {
        updateRealtimeArrivals(updates, Long.MIN_VALUE);
    }

    /**
     * Handles updateRealtimeArrivals.
     */
    public void updateRealtimeArrivals(List<TripUpdateRecord> updates, long referenceEpochSeconds) {
        if (updates == null || updates.isEmpty()) {
            synchronized (realtimeArrivals) {
                realtimeArrivals.clear();
                realtimeArrivalsByRoute.clear();
            }
            return;
        }

        boolean enforceWindow = referenceEpochSeconds > 0;
        long minAllowedEpoch = enforceWindow ? referenceEpochSeconds - RT_HISTORY_SECONDS : Long.MIN_VALUE;
        long maxAllowedEpoch = enforceWindow ? referenceEpochSeconds + RT_LOOKAHEAD_SECONDS : Long.MAX_VALUE;

        synchronized (realtimeArrivals) {
            realtimeArrivals.clear();
            realtimeArrivalsByRoute.clear();
            for (TripUpdateRecord update : updates) {
                long arrivalEpoch = update.getArrivalEpochSeconds();
                if (arrivalEpoch <= 0 || arrivalEpoch < minAllowedEpoch || arrivalEpoch > maxAllowedEpoch) {
                    continue;
                }

                String rawFeedTrip = update.getTripId();
                Set<String> variants = TripIdUtils.generateVariants(rawFeedTrip);
                if (variants.isEmpty()) {
                    String fallback = ArrivalMatchingUtils.normalizeTripKey(rawFeedTrip);
                    if (fallback != null && !fallback.isBlank()) {
                        variants = Set.of(fallback);
                    }
                }
                if (variants.isEmpty()) {
                    continue;
                }

                Set<String> stopVariants = ArrivalMatchingUtils.generateStopIdVariants(update.getStopId());
                if (stopVariants.isEmpty()) {
                    continue;
                }

                for (String key : variants) {
                    Map<String, Long> byStop = realtimeArrivals.computeIfAbsent(key, k -> new HashMap<>());
                    for (String stopKey : stopVariants) {
                        byStop.merge(stopKey, arrivalEpoch, Math::min);
                    }
                }

                Set<String> routeVariants = ArrivalMatchingUtils.generateRouteIdVariants(update.getRouteId());
                for (String routeKey : routeVariants) {
                    Map<String, NavigableSet<Long>> byStop =
                            realtimeArrivalsByRoute.computeIfAbsent(routeKey, k -> new HashMap<>());
                    for (String stopKey : stopVariants) {
                        NavigableSet<Long> epochs = byStop.computeIfAbsent(stopKey, k -> new TreeSet<>());
                        epochs.add(arrivalEpoch);
                        while (epochs.size() > MAX_ROUTE_EPOCHS_PER_STOP) {
                            epochs.pollLast();
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the result of computeArrivalsForStop.
     */
    public List<String> computeArrivalsForStop(String stopId, ConnectionMode mode, long currentFeedTs) {
        List<StopTime> times = stopTripMapper.getStopTimesForStop(stopId);
        if (times == null || times.isEmpty()) {
            return List.of("Nessun arrivo imminente");
        }

        final long nowEpoch = Instant.now().getEpochSecond();
        final LocalDate feedDate = Instant.ofEpochSecond(currentFeedTs)
                .atZone(ZoneId.systemDefault()).toLocalDate();

        Map<String, RouteArrivalInfo> perRoute = new HashMap<>();

        for (StopTime stopTime : times) {
            Trip trip = matcher.matchByTripId(stopTime.getTripId());
            if (trip == null) {
                continue;
            }

            String routeId = trip.getRouteId();
            if (!isTripRunningOnFeedDate(trip, feedDate)) {
                continue;
            }

            LocalTime arrival = stopTime.getArrivalTime();
            if (arrival == null) {
                continue;
            }

            long scheduledEpoch = ArrivalMatchingUtils.computeScheduledEpochForFeed(arrival, currentFeedTs);
            if (scheduledEpoch <= 0) {
                continue;
            }

            long staticDiffMin = (scheduledEpoch - nowEpoch) / 60;
            if (staticDiffMin < -2 || staticDiffMin > AppConstants.STATIC_WINDOW_MIN) {
                continue;
            }

            Long predictedEpoch = (mode == ConnectionMode.ONLINE)
                    ? lookupRealtimeArrivalEpochStrictByStop(stopTime, stopId, routeId, scheduledEpoch)
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
                .map(info -> ArrivalFormattingSupport.formatArrivalInfo(info, nowEpoch))
                .collect(Collectors.toList());

        if (arrivi.isEmpty()) {
            arrivi = new ArrayList<>();
            arrivi.add("Nessun arrivo imminente");
        }
        return arrivi;
    }

    /**
     * Returns the all trips for stop today.
     */
    public List<String> getAllTripsForStopToday(String stopId, ConnectionMode mode, long currentFeedTs) {
        List<StopTime> times = stopTripMapper.getStopTimesForStop(stopId);
        if (times == null || times.isEmpty()) {
            return List.of("Nessun passaggio programmato per oggi");
        }

        final LocalDate feedDate = Instant.ofEpochSecond(currentFeedTs)
                .atZone(ZoneId.systemDefault()).toLocalDate();

        List<TripArrivalInfo> allTrips = new ArrayList<>();

        for (StopTime stopTime : times) {
            Trip trip = matcher.matchByTripId(stopTime.getTripId());
            if (trip == null) {
                continue;
            }

            if (!isTripRunningOnFeedDate(trip, feedDate)) {
                continue;
            }

            LocalTime arr = stopTime.getArrivalTime();
            if (arr == null) {
                continue;
            }

            long scheduledEpoch = ArrivalMatchingUtils.computeScheduledEpochForFeed(arr, currentFeedTs);
            if (scheduledEpoch <= 0) {
                continue;
            }

            Long predictedEpoch = (mode == ConnectionMode.ONLINE)
                    ? lookupRealtimeArrivalEpochByTripAndStop(stopTime, stopId)
                    : null;

            allTrips.add(new TripArrivalInfo(
                    trip.getRouteId(),
                    trip.getTripHeadsign(),
                    arr,
                    scheduledEpoch,
                    predictedEpoch
            ));
        }

        allTrips.sort(Comparator.comparing(t -> t.arrivalTime));
        if (mode == ConnectionMode.ONLINE) {
            routeFallbackPredictionAssigner.assignRouteFallbackPredictions(stopId, allTrips);
        }

        List<String> result = new ArrayList<>();
        for (TripArrivalInfo info : allTrips) {
            result.add(ArrivalFormattingSupport.formatTripInfo(info));
        }

        if (result.isEmpty()) {
            result.add("Nessun passaggio programmato per oggi");
        }
        return result;
    }

    private boolean isTripRunningOnFeedDate(Trip trip, LocalDate feedDate) {
        String serviceId = trip.getServiceId();
        if (serviceId == null || serviceId.isEmpty()) {
            return true;
        }
        return tripServiceCalendar.serviceRunsOnDate(serviceId, feedDate);
    }

    private Long lookupRealtimeArrivalEpochStrictByStop(StopTime stopTime,
                                                        String stopId,
                                                        String routeId,
                                                        long scheduledEpoch) {
        Long direct = lookupRealtimeArrivalEpochByTripAndStop(stopTime, stopId);
        if (direct != null) {
            return direct;
        }
        return routeFallbackPredictionAssigner.lookupRouteFallbackArrivalEpoch(stopId, routeId, scheduledEpoch);
    }

    private Long lookupRealtimeArrivalEpochByTripAndStop(StopTime stopTime, String stopId) {
        String rawStaticTrip = stopTime.getTripId();
        String normalizedStaticKey = ArrivalMatchingUtils.normalizeTripKey(rawStaticTrip);
        Set<String> staticVariants = TripIdUtils.generateVariants(normalizedStaticKey);
        Set<String> stopVariants = ArrivalMatchingUtils.generateStopIdVariants(stopId);

        synchronized (realtimeArrivals) {
            for (String variant : staticVariants) {
                Map<String, Long> byStop = realtimeArrivals.get(variant);
                if (byStop == null) {
                    continue;
                }
                for (String stopVariant : stopVariants) {
                    Long direct = byStop.get(stopVariant);
                    if (direct != null) {
                        return direct;
                    }
                }
            }

            for (String key : realtimeArrivals.keySet()) {
                for (String variant : staticVariants) {
                    if (key == null || variant == null) {
                        continue;
                    }
                    if (key.contains(variant) || key.endsWith(variant)) {
                        Map<String, Long> candidate = realtimeArrivals.get(key);
                        if (candidate == null) {
                            continue;
                        }
                        for (String stopVariant : stopVariants) {
                            Long value = candidate.get(stopVariant);
                            if (value != null) {
                                return value;
                            }
                        }
                    }
                }
            }
            return null;
        }
    }
}

package damose.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Route-level fallback prediction support for missing trip-level RT updates.
 */
final class RouteFallbackPredictionAssigner {

    private final Map<String, Map<String, NavigableSet<Long>>> realtimeArrivalsByRoute;
    private final Object syncLock;
    private final long routeFallbackMaxDiffSeconds;

    RouteFallbackPredictionAssigner(Map<String, Map<String, NavigableSet<Long>>> realtimeArrivalsByRoute,
                                    Object syncLock,
                                    long routeFallbackMaxDiffSeconds) {
        this.realtimeArrivalsByRoute = realtimeArrivalsByRoute;
        this.syncLock = syncLock;
        this.routeFallbackMaxDiffSeconds = routeFallbackMaxDiffSeconds;
    }

    Long lookupRouteFallbackArrivalEpoch(String stopId, String routeId, long scheduledEpoch) {
        Set<String> routeVariants = ArrivalMatchingUtils.generateRouteIdVariants(routeId);
        Set<String> stopVariants = ArrivalMatchingUtils.generateStopIdVariants(stopId);

        synchronized (syncLock) {
            Long bestRouteFallback = null;
            long bestDiff = Long.MAX_VALUE;
            for (String routeVariant : routeVariants) {
                Map<String, NavigableSet<Long>> byStop = realtimeArrivalsByRoute.get(routeVariant);
                if (byStop == null) {
                    continue;
                }
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
            if (bestRouteFallback != null && bestDiff <= routeFallbackMaxDiffSeconds) {
                return bestRouteFallback;
            }
        }
        return null;
    }

    void assignRouteFallbackPredictions(String stopId, List<TripArrivalInfo> allTrips) {
        Map<String, List<TripArrivalInfo>> unresolvedByRoute = new HashMap<>();
        for (TripArrivalInfo info : allTrips) {
            if (info.predictedEpoch != null) {
                continue;
            }
            if (info.routeId == null || info.routeId.isBlank()) {
                continue;
            }
            unresolvedByRoute.computeIfAbsent(info.routeId, k -> new ArrayList<>()).add(info);
        }

        for (Map.Entry<String, List<TripArrivalInfo>> entry : unresolvedByRoute.entrySet()) {
            List<Long> fallbackEpochs = collectRouteFallbackEpochs(entry.getKey(), stopId);
            if (fallbackEpochs.isEmpty()) {
                continue;
            }
            assignClosestRouteFallback(entry.getValue(), fallbackEpochs);
        }
    }

    private List<Long> collectRouteFallbackEpochs(String routeId, String stopId) {
        Set<String> routeVariants = ArrivalMatchingUtils.generateRouteIdVariants(routeId);
        Set<String> stopVariants = ArrivalMatchingUtils.generateStopIdVariants(stopId);
        NavigableSet<Long> allEpochs = new TreeSet<>();

        synchronized (syncLock) {
            for (String routeVariant : routeVariants) {
                Map<String, NavigableSet<Long>> byStop = realtimeArrivalsByRoute.get(routeVariant);
                if (byStop == null) {
                    continue;
                }
                for (String stopVariant : stopVariants) {
                    NavigableSet<Long> epochs = byStop.get(stopVariant);
                    if (epochs != null) {
                        allEpochs.addAll(epochs);
                    }
                }
            }
        }
        return new ArrayList<>(allEpochs);
    }

    private void assignClosestRouteFallback(List<TripArrivalInfo> unresolvedTrips, List<Long> availableEpochs) {
        List<TripArrivalInfo> pendingTrips = new ArrayList<>(unresolvedTrips);
        List<Long> pendingEpochs = new ArrayList<>(availableEpochs);

        while (!pendingTrips.isEmpty() && !pendingEpochs.isEmpty()) {
            int bestTripIdx = -1;
            int bestEpochIdx = -1;
            long bestDiff = Long.MAX_VALUE;

            for (int i = 0; i < pendingTrips.size(); i++) {
                long scheduled = pendingTrips.get(i).scheduledEpoch;
                for (int j = 0; j < pendingEpochs.size(); j++) {
                    long diff = Math.abs(pendingEpochs.get(j) - scheduled);
                    if (diff < bestDiff) {
                        bestDiff = diff;
                        bestTripIdx = i;
                        bestEpochIdx = j;
                    }
                }
            }

            if (bestTripIdx < 0 || bestEpochIdx < 0 || bestDiff > routeFallbackMaxDiffSeconds) {
                break;
            }

            TripArrivalInfo assignedTrip = pendingTrips.remove(bestTripIdx);
            Long assignedEpoch = pendingEpochs.remove(bestEpochIdx);
            assignedTrip.predictedEpoch = assignedEpoch;
        }
    }

    private static Long pickClosestEpoch(NavigableSet<Long> epochs, long targetEpoch) {
        if (epochs == null || epochs.isEmpty()) {
            return null;
        }
        Long floor = epochs.floor(targetEpoch);
        Long ceil = epochs.ceiling(targetEpoch);
        if (floor == null) {
            return ceil;
        }
        if (ceil == null) {
            return floor;
        }
        return Math.abs(targetEpoch - floor) <= Math.abs(ceil - targetEpoch) ? floor : ceil;
    }
}

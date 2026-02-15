package damose.view.map;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import damose.data.mapper.TripIdUtils;
import damose.model.Trip;

/**
 * Mutable trip index used to resolve realtime trip ids against static trips.
 */
final class TripLookupIndex {

    private final Map<String, List<Trip>> tripByExactId = new HashMap<>();
    private final Map<String, List<Trip>> tripByNormalizedId = new HashMap<>();
    private List<Trip> indexedTripsRef;

    void ensureIndexed(List<Trip> trips) {
        if (trips == null) {
            return;
        }
        if (indexedTripsRef == trips && !tripByExactId.isEmpty()) {
            return;
        }

        tripByExactId.clear();
        tripByNormalizedId.clear();
        indexedTripsRef = trips;

        for (Trip trip : trips) {
            if (trip == null) continue;
            String staticTripId = trip.getTripId();
            if (staticTripId == null || staticTripId.isBlank()) continue;

            tripByExactId.computeIfAbsent(staticTripId, k -> new ArrayList<>()).add(trip);
            String normalized = TripIdUtils.normalizeSimple(staticTripId);
            if (normalized != null && !normalized.isBlank()) {
                tripByNormalizedId.computeIfAbsent(normalized, k -> new ArrayList<>()).add(trip);
            }
        }
    }

    Trip findTrip(String tripId, String routeId, Integer directionId) {
        if (tripId == null || tripId.isBlank()) return null;

        Trip exact = chooseBestCandidate(tripByExactId.get(tripId), routeId, directionId);
        if (exact != null) {
            return exact;
        }

        Set<Trip> candidates = new LinkedHashSet<>();
        Set<String> rtVariants = TripIdUtils.generateVariants(tripId);
        for (String variant : rtVariants) {
            List<Trip> normalized = tripByNormalizedId.get(variant);
            if (normalized != null && !normalized.isEmpty()) {
                candidates.addAll(normalized);
            }
        }
        return chooseBestCandidate(new ArrayList<>(candidates), routeId, directionId);
    }

    private static Trip chooseBestCandidate(List<Trip> candidates, String routeId, Integer directionId) {
        if (candidates == null || candidates.isEmpty()) return null;

        String preferredRoute = trimToNull(routeId);
        if (preferredRoute != null) {
            List<Trip> routeMatches = new ArrayList<>();
            for (Trip t : candidates) {
                if (t == null || t.getRouteId() == null) continue;
                if (preferredRoute.equalsIgnoreCase(t.getRouteId().trim())) {
                    routeMatches.add(t);
                }
            }
            if (routeMatches.size() == 1) {
                return routeMatches.get(0);
            }
            if (routeMatches.size() > 1) {
                if (directionId != null && directionId >= 0) {
                    List<Trip> directional = new ArrayList<>();
                    for (Trip t : routeMatches) {
                        if (t != null && t.getDirectionId() == directionId) {
                            directional.add(t);
                        }
                    }
                    if (directional.size() == 1) {
                        return directional.get(0);
                    }
                }
                return null;
            }
            return null;
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        if (directionId != null && directionId >= 0) {
            List<Trip> directional = new ArrayList<>();
            for (Trip t : candidates) {
                if (t != null && t.getDirectionId() == directionId) {
                    directional.add(t);
                }
            }
            if (directional.size() == 1) {
                return directional.get(0);
            }
        }

        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}


package damose.data.mapper;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import damose.model.Trip;
import damose.model.VehiclePosition;

/**
 * Data mapping logic for trip matcher.
 */
public class TripMatcher {

    private final Map<String, Trip> tripsById;
    private final Map<String, Trip> tripsByNormalizedId;
    private final Map<String, List<Trip>> tripsByExactIdCandidates;
    private final Map<String, List<Trip>> tripsByNormalizedIdCandidates;

    public TripMatcher(List<Trip> trips) {
        this.tripsById = new HashMap<>();
        this.tripsByNormalizedId = new HashMap<>();
        this.tripsByExactIdCandidates = new HashMap<>();
        this.tripsByNormalizedIdCandidates = new HashMap<>();

        for (Trip trip : trips) {
            if (trip == null || trip.getTripId() == null) continue;
            tripsById.putIfAbsent(trip.getTripId(), trip);
            tripsByExactIdCandidates
                    .computeIfAbsent(trip.getTripId(), k -> new java.util.ArrayList<>())
                    .add(trip);

            String normalized = TripIdUtils.normalizeSimple(trip.getTripId());
            if (normalized != null && !normalized.isBlank()) {
                tripsByNormalizedId.putIfAbsent(normalized, trip);
                tripsByNormalizedIdCandidates
                        .computeIfAbsent(normalized, k -> new java.util.ArrayList<>())
                        .add(trip);
            }
        }
    }

    /**
     * Returns the result of match.
     */
    public Trip match(VehiclePosition vp) {
        if (vp == null) return null;
        Integer direction = vp.getDirectionId() >= 0 ? vp.getDirectionId() : null;
        Trip match = matchByTripIdAndRoute(vp.getTripId(), vp.getRouteId(), direction);
        if (match != null) return match;
        return tripsById.get(vp.getTripId());
    }

    /**
     * Returns the result of matchByTripId.
     */
    public Trip matchByTripId(String tripId) {
        if (tripId == null || tripId.isBlank()) return null;

        Trip exact = tripsById.get(tripId);
        if (exact != null) return exact;

        Set<String> variants = TripIdUtils.generateVariants(tripId);
        for (String variant : variants) {
            Trip byNormalized = tripsByNormalizedId.get(variant);
            if (byNormalized != null) {
                return byNormalized;
            }
        }

        return null;
    }

    /**
     * Returns the result of matchByTripIdAndRoute.
     */
    public Trip matchByTripIdAndRoute(String tripId, String routeId, Integer directionId) {
        if (tripId == null || tripId.isBlank()) return null;

        Trip exact = chooseBestCandidate(tripsByExactIdCandidates.get(tripId), routeId, directionId);
        if (exact != null) {
            return exact;
        }

        Set<Trip> allCandidates = new LinkedHashSet<>();
        Set<String> variants = TripIdUtils.generateVariants(tripId);
        for (String variant : variants) {
            List<Trip> list = tripsByNormalizedIdCandidates.get(variant);
            if (list != null && !list.isEmpty()) {
                allCandidates.addAll(list);
            }
        }
        return chooseBestCandidate(new java.util.ArrayList<>(allCandidates), routeId, directionId);
    }

    private Trip chooseBestCandidate(List<Trip> candidates, String routeId, Integer directionId) {
        if (candidates == null || candidates.isEmpty()) return null;

        String preferredRoute = trimToNull(routeId);
        if (preferredRoute != null) {
            List<Trip> routeMatches = candidates.stream()
                    .filter(t -> t != null
                            && t.getRouteId() != null
                            && preferredRoute.equalsIgnoreCase(t.getRouteId().trim()))
                    .collect(Collectors.toList());
            if (routeMatches.size() == 1) {
                return routeMatches.get(0);
            }
            if (routeMatches.size() > 1) {
                if (directionId != null && directionId >= 0) {
                    List<Trip> directional = routeMatches.stream()
                            .filter(t -> t.getDirectionId() == directionId)
                            .collect(Collectors.toList());
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
            List<Trip> directional = candidates.stream()
                    .filter(t -> t != null && t.getDirectionId() == directionId)
                    .collect(Collectors.toList());
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

    /**
     * Returns the result of searchByRouteOrHeadsign.
     */
    public List<Trip> searchByRouteOrHeadsign(String query) {
        String q = query.toLowerCase();
        return tripsById.values().stream()
                .filter(t -> t.getRouteId().toLowerCase().contains(q)
                          || t.getTripHeadsign().toLowerCase().contains(q)
                          || (t.getTripShortName() != null && t.getTripShortName().toLowerCase().contains(q)))
                .collect(Collectors.toList());
    }
}


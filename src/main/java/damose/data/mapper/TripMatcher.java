package damose.data.mapper;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import damose.model.Trip;
import damose.model.VehiclePosition;

/**
 * Data mapping logic for trip matcher.
 */
public class TripMatcher {

    private final List<Trip> trips;
    private final Map<String, List<Trip>> tripsByExactIdCandidates;
    private final Map<String, List<Trip>> tripsByNormalizedIdCandidates;

    public TripMatcher(List<Trip> trips) {
        this.trips = trips != null ? trips : List.of();
        this.tripsByExactIdCandidates = new HashMap<>();
        this.tripsByNormalizedIdCandidates = new HashMap<>();

        for (Trip trip : this.trips) {
            if (trip == null || trip.getTripId() == null) continue;
            tripsByExactIdCandidates
                    .computeIfAbsent(trip.getTripId(), k -> new java.util.ArrayList<>())
                    .add(trip);

            String normalized = TripIdUtils.normalizeSimple(trip.getTripId());
            if (normalized != null && !normalized.isBlank()) {
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
        return matchByTripIdAndRoute(vp.getTripId(), vp.getRouteId(), direction);
    }

    /**
     * Returns the result of matchByTripId.
     */
    public Trip matchByTripId(String tripId) {
        if (tripId == null || tripId.isBlank()) return null;

        List<Trip> exact = tripsByExactIdCandidates.get(tripId);
        if (exact != null && !exact.isEmpty()) return exact.get(0);

        Set<String> variants = TripIdUtils.generateVariants(tripId);
        for (String variant : variants) {
            List<Trip> byNormalized = tripsByNormalizedIdCandidates.get(variant);
            if (byNormalized != null && !byNormalized.isEmpty()) {
                return byNormalized.get(0);
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
            Trip onlyRouteMatch = null;
            int routeMatches = 0;
            for (Trip t : candidates) {
                if (t == null || t.getRouteId() == null) continue;
                if (!preferredRoute.equalsIgnoreCase(t.getRouteId().trim())) continue;
                routeMatches++;
                if (routeMatches == 1) {
                    onlyRouteMatch = t;
                } else {
                    onlyRouteMatch = null;
                    break;
                }
            }
            if (routeMatches == 1) {
                return onlyRouteMatch;
            }
            if (routeMatches > 1) {
                if (directionId != null && directionId >= 0) {
                    Trip onlyDirectionalMatch = null;
                    int directionalMatches = 0;
                    for (Trip t : candidates) {
                        if (t == null || t.getRouteId() == null) continue;
                        if (!preferredRoute.equalsIgnoreCase(t.getRouteId().trim())) continue;
                        if (t.getDirectionId() != directionId) continue;
                        directionalMatches++;
                        if (directionalMatches == 1) {
                            onlyDirectionalMatch = t;
                        } else {
                            onlyDirectionalMatch = null;
                            break;
                        }
                    }
                    if (directionalMatches == 1) {
                        return onlyDirectionalMatch;
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
            Trip onlyDirectionalMatch = null;
            int directionalMatches = 0;
            for (Trip t : candidates) {
                if (t == null || t.getDirectionId() != directionId) continue;
                directionalMatches++;
                if (directionalMatches == 1) {
                    onlyDirectionalMatch = t;
                } else {
                    onlyDirectionalMatch = null;
                    break;
                }
            }
            if (directionalMatches == 1) {
                return onlyDirectionalMatch;
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
        return trips.stream()
                .filter(t -> t != null
                        && t.getRouteId() != null
                        && t.getTripHeadsign() != null
                        && (t.getRouteId().toLowerCase().contains(q)
                        || t.getTripHeadsign().toLowerCase().contains(q)
                        || (t.getTripShortName() != null && t.getTripShortName().toLowerCase().contains(q))))
                .toList();
    }
}


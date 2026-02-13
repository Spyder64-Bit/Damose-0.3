package damose.data.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import damose.model.Trip;
import damose.model.VehiclePosition;

public class TripMatcher {

    private final Map<String, Trip> tripsById;
    private final Map<String, Trip> tripsByNormalizedId;

    public TripMatcher(List<Trip> trips) {
        this.tripsById = new HashMap<>();
        this.tripsByNormalizedId = new HashMap<>();

        for (Trip trip : trips) {
            if (trip == null || trip.getTripId() == null) continue;
            tripsById.putIfAbsent(trip.getTripId(), trip);

            String normalized = TripIdUtils.normalizeSimple(trip.getTripId());
            if (normalized != null && !normalized.isBlank()) {
                tripsByNormalizedId.putIfAbsent(normalized, trip);
            }
        }
    }

    public Trip match(VehiclePosition vp) {
        return tripsById.get(vp.getTripId());
    }

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

    public List<Trip> searchByRouteOrHeadsign(String query) {
        String q = query.toLowerCase();
        return tripsById.values().stream()
                .filter(t -> t.getRouteId().toLowerCase().contains(q)
                          || t.getTripHeadsign().toLowerCase().contains(q)
                          || (t.getTripShortName() != null && t.getTripShortName().toLowerCase().contains(q)))
                .collect(Collectors.toList());
    }
}


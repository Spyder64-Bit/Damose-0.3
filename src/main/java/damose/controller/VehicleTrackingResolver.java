package damose.controller;

import java.util.List;

import damose.data.mapper.TripMatcher;
import damose.model.Trip;
import damose.model.VehiclePosition;

/**
 * Resolves a tracked vehicle from marker id and current route/direction filters.
 */
final class VehicleTrackingResolver {

    VehiclePosition findByMarkerId(String markerId,
                                   List<VehiclePosition> positions,
                                   String routeFilter,
                                   Integer directionFilter,
                                   TripMatcher tripMatcher) {
        if (markerId == null || positions == null || positions.isEmpty() || tripMatcher == null) {
            return null;
        }

        String normalizedRouteFilter = trimToNull(routeFilter);

        for (VehiclePosition vp : positions) {
            if (vp == null || vp.getPosition() == null) continue;
            if (!markerId.equalsIgnoreCase(resolveMarkerId(vp))) continue;

            Integer vpDirection = vp.getDirectionId() >= 0 ? vp.getDirectionId() : null;
            String vpRouteId = trimToNull(vp.getRouteId());
            Trip trip = tripMatcher.matchByTripIdAndRoute(vp.getTripId(), vpRouteId, vpDirection);

            String effectiveRouteId = vpRouteId != null
                    ? vpRouteId
                    : trimToNull(trip != null ? trip.getRouteId() : null);
            int effectiveDirection = vpDirection != null
                    ? vpDirection
                    : (trip != null ? trip.getDirectionId() : -1);

            if (!matchesRouteFilter(normalizedRouteFilter, effectiveRouteId)) continue;
            if (directionFilter != null && effectiveDirection != directionFilter) continue;
            return vp;
        }
        return null;
    }

    private static String resolveMarkerId(VehiclePosition vp) {
        String markerId = trimToNull(vp.getVehicleId());
        if (markerId != null) return markerId;

        markerId = trimToNull(vp.getTripId());
        if (markerId != null) return markerId;

        return "unknown";
    }

    private static boolean matchesRouteFilter(String filterRouteId, String candidateRouteId) {
        if (filterRouteId == null) return true;
        if (candidateRouteId == null) return false;

        String filter = filterRouteId.trim();
        String candidate = candidateRouteId.trim();
        if (filter.isEmpty() || candidate.isEmpty()) return false;
        return filter.equalsIgnoreCase(candidate);
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

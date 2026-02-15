package damose.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jxmapviewer.viewer.GeoPosition;

import damose.data.mapper.TripMatcher;
import damose.model.Route;
import damose.model.Stop;
import damose.model.Trip;
import damose.model.VehiclePosition;
import damose.model.VehicleType;
import damose.view.component.RouteSidePanel;

/**
 * Coordinates application flow for route vehicle marker builder.
 */
public final class RouteVehicleMarkerBuilder {

    @FunctionalInterface
    public interface RouteLookup {
        Route findById(String routeId);
    }

    private final TripMatcher tripMatcher;
    private final RouteLookup routeLookup;

    public RouteVehicleMarkerBuilder(TripMatcher tripMatcher, RouteLookup routeLookup) {
        this.tripMatcher = tripMatcher;
        this.routeLookup = routeLookup;
    }

    public List<RouteSidePanel.VehicleMarker> buildForRoute(List<VehiclePosition> positions,
                                                            String routeId,
                                                            List<Stop> routeStops,
                                                            Integer directionFilter) {
        if (positions == null || routeId == null || routeStops == null || routeStops.size() < 2) {
            return List.of();
        }

        Map<String, RouteSidePanel.VehicleMarker> byVehicle = new LinkedHashMap<>();
        for (VehiclePosition vp : positions) {
            if (vp == null || vp.getPosition() == null) continue;

            Integer vpDirection = vp.getDirectionId() >= 0 ? vp.getDirectionId() : null;
            String vpRouteId = trimToNull(vp.getRouteId());
            Trip trip = tripMatcher.matchByTripIdAndRoute(vp.getTripId(), vpRouteId, vpDirection);

            String effectiveRouteId = vpRouteId != null ? vpRouteId : (trip != null ? trimToNull(trip.getRouteId()) : null);
            int effectiveDirection = vpDirection != null ? vpDirection : (trip != null ? trip.getDirectionId() : -1);
            if (!matchesRouteFilter(routeId, effectiveRouteId)) continue;
            if (directionFilter != null && effectiveDirection != directionFilter) continue;

            double progress = computeRouteProgress(vp.getPosition(), routeStops);
            VehicleType vehicleType = resolveVehicleType(effectiveRouteId);
            String markerId = resolveMarkerId(vp);
            String routeCode = resolveRouteCode(effectiveRouteId);
            String vehicleKind = vehicleType == VehicleType.TRAM ? "TRAM" : "BUS";
            int progressPct = (int) Math.round(progress * 100.0);
            String markerDetails = "L:" + routeCode + "  ID:" + markerId + "  " + progressPct + "%";

            byVehicle.put(markerId, new RouteSidePanel.VehicleMarker(
                    progress, markerId, vehicleType, vehicleKind, markerDetails));
        }

        List<RouteSidePanel.VehicleMarker> markers = new ArrayList<>(byVehicle.values());
        markers.sort(Comparator.comparingDouble(RouteSidePanel.VehicleMarker::getProgress));
        return markers;
    }

    private VehicleType resolveVehicleType(String routeId) {
        Route route = routeLookup.findById(routeId);
        return route != null ? route.getVehicleType() : VehicleType.BUS;
    }

    private String resolveRouteCode(String routeId) {
        Route route = routeLookup.findById(routeId);
        if (route == null) return routeId == null ? "" : routeId;
        String shortName = safe(route.getRouteShortName());
        return shortName.isEmpty() ? route.getRouteId() : shortName;
    }

    private static String resolveMarkerId(VehiclePosition vp) {
        String markerId = trimToNull(vp.getVehicleId());
        if (markerId != null) return markerId;

        markerId = trimToNull(vp.getTripId());
        if (markerId != null) return markerId;

        return "unknown@" + Integer.toHexString(System.identityHashCode(vp));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean matchesRouteFilter(String filterRouteId, String candidateRouteId) {
        if (filterRouteId == null || candidateRouteId == null) return false;

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

    private static double computeRouteProgress(GeoPosition pos, List<Stop> routeStops) {
        if (pos == null || routeStops == null || routeStops.size() < 2) return 0.0;

        double refLatRad = Math.toRadians(pos.getLatitude());
        double cosRef = Math.max(0.2, Math.cos(refLatRad));

        int n = routeStops.size();
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            Stop s = routeStops.get(i);
            xs[i] = s.getStopLon() * cosRef;
            ys[i] = s.getStopLat();
        }

        double[] prefixLen = new double[n];
        for (int i = 1; i < n; i++) {
            double dx = xs[i] - xs[i - 1];
            double dy = ys[i] - ys[i - 1];
            prefixLen[i] = prefixLen[i - 1] + Math.hypot(dx, dy);
        }

        double total = prefixLen[n - 1];
        if (total <= 0.0) return 0.0;

        double px = pos.getLongitude() * cosRef;
        double py = pos.getLatitude();
        double bestDist2 = Double.MAX_VALUE;
        double bestPathLen = 0.0;

        for (int i = 0; i < n - 1; i++) {
            double ax = xs[i];
            double ay = ys[i];
            double bx = xs[i + 1];
            double by = ys[i + 1];
            double dx = bx - ax;
            double dy = by - ay;
            double segLen2 = dx * dx + dy * dy;
            if (segLen2 <= 1e-12) continue;

            double t = ((px - ax) * dx + (py - ay) * dy) / segLen2;
            t = Math.max(0.0, Math.min(1.0, t));

            double qx = ax + t * dx;
            double qy = ay + t * dy;
            double dist2 = (px - qx) * (px - qx) + (py - qy) * (py - qy);

            if (dist2 < bestDist2) {
                bestDist2 = dist2;
                bestPathLen = prefixLen[i] + Math.sqrt(segLen2) * t;
            }
        }

        double progress = bestPathLen / total;
        return Math.max(0.0, Math.min(1.0, progress));
    }
}


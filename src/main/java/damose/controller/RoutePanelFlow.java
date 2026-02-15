package damose.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jxmapviewer.viewer.GeoPosition;

import damose.data.loader.RoutesLoader;
import damose.model.Route;
import damose.model.Stop;
import damose.model.VehicleType;
import damose.view.MainView;
import damose.view.map.MapOverlayManager;

/**
 * Handles route panel state transitions and related map/view updates.
 */
final class RoutePanelFlow {

    private final MainView view;
    private final ControllerDataContext dataContext;
    private final RouteViewportNavigator routeViewport;
    private final RoutePanelState routePanelState;
    private final Runnable refreshMapOverlay;

    RoutePanelFlow(MainView view,
                   ControllerDataContext dataContext,
                   RouteViewportNavigator routeViewport,
                   RoutePanelState routePanelState,
                   Runnable refreshMapOverlay) {
        this.view = view;
        this.dataContext = dataContext;
        this.routeViewport = routeViewport;
        this.routePanelState = routePanelState;
        this.refreshMapOverlay = refreshMapOverlay;
    }

    Integer chooseInitialDirection(List<Integer> directions) {
        if (directions == null || directions.isEmpty()) return null;
        if (directions.contains(0)) return 0;
        return directions.get(0);
    }

    void closeRoutePanelOverlay(boolean hideFloatingPanel) {
        routePanelState.reset();
        view.hideRouteSidePanel();
        if (hideFloatingPanel) {
            view.hideFloatingPanel();
        }
        MapOverlayManager.clearRoute();
        MapOverlayManager.clearBusRouteFilter();
        MapOverlayManager.clearBusDirectionFilter();
    }

    void resetRoutePanelUiState() {
        routePanelState.reset();
        view.hideRouteSidePanel();
        MapOverlayManager.clearBusDirectionFilter();
    }

    void applyRouteSelectionStateAndView(String routeId,
                                         String routeName,
                                         List<Stop> routeStops,
                                         List<GeoPosition> routeShape,
                                         List<Integer> directions,
                                         Integer selectedDirection,
                                         boolean hideFloatingPanel) {
        routePanelState.apply(routeId, routeName, routeStops, selectedDirection, isCircularRoute(routeStops));

        List<Integer> effectiveDirections = normalizeDirectionsForCircular(
                directions, selectedDirection, routePanelState.isCircular()
        );
        view.setRouteSidePanelDirections(
                buildDirectionLabels(routeId, effectiveDirections),
                resolveUiSelectedDirection(selectedDirection, effectiveDirections)
        );
        view.showRouteSidePanel(routeName, routeStops);

        MapOverlayManager.clearVisibleStops();
        MapOverlayManager.setRouteStyleForVehicleType(resolveRouteVehicleType(routeId));
        MapOverlayManager.setRoute(routeStops, routeShape);
        MapOverlayManager.setBusRouteFilter(routeId);
        MapOverlayManager.setBusDirectionFilter(selectedDirection);
        refreshMapOverlay.run();
        routeViewport.fitMapToRoute(view.getMapViewer(), routeStops, routePanelState.hasRoute());

        if (hideFloatingPanel) {
            view.hideFloatingPanel();
        }
    }

    private VehicleType resolveRouteVehicleType(String routeId) {
        Route route = RoutesLoader.getRouteById(routeId);
        return route != null ? route.getVehicleType() : VehicleType.BUS;
    }

    private static boolean isCircularRoute(List<Stop> routeStops) {
        if (routeStops == null || routeStops.size() < 2) return false;

        Stop first = routeStops.get(0);
        Stop last = routeStops.get(routeStops.size() - 1);
        if (first == null || last == null) return false;

        String firstId = first.getStopId();
        String lastId = last.getStopId();
        if (firstId != null && lastId != null
                && firstId.trim().equalsIgnoreCase(lastId.trim())) {
            return true;
        }

        double latDiff = Math.abs(first.getStopLat() - last.getStopLat());
        double lonDiff = Math.abs(first.getStopLon() - last.getStopLon());
        return latDiff < 0.0001 && lonDiff < 0.0001;
    }

    private static List<Integer> normalizeDirectionsForCircular(List<Integer> directions,
                                                                Integer selectedDirection,
                                                                boolean circularRoute) {
        if (!circularRoute || directions == null || directions.size() <= 1) {
            return directions;
        }

        if (selectedDirection != null) {
            return List.of(selectedDirection);
        }
        return List.of(directions.get(0));
    }

    private Map<Integer, String> buildDirectionLabels(String routeId, List<Integer> directions) {
        Map<Integer, String> labels = new LinkedHashMap<>();
        if (directions == null) return labels;

        for (Integer direction : directions) {
            if (direction == null) continue;
            String headsign = safe(dataContext.getRouteService()
                    .getRepresentativeHeadsignForRouteAndDirection(routeId, direction));
            String label = !headsign.isEmpty() ? headsign : "Direzione";
            labels.put(direction, label);
        }
        return labels;
    }

    private static int resolveUiSelectedDirection(Integer selectedDirection, List<Integer> effectiveDirections) {
        if (selectedDirection != null) {
            return selectedDirection;
        }
        if (effectiveDirections != null && !effectiveDirections.isEmpty() && effectiveDirections.get(0) != null) {
            return effectiveDirections.get(0);
        }
        return 0;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

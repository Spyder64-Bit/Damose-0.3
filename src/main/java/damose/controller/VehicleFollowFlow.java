package damose.controller;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import damose.data.mapper.TripMatcher;
import damose.model.Stop;
import damose.model.VehiclePosition;
import damose.view.MainView;
import damose.view.component.RouteSidePanel;
import damose.view.map.MapOverlayManager;

/**
 * Handles tracked-vehicle selection, matching, and floating panel updates.
 */
final class VehicleFollowFlow {
    private static final int FOLLOWED_VEHICLE_MAX_MISSES = 3;

    private final MainView view;
    private final RouteViewportNavigator routeViewport;
    private final VehicleTrackingResolver vehicleTrackingResolver;
    private final RoutePanelState routePanelState;
    private final FollowedVehicleState followedVehicleState;
    private final VehiclePanelInfoBuilder vehiclePanelInfoBuilder;
    private final Supplier<TripMatcher> tripMatcherSupplier;

    private volatile List<VehiclePosition> latestVehiclePositions = Collections.emptyList();

    VehicleFollowFlow(MainView view,
                      RouteViewportNavigator routeViewport,
                      VehicleTrackingResolver vehicleTrackingResolver,
                      RoutePanelState routePanelState,
                      FollowedVehicleState followedVehicleState,
                      VehiclePanelInfoBuilder vehiclePanelInfoBuilder,
                      Supplier<TripMatcher> tripMatcherSupplier) {
        this.view = view;
        this.routeViewport = routeViewport;
        this.vehicleTrackingResolver = vehicleTrackingResolver;
        this.routePanelState = routePanelState;
        this.followedVehicleState = followedVehicleState;
        this.vehiclePanelInfoBuilder = vehiclePanelInfoBuilder;
        this.tripMatcherSupplier = tripMatcherSupplier;
    }

    void onRouteVehicleSelected(RouteSidePanel.VehicleMarker marker) {
        if (marker == null) return;
        String markerId = trimToNull(marker.getVehicleId());
        if (markerId == null) return;

        followedVehicleState.follow(markerId);
        MapOverlayManager.setSelectedVehicleMarkerId(markerId);
        updateFollowedVehicleTracking(latestVehiclePositions, true);
    }

    void onVehiclePositionsUpdated(List<VehiclePosition> positions,
                                   RouteVehicleMarkerBuilder routeVehicleMarkerBuilder) {
        List<VehiclePosition> snapshot = positions == null ? List.of() : List.copyOf(positions);
        latestVehiclePositions = snapshot;

        String routeId = routePanelState.routeId();
        List<Stop> routeStops = routePanelState.routeStops();
        Integer directionFilter = routePanelState.direction();
        if (routeVehicleMarkerBuilder != null && routeId != null && routeStops != null && routeStops.size() >= 2) {
            view.updateRouteSidePanelVehicles(
                    routeVehicleMarkerBuilder.buildForRoute(snapshot, routeId, routeStops, directionFilter)
            );
        }

        updateFollowedVehicleTracking(snapshot, false);
    }

    void clearFollowedVehicle() {
        clearFollowedVehicle(false);
    }

    void clearFollowedVehicle(boolean hidePanel) {
        followedVehicleState.clear();
        MapOverlayManager.clearSelectedVehicleMarkerId();
        if (hidePanel) {
            SwingUtilities.invokeLater(view::hideFloatingPanel);
        }
    }

    private void updateFollowedVehicleTracking(List<VehiclePosition> positions, boolean animate) {
        String markerId = trimToNull(followedVehicleState.markerId());
        if (markerId == null) {
            return;
        }

        VehiclePosition tracked = vehicleTrackingResolver.findByMarkerId(
                markerId,
                positions,
                routePanelState.routeId(),
                routePanelState.direction(),
                tripMatcherSupplier.get()
        );
        if (tracked == null) {
            if (followedVehicleState.incrementMissAndReached(FOLLOWED_VEHICLE_MAX_MISSES)) {
                clearFollowedVehicle(true);
            }
            return;
        }

        followedVehicleState.resetMisses();
        VehiclePanelInfoBuilder.VehiclePanelInfo panelData =
                vehiclePanelInfoBuilder != null
                        ? vehiclePanelInfoBuilder.build(tracked)
                        : new VehiclePanelInfoBuilder.VehiclePanelInfo("Veicolo", List.of("Prossimo arrivo: non disponibile"));

        Runnable uiUpdate = () -> {
            if (animate) {
                routeViewport.focusOnVehicle(view.getMapViewer(), tracked.getPosition(), true);
            }
            if (view.isFloatingPanelVisible()) {
                view.refreshVehicleFloatingPanel(panelData.title(), panelData.rows(), tracked.getPosition());
            } else {
                view.showVehicleFloatingPanel(panelData.title(), panelData.rows(), tracked.getPosition());
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            uiUpdate.run();
        } else {
            SwingUtilities.invokeLater(uiUpdate);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

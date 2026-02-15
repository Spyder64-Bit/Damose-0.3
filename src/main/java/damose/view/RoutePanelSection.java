package damose.view;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import javax.swing.JPanel;

import damose.model.Stop;
import damose.view.component.RouteSidePanel;

/**
 * Public-facing route side panel section for MainView delegation.
 */
final class RoutePanelSection {

    private final RouteSidePanelCoordinator coordinator;

    RoutePanelSection(JPanel hostPanel,
                      int hostWidth,
                      int hostHeight,
                      int panelWidth,
                      int panelTop,
                      int panelMargin) {
        coordinator = new RouteSidePanelCoordinator(
                hostPanel,
                hostWidth,
                hostHeight,
                panelWidth,
                panelTop,
                panelMargin
        );
    }

    void setOnClose(Runnable callback) {
        coordinator.setOnClose(callback);
    }

    void setOnDirectionSelected(IntConsumer callback) {
        coordinator.setOnDirectionSelected(callback);
    }

    void setOnVehicleMarkerSelected(Consumer<RouteSidePanel.VehicleMarker> callback) {
        coordinator.setOnVehicleMarkerSelected(callback);
    }

    void setOnStopSelected(Consumer<Stop> callback) {
        coordinator.setOnStopSelected(callback);
    }

    void updateBounds(int hostWidth, int hostHeight) {
        coordinator.updateBounds(hostWidth, hostHeight);
    }

    void showRoute(String routeName, List<Stop> routeStops) {
        coordinator.showRoute(routeName, routeStops);
    }

    void setDirections(Map<Integer, String> directions, int selectedDirection) {
        coordinator.setDirections(directions, selectedDirection);
    }

    void setVehicleMarkers(List<RouteSidePanel.VehicleMarker> markers) {
        coordinator.setVehicleMarkers(markers);
    }

    void hide() {
        coordinator.hide();
    }
}

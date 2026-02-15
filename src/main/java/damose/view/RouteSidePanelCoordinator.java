package damose.view;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import javax.swing.JPanel;

import damose.model.Stop;
import damose.view.component.RouteSidePanel;

/**
 * Coordinates RouteSidePanel lifecycle, callbacks and layout updates.
 */
final class RouteSidePanelCoordinator {

    private final RouteSidePanel panel;
    private final int panelWidth;
    private final int panelTop;
    private final int panelMargin;

    RouteSidePanelCoordinator(JPanel hostPanel,
                              int hostWidth,
                              int hostHeight,
                              int panelWidth,
                              int panelTop,
                              int panelMargin) {
        this.panelWidth = panelWidth;
        this.panelTop = panelTop;
        this.panelMargin = panelMargin;

        panel = new RouteSidePanel();
        panel.setVisible(false);
        panel.setBounds(
                hostWidth - panelWidth - panelMargin,
                panelTop,
                panelWidth,
                hostHeight - panelTop - panelMargin
        );
        hostPanel.add(panel);
    }

    void setOnClose(Runnable callback) {
        panel.setOnClose(() -> {
            if (callback != null) {
                callback.run();
            }
        });
    }

    void setOnDirectionSelected(IntConsumer callback) {
        panel.setOnDirectionSelected(directionId -> {
            if (callback != null) {
                callback.accept(directionId);
            }
        });
    }

    void setOnVehicleMarkerSelected(Consumer<RouteSidePanel.VehicleMarker> callback) {
        panel.setOnVehicleMarkerSelected(marker -> {
            if (callback != null) {
                callback.accept(marker);
            }
        });
    }

    void setOnStopSelected(Consumer<Stop> callback) {
        panel.setOnStopSelected(stop -> {
            if (callback != null) {
                callback.accept(stop);
            }
        });
    }

    void updateBounds(int hostWidth, int hostHeight) {
        panel.setBounds(
                hostWidth - panelWidth - panelMargin,
                panelTop,
                panelWidth,
                hostHeight - panelTop - panelMargin
        );
    }

    void showRoute(String routeName, List<Stop> routeStops) {
        panel.setRoute(routeName, routeStops);
        panel.setVisible(true);
        panel.repaint();
    }

    void setDirections(Map<Integer, String> directions, int selectedDirection) {
        panel.setDirectionOptions(directions, selectedDirection);
    }

    void setVehicleMarkers(List<RouteSidePanel.VehicleMarker> markers) {
        if (!panel.isVisible()) {
            return;
        }
        panel.setVehicleMarkers(markers);
    }

    void hide() {
        panel.setVisible(false);
    }
}

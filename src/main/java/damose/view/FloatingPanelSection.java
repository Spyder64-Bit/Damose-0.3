package damose.view;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Point2D;
import java.util.List;

import javax.swing.JPanel;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import damose.view.component.FloatingArrivalPanel;

/**
 * Coordinates floating panel lifecycle, callbacks and map-anchor sync.
 */
final class FloatingPanelSection {

    private final FloatingArrivalPanel floatingPanel;
    private final FloatingPanelCoordinator coordinator;
    private Runnable onClose;

    FloatingPanelSection(JXMapViewer mapViewer, JPanel hostPanel) {
        floatingPanel = new FloatingArrivalPanel();
        floatingPanel.setVisible(false);
        coordinator = new FloatingPanelCoordinator(mapViewer, floatingPanel);
        floatingPanel.setOnClose(() -> {
            coordinator.clearAnchor();
            if (onClose != null) {
                onClose.run();
            }
        });
        floatingPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                coordinator.updatePosition();
            }
        });
        hostPanel.add(floatingPanel);
    }

    void setOnClose(Runnable callback) {
        this.onClose = callback;
    }

    void setPreferredMaxRows(int maxRows) {
        floatingPanel.setPreferredRowsMax(maxRows);
    }

    void setOnFavoriteToggle(Runnable callback) {
        floatingPanel.setOnFavoriteToggle(callback);
    }

    void setOnViewAllTrips(Runnable callback) {
        floatingPanel.setOnViewAllTrips(callback);
    }

    void updateFavoriteStatus(boolean isFavorite) {
        floatingPanel.setFavoriteStatus(isFavorite);
    }

    void showStopPanel(String stopName, String stopId, List<String> arrivals,
                       boolean isFavorite, Point2D position, GeoPosition anchorGeo) {
        coordinator.showStopPanel(stopName, stopId, arrivals, isFavorite, position, anchorGeo);
    }

    void refreshStopPanel(String stopName, String stopId, List<String> arrivals, boolean isFavorite) {
        coordinator.refreshStopPanel(stopName, stopId, arrivals, isFavorite);
    }

    void showVehiclePanel(String panelTitle, List<String> rows, GeoPosition anchorGeo) {
        coordinator.showVehiclePanel(panelTitle, rows, anchorGeo);
    }

    void refreshVehiclePanel(String panelTitle, List<String> rows, GeoPosition anchorGeo) {
        coordinator.refreshVehiclePanel(panelTitle, rows, anchorGeo);
    }

    void showAllTrips(List<String> allTrips) {
        coordinator.showAllTrips(allTrips);
    }

    void hide() {
        coordinator.hide();
    }

    void updatePosition() {
        coordinator.updatePosition();
    }

    boolean isVisible() {
        return coordinator.isVisible();
    }

    String getCurrentStopId() {
        return coordinator.getCurrentStopId();
    }
}

package damose.view;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.List;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import damose.view.component.FloatingArrivalPanel;

/**
 * Coordinates floating panel state and positioning on map changes.
 */
final class FloatingPanelCoordinator {

    private enum PanelKind {
        NONE,
        STOP,
        VEHICLE
    }

    private final JXMapViewer mapViewer;
    private final FloatingArrivalPanel floatingPanel;
    private GeoPosition floatingAnchorGeo;
    private PanelKind panelKind = PanelKind.NONE;

    FloatingPanelCoordinator(JXMapViewer mapViewer, FloatingArrivalPanel floatingPanel) {
        this.mapViewer = mapViewer;
        this.floatingPanel = floatingPanel;
    }

    void clearAnchor() {
        floatingAnchorGeo = null;
    }

    void showStopPanel(String stopName, String stopId, List<String> arrivals,
                       boolean isFavorite, Point2D pos, GeoPosition anchorGeo) {
        floatingPanel.setActionButtonsVisible(true, true);
        floatingPanel.update(stopName, stopId, arrivals, isFavorite);
        floatingAnchorGeo = anchorGeo;
        panelKind = PanelKind.STOP;

        Point2D p = pos;
        if (p == null && anchorGeo != null) {
            p = mapViewer.convertGeoPositionToPoint(anchorGeo);
        }

        if (p != null) {
            placePanelAtPoint(p);
        }

        floatingPanel.revalidate();
        floatingPanel.repaint();
        floatingPanel.fadeIn(300, 15);
    }

    void showVehiclePanel(String panelTitle, List<String> rows, GeoPosition anchorGeo) {
        List<String> safeRows = rows == null ? List.of() : rows;
        floatingPanel.updateInfo(panelTitle, safeRows);
        floatingAnchorGeo = anchorGeo;
        panelKind = PanelKind.VEHICLE;

        Point2D p = anchorGeo != null ? mapViewer.convertGeoPositionToPoint(anchorGeo) : null;
        if (p != null) {
            placePanelAtPoint(p);
        }

        floatingPanel.revalidate();
        floatingPanel.repaint();
        if (!floatingPanel.isVisible()) {
            floatingPanel.fadeIn(220, 12);
        } else {
            floatingPanel.setVisible(true);
        }
    }

    void refreshVehiclePanel(String panelTitle, List<String> rows, GeoPosition anchorGeo) {
        List<String> safeRows = rows == null ? List.of() : rows;
        floatingPanel.updateInfo(panelTitle, safeRows);
        floatingAnchorGeo = anchorGeo;
        panelKind = PanelKind.VEHICLE;
        updatePosition();
        if (!floatingPanel.isVisible()) {
            floatingPanel.setVisible(true);
        }
        floatingPanel.repaint();
    }

    void refreshStopPanel(String stopName, String stopId, List<String> arrivals, boolean isFavorite) {
        floatingPanel.setActionButtonsVisible(true, true);
        floatingPanel.update(stopName, stopId, arrivals, isFavorite);
        panelKind = PanelKind.STOP;
        updatePosition();
        floatingPanel.repaint();
    }

    void showAllTrips(List<String> allTrips) {
        floatingPanel.showAllTripsView(allTrips);
        updatePosition();
    }

    String getCurrentStopId() {
        return floatingPanel.getCurrentStopId();
    }

    boolean isVisible() {
        return floatingPanel.isVisible();
    }

    void hide() {
        floatingPanel.setVisible(false);
        floatingAnchorGeo = null;
        panelKind = PanelKind.NONE;
    }

    boolean isVehiclePanelVisible() {
        return panelKind == PanelKind.VEHICLE && floatingPanel.isVisible();
    }

    void updatePosition() {
        if (floatingAnchorGeo == null) return;

        Point2D projectedPos = mapViewer.convertGeoPositionToPoint(floatingAnchorGeo);
        Dimension pref = floatingPanel.getPreferredPanelSize();
        int panelWidth = pref.width;
        int panelHeight = pref.height;

        if (projectedPos == null) {
            if (floatingPanel.isVisible()) {
                floatingPanel.setVisible(false);
            }
            return;
        }

        int mapWidth = mapViewer.getWidth();
        int mapHeight = mapViewer.getHeight();
        if (mapWidth <= 0 || mapHeight <= 0) {
            if (floatingPanel.isVisible()) {
                floatingPanel.setVisible(false);
            }
            return;
        }

        double stopX = projectedPos.getX();
        double stopY = projectedPos.getY();
        int anchorOffscreenMargin = 6;
        boolean anchorOutsideViewport = stopX < -anchorOffscreenMargin
                || stopX > mapWidth + anchorOffscreenMargin
                || stopY < -anchorOffscreenMargin
                || stopY > mapHeight + anchorOffscreenMargin;
        if (anchorOutsideViewport) {
            // Keep anchor state so the panel can reappear automatically when the map returns to it.
            if (floatingPanel.isVisible()) {
                floatingPanel.setVisible(false);
            }
            return;
        }

        int targetX = (int) stopX - panelWidth / 2;
        int targetY = (int) stopY - panelHeight - 8;

        int minValidX = 5;
        int maxValidX = mapWidth - panelWidth - 5;
        int minValidY = 5;
        int maxValidY = mapHeight - panelHeight - 5;

        if (maxValidX < minValidX || maxValidY < minValidY) {
            if (floatingPanel.isVisible()) {
                floatingPanel.setVisible(false);
            }
            return;
        }

        // Hide earlier: as soon as panel would touch viewport bounds/corners.
        boolean panelTouchesBounds = targetX <= minValidX
                || targetX >= maxValidX
                || targetY <= minValidY
                || targetY >= maxValidY;
        if (panelTouchesBounds) {
            if (floatingPanel.isVisible()) {
                floatingPanel.setVisible(false);
            }
            return;
        }

        targetX = Math.max(minValidX, Math.min(targetX, maxValidX));
        targetY = Math.max(minValidY, Math.min(targetY, maxValidY));

        if (!floatingPanel.isVisible()) {
            floatingPanel.setVisible(true);
        }

        floatingPanel.setBounds(targetX, targetY, panelWidth, panelHeight);
        floatingPanel.revalidate();
        floatingPanel.repaint();
    }

    private void placePanelAtPoint(Point2D p) {
        Dimension pref = floatingPanel.getPreferredPanelSize();
        int panelWidth = pref.width;
        int panelHeight = pref.height;
        int x = (int) p.getX() - panelWidth / 2;
        int y = (int) p.getY() - panelHeight - 8;

        int maxX = Math.max(10, mapViewer.getWidth() - panelWidth - 10);
        int maxY = Math.max(10, mapViewer.getHeight() - panelHeight - 10);
        x = Math.max(10, Math.min(x, maxX));
        y = Math.max(10, Math.min(y, maxY));

        floatingPanel.setBounds(x, y, panelWidth, panelHeight);
    }

}

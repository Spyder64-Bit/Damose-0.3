package damose.controller;

import java.awt.geom.Point2D;
import java.util.List;

import javax.swing.SwingUtilities;

import org.jxmapviewer.viewer.GeoPosition;

import damose.model.ConnectionMode;
import damose.model.Stop;
import damose.service.FavoritesService;
import damose.view.MainView;

/**
 * Handles stop-focused floating panel presentation and refresh.
 */
final class StopPanelFlow {

    private final MainView view;
    private final ControllerDataContext dataContext;

    StopPanelFlow(MainView view, ControllerDataContext dataContext) {
        this.view = view;
        this.dataContext = dataContext;
    }

    void refreshFloatingPanelIfVisible(ConnectionMode mode, long currentFeedTs) {
        String stopId = view.getFloatingPanelStopId();
        if (stopId == null || !view.isFloatingPanelVisible()) {
            return;
        }

        Stop stop = findStopById(stopId);
        if (stop == null) {
            return;
        }

        List<String> arrivi = dataContext.getArrivalService()
                .computeArrivalsForStop(stopId, mode, currentFeedTs);
        boolean isFavorite = FavoritesService.isFavorite(stopId);
        view.refreshFloatingPanel(stop.getStopName(), stopId, arrivi, isFavorite);
    }

    void showFloatingArrivals(Stop stop, ConnectionMode mode, long currentFeedTs) {
        List<String> arrivi = dataContext.getArrivalService()
                .computeArrivalsForStop(stop.getStopId(), mode, currentFeedTs);
        boolean isFavorite = FavoritesService.isFavorite(stop.getStopId());
        showPanel(stop, arrivi, isFavorite);
    }

    private Stop findStopById(String stopId) {
        if (stopId == null || dataContext.getStops() == null) return null;
        for (Stop s : dataContext.getStops()) {
            if (stopId.equals(s.getStopId())) return s;
        }
        return null;
    }

    private void showPanel(Stop stop, List<String> arrivi, boolean isFavorite) {
        GeoPosition anchorGeo = new GeoPosition(stop.getStopLat(), stop.getStopLon());
        Point2D p2d = view.getMapViewer().convertGeoPositionToPoint(anchorGeo);
        SwingUtilities.invokeLater(() -> view.showFloatingPanel(
                stop.getStopName(), stop.getStopId(), arrivi, isFavorite, p2d, anchorGeo));
    }
}

package damose.view;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import damose.model.Stop;
import damose.view.map.GeoUtils;

/**
 * Installs map click-to-nearest-stop selection behavior.
 */
final class MapStopSelectionSupport {

    private MapStopSelectionSupport() {
    }

    static void install(JXMapViewer mapViewer,
                        Supplier<List<Stop>> stopsSupplier,
                        Consumer<Stop> stopConsumer) {
        mapViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                List<Stop> allStops = stopsSupplier.get();
                if (allStops == null || allStops.isEmpty()) return;

                int x = e.getX();
                int y = e.getY();

                GeoPosition clickedPos = mapViewer.convertPointToGeoPosition(e.getPoint());
                Stop nearest = findNearestStop(allStops, clickedPos);

                if (nearest != null && GeoUtils.isClickCloseToStop(mapViewer, nearest, x, y)) {
                    stopConsumer.accept(nearest);
                }
            }
        });
    }

    private static Stop findNearestStop(List<Stop> stops, GeoPosition pos) {
        double minDist = Double.MAX_VALUE;
        Stop nearest = null;

        for (Stop s : stops) {
            double d = GeoUtils.haversine(
                    pos.getLatitude(), pos.getLongitude(),
                    s.getStopLat(), s.getStopLon()
            );
            if (d < minDist) {
                minDist = d;
                nearest = s;
            }
        }
        return nearest;
    }
}

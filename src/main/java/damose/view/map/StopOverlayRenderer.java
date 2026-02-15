package damose.view.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import damose.model.Stop;

/**
 * Draws stop overlays (visible stops and route stops) on top of the map.
 */
final class StopOverlayRenderer {

    private Image stopIcon;
    private Image stopIconSmall;

    void loadIcons() {
        try {
            ImageIcon stop = new ImageIcon(StopOverlayRenderer.class.getResource("/sprites/stop.png"));
            stopIcon = stop.getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH);
            stopIconSmall = stop.getImage().getScaledInstance(22, 22, Image.SCALE_SMOOTH);
        } catch (Exception e) {
            System.out.println("Failed to load stop icon: " + e.getMessage());
        }
    }

    void drawStops(Graphics2D g, JXMapViewer map, List<Stop> visibleStops, List<Stop> routeStops) {
        if ((visibleStops == null || visibleStops.isEmpty()) && (routeStops == null || routeStops.isEmpty())) {
            return;
        }

        Rectangle2D viewport = map.getViewportBounds();
        int zoom = map.getZoom();

        int size;
        if (zoom >= 8) size = 10;
        else if (zoom >= 7) size = 14;
        else if (zoom >= 5) size = 22;
        else size = 36;

        Image icon = size <= 22 ? stopIconSmall : stopIcon;

        if (visibleStops != null) {
            for (Stop stop : visibleStops) {
                if (stop == null) continue;

                GeoPosition pos = new GeoPosition(stop.getStopLat(), stop.getStopLon());
                Point2D worldPt = map.getTileFactory().geoToPixel(pos, zoom);
                int screenX = (int) (worldPt.getX() - viewport.getX());
                int screenY = (int) (worldPt.getY() - viewport.getY());

                if (screenX < -size || screenX > map.getWidth() + size
                        || screenY < -size || screenY > map.getHeight() + size) {
                    continue;
                }

                drawStop(g, screenX, screenY, size, icon);
            }
        }

        if (routeStops == null || routeStops.isEmpty()) {
            return;
        }

        int minRouteDistancePx = routeStopMinDistancePx(zoom);
        List<Point2D> renderedRouteStops = new ArrayList<>();
        int lastIndex = routeStops.size() - 1;
        for (int i = 0; i < routeStops.size(); i++) {
            Stop stop = routeStops.get(i);
            if (stop == null) continue;

            boolean forceRender = (i == 0 || i == lastIndex);

            GeoPosition pos = new GeoPosition(stop.getStopLat(), stop.getStopLon());
            Point2D worldPt = map.getTileFactory().geoToPixel(pos, zoom);
            int screenX = (int) (worldPt.getX() - viewport.getX());
            int screenY = (int) (worldPt.getY() - viewport.getY());

            if (screenX < -size || screenX > map.getWidth() + size
                    || screenY < -size || screenY > map.getHeight() + size) {
                continue;
            }

            if (!forceRender && minRouteDistancePx > 0
                    && isTooClose(screenX, screenY, renderedRouteStops, minRouteDistancePx)) {
                continue;
            }

            drawStop(g, screenX, screenY, size, icon);
            if (minRouteDistancePx > 0) {
                renderedRouteStops.add(new Point2D.Double(screenX, screenY));
            }
        }
    }

    private static void drawStop(Graphics2D g, int screenX, int screenY, int size, Image icon) {
        if (icon != null) {
            g.drawImage(icon, screenX - size / 2, screenY - size / 2, size, size, null);
            return;
        }

        g.setColor(new Color(220, 50, 50));
        g.fillOval(screenX - size / 2, screenY - size / 2, size, size);
        g.setColor(Color.WHITE);
        g.drawOval(screenX - size / 2, screenY - size / 2, size, size);
    }

    private static int routeStopMinDistancePx(int zoom) {
        if (zoom >= 8) return 48;
        if (zoom >= 7) return 34;
        if (zoom >= 6) return 24;
        return 0;
    }

    private static boolean isTooClose(int x, int y, List<Point2D> points, int minDistancePx) {
        int minDistance2 = minDistancePx * minDistancePx;
        for (Point2D p : points) {
            double dx = x - p.getX();
            double dy = y - p.getY();
            if ((dx * dx + dy * dy) < minDistance2) {
                return true;
            }
        }
        return false;
    }
}

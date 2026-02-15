package damose.view.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.ImageIcon;

import org.jxmapviewer.JXMapViewer;

import damose.model.BusWaypoint;
import damose.model.VehicleType;

/**
 * Draws vehicle overlays (icons or dots) on top of the map.
 */
final class VehicleOverlayRenderer {

    private static final Color BUS_DOT_COLOR = new Color(220, 50, 50, 230);
    private static final Color TRAM_DOT_COLOR = new Color(70, 150, 255, 230);
    private static final int VEHICLE_DOT_ZOOM_THRESHOLD = 9;

    private Image busIcon;
    private Image busIconSmall;
    private Image tramIcon;
    private Image tramIconSmall;

    void loadIcons() {
        try {
            ImageIcon bus = new ImageIcon(MapOverlayManager.class.getResource("/sprites/bus.png"));
            busIcon = bus.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            busIconSmall = bus.getImage().getScaledInstance(26, 26, Image.SCALE_SMOOTH);
        } catch (Exception e) {
            System.out.println("Failed to load bus icon: " + e.getMessage());
        }

        try {
            ImageIcon tram = new ImageIcon(MapOverlayManager.class.getResource("/sprites/tram.png"));
            tramIcon = tram.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            tramIconSmall = tram.getImage().getScaledInstance(26, 26, Image.SCALE_SMOOTH);
        } catch (Exception e) {
            System.out.println("Failed to load tram icon: " + e.getMessage());
        }
    }

    void drawVehicles(Graphics2D g,
                      JXMapViewer map,
                      List<BusWaypoint> busWaypoints,
                      String busRouteFilter,
                      Integer busDirectionFilter,
                      boolean busesVisible,
                      boolean forceDotMode,
                      String selectedMarkerId) {
        if (busWaypoints == null || busWaypoints.isEmpty()) return;
        if (!busesVisible && busRouteFilter == null) return;

        Rectangle2D viewport = map.getViewportBounds();
        int zoom = map.getZoom();
        boolean dotMode = zoom >= VEHICLE_DOT_ZOOM_THRESHOLD || forceDotMode;
        int size = dotMode ? vehicleDotSize(zoom) : ((zoom > 5) ? 26 : 40);

        for (BusWaypoint wp : busWaypoints) {
            if (wp == null || wp.getPosition() == null) continue;

            if (busRouteFilter != null && !matchesRouteFilter(busRouteFilter, wp.getRouteId())) {
                continue;
            }
            if (busDirectionFilter != null && wp.getDirectionId() != busDirectionFilter) {
                continue;
            }

            Point2D worldPt = map.getTileFactory().geoToPixel(wp.getPosition(), zoom);
            int screenX = (int) (worldPt.getX() - viewport.getX());
            int screenY = (int) (worldPt.getY() - viewport.getY());

            if (screenX < -size || screenX > map.getWidth() + size
                    || screenY < -size || screenY > map.getHeight() + size) {
                continue;
            }

            boolean selected = isSelectedMarker(selectedMarkerId, wp);
            if (dotMode) {
                int dotSize = selected ? size + 5 : size;
                if (selected) {
                    Color halo = wp.getVehicleType() == VehicleType.TRAM
                            ? new Color(70, 150, 255, 190)
                            : new Color(220, 50, 50, 190);
                    g.setColor(halo);
                    g.fillOval(screenX - dotSize / 2 - 4, screenY - dotSize / 2 - 4, dotSize + 8, dotSize + 8);
                    g.setColor(new Color(255, 255, 255, 240));
                    g.setStroke(new BasicStroke(1.8f));
                    g.drawOval(screenX - dotSize / 2 - 4, screenY - dotSize / 2 - 4, dotSize + 8, dotSize + 8);
                }
                g.setColor(wp.getVehicleType() == VehicleType.TRAM ? TRAM_DOT_COLOR : BUS_DOT_COLOR);
                g.fillOval(screenX - dotSize / 2, screenY - dotSize / 2, dotSize, dotSize);
                continue;
            }

            int drawSize = selected ? size + 14 : size;
            Image icon = getVehicleIcon(wp.getVehicleType(), zoom);
            if (selected) {
                Color halo = wp.getVehicleType() == VehicleType.TRAM
                        ? new Color(70, 150, 255, 160)
                        : new Color(220, 50, 50, 160);
                g.setColor(halo);
                g.fillOval(screenX - drawSize / 2 - 5, screenY - drawSize / 2 - 5, drawSize + 10, drawSize + 10);
                g.setColor(new Color(255, 255, 255, 240));
                g.setStroke(new BasicStroke(2.2f));
                g.drawOval(screenX - drawSize / 2 - 5, screenY - drawSize / 2 - 5, drawSize + 10, drawSize + 10);
            }
            if (icon != null) {
                g.drawImage(icon, screenX - drawSize / 2, screenY - drawSize / 2, drawSize, drawSize, null);
            } else {
                g.setColor(wp.getVehicleType() == VehicleType.TRAM
                        ? new Color(255, 140, 0)
                        : new Color(0, 120, 255));
                g.fillOval(screenX - drawSize / 2, screenY - drawSize / 2, drawSize, drawSize);
                g.setColor(Color.WHITE);
                g.drawOval(screenX - drawSize / 2, screenY - drawSize / 2, drawSize, drawSize);
            }
        }
    }

    private Image getVehicleIcon(VehicleType type, int zoom) {
        boolean small = zoom > 5;
        if (type == VehicleType.TRAM) {
            return small ? tramIconSmall : tramIcon;
        }
        return small ? busIconSmall : busIcon;
    }

    private static boolean matchesRouteFilter(String filterRouteId, String candidateRouteId) {
        if (filterRouteId == null || candidateRouteId == null) return false;

        String filter = filterRouteId.trim();
        String candidate = candidateRouteId.trim();
        if (filter.isEmpty() || candidate.isEmpty()) return false;
        return filter.equalsIgnoreCase(candidate);
    }

    private static int vehicleDotSize(int zoom) {
        if (zoom >= 12) return 3;
        if (zoom >= 10) return 4;
        return 5;
    }

    private static boolean isSelectedMarker(String selectedMarkerId, BusWaypoint waypoint) {
        if (selectedMarkerId == null || waypoint == null) {
            return false;
        }

        String selected = trimToNull(selectedMarkerId);
        if (selected == null) {
            return false;
        }

        String vehicleId = trimToNull(waypoint.getVehicleId());
        if (vehicleId != null && selected.equalsIgnoreCase(vehicleId)) {
            return true;
        }

        String tripId = trimToNull(waypoint.getTripId());
        return tripId != null && selected.equalsIgnoreCase(tripId);
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

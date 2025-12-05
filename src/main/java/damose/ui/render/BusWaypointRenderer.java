package damose.ui.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.WaypointRenderer;

import damose.model.BusWaypoint;
import damose.model.VehicleType;

/**
 * Renderer for vehicle waypoints on the map.
 * Shows different icons/colors for bus, tram, and metro.
 */
public class BusWaypointRenderer implements WaypointRenderer<BusWaypoint> {

    private final EnumMap<VehicleType, Image> originalImages = new EnumMap<>(VehicleType.class);
    private final EnumMap<VehicleType, Map<Integer, Image>> sizeCaches = new EnumMap<>(VehicleType.class);
    
    // Fallback colors for each vehicle type when image not available
    private static final EnumMap<VehicleType, Color> FALLBACK_COLORS = new EnumMap<>(VehicleType.class);
    
    static {
        FALLBACK_COLORS.put(VehicleType.BUS, new Color(76, 175, 80));     // Green
        FALLBACK_COLORS.put(VehicleType.TRAM, new Color(255, 152, 0));    // Orange
        FALLBACK_COLORS.put(VehicleType.METRO, new Color(63, 81, 181));   // Indigo
        FALLBACK_COLORS.put(VehicleType.RAIL, new Color(139, 69, 19));    // Brown
        FALLBACK_COLORS.put(VehicleType.FERRY, new Color(0, 188, 212));   // Cyan
        FALLBACK_COLORS.put(VehicleType.UNKNOWN, new Color(158, 158, 158)); // Gray
    }

    public BusWaypointRenderer() {
        // Load images for each vehicle type
        loadImage(VehicleType.BUS, "/sprites/bus.png");
        loadImage(VehicleType.TRAM, "/sprites/tram.png");
        loadImage(VehicleType.METRO, "/sprites/metro.png");
        
        // Pre-cache common sizes for loaded images
        for (VehicleType type : originalImages.keySet()) {
            Map<Integer, Image> cache = new HashMap<>();
            Image orig = originalImages.get(type);
            for (int s = 18; s <= 52; s += 4) {
                cache.put(s, orig.getScaledInstance(s, s, Image.SCALE_FAST));
            }
            sizeCaches.put(type, cache);
        }
    }
    
    private void loadImage(VehicleType type, String path) {
        URL imageUrl = getClass().getResource(path);
        if (imageUrl != null) {
            ImageIcon icon = new ImageIcon(imageUrl);
            if (icon.getImage() != null) {
                originalImages.put(type, icon.getImage());
            }
        } else {
            System.out.println("Vehicle icon not found: " + path + " (will use colored circle)");
        }
    }

    private Image getScaled(VehicleType type, int size) {
        Image original = originalImages.get(type);
        if (original == null) {
            // Fall back to bus image if available
            original = originalImages.get(VehicleType.BUS);
            if (original == null) return null;
            type = VehicleType.BUS;
        }
        
        int cachedSize = ((size + 2) / 4) * 4;
        cachedSize = Math.max(18, Math.min(cachedSize, 52));
        
        Map<Integer, Image> cache = sizeCaches.computeIfAbsent(type, k -> new HashMap<>());
        final Image origImage = original;
        
        return cache.computeIfAbsent(cachedSize,
            s -> origImage.getScaledInstance(s, s, Image.SCALE_FAST)
        );
    }

    @Override
    public void paintWaypoint(Graphics2D g, JXMapViewer map, BusWaypoint wp) {
        if (wp == null || wp.getPosition() == null) return;
        
        // Convert geo position to world pixel coordinates
        Point2D worldPt = map.getTileFactory().geoToPixel(wp.getPosition(), map.getZoom());
        
        // Convert world coordinates to screen coordinates
        Rectangle2D viewport = map.getViewportBounds();
        int screenX = (int) (worldPt.getX() - viewport.getX());
        int screenY = (int) (worldPt.getY() - viewport.getY());
        
        // Skip if outside visible area
        if (screenX < -55 || screenX > map.getWidth() + 55 ||
            screenY < -55 || screenY > map.getHeight() + 55) {
            return;
        }

        int zoom = map.getZoom();
        int size = 55 - (zoom * 2);
        size = Math.max(18, Math.min(size, 52));

        VehicleType type = wp.getVehicleType();
        Image img = getScaled(type, size);
        
        if (img != null) {
            g.drawImage(img, screenX - size / 2, screenY - size / 2, null);
        } else {
            // Fallback: draw a colored shape based on vehicle type
            drawFallbackShape(g, screenX, screenY, size, type);
        }
        
        // Draw route label for larger zoom levels
        if (zoom <= 4 && wp.getRouteId() != null) {
            drawRouteLabel(g, screenX, screenY, size, wp.getRouteId(), type);
        }
    }
    
    private void drawFallbackShape(Graphics2D g, int x, int y, int size, VehicleType type) {
        Color color = FALLBACK_COLORS.getOrDefault(type, FALLBACK_COLORS.get(VehicleType.UNKNOWN));
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        switch (type) {
            case TRAM:
                // Draw rounded rectangle for tram
                g.setColor(color);
                g.fillRoundRect(x - size/2, y - size/3, size, (int)(size * 0.66), 8, 8);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(x - size/2, y - size/3, size, (int)(size * 0.66), 8, 8);
                break;
                
            case METRO:
                // Draw octagon/diamond for metro
                g.setColor(color);
                int[] xPoints = {x, x + size/2, x + size/2, x, x - size/2, x - size/2};
                int[] yPoints = {y - size/2, y - size/4, y + size/4, y + size/2, y + size/4, y - size/4};
                g.fillPolygon(xPoints, yPoints, 6);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2));
                g.drawPolygon(xPoints, yPoints, 6);
                break;
                
            default:
                // Draw circle for bus and others
                g.setColor(color);
                g.fillOval(x - size/2, y - size/2, size, size);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2));
                g.drawOval(x - size/2, y - size/2, size, size);
        }
    }
    
    private void drawRouteLabel(Graphics2D g, int x, int y, int size, String routeId, VehicleType type) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Draw label below the icon
        Font font = new Font("SansSerif", Font.BOLD, 10);
        g.setFont(font);
        
        String label = routeId;
        if (label.length() > 4) {
            label = label.substring(0, 4);
        }
        
        int labelWidth = g.getFontMetrics().stringWidth(label);
        int labelX = x - labelWidth / 2;
        int labelY = y + size / 2 + 12;
        
        // Background
        Color bgColor = type.getColor();
        g.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 200));
        g.fillRoundRect(labelX - 3, labelY - 10, labelWidth + 6, 13, 4, 4);
        
        // Text
        g.setColor(Color.WHITE);
        g.drawString(label, labelX, labelY);
    }
}

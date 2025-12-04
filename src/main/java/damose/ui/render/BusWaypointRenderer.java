package damose.ui.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.WaypointRenderer;

import damose.model.BusWaypoint;

/**
 * Renderer for bus waypoints on the map.
 * Optimized with proper viewport translation and image caching.
 */
public class BusWaypointRenderer implements WaypointRenderer<BusWaypoint> {

    private final Image originalImage;
    private final Map<Integer, Image> sizeCache = new HashMap<>();
    private final boolean imageLoaded;

    public BusWaypointRenderer() {
        URL imageUrl = getClass().getResource("/sprites/bus.png");
        if (imageUrl != null) {
            ImageIcon icon = new ImageIcon(imageUrl);
            originalImage = icon.getImage();
            imageLoaded = (originalImage != null);
            
            if (imageLoaded) {
                // Pre-cache common sizes
                for (int s = 18; s <= 52; s += 4) {
                    sizeCache.put(s, originalImage.getScaledInstance(s, s, Image.SCALE_FAST));
                }
            }
        } else {
            originalImage = null;
            imageLoaded = false;
            System.out.println("WARNING: bus.png not found!");
        }
    }

    private Image getScaled(int size) {
        if (!imageLoaded) return null;
        
        int cachedSize = ((size + 2) / 4) * 4;
        cachedSize = Math.max(18, Math.min(cachedSize, 52));
        
        return sizeCache.computeIfAbsent(cachedSize,
            s -> originalImage.getScaledInstance(s, s, Image.SCALE_FAST)
        );
    }

    private static int paintCallCount = 0;
    private static long lastLogTime = 0;

    @Override
    public void paintWaypoint(Graphics2D g, JXMapViewer map, BusWaypoint wp) {
        paintCallCount++;
        
        // Log every 5 seconds
        long now = System.currentTimeMillis();
        if (now - lastLogTime > 5000) {
            System.out.println("BusRenderer: paintWaypoint called " + paintCallCount + " times, imageLoaded=" + imageLoaded);
            paintCallCount = 0;
            lastLogTime = now;
        }
        
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

        Image img = getScaled(size);
        
        if (img != null) {
            g.drawImage(img, screenX - size / 2, screenY - size / 2, null);
        } else {
            // Fallback: draw a colored circle if image not available
            g.setColor(new Color(0, 150, 255));
            g.fillOval(screenX - size / 2, screenY - size / 2, size, size);
            g.setColor(Color.WHITE);
            g.drawOval(screenX - size / 2, screenY - size / 2, size, size);
        }
    }
}


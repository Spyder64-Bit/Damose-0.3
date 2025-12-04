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

import damose.model.StopWaypoint;

/**
 * Renderer for stop waypoints on the map.
 * Optimized with proper viewport translation and image caching.
 */
public class StopWaypointRenderer implements WaypointRenderer<StopWaypoint> {

    private final Image originalImage;
    private final Map<Integer, Image> sizeCache = new HashMap<>();
    private final boolean imageLoaded;

    public StopWaypointRenderer() {
        URL imageUrl = getClass().getResource("/sprites/stop.png");
        if (imageUrl != null) {
            ImageIcon icon = new ImageIcon(imageUrl);
            originalImage = icon.getImage();
            imageLoaded = (originalImage != null);
            
            if (imageLoaded) {
                // Pre-cache common sizes to avoid lag during zoom
                for (int s = 16; s <= 48; s += 4) {
                    sizeCache.put(s, originalImage.getScaledInstance(s, s, Image.SCALE_FAST));
                }
            }
        } else {
            originalImage = null;
            imageLoaded = false;
            System.out.println("WARNING: stop.png not found!");
        }
    }

    private Image getScaled(int size) {
        if (!imageLoaded) return null;
        
        // Round to nearest cached size for better cache hit rate
        int cachedSize = ((size + 2) / 4) * 4;
        cachedSize = Math.max(16, Math.min(cachedSize, 48));
        
        return sizeCache.computeIfAbsent(cachedSize,
            s -> originalImage.getScaledInstance(s, s, Image.SCALE_FAST)
        );
    }

    @Override
    public void paintWaypoint(Graphics2D g, JXMapViewer map, StopWaypoint wp) {
        if (wp == null || wp.getPosition() == null) return;
        
        // Convert geo position to world pixel coordinates
        Point2D worldPt = map.getTileFactory().geoToPixel(wp.getPosition(), map.getZoom());
        
        // Convert world coordinates to screen coordinates by subtracting viewport offset
        Rectangle2D viewport = map.getViewportBounds();
        int screenX = (int) (worldPt.getX() - viewport.getX());
        int screenY = (int) (worldPt.getY() - viewport.getY());
        
        // Skip if outside visible area (with margin for icon size)
        if (screenX < -50 || screenX > map.getWidth() + 50 ||
            screenY < -50 || screenY > map.getHeight() + 50) {
            return;
        }

        int zoom = map.getZoom();
        int size = 50 - (zoom * 2);
        size = Math.max(16, Math.min(size, 48));

        Image img = getScaled(size);

        if (img != null) {
            g.drawImage(img, screenX - size / 2, screenY - size / 2, null);
        } else {
            // Fallback: draw a colored circle if image not available
            g.setColor(new Color(220, 50, 50));
            g.fillOval(screenX - size / 2, screenY - size / 2, size, size);
            g.setColor(Color.WHITE);
            g.drawOval(screenX - size / 2, screenY - size / 2, size, size);
        }
    }
}


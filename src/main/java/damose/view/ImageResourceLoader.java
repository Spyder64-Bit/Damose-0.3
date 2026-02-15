package damose.view;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

/**
 * Utility to load image resources and trim transparent borders.
 */
public final class ImageResourceLoader {

    private ImageResourceLoader() {
    }

    public static Image loadTrimmedImage(Class<?> resourceOwner, String path) {
        if (resourceOwner == null || path == null || path.isBlank()) {
            return null;
        }

        java.net.URL url = resourceOwner.getResource(path);
        if (url == null) {
            return null;
        }

        ImageIcon rawIcon = new ImageIcon(url);
        if (rawIcon.getIconWidth() <= 0 || rawIcon.getIconHeight() <= 0) {
            return null;
        }

        BufferedImage source = new BufferedImage(
                rawIcon.getIconWidth(),
                rawIcon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2 = source.createGraphics();
        g2.drawImage(rawIcon.getImage(), 0, 0, null);
        g2.dispose();

        return trimTransparentBorders(source);
    }

    private static BufferedImage trimTransparentBorders(BufferedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        int minX = w;
        int minY = h;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int alpha = (source.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha > 8) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return source;
        }

        return source.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}

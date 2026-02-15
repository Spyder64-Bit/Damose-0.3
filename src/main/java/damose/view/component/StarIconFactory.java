package damose.view.component;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

/**
 * Builds star icon variants used by favorite actions.
 */
final class StarIconFactory {

    private StarIconFactory() {
    }

    static StarIcons load(Class<?> anchor, int size) {
        try {
            ImageIcon starIcon = new ImageIcon(anchor.getResource("/sprites/star.png"));
            Image scaled = starIcon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            ImageIcon filled = new ImageIcon(scaled);
            ImageIcon outline = new ImageIcon(createOutlineImage(size));
            return new StarIcons(filled, outline);
        } catch (Exception e) {
            return new StarIcons(null, null);
        }
    }

    private static Image createOutlineImage(int size) {
        BufferedImage outline = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = outline.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int[] xPoints = new int[10];
        int[] yPoints = new int[10];
        double angleStep = Math.PI / 5;
        int cx = size / 2;
        int cy = size / 2;
        int outerR = size / 2 - 1;
        int innerR = size / 4;

        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * angleStep;
            int r = (i % 2 == 0) ? outerR : innerR;
            xPoints[i] = (int) (cx + r * Math.cos(angle));
            yPoints[i] = (int) (cy + r * Math.sin(angle));
        }

        g2.setColor(new Color(255, 200, 50));
        g2.fillPolygon(xPoints, yPoints, 10);
        g2.setColor(new Color(200, 150, 0));
        g2.drawPolygon(xPoints, yPoints, 10);
        g2.dispose();
        return outline;
    }

    static final class StarIcons {
        final ImageIcon filled;
        final ImageIcon outline;

        StarIcons(ImageIcon filled, ImageIcon outline) {
            this.filled = filled;
            this.outline = outline;
        }
    }
}

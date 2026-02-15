package damose.view.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Icon;

/**
 * Generic close glyph icon.
 */
final class CloseGlyphIcon implements Icon {

    private final int size;
    private final Color color;

    CloseGlyphIcon(int size, Color color) {
        this.size = Math.max(8, size);
        this.color = color == null ? Color.WHITE : color;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(
                2f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
        ));
        int max = size - 2;
        g2.drawLine(x + 1, y + 1, x + max, y + max);
        g2.drawLine(x + max, y + 1, x + 1, y + max);
        g2.dispose();
    }
}

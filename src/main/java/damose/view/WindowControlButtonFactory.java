package damose.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;

/**
 * Factory for frameless window control buttons.
 */
final class WindowControlButtonFactory {

    enum Type {
        CLOSE,
        MAXIMIZE,
        MINIMIZE
    }

    private WindowControlButtonFactory() {
    }

    static JButton create(Type type) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                boolean hovered = getModel().isRollover();

                int w = getWidth();
                int h = getHeight();
                int d = Math.min(w, h) - 6;
                int x = (w - d) / 2;
                int y = (h - d) / 2;

                Color fill = hovered
                        ? (type == Type.CLOSE ? new Color(235, 85, 85, 230) : new Color(125, 125, 140, 220))
                        : new Color(28, 28, 34, 200);
                Color border = hovered
                        ? (type == Type.CLOSE ? new Color(255, 140, 140, 220) : new Color(165, 165, 180, 210))
                        : new Color(80, 80, 94, 170);
                Color glyph = hovered && type != Type.CLOSE
                        ? new Color(250, 250, 255)
                        : new Color(228, 228, 236);

                g2.setColor(fill);
                g2.fillOval(x, y, d, d);
                g2.setColor(border);
                g2.drawOval(x, y, d, d);

                int cx = w / 2;
                int cy = h / 2;
                g2.setColor(glyph);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                switch (type) {
                    case CLOSE -> {
                        g2.drawLine(cx - 5, cy - 5, cx + 5, cy + 5);
                        g2.drawLine(cx + 5, cy - 5, cx - 5, cy + 5);
                    }
                    case MAXIMIZE -> g2.drawRect(cx - 5, cy - 5, 10, 10);
                    case MINIMIZE -> g2.drawLine(cx - 6, cy + 3, cx + 6, cy + 3);
                }
                g2.dispose();
            }
        };

        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(false);
        btn.setRolloverEnabled(true);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.repaint();
            }
        });
        return btn;
    }
}

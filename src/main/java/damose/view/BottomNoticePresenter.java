package damose.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.JLayeredPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import damose.config.AppConstants;

/**
 * Handles creation, rendering and positioning of the bottom notice panel.
 */
final class BottomNoticePresenter {

    private static final int BOTTOM_NOTICE_MARGIN = 16;
    private static final int BOTTOM_NOTICE_MAX_WIDTH = 620;

    private JPanel panel;
    private JLabel label;
    private Timer timer;
    private JLayeredPane layeredPane;

    void attachTo(JLayeredPane targetLayeredPane) {
        layeredPane = targetLayeredPane;
        if (layeredPane == null) return;

        panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                g2.setColor(AppConstants.OVERLAY_CARD_BG);
                g2.fillRoundRect(0, 0, w, h, AppConstants.OVERLAY_CARD_ARC, AppConstants.OVERLAY_CARD_ARC);
                g2.setColor(AppConstants.OVERLAY_CARD_BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, AppConstants.OVERLAY_CARD_ARC, AppConstants.OVERLAY_CARD_ARC);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel warningIconLabel = new JLabel(new WarningIcon(18));
        warningIconLabel.setBorder(new EmptyBorder(2, 0, 0, 10));
        warningIconLabel.setVerticalAlignment(JLabel.TOP);
        panel.add(warningIconLabel, BorderLayout.WEST);

        label = new JLabel();
        label.setForeground(AppConstants.TEXT_PRIMARY);
        label.setFont(AppConstants.FONT_BODY);
        panel.add(label, BorderLayout.CENTER);
        panel.setVisible(false);

        layeredPane.add(panel, JLayeredPane.DRAG_LAYER);
    }

    void show(String message) {
        Runnable showTask = () -> {
            if (panel == null || label == null || layeredPane == null) {
                return;
            }

            String safeMessage = message == null ? "" : message.trim();
            int textWidth = Math.min(520, Math.max(220, layeredPane.getWidth() - 80));
            String html = "<html><body style='width:" + textWidth + "px'>"
                    + escapeHtml(safeMessage).replace("\n", "<br>")
                    + "</body></html>";
            label.setText(html);

            panel.revalidate();
            position();
            panel.setVisible(true);
            panel.repaint();

            if (timer != null) {
                timer.stop();
            }
            timer = new Timer(5200, e -> panel.setVisible(false));
            timer.setRepeats(false);
            timer.start();
        };

        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            showTask.run();
        } else {
            javax.swing.SwingUtilities.invokeLater(showTask);
        }
    }

    void position() {
        if (panel == null || layeredPane == null) return;

        int layerW = layeredPane.getWidth();
        int layerH = layeredPane.getHeight();
        if (layerW <= 0 || layerH <= 0) return;

        int width = Math.min(BOTTOM_NOTICE_MAX_WIDTH, Math.max(260, layerW - 24));
        Dimension pref = panel.getPreferredSize();
        int height = Math.max(48, pref.height);
        int x = Math.max(12, (layerW - width) / 2);
        int y = Math.max(12, layerH - height - BOTTOM_NOTICE_MARGIN);
        panel.setBounds(x, y, width, height);
    }

    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static final class WarningIcon implements Icon {
        private final int size;

        private WarningIcon(int size) {
            this.size = Math.max(12, size);
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
        public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int d = size - 1;
            g2.setColor(AppConstants.WARNING_COLOR);
            g2.fillOval(x, y, d, d);
            g2.setColor(new Color(45, 40, 30, 220));
            g2.drawOval(x, y, d, d);

            String mark = "!";
            Font markFont = new Font("Segoe UI", Font.BOLD, Math.max(12, size - 6));
            g2.setFont(markFont);
            g2.setColor(new Color(32, 32, 36, 235));
            java.awt.FontMetrics fm = g2.getFontMetrics();
            int tx = x + (d - fm.stringWidth(mark)) / 2;
            int ty = y + ((d - fm.getHeight()) / 2) + fm.getAscent() - 1;
            g2.drawString(mark, tx, ty);
            g2.dispose();
        }
    }
}

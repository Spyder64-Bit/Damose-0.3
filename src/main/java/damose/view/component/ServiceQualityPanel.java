package damose.view.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import damose.config.AppConstants;
import damose.service.ServiceQualityTracker;
import damose.service.ServiceQualityTracker.ServiceStatus;

/**
 * UI component for service quality panel.
 */
public class ServiceQualityPanel extends JPanel {

    private static final Color BG_COLOR = AppConstants.OVERLAY_CARD_BG;
    private static final Color BORDER_COLOR = AppConstants.OVERLAY_CARD_BORDER;
    private static final Color TEXT_PRIMARY = new Color(240, 240, 245);
    private static final Color TEXT_SECONDARY = new Color(160, 160, 170);
    private static final int CORNER_ARC = AppConstants.OVERLAY_CARD_ARC;

    private JLabel statusLabel;
    private JLabel vehicleCountLabel;
    private JLabel delayLabel;
    private JLabel lastUpdateLabel;
    private JPanel detailsPanel;
    private MiniChart vehicleChart;

    private boolean expanded = false;
    private Timer updateTimer;

    public ServiceQualityPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        initComponents();
        startAutoRefresh();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                g2.setColor(BG_COLOR);
                g2.fillRoundRect(0, 0, w, h, CORNER_ARC, CORNER_ARC);

                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, w - 1, h - 1, CORNER_ARC, CORNER_ARC);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(4, 10, 4, 10));
        mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(1, 3, 1, 3);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        statusLabel = createLabel("", 16, true);
        statusLabel.setPreferredSize(new Dimension(18, 30));
        statusLabel.setVerticalAlignment(JLabel.CENTER);
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(statusLabel, gbc);

        vehicleCountLabel = createLabel("-- Bus", 13, true);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        mainPanel.add(vehicleCountLabel, gbc);

        delayLabel = createLabel("-- min", 11, false);
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(delayLabel, gbc);

        vehicleChart = new MiniChart();
        vehicleChart.setPreferredSize(new Dimension(50, 24));
        gbc.gridx = 2; gbc.gridy = 0; gbc.gridheight = 2;
        gbc.insets = new Insets(1, 6, 1, 3);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        mainPanel.add(vehicleChart, gbc);

        detailsPanel = createDetailsPanel();
        detailsPanel.setVisible(false);

        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { toggleExpanded(); }
        });

        add(mainPanel, BorderLayout.NORTH);
        add(detailsPanel, BorderLayout.CENTER);

        refresh();
    }

    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                g2.setColor(BG_COLOR);
                g2.fillRoundRect(0, 0, w, h, CORNER_ARC, CORNER_ARC);

                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, w - 1, h - 1, CORNER_ARC, CORNER_ARC);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 12, 8, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 0, 2, 8);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createLabel("Ultimo aggiornamento:", 10, false), gbc);

        lastUpdateLabel = createLabel("--:--:--", 10, true);
        gbc.gridx = 1;
        panel.add(lastUpdateLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(createLabel("Puntualit????????:", 10, false), gbc);

        JLabel onTimeLabel = createLabel("--%", 10, true);
        gbc.gridx = 1;
        panel.add(onTimeLabel, gbc);

        return panel;
    }

    private JLabel createLabel(String text, int fontSize, boolean bold) {
        JLabel label = new JLabel(text);
        label.setForeground(bold ? TEXT_PRIMARY : TEXT_SECONDARY);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, fontSize));
        return label;
    }

    private void toggleExpanded() {
        expanded = !expanded;
        detailsPanel.setVisible(expanded);
        revalidate();
        repaint();
    }

    /**
     * Handles refresh.
     */
    public void refresh() {
        ServiceQualityTracker tracker = ServiceQualityTracker.getInstance();

        ServiceStatus status = tracker.getServiceStatus();
        statusLabel.setIcon(new DotIcon(12, status.getColor()));
        statusLabel.setText("");
        statusLabel.setToolTipText(status.getDescription());

        int vehicles = tracker.getActiveVehicles();
        vehicleCountLabel.setText(vehicles + " Bus");

        double avgDelay = tracker.getAverageDelayMinutes();
        if (avgDelay > 0.5) {
            delayLabel.setText(String.format("+%.0f min", avgDelay));
            delayLabel.setForeground(new Color(255, 152, 0));
        } else if (avgDelay < -0.5) {
            delayLabel.setText(String.format("%.0f min", avgDelay));
            delayLabel.setForeground(new Color(100, 181, 246));
        } else {
            delayLabel.setText("In orario");
            delayLabel.setForeground(new Color(129, 199, 132));
        }

        lastUpdateLabel.setText(tracker.getLastUpdateTime());

        vehicleChart.setData(tracker.getVehicleHistory());
    }

    private void startAutoRefresh() {
        updateTimer = new Timer("service-quality-refresh", true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            /**
             * Handles run.
             */
            public void run() {
                SwingUtilities.invokeLater(() -> refresh());
            }
        }, 5000, 10000);
    }

    private static class DotIcon implements Icon {
        private final int size;
        private final Color color;

        DotIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        /**
         * Handles paintIcon.
         */
        public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x + 2, y + 5, size, size);
            g2.dispose();
        }

        @Override public int getIconWidth() { return size + 4; }
        @Override public int getIconHeight() { return size + 8; }
    }

    private static class MiniChart extends JPanel {
        private List<Integer> data;

        MiniChart() { setOpaque(false); }

        void setData(List<Integer> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.size() < 2) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int padding = 2;

            int min = data.stream().mapToInt(Integer::intValue).min().orElse(0);
            int max = data.stream().mapToInt(Integer::intValue).max().orElse(1);
            if (max == min) max = min + 1;

            int[] xPoints = new int[data.size()];
            int[] yPoints = new int[data.size()];

            for (int i = 0; i < data.size(); i++) {
                xPoints[i] = padding + (i * (w - 2 * padding)) / (data.size() - 1);
                double normalized = (data.get(i) - min) / (double) (max - min);
                yPoints[i] = h - padding - (int) (normalized * (h - 2 * padding));
            }

            GradientPaint gp = new GradientPaint(0, 0, new Color(76, 175, 80, 80),
                                                 0, h, new Color(76, 175, 80, 0));
            g2.setPaint(gp);

            int[] fillX = new int[data.size() + 2];
            int[] fillY = new int[data.size() + 2];
            System.arraycopy(xPoints, 0, fillX, 0, data.size());
            System.arraycopy(yPoints, 0, fillY, 0, data.size());
            fillX[data.size()] = xPoints[data.size() - 1];
            fillY[data.size()] = h;
            fillX[data.size() + 1] = xPoints[0];
            fillY[data.size() + 1] = h;
            g2.fillPolygon(fillX, fillY, data.size() + 2);

            g2.setColor(new Color(76, 175, 80));
            g2.drawPolyline(xPoints, yPoints, data.size());

            if (!data.isEmpty()) {
                int lastX = xPoints[data.size() - 1];
                int lastY = yPoints[data.size() - 1];
                g2.setColor(new Color(76, 175, 80));
                g2.fillOval(lastX - 3, lastY - 3, 6, 6);
            }

            g2.dispose();
        }
    }

    @Override
    /**
     * Returns the preferred size.
     */
    public Dimension getPreferredSize() {
        return new Dimension(300, expanded ? 110 : 60);
    }
}


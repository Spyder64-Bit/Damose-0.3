package damose.ui.component;

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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import damose.config.AppConstants;
import damose.service.ServiceQualityTracker;
import damose.service.ServiceQualityTracker.ServiceStatus;

/**
 * Panel showing real-time service quality metrics.
 * Compact display with expandable details.
 */
public class ServiceQualityPanel extends JPanel {
    
    private static final Color BG_COLOR = new Color(30, 30, 35, 240);
    private static final Color BORDER_COLOR = new Color(60, 60, 70);
    private static final Color TEXT_PRIMARY = new Color(240, 240, 245);
    private static final Color TEXT_SECONDARY = new Color(160, 160, 170);
    
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
        // Main compact panel
        JPanel mainPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background
                g2.setColor(BG_COLOR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                // Border
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(8, 12, 8, 12));
        mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        
        // Status indicator
        statusLabel = createLabel("⚪", 16, true);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        mainPanel.add(statusLabel, gbc);
        
        // Vehicle count
        vehicleCountLabel = createLabel("-- 🚌", 13, true);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(vehicleCountLabel, gbc);
        
        // Average delay
        delayLabel = createLabel("-- min", 11, false);
        gbc.gridy = 1;
        mainPanel.add(delayLabel, gbc);
        
        // Mini chart
        vehicleChart = new MiniChart();
        vehicleChart.setPreferredSize(new Dimension(50, 24));
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.insets = new Insets(2, 8, 2, 4);
        mainPanel.add(vehicleChart, gbc);
        
        // Expandable details
        detailsPanel = createDetailsPanel();
        detailsPanel.setVisible(false);
        
        // Click to expand
        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleExpanded();
            }
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
                g2.setColor(new Color(25, 25, 30, 240));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 12, 8, 12));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 0, 2, 8);
        
        // Last update
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createLabel("Ultimo aggiornamento:", 10, false), gbc);
        
        lastUpdateLabel = createLabel("--:--:--", 10, true);
        gbc.gridx = 1;
        panel.add(lastUpdateLabel, gbc);
        
        // On-time performance
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(createLabel("Puntualità:", 10, false), gbc);
        
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
     * Refresh display with current metrics.
     */
    public void refresh() {
        ServiceQualityTracker tracker = ServiceQualityTracker.getInstance();
        
        // Update status
        ServiceStatus status = tracker.getServiceStatus();
        statusLabel.setText(status.getEmoji());
        statusLabel.setToolTipText(status.getDescription());
        
        // Update vehicle count
        int vehicles = tracker.getActiveVehicles();
        vehicleCountLabel.setText(vehicles + " 🚌");
        
        // Update delay
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
        
        // Update last update time
        lastUpdateLabel.setText(tracker.getLastUpdateTime());
        
        // Update chart
        vehicleChart.setData(tracker.getVehicleHistory());
    }
    
    private void startAutoRefresh() {
        updateTimer = new Timer("service-quality-refresh", true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> refresh());
            }
        }, 5000, 10000); // Refresh every 10 seconds
    }
    
    /**
     * Mini sparkline chart for vehicle count history.
     */
    private static class MiniChart extends JPanel {
        private List<Integer> data;
        
        MiniChart() {
            setOpaque(false);
        }
        
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
            
            // Find min/max
            int min = data.stream().mapToInt(Integer::intValue).min().orElse(0);
            int max = data.stream().mapToInt(Integer::intValue).max().orElse(1);
            if (max == min) max = min + 1;
            
            // Draw line
            int[] xPoints = new int[data.size()];
            int[] yPoints = new int[data.size()];
            
            for (int i = 0; i < data.size(); i++) {
                xPoints[i] = padding + (i * (w - 2 * padding)) / (data.size() - 1);
                double normalized = (data.get(i) - min) / (double)(max - min);
                yPoints[i] = h - padding - (int)(normalized * (h - 2 * padding));
            }
            
            // Gradient fill
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
            
            // Line
            g2.setColor(new Color(76, 175, 80));
            g2.drawPolyline(xPoints, yPoints, data.size());
            
            // Current value dot
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
    public Dimension getPreferredSize() {
        return new Dimension(180, expanded ? 100 : 50);
    }
}


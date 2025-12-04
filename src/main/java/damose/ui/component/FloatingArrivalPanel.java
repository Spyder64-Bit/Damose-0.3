package damose.ui.component;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;

import damose.config.AppConstants;

/**
 * Floating panel showing arrivals at a stop.
 * Clean design with dark background and light text.
 */
public class FloatingArrivalPanel extends JPanel {

    private JLabel title;
    private JPanel arrivalsList;
    private JButton closeButton;
    private JButton favoriteButton;
    private JButton viewAllButton;
    private JButton backButton;
    private JPanel content;
    private JScrollPane scrollPane;
    private JPanel footerPanel;

    private int maxRows = 8;
    private Runnable onClose;
    private Runnable onFavoriteToggle;
    private Runnable onViewAllTrips;
    private String currentStopId;
    private String currentStopName;
    private boolean isFavorite;
    
    // View mode: false = normal arrivals, true = all trips of day
    private boolean viewAllMode = false;
    private List<String> normalArrivals = new ArrayList<>();
    private List<String> allTripsData = new ArrayList<>();

    private float alpha = 1.0f;
    private Timer fadeTimer;
    
    private ImageIcon starFilledIcon;
    private ImageIcon starEmptyIcon;

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 16);
    private static final Font ARRIVAL_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 12);

    public FloatingArrivalPanel() {
        setLayout(null);
        setOpaque(false);
        
        // Load star icons
        loadStarIcons();

        content = new JPanel(new BorderLayout());
        content.setBackground(AppConstants.PANEL_BG);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppConstants.PANEL_BORDER, 2),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        content.setOpaque(true);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        title = new JLabel("Arrivi");
        title.setForeground(Color.WHITE);
        title.setFont(TITLE_FONT);
        title.setMaximumSize(new Dimension(AppConstants.FLOATING_PANEL_WIDTH - 60, 30));

        // Favorite button (star)
        favoriteButton = new JButton();
        favoriteButton.setFocusPainted(false);
        favoriteButton.setOpaque(false);
        favoriteButton.setContentAreaFilled(false);
        favoriteButton.setBorderPainted(false);
        favoriteButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        favoriteButton.setPreferredSize(new Dimension(28, 28));
        favoriteButton.setIcon(starFilledIcon); // star.png by default (not favorite)
        favoriteButton.setToolTipText("Aggiungi ai preferiti");
        favoriteButton.addActionListener(e -> {
            if (onFavoriteToggle != null) {
                onFavoriteToggle.run();
            }
        });

        closeButton = new JButton();
        closeButton.setFocusPainted(false);
        closeButton.setOpaque(true);
        closeButton.setContentAreaFilled(true);
        closeButton.setBackground(AppConstants.PANEL_BORDER);
        closeButton.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        closeButton.setPreferredSize(new Dimension(32, 32));
        closeButton.setIcon(new XIcon(14, Color.WHITE));
        closeButton.addActionListener(e -> {
            setVisible(false);
            if (onClose != null) onClose.run();
            stopFade();
            alpha = 1f;
        });

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setOpaque(false);
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
        buttonsPanel.add(favoriteButton);
        buttonsPanel.add(Box.createHorizontalStrut(4));
        buttonsPanel.add(closeButton);

        header.add(title, BorderLayout.CENTER);
        header.add(buttonsPanel, BorderLayout.EAST);

        arrivalsList = new JPanel();
        arrivalsList.setLayout(new BoxLayout(arrivalsList, BoxLayout.Y_AXIS));
        arrivalsList.setOpaque(false);

        scrollPane = new JScrollPane(arrivalsList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Footer with "View All Trips" button
        footerPanel = new JPanel();
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
        footerPanel.setOpaque(false);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));
        
        viewAllButton = new JButton(">> Vedi tutti i passaggi del giorno");
        viewAllButton.setFont(SMALL_FONT);
        viewAllButton.setForeground(AppConstants.ACCENT);
        viewAllButton.setContentAreaFilled(false);
        viewAllButton.setBorderPainted(false);
        viewAllButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewAllButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        viewAllButton.addActionListener(e -> {
            if (onViewAllTrips != null) {
                onViewAllTrips.run();
            }
        });
        
        backButton = new JButton("<< Torna agli arrivi");
        backButton.setFont(SMALL_FONT);
        backButton.setForeground(AppConstants.ACCENT);
        backButton.setContentAreaFilled(false);
        backButton.setBorderPainted(false);
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.setVisible(false);
        backButton.addActionListener(e -> showNormalView());
        
        footerPanel.add(viewAllButton);
        footerPanel.add(backButton);

        content.add(header, BorderLayout.NORTH);
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(footerPanel, BorderLayout.SOUTH);

        add(content);
        setVisible(false);
    }

    public void setOnClose(Runnable r) {
        this.onClose = r;
    }
    
    public void setOnFavoriteToggle(Runnable r) {
        this.onFavoriteToggle = r;
    }
    
    public void setOnViewAllTrips(Runnable r) {
        this.onViewAllTrips = r;
    }

    public void setPreferredRowsMax(int max) {
        this.maxRows = Math.max(1, max);
    }
    
    public String getCurrentStopId() {
        return currentStopId;
    }
    
    public String getStopName() {
        return currentStopName;
    }
    
    public void setFavoriteStatus(boolean favorite) {
        this.isFavorite = favorite;
        // favorite=TRUE → Yellow outline star (starEmptyIcon - now yellow)
        // favorite=FALSE → star.png (starFilledIcon)
        if (favorite) {
            favoriteButton.setIcon(starEmptyIcon);  // Yellow outline
            favoriteButton.setToolTipText("Rimuovi dai preferiti");
        } else {
            favoriteButton.setIcon(starFilledIcon); // star.png
            favoriteButton.setToolTipText("Aggiungi ai preferiti");
        }
    }
    
    /**
     * Show all trips data for the day.
     */
    public void showAllTripsView(List<String> allTrips) {
        this.allTripsData = new ArrayList<>(allTrips);
        this.viewAllMode = true;
        
        title.setText("Passaggi del giorno (" + allTrips.size() + ")");
        viewAllButton.setVisible(false);
        backButton.setVisible(true);
        
        // Update arrivals list with all trips
        arrivalsList.removeAll();
        
        if (allTrips.isEmpty()) {
            JLabel noData = new JLabel("Nessun passaggio programmato");
            noData.setForeground(AppConstants.TEXT_SECONDARY);
            noData.setFont(ARRIVAL_FONT);
            noData.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
            arrivalsList.add(noData);
        } else {
            for (String trip : allTrips) {
                JLabel label = new JLabel(trip);
                label.setForeground(Color.WHITE);
                label.setFont(SMALL_FONT);
                label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                
                // Color based on RT status
                if (trip.contains("[+") || trip.contains("ritardo")) {
                    label.setForeground(AppConstants.ERROR_COLOR);
                } else if (trip.contains("[-") || trip.contains("[OK]")) {
                    label.setForeground(AppConstants.SUCCESS_COLOR);
                }
                
                arrivalsList.add(label);
            }
        }
        
        // For all trips view, show more rows (up to 12)
        int visibleRows = Math.min(Math.max(allTrips.size(), 1), 12);
        arrivalsList.revalidate();
        updatePanelSizeForTrips(visibleRows);
    }
    
    /**
     * Return to normal arrivals view.
     */
    public void showNormalView() {
        viewAllMode = false;
        
        String safeName = currentStopName == null ? "" : currentStopName;
        String displayName = safeName.length() > 25 ? safeName.substring(0, 25) + "..." : safeName;
        title.setText("Arrivi a " + displayName);
        
        viewAllButton.setVisible(true);
        backButton.setVisible(false);
        
        // Restore normal arrivals
        arrivalsList.removeAll();
        displayArrivals(normalArrivals);
        
        arrivalsList.revalidate();
        updatePanelSize(Math.min(normalArrivals.size(), maxRows));
    }
    
    private void displayArrivals(List<String> arrivals) {
        for (String a : arrivals) {
            Color dotColor = Color.WHITE;
            String lower = a.toLowerCase();
            if (lower.contains("ritardo")) {
                dotColor = AppConstants.ERROR_COLOR;
            } else if (lower.contains("anticipo") || lower.contains("in orario")) {
                dotColor = AppConstants.SUCCESS_COLOR;
            } else if (lower.contains("statico")) {
                dotColor = AppConstants.TEXT_SECONDARY;
            }

            JLabel label = new JLabel(a);
            label.setForeground(Color.WHITE);
            label.setFont(ARRIVAL_FONT);
            label.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
            label.setIcon(new DotIcon(10, dotColor));
            label.setIconTextGap(12);
            arrivalsList.add(label);
        }
    }
    
    private void updatePanelSize(int rows) {
        // For normal arrivals: header (50) + rows*36 + footer (40) + padding (10)
        int rowHeight = 36;
        int headerHeight = 50;
        int footerHeight = 40;
        int padding = 10;
        
        int scrollHeight = Math.max(rows * rowHeight, rowHeight);
        int contentHeight = headerHeight + scrollHeight + footerHeight + padding;
        
        applyPanelSize(contentHeight, scrollHeight);
    }
    
    private void updatePanelSizeForTrips(int rows) {
        // For all trips view: smaller row height, more rows visible
        int rowHeight = 28;
        int headerHeight = 50;
        int footerHeight = 40;
        int padding = 10;
        
        int scrollHeight = Math.max(rows * rowHeight, rowHeight);
        int contentHeight = headerHeight + scrollHeight + footerHeight + padding;
        
        // Make panel taller for trips view (minimum 300px scroll area)
        scrollHeight = Math.max(scrollHeight, 280);
        contentHeight = headerHeight + scrollHeight + footerHeight + padding;
        
        applyPanelSize(contentHeight, scrollHeight);
    }
    
    private void applyPanelSize(int contentHeight, int scrollHeight) {
        // Update content bounds
        content.setBounds(0, 0, AppConstants.FLOATING_PANEL_WIDTH, contentHeight);
        scrollPane.setPreferredSize(new Dimension(
                AppConstants.FLOATING_PANEL_WIDTH - 28, 
                scrollHeight));
        
        // Update this panel's size - critical for proper display
        setPreferredSize(new Dimension(AppConstants.FLOATING_PANEL_WIDTH, contentHeight));
        setSize(AppConstants.FLOATING_PANEL_WIDTH, contentHeight);
        
        // Force full revalidation and repaint
        content.revalidate();
        content.repaint();
        revalidate();
        repaint();
        
        // Also repaint parent if exists
        if (getParent() != null) {
            getParent().revalidate();
            getParent().repaint();
        }
    }
    
    private void loadStarIcons() {
        try {
            // Load star.png for NOT FAVORITE state
            ImageIcon starIcon = new ImageIcon(getClass().getResource("/sprites/star.png"));
            Image scaled = starIcon.getImage().getScaledInstance(22, 22, Image.SCALE_SMOOTH);
            
            // starFilledIcon = star.png - shown when NOT FAVORITE
            starFilledIcon = new ImageIcon(scaled);
            
            // starEmptyIcon = YELLOW filled star - shown when IS FAVORITE
            starEmptyIcon = new ImageIcon(createOutlineImage(starIcon.getImage(), 22));
            
        } catch (Exception e) {
            System.out.println("Could not load star icons: " + e.getMessage());
            starFilledIcon = null;
            starEmptyIcon = null;
        }
    }
    
    private Image createOutlineImage(Image ignoredSrc, int size) {
        java.awt.image.BufferedImage outline = new java.awt.image.BufferedImage(
            size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = outline.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw a YELLOW filled star (for IS FAVORITE)
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
        
        // Yellow filled star (IS FAVORITE)
        g2.setColor(new Color(255, 200, 50)); // Gold/Yellow
        g2.fillPolygon(xPoints, yPoints, 10);
        g2.setColor(new Color(200, 150, 0)); // Darker border
        g2.setStroke(new BasicStroke(1f));
        g2.drawPolygon(xPoints, yPoints, 10);
        g2.dispose();
        return outline;
    }

    private static class DotIcon implements Icon {
        private final int size;
        private final Color color;

        DotIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y + 2, size, size);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    private static class XIcon implements Icon {
        private final int size;
        private final Color color;

        XIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(color);
            int pad = 2;
            g2.drawLine(x + pad, y + pad, x + size - pad, y + size - pad);
            g2.drawLine(x + pad, y + size - pad, x + size - pad, y + pad);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    public void update(String stopName, String stopId, List<String> arrivi, boolean isFavorite) {
        this.currentStopId = stopId;
        this.currentStopName = stopName;
        this.normalArrivals = new ArrayList<>(arrivi);
        this.viewAllMode = false;
        setFavoriteStatus(isFavorite);
        
        String safeName = stopName == null ? "" : stopName;
        String displayName = safeName.length() > 25 ? safeName.substring(0, 25) + "..." : safeName;
        title.setText("Arrivi a " + displayName);

        // Reset to normal view
        viewAllButton.setVisible(true);
        backButton.setVisible(false);
        
        arrivalsList.removeAll();
        displayArrivals(arrivi);

        arrivalsList.revalidate();
        int rows = Math.min(Math.max(arrivi.size(), 1), maxRows);
        updatePanelSize(rows);
    }

    public void fadeIn(int durationMs, int steps) {
        stopFade();
        alpha = 0f;
        setVisible(true);

        int delay = Math.max(10, durationMs / Math.max(1, steps));
        fadeTimer = new Timer(delay, null);
        fadeTimer.addActionListener(e -> {
            alpha += 1f / steps;
            if (alpha >= 1f) {
                alpha = 1f;
                stopFade();
            }
            repaint();
        });
        fadeTimer.start();
    }

    private void stopFade() {
        if (fadeTimer != null) {
            fadeTimer.stop();
            fadeTimer = null;
        }
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        super.paint(g2);
        g2.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (content == null) return;
        Rectangle cb = content.getBounds();
        if (cb.width == 0 || cb.height == 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = cb.x + cb.width / 2;
        int arrowW = 18;
        int arrowH = 10;

        Polygon triangle = new Polygon();
        triangle.addPoint(cx - arrowW / 2, cb.y + cb.height);
        triangle.addPoint(cx + arrowW / 2, cb.y + cb.height);
        triangle.addPoint(cx, cb.y + cb.height + arrowH);

        g2.setColor(AppConstants.PANEL_BORDER);
        g2.fill(triangle);

        Polygon inner = new Polygon();
        int innerInset = 2;
        inner.addPoint(cx - arrowW / 2 + innerInset, cb.y + cb.height);
        inner.addPoint(cx + arrowW / 2 - innerInset, cb.y + cb.height);
        inner.addPoint(cx, cb.y + cb.height + arrowH - innerInset);

        g2.setColor(content.getBackground());
        g2.fill(inner);

        g2.dispose();
    }

    public Dimension getPreferredPanelSize() {
        Rectangle cb = content.getBounds();
        if (cb.width == 0) return new Dimension(AppConstants.FLOATING_PANEL_WIDTH, 160);
        return new Dimension(cb.width, cb.height + 14);
    }
}


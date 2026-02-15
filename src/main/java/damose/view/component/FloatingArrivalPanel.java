package damose.view.component;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
 * UI component for floating arrival panel.
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

    private boolean viewAllMode = false;
    private List<String> normalArrivals = new ArrayList<>();
    private List<String> allTripsData = new ArrayList<>();
    private boolean favoriteEnabled = true;
    private boolean viewAllEnabled = true;
    private boolean compactRowsMode = false;

    private float alpha = 1.0f;
    private Timer fadeTimer;

    private ImageIcon starFilledIcon;
    private ImageIcon starEmptyIcon;

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 16);
    private static final Font ARRIVAL_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final int MAX_PANEL_HEIGHT = 420;
    private static final int MIN_SCROLL_HEIGHT = 60;

    public FloatingArrivalPanel() {
        setLayout(null);
        setOpaque(false);

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

        favoriteButton = new JButton();
        favoriteButton.setFocusPainted(false);
        favoriteButton.setOpaque(false);
        favoriteButton.setContentAreaFilled(false);
        favoriteButton.setBorderPainted(false);
        favoriteButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        favoriteButton.setPreferredSize(new Dimension(28, 28));
        favoriteButton.setIcon(starFilledIcon);
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

    /**
     * Registers callback for close.
     */
    public void setOnClose(Runnable r) {
        this.onClose = r;
    }

    /**
     * Registers callback for favorite toggle.
     */
    public void setOnFavoriteToggle(Runnable r) {
        this.onFavoriteToggle = r;
    }

    /**
     * Registers callback for view all trips.
     */
    public void setOnViewAllTrips(Runnable r) {
        this.onViewAllTrips = r;
    }

    /**
     * Updates the preferred rows max value.
     */
    public void setPreferredRowsMax(int max) {
        this.maxRows = Math.max(1, max);
    }

    /**
     * Returns the current stop id.
     */
    public String getCurrentStopId() {
        return currentStopId;
    }

    /**
     * Returns the stop name.
     */
    public String getStopName() {
        return currentStopName;
    }

    /**
     * Updates the favorite status value.
     */
    public void setFavoriteStatus(boolean favorite) {
        this.isFavorite = favorite;
        if (favorite) {
            favoriteButton.setIcon(starEmptyIcon);
            favoriteButton.setToolTipText("Rimuovi dai preferiti");
        } else {
            favoriteButton.setIcon(starFilledIcon);
            favoriteButton.setToolTipText("Aggiungi ai preferiti");
        }
    }

    /**
     * Handles showAllTripsView.
     */
    public void showAllTripsView(List<String> allTrips) {
        if (!viewAllEnabled) {
            return;
        }
        this.allTripsData = new ArrayList<>(allTrips);
        this.viewAllMode = true;

        title.setText("Passaggi del giorno (" + allTrips.size() + ")");
        applyActionVisibility();

        arrivalsList.removeAll();

        if (allTrips.isEmpty()) {
            arrivalsList.add(FloatingArrivalRowFactory.createNoTripsLabel(ARRIVAL_FONT));
        } else {
            for (String trip : allTrips) {
                arrivalsList.add(FloatingArrivalRowFactory.createTripRowLabel(trip, SMALL_FONT));
            }
        }

        int visibleRows = Math.min(Math.max(allTrips.size(), 1), 12);
        arrivalsList.revalidate();
        updatePanelSizeForTrips(visibleRows);
    }

    /**
     * Handles showNormalView.
     */
    public void showNormalView() {
        viewAllMode = false;

        String safeName = currentStopName == null ? "" : currentStopName;
        String displayName = safeName.length() > 25 ? safeName.substring(0, 25) + "..." : safeName;
        title.setText("Arrivi a " + displayName);

        applyActionVisibility();

        arrivalsList.removeAll();
        displayArrivals(normalArrivals);

        arrivalsList.revalidate();
        updatePanelSize(Math.min(normalArrivals.size(), maxRows));
    }

    private void displayArrivals(List<String> arrivals) {
        for (String a : arrivals) {
            int wrapWidth = resolveArrivalTextWrapWidth();
            arrivalsList.add(FloatingArrivalRowFactory.createArrivalRowLabel(
                    a,
                    compactRowsMode,
                    wrapWidth,
                    ARRIVAL_FONT
            ));
        }
    }

    private int resolveArrivalTextWrapWidth() {
        int viewportWidth = scrollPane != null && scrollPane.getViewport() != null
                ? scrollPane.getViewport().getWidth()
                : 0;
        int baseWidth = viewportWidth > 0 ? viewportWidth : AppConstants.FLOATING_PANEL_WIDTH - 56;
        int iconAndPadding = 10 + (compactRowsMode ? 8 : 12) + 18;
        int scrollbarReserve = 18;
        return Math.max(120, baseWidth - iconAndPadding - scrollbarReserve);
    }

    private void updatePanelSize(int rows) {
        FloatingPanelLayoutPolicy.SizeTarget target =
                FloatingPanelLayoutPolicy.forArrivals(rows, compactRowsMode);
        applyPanelSize(target.contentHeight, target.scrollHeight);
    }

    private void updatePanelSizeForTrips(int rows) {
        FloatingPanelLayoutPolicy.SizeTarget target = FloatingPanelLayoutPolicy.forTrips(rows);
        applyPanelSize(target.contentHeight, target.scrollHeight);
    }

    private void applyPanelSize(int contentHeight, int scrollHeight) {
        int maxHeight = resolvePanelMaxHeight();
        FloatingPanelSizeCalculator.SizeResult clamped = FloatingPanelSizeCalculator.clamp(
                contentHeight,
                scrollHeight,
                MIN_SCROLL_HEIGHT,
                maxHeight
        );

        content.setBounds(0, 0, AppConstants.FLOATING_PANEL_WIDTH, clamped.contentHeight);
        scrollPane.setPreferredSize(new Dimension(
                AppConstants.FLOATING_PANEL_WIDTH - 28,
                clamped.scrollHeight));
        scrollPane.setMaximumSize(new Dimension(
                AppConstants.FLOATING_PANEL_WIDTH - 28,
                clamped.scrollHeight));

        setPreferredSize(new Dimension(AppConstants.FLOATING_PANEL_WIDTH, clamped.contentHeight));
        setSize(AppConstants.FLOATING_PANEL_WIDTH, clamped.contentHeight);

        content.revalidate();
        content.repaint();
        revalidate();
        repaint();

        if (getParent() != null) {
            getParent().revalidate();
            getParent().repaint();
        }
    }

    private int resolvePanelMaxHeight() {
        if (getParent() == null) {
            return MAX_PANEL_HEIGHT;
        }
        return FloatingPanelSizeCalculator.resolvePanelMaxHeight(getParent().getHeight(), MAX_PANEL_HEIGHT);
    }

    private void loadStarIcons() {
        StarIconFactory.StarIcons icons = StarIconFactory.load(getClass(), 22);
        if (icons.filled == null || icons.outline == null) {
            System.out.println("Could not load star icons");
            starFilledIcon = null;
            starEmptyIcon = null;
            return;
        }
        starFilledIcon = icons.filled;
        starEmptyIcon = icons.outline;
    }

    /**
     * Handles update.
     */
    public void update(String stopName, String stopId, List<String> arrivi, boolean isFavorite) {
        this.currentStopId = stopId;
        this.currentStopName = stopName;
        this.normalArrivals = new ArrayList<>(arrivi);
        this.viewAllMode = false;
        this.compactRowsMode = false;
        setActionButtonsVisible(true, true);
        setFavoriteStatus(isFavorite);

        String safeName = stopName == null ? "" : stopName;
        String displayName = safeName.length() > 25 ? safeName.substring(0, 25) + "..." : safeName;
        title.setText("Arrivi a " + displayName);

        viewAllButton.setVisible(true);
        backButton.setVisible(false);

        arrivalsList.removeAll();
        displayArrivals(arrivi);

        arrivalsList.revalidate();
        int rows = Math.min(Math.max(arrivi.size(), 1), maxRows);
        updatePanelSize(rows);
    }

    /**
     * Updates panel with generic info rows.
     */
    public void updateInfo(String panelTitle, List<String> rows) {
        this.currentStopId = null;
        this.currentStopName = panelTitle;
        this.normalArrivals = new ArrayList<>(rows);
        this.viewAllMode = false;
        this.compactRowsMode = true;
        setActionButtonsVisible(false, false);

        String safeTitle = panelTitle == null ? "" : panelTitle;
        title.setText(safeTitle);

        arrivalsList.removeAll();
        displayArrivals(rows);

        arrivalsList.revalidate();
        int visibleRows = Math.min(Math.max(rows.size(), 1), maxRows);
        updatePanelSize(visibleRows);
    }

    /**
     * Updates the action buttons visibility value.
     */
    public void setActionButtonsVisible(boolean favoriteVisible, boolean viewAllVisible) {
        this.favoriteEnabled = favoriteVisible;
        this.viewAllEnabled = viewAllVisible;
        applyActionVisibility();
    }

    private void applyActionVisibility() {
        favoriteButton.setVisible(favoriteEnabled);
        footerPanel.setVisible(viewAllEnabled);
        viewAllButton.setVisible(viewAllEnabled && !viewAllMode);
        backButton.setVisible(viewAllEnabled && viewAllMode);
    }

    /**
     * Handles fadeIn.
     */
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
    /**
     * Handles paint.
     */
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

    /**
     * Returns the preferred panel size.
     */
    public Dimension getPreferredPanelSize() {
        Rectangle cb = content.getBounds();
        if (cb.width == 0) return new Dimension(AppConstants.FLOATING_PANEL_WIDTH, 160);
        return new Dimension(cb.width, cb.height + 14);
    }
}


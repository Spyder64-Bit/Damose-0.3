package damose.view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

import javax.swing.ImageIcon;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import damose.config.AppConstants;
import damose.model.Stop;
import damose.view.component.ConnectionButton;
import damose.view.component.FloatingArrivalPanel;
import damose.view.component.InfoOverlay;
import damose.view.component.RouteSidePanel;
import damose.view.component.SearchOverlay;
import damose.view.component.ServiceQualityPanel;
import damose.view.map.GeoUtils;
import damose.view.map.MapFactory;


/**
 * Main application view and UI orchestration.
 */
public class MainView {

    private JFrame frame;
    private JXMapViewer mapViewer;
    private JButton searchButton;
    private JButton favoritesButton;
    private JButton closeButton;
    private JButton maxButton;
    private JButton minButton;
    private JButton busToggleButton;
    private JButton infoButton;
    private JPanel mapControlsPanel;
    private ConnectionButton connectionButton;
    private SearchOverlay searchOverlay;
    private InfoOverlay infoOverlay;
    private JPanel overlayPanel;
    private JLayeredPane layeredPane;
    private FloatingArrivalPanel floatingPanel;
    private RouteSidePanel routeSidePanel;
    private JPanel bottomNoticePanel;
    private JLabel bottomNoticeLabel;
    private Timer bottomNoticeTimer;
    private GeoPosition floatingAnchorGeo;
    private ServiceQualityPanel serviceQualityPanel;
    private List<Stop> allStopsCache = new ArrayList<>();
    private List<Stop> allLinesCache = new ArrayList<>();

    private Point dragOffset;
    private boolean isDragging = false;
    private Rectangle normalBounds = new Rectangle(100, 100, 1100, 750);
    private static final int LEFT_STACK_X = 10;
    private static final int LEFT_STACK_Y = 10;
    private static final int MAP_CONTROLS_WIDTH = 58;
    private static final int MAP_CONTROLS_HEIGHT = 280;
    private static final int ROUTE_PANEL_WIDTH = 290;
    private static final int ROUTE_PANEL_TOP = 48;
    private static final int ROUTE_PANEL_MARGIN = 12;
    private static final int WINDOW_CONTROL_SIZE = 34;
    private static final int WINDOW_CONTROL_TOP = 6;
    private static final int WINDOW_CONTROL_RIGHT_MARGIN = 6;
    private static final int WINDOW_CONTROL_GAP = 2;
    private static final int BOTTOM_NOTICE_MARGIN = 16;
    private static final int BOTTOM_NOTICE_MAX_WIDTH = 620;
    private Runnable onFloatingPanelClose;
    private Runnable onRoutePanelClose;
    private IntConsumer onRouteDirectionSelected;

    private final PropertyChangeListener mapListener = evt -> {
        String name = evt.getPropertyName();
        if ("zoom".equals(name) || "center".equals(name) || "tileFactory".equals(name)) {
            updateFloatingPanelPosition();
        }
    };

    /**
     * Handles showSearchOverlay.
     */
    public void showSearchOverlay() {
        if (searchOverlay != null) searchOverlay.showSearch();
    }

    /**
     * Updates the search data value.
     */
    public void setSearchData(List<Stop> stops, List<Stop> lines) {
        this.allLinesCache = lines != null ? lines : new ArrayList<>();
        if (searchOverlay != null) {
            searchOverlay.setData(stops, lines);
        }
    }

    /**
     * Registers callback for search select.
     */
    public void setOnSearchSelect(java.util.function.Consumer<Stop> callback) {
        if (searchOverlay != null) {
            searchOverlay.setOnSelect(callback);
        }
    }

    public void setOnSearchFavoritesLoginRequired(Runnable callback) {
        if (searchOverlay != null) {
            searchOverlay.setOnFavoritesLoginRequired(callback);
        }
    }

    /**
     * Returns the search button.
     */
    public JButton getSearchButton() {
        return searchButton;
    }

    /**
     * Returns the favorites button.
     */
    public JButton getFavoritesButton() {
        return favoritesButton;
    }

    /**
     * Returns the bus toggle button.
     */
    public JButton getBusToggleButton() {
        return busToggleButton;
    }

    /**
     * Returns the connection button.
     */
    public ConnectionButton getConnectionButton() {
        return connectionButton;
    }

    /**
     * Returns the info button.
     */
    public JButton getInfoButton() {
        return infoButton;
    }

    /**
     * Handles showInfoOverlay.
     */
    public void showInfoOverlay() {
        if (infoOverlay != null) {
            infoOverlay.showInfo();
        }
    }

    /**
     * Returns the map viewer.
     */
    public JXMapViewer getMapViewer() {
        return mapViewer;
    }

    /**
     * Updates the floating panel max rows value.
     */
    public void setFloatingPanelMaxRows(int maxRows) {
        if (floatingPanel != null) {
            floatingPanel.setPreferredRowsMax(maxRows);
        }
    }

    public void showBottomNotice(String message) {
        Runnable showTask = () -> {
            if (bottomNoticePanel == null || bottomNoticeLabel == null || layeredPane == null) {
                return;
            }
            String safeMessage = message == null ? "" : message.trim();
            int textWidth = Math.min(520, Math.max(220, layeredPane.getWidth() - 80));
            String html = "<html><body style='width:" + textWidth + "px'>"
                    + escapeHtml(safeMessage).replace("\n", "<br>")
                    + "</body></html>";
            bottomNoticeLabel.setText(html);

            bottomNoticePanel.revalidate();
            positionBottomNotice();
            bottomNoticePanel.setVisible(true);
            bottomNoticePanel.repaint();

            if (bottomNoticeTimer != null) {
                bottomNoticeTimer.stop();
            }
            bottomNoticeTimer = new Timer(5200, e -> bottomNoticePanel.setVisible(false));
            bottomNoticeTimer.setRepeats(false);
            bottomNoticeTimer.start();
        };

        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            showTask.run();
        } else {
            javax.swing.SwingUtilities.invokeLater(showTask);
        }
    }

    /**
     * Handles init.
     */
    public void init() {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
        } catch (Exception ignored) {
        }

        UIManager.put("Button.arc", 20);
        UIManager.put("TextField.arc", 15);

        frame = new JFrame("Damose");
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 750);
        frame.setLocationRelativeTo(null);


        frame.setShape(new RoundRectangle2D.Double(0, 0, 1100, 750, 20, 20));


        frame.addComponentListener(new ComponentAdapter() {
            @Override
            /**
             * Handles componentResized.
             */
            public void componentResized(ComponentEvent e) {
                if (frame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                    frame.setShape(null);
                } else {
                    frame.setShape(new RoundRectangle2D.Double(0, 0,
                        frame.getWidth(), frame.getHeight(), 20, 20));
                }
            }
        });


        try {
            Image trimmedIcon = loadTrimmedImage("/sprites/icon.png");
            if (trimmedIcon != null) {
                List<Image> icons = new ArrayList<>();
                icons.add(trimmedIcon.getScaledInstance(256, 256, Image.SCALE_SMOOTH));
                icons.add(trimmedIcon.getScaledInstance(128, 128, Image.SCALE_SMOOTH));
                icons.add(trimmedIcon.getScaledInstance(64, 64, Image.SCALE_SMOOTH));
                icons.add(trimmedIcon.getScaledInstance(48, 48, Image.SCALE_SMOOTH));
                icons.add(trimmedIcon.getScaledInstance(32, 32, Image.SCALE_SMOOTH));
                icons.add(trimmedIcon.getScaledInstance(16, 16, Image.SCALE_SMOOTH));
                frame.setIconImages(icons);
            }
        } catch (Exception e) {
            System.out.println("Could not load app icon: " + e.getMessage());
        }


        mapViewer = MapFactory.createMapViewer();

        layeredPane = new JLayeredPane();
        frame.setContentPane(layeredPane);

        mapViewer.setBounds(0, 0, 1100, 750);
        layeredPane.add(mapViewer, JLayeredPane.DEFAULT_LAYER);

        overlayPanel = new JPanel(null);
        overlayPanel.setOpaque(false);
        overlayPanel.setBounds(0, 0, 1100, 750);
        layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);
        initBottomNotice();

        mapControlsPanel = createMapControlsPanel();
        mapControlsPanel.setBounds(LEFT_STACK_X, LEFT_STACK_Y, MAP_CONTROLS_WIDTH, MAP_CONTROLS_HEIGHT);
        overlayPanel.add(mapControlsPanel);


        ImageIcon lensIcon = new ImageIcon(getClass().getResource("/sprites/lente.png"));
        Image scaledLens = lensIcon.getImage().getScaledInstance(44, 44, Image.SCALE_SMOOTH);
        searchButton = new JButton(new ImageIcon(scaledLens));
        searchButton.setContentAreaFilled(false);
        searchButton.setBorderPainted(false);
        searchButton.setFocusPainted(false);
        searchButton.setBounds(5, 5, 48, 48);
        searchButton.setToolTipText("Cerca fermate e linee");
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mapControlsPanel.add(searchButton);


        ImageIcon starIcon = new ImageIcon(getClass().getResource("/sprites/star.png"));
        Image scaledStar = starIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        favoritesButton = new JButton(new ImageIcon(scaledStar));
        favoritesButton.setContentAreaFilled(false);
        favoritesButton.setBorderPainted(false);
        favoritesButton.setFocusPainted(false);
        favoritesButton.setBounds(5, 60, 48, 48);
        favoritesButton.setToolTipText("Fermate preferite");
        favoritesButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mapControlsPanel.add(favoritesButton);


        ImageIcon busIcon = new ImageIcon(getClass().getResource("/sprites/bus1.png"));
        Image scaledBus = busIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        busToggleButton = new JButton(new ImageIcon(scaledBus));
        busToggleButton.setContentAreaFilled(false);
        busToggleButton.setBorderPainted(false);
        busToggleButton.setFocusPainted(false);
        busToggleButton.setBounds(5, 115, 48, 48);
        busToggleButton.setToolTipText("Mostra/Nascondi autobus");
        busToggleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mapControlsPanel.add(busToggleButton);


        createWindowControls();


        connectionButton = new ConnectionButton();
        connectionButton.setBounds((MAP_CONTROLS_WIDTH - ConnectionButton.BUTTON_WIDTH) / 2,
                170,
                ConnectionButton.BUTTON_WIDTH,
                ConnectionButton.BUTTON_HEIGHT);
        mapControlsPanel.add(connectionButton);

        infoButton = createInfoButton();
        infoButton.setBounds(5, 225, 48, 48);
        infoButton.addActionListener(e -> showInfoOverlay());
        mapControlsPanel.add(infoButton);


        serviceQualityPanel = new ServiceQualityPanel();
        serviceQualityPanel.setBounds(15, 750 - 65, 180, 50);
        overlayPanel.add(serviceQualityPanel);

        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            /**
             * Handles componentResized.
             */
            public void componentResized(ComponentEvent e) {
                int w = layeredPane.getWidth();
                int h = layeredPane.getHeight();
                mapViewer.setBounds(0, 0, w, h);
                overlayPanel.setBounds(0, 0, w, h);
                if (searchOverlay != null) {
                    searchOverlay.setBounds(0, 0, w, h);
                }
                if (infoOverlay != null) {
                    infoOverlay.setBounds(0, 0, w, h);
                }

                updateWindowControlPositions(w);
                if (serviceQualityPanel != null) {
                    serviceQualityPanel.setBounds(15, h - 65, 180, 50);
                }
                if (routeSidePanel != null) {
                    routeSidePanel.setBounds(w - ROUTE_PANEL_WIDTH - ROUTE_PANEL_MARGIN, ROUTE_PANEL_TOP,
                            ROUTE_PANEL_WIDTH, h - ROUTE_PANEL_TOP - ROUTE_PANEL_MARGIN);
                }
                updateFloatingPanelPosition();
                positionBottomNotice();
            }
        });


        enableWindowDrag();

        floatingPanel = new FloatingArrivalPanel();
        floatingPanel.setVisible(false);
        floatingPanel.setOnClose(() -> {
            floatingAnchorGeo = null;
            if (onFloatingPanelClose != null) {
                onFloatingPanelClose.run();
            }
        });
        floatingPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateFloatingPanelPosition();
            }
        });
        overlayPanel.add(floatingPanel);

        searchOverlay = new SearchOverlay();
        searchOverlay.setVisible(false);
        searchOverlay.setBounds(0, 0, 1100, 750);
        layeredPane.add(searchOverlay, JLayeredPane.POPUP_LAYER);

        infoOverlay = new InfoOverlay();
        infoOverlay.setVisible(false);
        infoOverlay.setBounds(0, 0, 1100, 750);
        layeredPane.add(infoOverlay, JLayeredPane.POPUP_LAYER);

        routeSidePanel = new RouteSidePanel();
        routeSidePanel.setVisible(false);
        routeSidePanel.setBounds(1100 - ROUTE_PANEL_WIDTH - ROUTE_PANEL_MARGIN, ROUTE_PANEL_TOP,
                ROUTE_PANEL_WIDTH, 750 - ROUTE_PANEL_TOP - ROUTE_PANEL_MARGIN);
        routeSidePanel.setOnClose(() -> {
            if (onRoutePanelClose != null) {
                onRoutePanelClose.run();
            }
        });
        routeSidePanel.setOnDirectionSelected(directionId -> {
            if (onRouteDirectionSelected != null) {
                onRouteDirectionSelected.accept(directionId);
            }
        });
        overlayPanel.add(routeSidePanel);

        mapViewer.addPropertyChangeListener(mapListener);
        setFloatingPanelMaxRows(10);

        frame.setVisible(true);
    }

    private void createWindowControls() {

        closeButton = createWindowControlButton(WindowControlType.CLOSE);
        closeButton.setBounds(1100 - 40, WINDOW_CONTROL_TOP, WINDOW_CONTROL_SIZE, WINDOW_CONTROL_SIZE);
        closeButton.addActionListener(e -> System.exit(0));
        overlayPanel.add(closeButton);


        maxButton = createWindowControlButton(WindowControlType.MAXIMIZE);
        maxButton.setBounds(1100 - 76, WINDOW_CONTROL_TOP, WINDOW_CONTROL_SIZE, WINDOW_CONTROL_SIZE);
        maxButton.addActionListener(e -> {
            if (frame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {

                frame.setExtendedState(JFrame.NORMAL);
                frame.setBounds(normalBounds);
            } else {

                normalBounds = frame.getBounds();
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });
        overlayPanel.add(maxButton);


        minButton = createWindowControlButton(WindowControlType.MINIMIZE);
        minButton.setBounds(1100 - 112, WINDOW_CONTROL_TOP, WINDOW_CONTROL_SIZE, WINDOW_CONTROL_SIZE);
        minButton.addActionListener(e -> frame.setState(JFrame.ICONIFIED));
        overlayPanel.add(minButton);
    }

    private JPanel createMapControlsPanel() {
        JPanel panel = createOverlayCardPanel();
        panel.setLayout(null);
        return panel;
    }

    private JPanel createOverlayCardPanel() {
        JPanel panel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
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
        return panel;
    }

    private void initBottomNotice() {
        bottomNoticePanel = new JPanel(new BorderLayout()) {
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
        bottomNoticePanel.setOpaque(false);
        bottomNoticePanel.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel warningIconLabel = new JLabel(new WarningIcon(18));
        warningIconLabel.setBorder(new EmptyBorder(2, 0, 0, 10));
        warningIconLabel.setVerticalAlignment(JLabel.TOP);
        bottomNoticePanel.add(warningIconLabel, BorderLayout.WEST);

        bottomNoticeLabel = new JLabel();
        bottomNoticeLabel.setForeground(AppConstants.TEXT_PRIMARY);
        bottomNoticeLabel.setFont(AppConstants.FONT_BODY);
        bottomNoticePanel.add(bottomNoticeLabel, BorderLayout.CENTER);
        bottomNoticePanel.setVisible(false);

        layeredPane.add(bottomNoticePanel, JLayeredPane.DRAG_LAYER);
    }

    private void positionBottomNotice() {
        if (bottomNoticePanel == null || layeredPane == null) return;

        int layerW = layeredPane.getWidth();
        int layerH = layeredPane.getHeight();
        if (layerW <= 0 || layerH <= 0) return;

        int width = Math.min(BOTTOM_NOTICE_MAX_WIDTH, Math.max(260, layerW - 24));
        Dimension pref = bottomNoticePanel.getPreferredSize();
        int height = Math.max(48, pref.height);
        int x = Math.max(12, (layerW - width) / 2);
        int y = Math.max(12, layerH - height - BOTTOM_NOTICE_MARGIN);
        bottomNoticePanel.setBounds(x, y, width, height);
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

    private enum WindowControlType {
        CLOSE,
        MAXIMIZE,
        MINIMIZE
    }

    private JButton createWindowControlButton(WindowControlType type) {
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
                        ? (type == WindowControlType.CLOSE ? new Color(235, 85, 85, 230) : new Color(125, 125, 140, 220))
                        : new Color(28, 28, 34, 200);
                Color border = hovered
                        ? (type == WindowControlType.CLOSE ? new Color(255, 140, 140, 220) : new Color(165, 165, 180, 210))
                        : new Color(80, 80, 94, 170);
                Color glyph = hovered && type != WindowControlType.CLOSE
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
            /**
             * Handles mouseEntered.
             */
            public void mouseEntered(MouseEvent e) {
                btn.repaint();
            }

            @Override
            /**
             * Handles mouseExited.
             */
            public void mouseExited(MouseEvent e) {
                btn.repaint();
            }
        });
        return btn;
    }

    private JButton createInfoButton() {
        JButton btn = new JButton();
        Image infoImage = loadTrimmedImage("/sprites/info.png");
        if (infoImage != null) {
            Image scaledInfo = infoImage.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            btn.setIcon(new ImageIcon(scaledInfo));
        } else {
            btn.setText("i");
            btn.setFont(new Font("Segoe UI", Font.BOLD, 22));
            btn.setForeground(new Color(220, 220, 230));
        }
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(false);
        btn.setToolTipText("Legenda e informazioni");

        return btn;
    }

    private void updateWindowControlPositions(int width) {
        int closeX = width - WINDOW_CONTROL_RIGHT_MARGIN - WINDOW_CONTROL_SIZE;
        int maxX = closeX - WINDOW_CONTROL_SIZE - WINDOW_CONTROL_GAP;
        int minX = maxX - WINDOW_CONTROL_SIZE - WINDOW_CONTROL_GAP;
        if (closeButton != null) closeButton.setBounds(closeX, WINDOW_CONTROL_TOP, WINDOW_CONTROL_SIZE, WINDOW_CONTROL_SIZE);
        if (maxButton != null) maxButton.setBounds(maxX, WINDOW_CONTROL_TOP, WINDOW_CONTROL_SIZE, WINDOW_CONTROL_SIZE);
        if (minButton != null) minButton.setBounds(minX, WINDOW_CONTROL_TOP, WINDOW_CONTROL_SIZE, WINDOW_CONTROL_SIZE);
    }

    private Image loadTrimmedImage(String path) {
        java.net.URL url = getClass().getResource(path);
        if (url == null) {
            return null;
        }

        ImageIcon rawIcon = new ImageIcon(url);
        if (rawIcon.getIconWidth() <= 0 || rawIcon.getIconHeight() <= 0) {
            return null;
        }

        BufferedImage source = new BufferedImage(rawIcon.getIconWidth(), rawIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = source.createGraphics();
        g2.drawImage(rawIcon.getImage(), 0, 0, null);
        g2.dispose();

        return trimTransparentBorders(source);
    }

    private BufferedImage trimTransparentBorders(BufferedImage source) {
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

    private void enableWindowDrag() {

        mapViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {

                if (e.getY() < 50 && e.getX() < mapViewer.getWidth() - 120) {
                    dragOffset = e.getPoint();
                    isDragging = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
            }
        });

        mapViewer.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging && dragOffset != null) {
                    Point loc = frame.getLocation();
                    frame.setLocation(loc.x + e.getX() - dragOffset.x, loc.y + e.getY() - dragOffset.y);
                }
            }
        });
    }

    /**
     * Handles addWaypointClickListener.
     */
    public void addWaypointClickListener() {
        mapViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (allStopsCache.isEmpty()) return;

                int x = e.getX();
                int y = e.getY();

                GeoPosition clickedPos = mapViewer.convertPointToGeoPosition(e.getPoint());
                Stop nearest = findNearestStop(clickedPos);

                if (nearest != null && GeoUtils.isClickCloseToStop(mapViewer, nearest, x, y)) {
                    if (stopClickListener != null) {
                        stopClickListener.onStopClicked(nearest);
                    }
                }
            }
        });
    }

    private Stop findNearestStop(GeoPosition pos) {
        double minDist = Double.MAX_VALUE;
        Stop nearest = null;

        for (Stop s : allStopsCache) {
            double d = GeoUtils.haversine(
                    pos.getLatitude(), pos.getLongitude(),
                    s.getStopLat(), s.getStopLon()
            );
            if (d < minDist) {
                minDist = d;
                nearest = s;
            }
        }
        return nearest;
    }

    public interface StopClickListener {
        void onStopClicked(Stop stop);
    }

    private StopClickListener stopClickListener;

    /**
     * Updates the stop click listener value.
     */
    public void setStopClickListener(StopClickListener listener) {
        this.stopClickListener = listener;
    }

    /**
     * Updates the all stops value.
     */
    public void setAllStops(List<Stop> stops) {
        this.allStopsCache = stops;
    }

    public void showFloatingPanel(String stopName, String stopId, List<String> arrivi,
                                   boolean isFavorite, Point2D pos, GeoPosition anchorGeo) {
        floatingPanel.update(stopName, stopId, arrivi, isFavorite);
        this.floatingAnchorGeo = anchorGeo;

        Point2D p = pos;
        if (p == null && anchorGeo != null) {
            p = mapViewer.convertGeoPositionToPoint(anchorGeo);
        }

        if (p != null) {
            Dimension pref = floatingPanel.getPreferredPanelSize();
            int panelWidth = pref.width;
            int panelHeight = pref.height;
            int x = (int) p.getX() - panelWidth / 2;
            int y = (int) p.getY() - panelHeight - 8;

            int maxX = Math.max(10, mapViewer.getWidth() - panelWidth - 10);
            int maxY = Math.max(10, mapViewer.getHeight() - panelHeight - 10);
            x = Math.max(10, Math.min(x, maxX));
            y = Math.max(10, Math.min(y, maxY));

            floatingPanel.setBounds(x, y, panelWidth, panelHeight);
        }

        floatingPanel.revalidate();
        floatingPanel.repaint();
        floatingPanel.fadeIn(300, 15);
    }

    /**
     * Handles showFloatingPanel.
     */
    public void showFloatingPanel(String stopName, List<String> arrivi, Point2D pos) {
        showFloatingPanel(stopName, null, arrivi, false, pos, null);
    }

    /**
     * Returns the floating panel stop id.
     */
    public String getFloatingPanelStopId() {
        return floatingPanel.getCurrentStopId();
    }

    /**
     * Returns whether floating panel visible.
     */
    public boolean isFloatingPanelVisible() {
        return floatingPanel.isVisible();
    }

    /**
     * Handles refreshFloatingPanel.
     */
    public void refreshFloatingPanel(String stopName, String stopId, List<String> arrivi, boolean isFavorite) {
        floatingPanel.update(stopName, stopId, arrivi, isFavorite);
        updateFloatingPanelPosition();
        floatingPanel.repaint();
    }

    /**
     * Handles updateFloatingPanelFavorite.
     */
    public void updateFloatingPanelFavorite(boolean isFavorite) {
        floatingPanel.setFavoriteStatus(isFavorite);
    }

    /**
     * Registers callback for favorite toggle.
     */
    public void setOnFavoriteToggle(Runnable callback) {
        floatingPanel.setOnFavoriteToggle(callback);
    }

    /**
     * Registers callback for view all trips.
     */
    public void setOnViewAllTrips(Runnable callback) {
        floatingPanel.setOnViewAllTrips(callback);
    }

    /**
     * Registers callback for floating panel close.
     */
    public void setOnFloatingPanelClose(Runnable callback) {
        this.onFloatingPanelClose = callback;
    }

    /**
     * Registers callback for route panel close.
     */
    public void setOnRoutePanelClose(Runnable callback) {
        this.onRoutePanelClose = callback;
    }

    /**
     * Registers callback for route direction selected.
     */
    public void setOnRouteDirectionSelected(IntConsumer callback) {
        this.onRouteDirectionSelected = callback;
    }

    /**
     * Handles showAllTripsInPanel.
     */
    public void showAllTripsInPanel(List<String> allTrips) {
        floatingPanel.showAllTripsView(allTrips);
        updateFloatingPanelPosition();
    }

    /**
     * Handles showFavoritesInSearch.
     */
    public void showFavoritesInSearch(List<Stop> favorites) {
        searchOverlay.showFavorites(favorites);
    }

    /**
     * Handles hideFloatingPanel.
     */
    public void hideFloatingPanel() {
        floatingPanel.setVisible(false);
        floatingAnchorGeo = null;
    }

    /**
     * Handles showRouteSidePanel.
     */
    public void showRouteSidePanel(String routeName, List<Stop> routeStops) {
        if (routeSidePanel == null) return;
        routeSidePanel.setRoute(routeName, routeStops);
        routeSidePanel.setVisible(true);
        routeSidePanel.repaint();
    }

    /**
     * Updates the route side panel directions value.
     */
    public void setRouteSidePanelDirections(Map<Integer, String> directions, int selectedDirection) {
        if (routeSidePanel == null) return;
        routeSidePanel.setDirectionOptions(directions, selectedDirection);
    }

    /**
     * Handles updateRouteSidePanelVehicles.
     */
    public void updateRouteSidePanelVehicles(List<RouteSidePanel.VehicleMarker> markers) {
        if (routeSidePanel == null || !routeSidePanel.isVisible()) return;
        routeSidePanel.setVehicleMarkers(markers);
    }

    /**
     * Handles hideRouteSidePanel.
     */
    public void hideRouteSidePanel() {
        if (routeSidePanel == null) return;
        routeSidePanel.setVisible(false);
    }

    private void updateFloatingPanelPosition() {
        if (floatingAnchorGeo == null) return;


        Point2D projectedPos = mapViewer.convertGeoPositionToPoint(floatingAnchorGeo);


        if (projectedPos == null) {
            if (floatingPanel.isVisible()) {
                floatingPanel.setVisible(false);
            }
            return;
        }

        int mapWidth = mapViewer.getWidth();
        int mapHeight = mapViewer.getHeight();
        double stopX = projectedPos.getX();
        double stopY = projectedPos.getY();



        int offScreenMargin = 200;
        if (stopX < -offScreenMargin || stopX > mapWidth + offScreenMargin ||
            stopY < -offScreenMargin || stopY > mapHeight + offScreenMargin) {
            if (floatingPanel.isVisible()) {
                floatingPanel.setVisible(false);
            }
            return;
        }


        Dimension pref = floatingPanel.getPreferredPanelSize();
        int panelWidth = pref.width;
        int panelHeight = pref.height;


        int targetX = (int) stopX - panelWidth / 2;
        int targetY = (int) stopY - panelHeight - 8;






        int minValidX = 5;
        int maxValidX = mapWidth - panelWidth - 5;
        int minValidY = 5;
        int maxValidY = mapHeight - panelHeight - 5;

        if (maxValidX < minValidX || maxValidY < minValidY
                || targetX < minValidX || targetX > maxValidX
                || targetY < minValidY || targetY > maxValidY) {
            if (floatingPanel.isVisible()) {
                floatingPanel.setVisible(false);
            }
            return;
        }



        if (!floatingPanel.isVisible()) {
            floatingPanel.setVisible(true);
        }


        floatingPanel.setBounds(targetX, targetY, panelWidth, panelHeight);
        floatingPanel.revalidate();
        floatingPanel.repaint();
    }
}


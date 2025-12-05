package damose.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import damose.config.AppConstants;
import damose.data.model.Stop;
import damose.ui.component.ConnectionButton;
import damose.ui.component.FloatingArrivalPanel;
import damose.ui.component.SearchOverlay;
import damose.ui.component.ServiceQualityPanel;
import damose.ui.map.GeoUtils;
import damose.ui.map.MapFactory;

/**
 * Main application view - Midnight Dark style.
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
    private ConnectionButton connectionButton;
    private SearchOverlay searchOverlay;
    private JPanel overlayPanel;
    private FloatingArrivalPanel floatingPanel;
    private GeoPosition floatingAnchorGeo;
    private ServiceQualityPanel serviceQualityPanel;
    private List<Stop> allStopsCache = new ArrayList<>();
    private List<Stop> allLinesCache = new ArrayList<>();

    private Point dragOffset;
    private boolean isDragging = false;
    private Rectangle normalBounds = new Rectangle(100, 100, 1100, 750); // Store normal window bounds

    private final PropertyChangeListener mapListener = evt -> {
        String name = evt.getPropertyName();
        if ("zoom".equals(name) || "center".equals(name) || "tileFactory".equals(name)) {
            updateFloatingPanelPosition();
        }
    };

    public void showSearchOverlay() {
        if (searchOverlay != null) searchOverlay.showSearch();
    }

    public void setSearchData(List<Stop> stops, List<Stop> lines) {
        this.allLinesCache = lines != null ? lines : new ArrayList<>();
        if (searchOverlay != null) {
            searchOverlay.setData(stops, lines);
        }
    }

    public void setOnSearchSelect(java.util.function.Consumer<Stop> callback) {
        if (searchOverlay != null) {
            searchOverlay.setOnSelect(callback);
        }
    }

    public JButton getSearchButton() {
        return searchButton;
    }
    
    public JButton getFavoritesButton() {
        return favoritesButton;
    }
    
    public JButton getBusToggleButton() {
        return busToggleButton;
    }
    
    public ConnectionButton getConnectionButton() {
        return connectionButton;
    }

    public JXMapViewer getMapViewer() {
        return mapViewer;
    }

    public void setFloatingPanelMaxRows(int maxRows) {
        if (floatingPanel != null) {
            floatingPanel.setPreferredRowsMax(maxRows);
        }
    }

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
        
        // Set rounded corners
        frame.setShape(new RoundRectangle2D.Double(0, 0, 1100, 750, 20, 20));
        
        // Update shape on resize
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (frame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                    frame.setShape(null);
                } else {
                    frame.setShape(new RoundRectangle2D.Double(0, 0, 
                        frame.getWidth(), frame.getHeight(), 20, 20));
                }
            }
        });
        
        // Set app icon
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/sprites/icon.png"));
            List<Image> icons = new ArrayList<>();
            icons.add(icon.getImage());
            frame.setIconImages(icons);
        } catch (Exception e) {
            System.out.println("Could not load app icon: " + e.getMessage());
        }

        // Map takes the whole window
        mapViewer = MapFactory.createMapViewer();

        JLayeredPane layeredPane = new JLayeredPane();
        frame.setContentPane(layeredPane);

        mapViewer.setBounds(0, 0, 1100, 750);
        layeredPane.add(mapViewer, JLayeredPane.DEFAULT_LAYER);

        overlayPanel = new JPanel(null);
        overlayPanel.setOpaque(false);
        overlayPanel.setBounds(0, 0, 1100, 750);
        layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);

        // Search button (top-left)
        ImageIcon lensIcon = new ImageIcon(getClass().getResource("/sprites/lente.png"));
        Image scaledLens = lensIcon.getImage().getScaledInstance(44, 44, Image.SCALE_SMOOTH);
        searchButton = new JButton(new ImageIcon(scaledLens));
        searchButton.setContentAreaFilled(false);
        searchButton.setBorderPainted(false);
        searchButton.setBounds(15, 15, 48, 48);
        searchButton.setToolTipText("Cerca fermate e linee");
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        overlayPanel.add(searchButton);
        
        // Favorites button (below search button)
        ImageIcon starIcon = new ImageIcon(getClass().getResource("/sprites/star.png"));
        Image scaledStar = starIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        favoritesButton = new JButton(new ImageIcon(scaledStar));
        favoritesButton.setContentAreaFilled(false);
        favoritesButton.setBorderPainted(false);
        favoritesButton.setBounds(15, 70, 48, 48);
        favoritesButton.setToolTipText("Fermate preferite");
        favoritesButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        overlayPanel.add(favoritesButton);
        
        // Bus visibility toggle button (below favorites)
        ImageIcon busIcon = new ImageIcon(getClass().getResource("/sprites/bus1.png"));
        Image scaledBus = busIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        busToggleButton = new JButton(new ImageIcon(scaledBus));
        busToggleButton.setContentAreaFilled(false);
        busToggleButton.setBorderPainted(false);
        busToggleButton.setBounds(15, 125, 48, 48);
        busToggleButton.setToolTipText("Mostra/Nascondi autobus");
        busToggleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        overlayPanel.add(busToggleButton);
        
        // Window control buttons (top-right corner) - add first so connection button is below
        createWindowControls();
        
        // Connection button (below window controls)
        connectionButton = new ConnectionButton();
        connectionButton.setBounds(1100 - 59, 48, 44, 44);
        overlayPanel.add(connectionButton);
        
        // Service quality panel (bottom-left)
        serviceQualityPanel = new ServiceQualityPanel();
        serviceQualityPanel.setBounds(15, 750 - 65, 180, 50);
        overlayPanel.add(serviceQualityPanel);

        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = layeredPane.getWidth();
                int h = layeredPane.getHeight();
                mapViewer.setBounds(0, 0, w, h);
                overlayPanel.setBounds(0, 0, w, h);
                if (searchOverlay != null) {
                    searchOverlay.setBounds(0, 0, w, h);
                }
                // Update button positions
                updateWindowControlPositions(w);
                if (connectionButton != null) {
                    connectionButton.setBounds(w - 59, 48, 44, 44);
                }
                if (serviceQualityPanel != null) {
                    serviceQualityPanel.setBounds(15, h - 65, 180, 50);
                }
                updateFloatingPanelPosition();
            }
        });
        
        // Enable window dragging from map
        enableWindowDrag();

        floatingPanel = new FloatingArrivalPanel();
        floatingPanel.setVisible(false);
        floatingPanel.setOnClose(() -> floatingAnchorGeo = null);
        overlayPanel.add(floatingPanel);

        searchOverlay = new SearchOverlay();
        searchOverlay.setVisible(false);
        searchOverlay.setBounds(0, 0, 1100, 750);
        layeredPane.add(searchOverlay, JLayeredPane.POPUP_LAYER);

        mapViewer.addPropertyChangeListener(mapListener);
        setFloatingPanelMaxRows(10);

        frame.setVisible(true);
    }
    
    private void createWindowControls() {
        // Close button (top-right corner)
        closeButton = createOverlayButton("X", AppConstants.ERROR_COLOR);
        closeButton.setBounds(1100 - 40, 6, 34, 34);
        closeButton.addActionListener(e -> System.exit(0));
        overlayPanel.add(closeButton);
        
        // Maximize button
        maxButton = createOverlayButton("O", new java.awt.Color(120, 120, 120));
        maxButton.setBounds(1100 - 76, 6, 34, 34);
        maxButton.addActionListener(e -> {
            if (frame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                // Restore to normal size
                frame.setExtendedState(JFrame.NORMAL);
                frame.setBounds(normalBounds);
            } else {
                // Save current bounds before maximizing
                normalBounds = frame.getBounds();
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });
        overlayPanel.add(maxButton);
        
        // Minimize button
        minButton = createOverlayButton("-", new java.awt.Color(120, 120, 120));
        minButton.setBounds(1100 - 112, 6, 34, 34);
        minButton.addActionListener(e -> frame.setState(JFrame.ICONIFIED));
        overlayPanel.add(minButton);
    }
    
    private JButton createOverlayButton(String text, java.awt.Color hoverColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setForeground(new java.awt.Color(60, 60, 60)); // Dark color
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(false);
        
        final java.awt.Color normalColor = new java.awt.Color(60, 60, 60);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(hoverColor);
            }
            public void mouseExited(MouseEvent e) {
                btn.setForeground(normalColor);
            }
        });
        return btn;
    }
    
    private void updateWindowControlPositions(int width) {
        if (closeButton != null) closeButton.setBounds(width - 40, 6, 34, 34);
        if (maxButton != null) maxButton.setBounds(width - 76, 6, 34, 34);
        if (minButton != null) minButton.setBounds(width - 112, 6, 34, 34);
    }
    
    private void enableWindowDrag() {
        // Allow dragging window from empty areas at the top
        mapViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Only allow drag from top 50px area
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

    public void setStopClickListener(StopClickListener listener) {
        this.stopClickListener = listener;
    }

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

    public void showFloatingPanel(String stopName, List<String> arrivi, Point2D pos) {
        showFloatingPanel(stopName, null, arrivi, false, pos, null);
    }
    
    public String getFloatingPanelStopId() {
        return floatingPanel.getCurrentStopId();
    }
    
    public boolean isFloatingPanelVisible() {
        return floatingPanel.isVisible();
    }
    
    public void refreshFloatingPanel(String stopName, String stopId, List<String> arrivi, boolean isFavorite) {
        floatingPanel.update(stopName, stopId, arrivi, isFavorite);
        floatingPanel.repaint();
    }
    
    public void updateFloatingPanelFavorite(boolean isFavorite) {
        floatingPanel.setFavoriteStatus(isFavorite);
    }
    
    public void setOnFavoriteToggle(Runnable callback) {
        floatingPanel.setOnFavoriteToggle(callback);
    }
    
    public void setOnViewAllTrips(Runnable callback) {
        floatingPanel.setOnViewAllTrips(callback);
    }
    
    public void showAllTripsInPanel(List<String> allTrips) {
        floatingPanel.showAllTripsView(allTrips);
    }
    
    public void showFavoritesInSearch(List<Stop> favorites) {
        searchOverlay.showFavorites(favorites);
    }

    public void hideFloatingPanel() {
        floatingPanel.setVisible(false);
        floatingAnchorGeo = null;
    }

    private void updateFloatingPanelPosition() {
        if (!floatingPanel.isVisible() || floatingAnchorGeo == null) return;

        Point2D p2d = mapViewer.convertGeoPositionToPoint(floatingAnchorGeo);
        if (p2d == null) return;

        Dimension pref = floatingPanel.getPreferredPanelSize();
        int panelWidth = pref.width;
        int panelHeight = pref.height;
        int x = (int) p2d.getX() - panelWidth / 2;
        int y = (int) p2d.getY() - panelHeight - 8;

        int maxX = Math.max(10, mapViewer.getWidth() - panelWidth - 10);
        int maxY = Math.max(10, mapViewer.getHeight() - panelHeight - 10);
        x = Math.max(10, Math.min(x, maxX));
        y = Math.max(10, Math.min(y, maxY));

        floatingPanel.setBounds(x, y, panelWidth, panelHeight);
        floatingPanel.revalidate();
        floatingPanel.repaint();
    }
}


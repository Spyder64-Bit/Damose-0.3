package damose.view;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import damose.model.Stop;
import damose.view.component.ConnectionButton;
import damose.view.component.RouteSidePanel;
import damose.view.component.ServiceQualityPanel;
import damose.view.map.MapFactory;

/**
 * Main application view and UI orchestration.
 */
public class MainView {

    private static final int INITIAL_WIDTH = 1100;
    private static final int INITIAL_HEIGHT = 750;
    private static final int LEFT_STACK_X = 10;
    private static final int LEFT_STACK_Y = 10;
    private static final int MAP_CONTROLS_WIDTH = 58;
    private static final int MAP_CONTROLS_HEIGHT = 280;
    private static final int ROUTE_PANEL_WIDTH = 236;
    private static final int ROUTE_PANEL_TOP = 48;
    private static final int ROUTE_PANEL_MARGIN = 12;

    private JFrame frame;
    private JXMapViewer mapViewer;
    private JButton searchButton;
    private JButton favoritesButton;
    private JButton busToggleButton;
    private JButton infoButton;
    private JPanel mapControlsPanel;
    private ConnectionButton connectionButton;
    private JPanel overlayPanel;
    private JLayeredPane layeredPane;
    private final BottomNoticePresenter bottomNoticePresenter = new BottomNoticePresenter();
    private ServiceQualityPanel serviceQualityPanel;
    private WindowControlsCoordinator windowControlsCoordinator;

    private final SearchOverlaySection searchOverlaySection = new SearchOverlaySection();
    private FloatingPanelSection floatingPanelSection;
    private RoutePanelSection routePanelSection;

    private List<Stop> allStopsCache = new ArrayList<>();
    private StopClickListener stopClickListener;

    private Runnable onFloatingPanelClose;
    private Runnable onFavoriteToggle;
    private Runnable onViewAllTrips;
    private Runnable onRoutePanelClose;
    private IntConsumer onRouteDirectionSelected;
    private Consumer<RouteSidePanel.VehicleMarker> onRouteVehicleSelected;
    private Consumer<Stop> onRouteStopSelected;

    private final PropertyChangeListener mapPositionSyncListener = evt -> {
        String name = evt.getPropertyName();
        if ("zoom".equals(name) || "center".equals(name) || "tileFactory".equals(name)) {
            updateFloatingPanelPosition();
        }
    };

    /**
     * Handles showSearchOverlay.
     */
    public void showSearchOverlay() {
        searchOverlaySection.showSearchOverlay();
    }

    /**
     * Updates the search data value.
     */
    public void setSearchData(List<Stop> stops, List<Stop> lines) {
        searchOverlaySection.setSearchData(stops, lines);
    }

    /**
     * Registers callback for search select.
     */
    public void setOnSearchSelect(Consumer<Stop> callback) {
        searchOverlaySection.setOnSearchSelect(callback);
    }

    public void setOnSearchFavoritesLoginRequired(Runnable callback) {
        searchOverlaySection.setOnFavoritesLoginRequired(callback);
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
        searchOverlaySection.showInfoOverlay();
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
        if (floatingPanelSection != null) {
            floatingPanelSection.setPreferredMaxRows(maxRows);
        }
    }

    public void showBottomNotice(String message) {
        bottomNoticePresenter.show(message);
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

        frame = MainFrameFactory.create(getClass(), "Damose", INITIAL_WIDTH, INITIAL_HEIGHT);

        mapViewer = MapFactory.createMapViewer();
        setupMapLayers();
        setupMapControlsPanel();
        setupWindowControls();
        setupServiceQualityPanel();
        setupLayerResizeListener();
        enableWindowDrag();
        setupFloatingPanel();
        setupPopupOverlays();
        setupRoutePanelSection();
        mapViewer.addPropertyChangeListener(mapPositionSyncListener);
        setFloatingPanelMaxRows(10);

        frame.setVisible(true);
    }

    private void setupMapLayers() {
        layeredPane = new JLayeredPane();
        frame.setContentPane(layeredPane);

        mapViewer.setBounds(0, 0, INITIAL_WIDTH, INITIAL_HEIGHT);
        layeredPane.add(mapViewer, JLayeredPane.DEFAULT_LAYER);

        overlayPanel = new JPanel(null);
        overlayPanel.setOpaque(false);
        overlayPanel.setBounds(0, 0, INITIAL_WIDTH, INITIAL_HEIGHT);
        layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);
        bottomNoticePresenter.attachTo(layeredPane);
    }

    private void setupMapControlsPanel() {
        MapControlsBuilder.Widgets mapControls = MapControlsBuilder.build(getClass(), this::showInfoOverlay);
        mapControlsPanel = mapControls.panel();
        searchButton = mapControls.searchButton();
        favoritesButton = mapControls.favoritesButton();
        busToggleButton = mapControls.busToggleButton();
        connectionButton = mapControls.connectionButton();
        infoButton = mapControls.infoButton();
        mapControlsPanel.setBounds(LEFT_STACK_X, LEFT_STACK_Y, MAP_CONTROLS_WIDTH, MAP_CONTROLS_HEIGHT);
        overlayPanel.add(mapControlsPanel);
    }

    private void setupServiceQualityPanel() {
        serviceQualityPanel = new ServiceQualityPanel();
        serviceQualityPanel.setBounds(15, INITIAL_HEIGHT - 65, 180, 50);
        overlayPanel.add(serviceQualityPanel);
    }

    private void setupLayerResizeListener() {
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = layeredPane.getWidth();
                int height = layeredPane.getHeight();
                mapViewer.setBounds(0, 0, width, height);
                overlayPanel.setBounds(0, 0, width, height);
                searchOverlaySection.updateBounds(width, height);

                if (windowControlsCoordinator != null) {
                    windowControlsCoordinator.updatePositions(width);
                }
                if (serviceQualityPanel != null) {
                    serviceQualityPanel.setBounds(15, height - 65, 180, 50);
                }
                if (routePanelSection != null) {
                    routePanelSection.updateBounds(width, height);
                }

                updateFloatingPanelPosition();
                bottomNoticePresenter.position();
            }
        });
    }

    private void setupFloatingPanel() {
        floatingPanelSection = new FloatingPanelSection(mapViewer, overlayPanel);
        if (onFavoriteToggle != null) {
            floatingPanelSection.setOnFavoriteToggle(onFavoriteToggle);
        }
        if (onViewAllTrips != null) {
            floatingPanelSection.setOnViewAllTrips(onViewAllTrips);
        }
        floatingPanelSection.setOnClose(() -> {
            if (onFloatingPanelClose != null) {
                onFloatingPanelClose.run();
            }
        });
    }

    private void setupPopupOverlays() {
        searchOverlaySection.initialize(layeredPane, INITIAL_WIDTH, INITIAL_HEIGHT);
    }

    private void setupRoutePanelSection() {
        routePanelSection = new RoutePanelSection(
                overlayPanel,
                INITIAL_WIDTH,
                INITIAL_HEIGHT,
                ROUTE_PANEL_WIDTH,
                ROUTE_PANEL_TOP,
                ROUTE_PANEL_MARGIN
        );
        routePanelSection.setOnClose(() -> {
            if (onRoutePanelClose != null) {
                onRoutePanelClose.run();
            }
        });
        routePanelSection.setOnDirectionSelected(directionId -> {
            if (onRouteDirectionSelected != null) {
                onRouteDirectionSelected.accept(directionId);
            }
        });
        routePanelSection.setOnVehicleMarkerSelected(marker -> {
            if (onRouteVehicleSelected != null) {
                onRouteVehicleSelected.accept(marker);
            }
        });
        routePanelSection.setOnStopSelected(stop -> {
            if (onRouteStopSelected != null) {
                onRouteStopSelected.accept(stop);
            }
        });
    }

    private void setupWindowControls() {
        windowControlsCoordinator = new WindowControlsCoordinator(frame, overlayPanel);
        windowControlsCoordinator.create(INITIAL_WIDTH);
    }

    private void enableWindowDrag() {
        MapWindowDragSupport.install(frame, mapViewer);
    }

    /**
     * Handles addWaypointClickListener.
     */
    public void addWaypointClickListener() {
        MapStopSelectionSupport.install(
                mapViewer,
                () -> allStopsCache,
                stop -> {
                    if (stopClickListener != null) {
                        stopClickListener.onStopClicked(stop);
                    }
                }
        );
    }

    public interface StopClickListener {
        void onStopClicked(Stop stop);
    }

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
        if (floatingPanelSection == null) {
            return;
        }
        floatingPanelSection.showStopPanel(stopName, stopId, arrivi, isFavorite, pos, anchorGeo);
    }

    /**
     * Handles showVehicleFloatingPanel.
     */
    public void showVehicleFloatingPanel(String panelTitle, List<String> rows, GeoPosition anchorGeo) {
        if (floatingPanelSection == null) {
            return;
        }
        floatingPanelSection.showVehiclePanel(panelTitle, rows, anchorGeo);
    }

    /**
     * Handles refreshVehicleFloatingPanel.
     */
    public void refreshVehicleFloatingPanel(String panelTitle, List<String> rows, GeoPosition anchorGeo) {
        if (floatingPanelSection == null) {
            return;
        }
        floatingPanelSection.refreshVehiclePanel(panelTitle, rows, anchorGeo);
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
        return floatingPanelSection != null ? floatingPanelSection.getCurrentStopId() : null;
    }

    /**
     * Returns whether floating panel visible.
     */
    public boolean isFloatingPanelVisible() {
        return floatingPanelSection != null && floatingPanelSection.isVisible();
    }

    /**
     * Handles refreshFloatingPanel.
     */
    public void refreshFloatingPanel(String stopName, String stopId, List<String> arrivi, boolean isFavorite) {
        if (floatingPanelSection == null) {
            return;
        }
        floatingPanelSection.refreshStopPanel(stopName, stopId, arrivi, isFavorite);
    }

    /**
     * Handles updateFloatingPanelFavorite.
     */
    public void updateFloatingPanelFavorite(boolean isFavorite) {
        if (floatingPanelSection != null) {
            floatingPanelSection.updateFavoriteStatus(isFavorite);
        }
    }

    /**
     * Registers callback for favorite toggle.
     */
    public void setOnFavoriteToggle(Runnable callback) {
        onFavoriteToggle = callback;
        if (floatingPanelSection != null) {
            floatingPanelSection.setOnFavoriteToggle(callback);
        }
    }

    /**
     * Registers callback for view all trips.
     */
    public void setOnViewAllTrips(Runnable callback) {
        onViewAllTrips = callback;
        if (floatingPanelSection != null) {
            floatingPanelSection.setOnViewAllTrips(callback);
        }
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
     * Registers callback for route vehicle selected.
     */
    public void setOnRouteVehicleSelected(Consumer<RouteSidePanel.VehicleMarker> callback) {
        this.onRouteVehicleSelected = callback;
    }

    /**
     * Registers callback for route stop selected.
     */
    public void setOnRouteStopSelected(Consumer<Stop> callback) {
        this.onRouteStopSelected = callback;
    }

    /**
     * Handles showAllTripsInPanel.
     */
    public void showAllTripsInPanel(List<String> allTrips) {
        if (floatingPanelSection != null) {
            floatingPanelSection.showAllTrips(allTrips);
        }
    }

    /**
     * Handles showFavoritesInSearch.
     */
    public void showFavoritesInSearch(List<Stop> favorites) {
        searchOverlaySection.showFavorites(favorites);
    }

    /**
     * Handles hideFloatingPanel.
     */
    public void hideFloatingPanel() {
        if (floatingPanelSection != null) {
            floatingPanelSection.hide();
        }
    }

    /**
     * Handles showRouteSidePanel.
     */
    public void showRouteSidePanel(String routeName, List<Stop> routeStops) {
        if (routePanelSection != null) {
            routePanelSection.showRoute(routeName, routeStops);
        }
    }

    /**
     * Updates the route side panel directions value.
     */
    public void setRouteSidePanelDirections(Map<Integer, String> directions, int selectedDirection) {
        if (routePanelSection != null) {
            routePanelSection.setDirections(directions, selectedDirection);
        }
    }

    /**
     * Handles updateRouteSidePanelVehicles.
     */
    public void updateRouteSidePanelVehicles(List<RouteSidePanel.VehicleMarker> markers) {
        if (routePanelSection != null) {
            routePanelSection.setVehicleMarkers(markers);
        }
    }

    /**
     * Handles hideRouteSidePanel.
     */
    public void hideRouteSidePanel() {
        if (routePanelSection != null) {
            routePanelSection.hide();
        }
    }

    private void updateFloatingPanelPosition() {
        if (floatingPanelSection != null) {
            floatingPanelSection.updatePosition();
        }
    }
}

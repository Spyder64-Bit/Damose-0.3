package damose.controller;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jxmapviewer.viewer.GeoPosition;

import damose.data.loader.RoutesLoader;
import damose.database.SessionManager;
import damose.model.ConnectionMode;
import damose.model.Route;
import damose.model.Stop;
import damose.model.VehiclePosition;
import damose.service.FavoritesService;
import damose.service.RealtimeService;
import damose.util.MemoryManager;
import damose.view.MainView;
import damose.view.map.MapOverlayManager;

/**
 * Main application controller and coordination logic.
 */
public class MainController {
    private final ControllerDataLoader dataLoader = new ControllerDataLoader();
    private final RealtimeUpdateScheduler realtimeScheduler = new RealtimeUpdateScheduler();
    private final RouteViewportNavigator routeViewport = new RouteViewportNavigator();
    private final BackgroundTaskRunner backgroundRunner = new BackgroundTaskRunner("main-controller");
    private final VehicleTrackingResolver vehicleTrackingResolver = new VehicleTrackingResolver();
    private final RoutePanelState routePanelState = new RoutePanelState();

    private ControllerDataContext dataContext;
    private RouteVehicleMarkerBuilder routeVehicleMarkerBuilder;
    private VehiclePanelInfoBuilder vehiclePanelInfoBuilder;
    private RoutePanelFlow routePanelFlow;
    private StopPanelFlow stopPanelFlow;
    private VehicleFollowFlow vehicleFollowFlow;
    private ConnectionMode mode = ConnectionMode.ONLINE;
    private boolean autoOfflineNoticeShown = false;
    private MainView view;
    private volatile long currentFeedTs = Instant.now().getEpochSecond();

    private List<Stop> linesList;

    /**
     * Handles start.
     */
    public void start() {
        System.out.println("Starting application...");

        dataContext = dataLoader.load();
        routeVehicleMarkerBuilder = new RouteVehicleMarkerBuilder(
                dataContext.getTripMatcher(),
                RoutesLoader::getRouteById
        );
        vehiclePanelInfoBuilder = new VehiclePanelInfoBuilder(dataContext, () -> currentFeedTs);

        view = new MainView();
        view.init();
        view.setAllStops(dataContext.getStops());
        routePanelFlow = new RoutePanelFlow(view, dataContext, routeViewport, routePanelState, this::refreshMapOverlay);
        stopPanelFlow = new StopPanelFlow(view, dataContext);
        vehicleFollowFlow = new VehicleFollowFlow(
                view,
                routeViewport,
                vehicleTrackingResolver,
                routePanelState,
                new FollowedVehicleState(),
                vehiclePanelInfoBuilder,
                () -> dataContext != null ? dataContext.getTripMatcher() : null
        );

        setupSearchPanel();

        FavoritesService.init(dataContext.getStops(), linesList);
        setupViewCallbacks();

        view.addWaypointClickListener();
        MapOverlayManager.updateMap(
                view.getMapViewer(),
                Collections.emptyList(),
                Collections.emptyList(),
                dataContext.getTrips()
        );

        checkInitialConnectionMode();
        MemoryManager.start();

        System.out.println("Application started successfully");
        System.out.println(MemoryManager.getMemoryInfo());
    }

    private void setupViewCallbacks() {
        setupStopClickListener();
        setupConnectionButton();
        setupFavoritesButton();
        setupFloatingPanelFavorite();
        setupFloatingPanelClose();
        setupRoutePanelClose();
        setupRouteDirectionSwitch();
        setupRouteVehicleSelection();
        setupRouteStopSelection();
        setupBusToggleButton();
    }

    private void checkInitialConnectionMode() {
        System.out.println("Checking RT data availability...");
        view.getConnectionButton().showConnecting();

        backgroundRunner.run(() -> {
            RealtimeService.setMode(ConnectionMode.ONLINE);
            RealtimeService.startPolling();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }

            boolean hasData = RealtimeService.hasRealTimeData();

            final boolean initialHasData = hasData;
            SwingUtilities.invokeLater(() -> {
                if (initialHasData) {
                    mode = ConnectionMode.ONLINE;
                    view.getConnectionButton().setOnline();
                    System.out.println("RT data available - Starting in Online mode");
                } else {
                    mode = ConnectionMode.OFFLINE;
                    RealtimeService.setMode(ConnectionMode.OFFLINE);
                    RealtimeService.stopPolling();
                    view.getConnectionButton().setOffline();
                    System.out.println("RT data not available - Starting in Offline mode");
                }

                refreshMapOverlay();
                startRealtimeUpdates();
            });
        });
    }

    private void setupConnectionButton() {
        view.getConnectionButton().setOnModeToggle(this::toggleConnectionMode);
    }

    private void setupFavoritesButton() {
        view.getFavoritesButton().addActionListener(e -> showFavoritesDialog());
    }

    private void setupBusToggleButton() {
        view.getBusToggleButton().addActionListener(e -> {
            boolean visible = MapOverlayManager.toggleBusesVisible();
            view.getBusToggleButton().setToolTipText(
                    visible ? "Nascondi autobus" : "Mostra autobus"
            );
            view.getBusToggleButton().setEnabled(true);
            view.getBusToggleButton().getModel().setPressed(!visible);
        });
    }

    private void setupFloatingPanelFavorite() {
        view.setOnFavoriteToggle(() -> {
            if (!canSaveFavorites()) {
                showFavoritesLoginRequiredPopup();
                return;
            }
            String stopId = view.getFloatingPanelStopId();
            if (stopId != null) {
                boolean isFav = FavoritesService.toggleFavorite(stopId);
                view.updateFloatingPanelFavorite(isFav);
            }
        });

        view.setOnViewAllTrips(() -> {
            String stopId = view.getFloatingPanelStopId();
            if (stopId != null) {
                List<String> allTrips = dataContext.getArrivalService()
                        .getAllTripsForStopToday(stopId, mode, currentFeedTs);
                view.showAllTripsInPanel(allTrips);
            }
        });
    }

    private void showFavoritesDialog() {
        List<Stop> favorites = FavoritesService.getAllFavorites();
        view.showFavoritesInSearch(favorites);
    }

    private boolean canSaveFavorites() {
        return SessionManager.isLoggedIn() && SessionManager.getCurrentUser() != null;
    }

    private void showFavoritesLoginRequiredPopup() {
        view.showBottomNotice(
                "Per salvare i preferiti devi creare un account.\n"
                        + "Chiudi l'applicazione, riaprila e crea un account se vuoi usare i preferiti."
        );
    }

    private void toggleConnectionMode() {
        ConnectionMode newMode = (mode == ConnectionMode.ONLINE)
                ? ConnectionMode.OFFLINE
                : ConnectionMode.ONLINE;

        System.out.println("Switching mode: " + mode + " -> " + newMode);
        view.getConnectionButton().showConnecting();

        if (newMode == ConnectionMode.ONLINE) {
            RealtimeService.setMode(ConnectionMode.ONLINE);
            RealtimeService.startPolling();

            backgroundRunner.run(() -> {
                try {
                    Thread.sleep(2500);

                    boolean hasData = RealtimeService.hasRealTimeData();
                    SwingUtilities.invokeLater(() -> {
                        if (hasData) {
                            mode = ConnectionMode.ONLINE;
                            view.getConnectionButton().setOnline();
                            autoOfflineNoticeShown = false;
                            System.out.println("Connected successfully - Online mode active");
                            if (stopPanelFlow != null) {
                                stopPanelFlow.refreshFloatingPanelIfVisible(mode, currentFeedTs);
                            }
                        } else {
                            mode = ConnectionMode.OFFLINE;
                            RealtimeService.setMode(ConnectionMode.OFFLINE);
                            RealtimeService.stopPolling();
                            view.getConnectionButton().setOffline();
                            System.out.println("Connection failed - Staying offline");

                            JOptionPane.showMessageDialog(
                                    view.getMapViewer(),
                                    "Dati Real-Time non disponibili.\nControlla la tua connessione.",
                                    "Connessione non riuscita",
                                    JOptionPane.WARNING_MESSAGE
                            );
                        }
                        refreshMapOverlay();
                    });
                } catch (InterruptedException ignored) {
                }
            });
        } else {
            mode = ConnectionMode.OFFLINE;
            RealtimeService.setMode(ConnectionMode.OFFLINE);
            RealtimeService.stopPolling();

            SwingUtilities.invokeLater(() -> {
                view.getConnectionButton().setOffline();
                refreshMapOverlay();
                if (stopPanelFlow != null) {
                    stopPanelFlow.refreshFloatingPanelIfVisible(mode, currentFeedTs);
                }
                System.out.println("Offline mode active");
            });
        }
    }

    private void setupStopClickListener() {
        view.setStopClickListener(stop -> {
            if (stop == null) return;
            handleStopSelection(stop, false);
        });
    }

    private void setupFloatingPanelClose() {
        view.setOnFloatingPanelClose(() -> {
            clearFollowedVehicle();
            MapOverlayManager.clearVisibleStops();
            refreshMapOverlay();
        });
    }

    private void setupRoutePanelClose() {
        view.setOnRoutePanelClose(() -> {
            clearFollowedVehicle(true);
            if (routePanelFlow != null) {
                routePanelFlow.closeRoutePanelOverlay(true);
            }
            MapOverlayManager.clearVisibleStops();
            refreshMapOverlay();
        });
    }

    private void setupRouteDirectionSwitch() {
        view.setOnRouteDirectionSelected(this::onRouteDirectionSelected);
    }

    private void setupRouteVehicleSelection() {
        view.setOnRouteVehicleSelected(marker -> {
            if (vehicleFollowFlow != null) {
                vehicleFollowFlow.onRouteVehicleSelected(marker);
            }
        });
    }

    private void setupRouteStopSelection() {
        view.setOnRouteStopSelected(this::onRouteStopSelected);
    }

    private void setupSearchPanel() {
        Map<String, Route> routesById = RoutesLoader.getRoutesById();
        linesList = LineSearchDataBuilder.build(routesById);

        view.setSearchData(dataContext.getStops(), linesList);
        view.getSearchButton().addActionListener(e -> view.showSearchOverlay());

        view.setOnSearchSelect(stop -> {
            if (stop != null) {
                handleStopSelection(stop, true);
            }
        });
        view.setOnSearchFavoritesLoginRequired(this::showFavoritesLoginRequiredPopup);
    }

    private void handleStopSelection(Stop stop, boolean fromSearch) {
        if (stop.isFakeLine()) {
            handleLineSelection(stop);
        } else {
            clearFollowedVehicle(true);
            if (routePanelFlow != null) {
                routePanelFlow.closeRoutePanelOverlay(false);
            }
            MapOverlayManager.setVisibleStops(Collections.singletonList(stop));
            if (fromSearch) {
                routeViewport.centerOnStopWithBottomAnchor(view.getMapViewer(), stop);
            } else {
                routeViewport.centerOnStop(view.getMapViewer(), stop);
            }
            if (stopPanelFlow != null) {
                stopPanelFlow.showFloatingArrivals(stop, mode, currentFeedTs);
            }
            refreshMapOverlay();
        }
    }

    private void handleLineSelection(Stop fakeLine) {
        clearFollowedVehicle(true);
        String routeId = fakeLine.getStopId();
        String routeName = fakeLine.getStopName();

        backgroundRunner.run(() -> {
            try {
                List<Integer> directions = dataContext.getRouteService().getDirectionsForRoute(routeId);
                Integer initialDirection = routePanelFlow != null
                        ? routePanelFlow.chooseInitialDirection(directions)
                        : null;

                List<Stop> routeStops = initialDirection == null
                        ? dataContext.getRouteService().getStopsForRoute(routeId)
                        : dataContext.getRouteService().getStopsForRouteAndDirection(routeId, initialDirection);
                List<GeoPosition> routeShape = initialDirection == null
                        ? dataContext.getRouteService().getShapeForRoute(routeId)
                        : dataContext.getRouteService().getShapeForRouteAndDirection(routeId, initialDirection);
                if (routeStops.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        if (routePanelFlow != null) {
                            routePanelFlow.resetRoutePanelUiState();
                        }
                    });
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    if (routePanelFlow != null) {
                        routePanelFlow.applyRouteSelectionStateAndView(
                                routeId,
                                routeName,
                                routeStops,
                                routeShape,
                                directions,
                                initialDirection,
                                true
                        );
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        view.getMapViewer(),
                        "Errore durante il caricamento della linea " + routeId + ".",
                        "Errore linea",
                        JOptionPane.ERROR_MESSAGE
                ));
                SwingUtilities.invokeLater(() -> {
                    if (routePanelFlow != null) {
                        routePanelFlow.resetRoutePanelUiState();
                    }
                });
            }
        });
    }

    private void onRouteDirectionSelected(int directionId) {
        if (routePanelState.isCircular()) return;
        clearFollowedVehicle(true);

        String routeId = routePanelState.routeId();
        String routeName = routePanelState.routeName();
        if (routeId == null || routeName == null) return;

        backgroundRunner.run(() -> {
            List<Stop> routeStops = dataContext.getRouteService().getStopsForRouteAndDirection(routeId, directionId);
            List<GeoPosition> routeShape = dataContext.getRouteService()
                    .getShapeForRouteAndDirection(routeId, directionId);
            if (routeStops.isEmpty()) return;

            SwingUtilities.invokeLater(() -> {
                List<Integer> directions = dataContext.getRouteService().getDirectionsForRoute(routeId);
                if (routePanelFlow != null) {
                    routePanelFlow.applyRouteSelectionStateAndView(
                            routeId,
                            routeName,
                            routeStops,
                            routeShape,
                            directions,
                            directionId,
                            false
                    );
                }
            });
        });
    }

    private void onRouteStopSelected(Stop stop) {
        if (stop == null) return;

        clearFollowedVehicle(true);
        MapOverlayManager.setVisibleStops(Collections.singletonList(stop));
        routeViewport.centerOnStop(view.getMapViewer(), stop);
        if (stopPanelFlow != null) {
            stopPanelFlow.showFloatingArrivals(stop, mode, currentFeedTs);
        }
        refreshMapOverlay();
    }

    private void clearFollowedVehicle() {
        if (vehicleFollowFlow != null) {
            vehicleFollowFlow.clearFollowedVehicle();
        }
    }

    private void clearFollowedVehicle(boolean hidePanel) {
        if (vehicleFollowFlow != null) {
            vehicleFollowFlow.clearFollowedVehicle(hidePanel);
        }
    }

    private void refreshMapOverlay() {
        realtimeScheduler.refreshMapOverlay(view, dataContext.getTrips(), mode, this::onVehiclePositionsUpdated);
    }

    private void startRealtimeUpdates() {
        realtimeScheduler.start(
                view,
                dataContext.getTrips(),
                dataContext.getStopTripMapper(),
                dataContext.getArrivalService(),
                () -> mode,
                ts -> currentFeedTs = ts,
                this::onVehiclePositionsUpdated,
                this::onRealtimeHealthChanged
        );
    }

    private void onVehiclePositionsUpdated(List<VehiclePosition> positions) {
        if (vehicleFollowFlow != null) {
            vehicleFollowFlow.onVehiclePositionsUpdated(positions, routeVehicleMarkerBuilder);
        }
    }

    private void onRealtimeHealthChanged(boolean healthy) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> onRealtimeHealthChanged(healthy));
            return;
        }

        if (mode != ConnectionMode.ONLINE) {
            return;
        }

        if (RealtimeService.getLastSuccessfulFetchEpochSeconds() == Long.MIN_VALUE) {
            return;
        }

        if (healthy) {
            autoOfflineNoticeShown = false;
            return;
        }

        mode = ConnectionMode.OFFLINE;
        RealtimeService.setMode(ConnectionMode.OFFLINE);
        RealtimeService.stopPolling();
        view.getConnectionButton().setOffline();
        if (!autoOfflineNoticeShown) {
            autoOfflineNoticeShown = true;
            view.showBottomNotice("Feed realtime non disponibile. Passaggio automatico in modalita offline.");
        }
        if (stopPanelFlow != null) {
            stopPanelFlow.refreshFloatingPanelIfVisible(mode, currentFeedTs);
        }
        refreshMapOverlay();
        System.out.println("Realtime feed unavailable - switched to Offline mode");
    }
}


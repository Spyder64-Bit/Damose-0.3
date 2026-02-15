package damose.controller;

import java.awt.geom.Point2D;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
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
import damose.model.VehicleType;
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

    private ControllerDataContext dataContext;
    private RouteVehicleMarkerBuilder routeVehicleMarkerBuilder;
    private ConnectionMode mode = ConnectionMode.ONLINE;
    private MainView view;
    private volatile long currentFeedTs = Instant.now().getEpochSecond();

    private List<Stop> linesList;
    private volatile String activeRoutePanelId;
    private volatile List<Stop> activeRoutePanelStops = Collections.emptyList();
    private volatile Integer activeRoutePanelDirection;
    private volatile String activeRoutePanelName;
    private volatile boolean activeRoutePanelCircular;

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

        view = new MainView();
        view.init();
        view.setAllStops(dataContext.getStops());

        setupSearchPanel();

        FavoritesService.init(dataContext.getStops(), linesList);
        setupStopClickListener();
        setupConnectionButton();
        setupFavoritesButton();
        setupFloatingPanelFavorite();
        setupFloatingPanelClose();
        setupRoutePanelClose();
        setupRouteDirectionSwitch();
        setupBusToggleButton();

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

    private void checkInitialConnectionMode() {
        System.out.println("Checking RT data availability...");
        view.getConnectionButton().showConnecting();

        new Thread(() -> {
            RealtimeService.setMode(ConnectionMode.ONLINE);
            RealtimeService.startPolling();

            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }

            boolean hasData = RealtimeService.hasRealTimeData();

            SwingUtilities.invokeLater(() -> {
                if (hasData) {
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
        }, "initial-connection-check").start();
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

            new Thread(() -> {
                try {
                    Thread.sleep(2500);

                    boolean hasData = RealtimeService.hasRealTimeData();
                    SwingUtilities.invokeLater(() -> {
                        if (hasData) {
                            mode = ConnectionMode.ONLINE;
                            view.getConnectionButton().setOnline();
                            System.out.println("Connected successfully - Online mode active");
                            refreshFloatingPanelIfVisible();
                        } else {
                            mode = ConnectionMode.OFFLINE;
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
            }, "connection-toggle-check").start();
        } else {
            mode = ConnectionMode.OFFLINE;
            RealtimeService.setMode(ConnectionMode.OFFLINE);
            RealtimeService.stopPolling();

            SwingUtilities.invokeLater(() -> {
                view.getConnectionButton().setOffline();
                refreshMapOverlay();
                refreshFloatingPanelIfVisible();
                System.out.println("Offline mode active");
            });
        }
    }

    private void refreshFloatingPanelIfVisible() {
        String stopId = view.getFloatingPanelStopId();
        if (stopId != null && view.isFloatingPanelVisible()) {
            Stop stop = findStopById(stopId);
            if (stop != null) {
                List<String> arrivi = dataContext.getArrivalService()
                        .computeArrivalsForStop(stopId, mode, currentFeedTs);
                boolean isFavorite = FavoritesService.isFavorite(stopId);
                view.refreshFloatingPanel(stop.getStopName(), stopId, arrivi, isFavorite);
            }
        }
    }

    private Stop findStopById(String stopId) {
        if (stopId == null || dataContext.getStops() == null) return null;
        for (Stop s : dataContext.getStops()) {
            if (stopId.equals(s.getStopId())) return s;
        }
        return null;
    }

    private void setupStopClickListener() {
        view.setStopClickListener(stop -> {
            if (stop == null) return;
            handleStopSelection(stop, false);
        });
    }

    private void setupFloatingPanelClose() {
        view.setOnFloatingPanelClose(() -> {
            MapOverlayManager.clearVisibleStops();
            refreshMapOverlay();
        });
    }

    private void setupRoutePanelClose() {
        view.setOnRoutePanelClose(() -> {
            activeRoutePanelId = null;
            activeRoutePanelStops = Collections.emptyList();
            activeRoutePanelDirection = null;
            activeRoutePanelName = null;
            activeRoutePanelCircular = false;
            view.hideRouteSidePanel();
            MapOverlayManager.clearRoute();
            MapOverlayManager.clearBusRouteFilter();
            MapOverlayManager.clearBusDirectionFilter();
            MapOverlayManager.clearVisibleStops();
            refreshMapOverlay();
        });
    }

    private void setupRouteDirectionSwitch() {
        view.setOnRouteDirectionSelected(this::onRouteDirectionSelected);
    }

    private void setupSearchPanel() {
        Map<String, Route> routesById = RoutesLoader.getRoutesById();
        linesList = routesById.values().stream()
                .map(route -> {
                    String shortName = safe(route.getRouteShortName());
                    String longName = safe(route.getRouteLongName());
                    String displayName = shortName.isEmpty() ? route.getRouteId() : shortName;
                    if (!longName.isEmpty() && !longName.equalsIgnoreCase(displayName)) {
                        displayName = displayName + " - " + longName;
                    }

                    Stop line = new Stop(
                            route.getRouteId(),
                            String.valueOf(route.getVehicleType().getGtfsCode()),
                            displayName,
                            0.0,
                            0.0
                    );
                    line.markAsFakeLine();
                    return line;
                })
                .sorted(Comparator.comparing(Stop::getStopName, String.CASE_INSENSITIVE_ORDER))
                .toList();

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
            activeRoutePanelId = null;
            activeRoutePanelStops = Collections.emptyList();
            activeRoutePanelDirection = null;
            activeRoutePanelName = null;
            activeRoutePanelCircular = false;
            view.hideRouteSidePanel();
            MapOverlayManager.clearRoute();
            MapOverlayManager.clearBusRouteFilter();
            MapOverlayManager.clearBusDirectionFilter();
            MapOverlayManager.setVisibleStops(Collections.singletonList(stop));
            if (fromSearch) {
                routeViewport.centerOnStopWithBottomAnchor(view.getMapViewer(), stop);
            } else {
                routeViewport.centerOnStop(view.getMapViewer(), stop);
            }
            showFloatingArrivals(stop);
            refreshMapOverlay();
        }
    }

    private void handleLineSelection(Stop fakeLine) {
        String routeId = fakeLine.getStopId();
        String routeName = fakeLine.getStopName();

        new Thread(() -> {
            try {
                List<Integer> directions = dataContext.getRouteService().getDirectionsForRoute(routeId);
                Integer initialDirection = chooseInitialDirection(directions);

                List<Stop> routeStops = initialDirection == null
                        ? dataContext.getRouteService().getStopsForRoute(routeId)
                        : dataContext.getRouteService().getStopsForRouteAndDirection(routeId, initialDirection);
                List<GeoPosition> routeShape = initialDirection == null
                        ? dataContext.getRouteService().getShapeForRoute(routeId)
                        : dataContext.getRouteService().getShapeForRouteAndDirection(routeId, initialDirection);
                if (routeStops.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        activeRoutePanelId = null;
                        activeRoutePanelStops = Collections.emptyList();
                        activeRoutePanelDirection = null;
                        activeRoutePanelName = null;
                        activeRoutePanelCircular = false;
                        view.hideRouteSidePanel();
                        MapOverlayManager.clearBusDirectionFilter();
                    });
                    return;
                }

                boolean circularRoute = isCircularRoute(routeStops);
                List<Integer> effectiveDirections = normalizeDirectionsForCircular(
                        directions, initialDirection, circularRoute
                );

                SwingUtilities.invokeLater(() -> {
                    activeRoutePanelId = routeId;
                    activeRoutePanelStops = new ArrayList<>(routeStops);
                    activeRoutePanelDirection = initialDirection;
                    activeRoutePanelName = routeName;
                    activeRoutePanelCircular = circularRoute;

                    Map<Integer, String> directionLabels = buildDirectionLabels(routeId, effectiveDirections);
                    view.setRouteSidePanelDirections(directionLabels, initialDirection != null ? initialDirection : 0);
                    view.showRouteSidePanel(routeName, routeStops);

                    MapOverlayManager.clearVisibleStops();
                    MapOverlayManager.setRouteStyleForVehicleType(resolveRouteVehicleType(routeId));
                    MapOverlayManager.setRoute(routeStops, routeShape);
                    MapOverlayManager.setBusRouteFilter(routeId);
                    MapOverlayManager.setBusDirectionFilter(initialDirection);
                    refreshMapOverlay();
                    routeViewport.fitMapToRoute(view.getMapViewer(), routeStops, activeRoutePanelId != null);
                    view.hideFloatingPanel();
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
                    activeRoutePanelId = null;
                    activeRoutePanelStops = Collections.emptyList();
                    activeRoutePanelDirection = null;
                    activeRoutePanelName = null;
                    activeRoutePanelCircular = false;
                    view.hideRouteSidePanel();
                    MapOverlayManager.clearBusDirectionFilter();
                });
            }
        }, "line-selection-" + routeId).start();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private VehicleType resolveRouteVehicleType(String routeId) {
        Route route = RoutesLoader.getRouteById(routeId);
        return route != null ? route.getVehicleType() : VehicleType.BUS;
    }

    private Integer chooseInitialDirection(List<Integer> directions) {
        if (directions == null || directions.isEmpty()) return null;
        if (directions.contains(0)) return 0;
        return directions.get(0);
    }

    private static boolean isCircularRoute(List<Stop> routeStops) {
        if (routeStops == null || routeStops.size() < 2) return false;

        Stop first = routeStops.get(0);
        Stop last = routeStops.get(routeStops.size() - 1);
        if (first == null || last == null) return false;

        String firstId = first.getStopId();
        String lastId = last.getStopId();
        if (firstId != null && lastId != null
                && firstId.trim().equalsIgnoreCase(lastId.trim())) {
            return true;
        }

        double latDiff = Math.abs(first.getStopLat() - last.getStopLat());
        double lonDiff = Math.abs(first.getStopLon() - last.getStopLon());
        return latDiff < 0.0001 && lonDiff < 0.0001;
    }

    private static List<Integer> normalizeDirectionsForCircular(List<Integer> directions,
                                                                Integer selectedDirection,
                                                                boolean circularRoute) {
        if (!circularRoute || directions == null || directions.size() <= 1) {
            return directions;
        }

        if (selectedDirection != null) {
            return List.of(selectedDirection);
        }
        return List.of(directions.get(0));
    }

    private Map<Integer, String> buildDirectionLabels(String routeId, List<Integer> directions) {
        Map<Integer, String> labels = new LinkedHashMap<>();
        if (directions == null) return labels;

        for (Integer direction : directions) {
            if (direction == null) continue;
            String headsign = safe(dataContext.getRouteService()
                    .getRepresentativeHeadsignForRouteAndDirection(routeId, direction));
            String label = !headsign.isEmpty() ? headsign : "Direzione";
            labels.put(direction, label);
        }
        return labels;
    }

    private void onRouteDirectionSelected(int directionId) {
        if (activeRoutePanelCircular) return;

        String routeId = activeRoutePanelId;
        String routeName = activeRoutePanelName;
        if (routeId == null || routeName == null) return;

        new Thread(() -> {
            List<Stop> routeStops = dataContext.getRouteService().getStopsForRouteAndDirection(routeId, directionId);
            List<GeoPosition> routeShape = dataContext.getRouteService()
                    .getShapeForRouteAndDirection(routeId, directionId);
            if (routeStops.isEmpty()) return;

            SwingUtilities.invokeLater(() -> {
                activeRoutePanelDirection = directionId;
                activeRoutePanelStops = new ArrayList<>(routeStops);
                activeRoutePanelCircular = isCircularRoute(routeStops);
                view.showRouteSidePanel(routeName, routeStops);

                List<Integer> directions = dataContext.getRouteService().getDirectionsForRoute(routeId);
                List<Integer> effectiveDirections = normalizeDirectionsForCircular(
                        directions, directionId, activeRoutePanelCircular
                );
                view.setRouteSidePanelDirections(buildDirectionLabels(routeId, effectiveDirections), directionId);

                MapOverlayManager.clearVisibleStops();
                MapOverlayManager.setRouteStyleForVehicleType(resolveRouteVehicleType(routeId));
                MapOverlayManager.setRoute(routeStops, routeShape);
                MapOverlayManager.setBusRouteFilter(routeId);
                MapOverlayManager.setBusDirectionFilter(directionId);
                refreshMapOverlay();
                routeViewport.fitMapToRoute(view.getMapViewer(), routeStops, activeRoutePanelId != null);
            });
        }, "route-direction-" + routeId + "-" + directionId).start();
    }

    private void showFloatingArrivals(Stop stop) {
        List<String> arrivi = dataContext.getArrivalService()
                .computeArrivalsForStop(stop.getStopId(), mode, currentFeedTs);
        boolean isFavorite = FavoritesService.isFavorite(stop.getStopId());
        showPanel(stop, arrivi, isFavorite);
    }

    private void showPanel(Stop stop, List<String> arrivi, boolean isFavorite) {
        GeoPosition anchorGeo = new GeoPosition(stop.getStopLat(), stop.getStopLon());
        Point2D p2d = view.getMapViewer().convertGeoPositionToPoint(anchorGeo);
        SwingUtilities.invokeLater(() -> view.showFloatingPanel(
                stop.getStopName(), stop.getStopId(), arrivi, isFavorite, p2d, anchorGeo));
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
                this::onVehiclePositionsUpdated
        );
    }

    private void onVehiclePositionsUpdated(List<VehiclePosition> positions) {
        String routeId = activeRoutePanelId;
        List<Stop> routeStops = activeRoutePanelStops;
        Integer directionFilter = activeRoutePanelDirection;
        if (routeVehicleMarkerBuilder == null || routeId == null || routeStops == null || routeStops.size() < 2) {
            return;
        }

        view.updateRouteSidePanelVehicles(
                routeVehicleMarkerBuilder.buildForRoute(positions, routeId, routeStops, directionFilter)
        );
    }
}


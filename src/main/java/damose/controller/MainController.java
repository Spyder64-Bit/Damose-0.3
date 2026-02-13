package damose.controller;

import java.awt.geom.Point2D;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jxmapviewer.viewer.GeoPosition;

import damose.model.ConnectionMode;
import damose.model.Stop;
import damose.service.FavoritesService;
import damose.service.RealtimeService;
import damose.util.MemoryManager;
import damose.view.MainView;
import damose.view.map.MapAnimator;
import damose.view.map.MapOverlayManager;

public class MainController {

    private final ControllerDataLoader dataLoader = new ControllerDataLoader();
    private final RealtimeUpdateScheduler realtimeScheduler = new RealtimeUpdateScheduler();

    private ControllerDataContext dataContext;
    private ConnectionMode mode = ConnectionMode.ONLINE;
    private MainView view;
    private volatile long currentFeedTs = Instant.now().getEpochSecond();

    private List<Stop> linesList;

    public void start() {
        System.out.println("Starting application...");

        dataContext = dataLoader.load();

        view = new MainView();
        view.init();
        view.setAllStops(dataContext.getStops());

        setupSearchPanel();

        FavoritesService.init(dataContext.getStops(), new ArrayList<>());
        setupStopClickListener();
        setupConnectionButton();
        setupFavoritesButton();
        setupFloatingPanelFavorite();
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
            handleStopSelection(stop);
        });
    }

    private void setupSearchPanel() {
        linesList = dataContext.getTrips().stream()
                .map(t -> t.getRouteId() + " - " + t.getTripHeadsign())
                .distinct()
                .map(lineName -> {
                    Stop fakeLine = new Stop(
                            "fake-" + lineName.replace(" ", ""),
                            "",
                            lineName,
                            0.0,
                            0.0
                    );
                    fakeLine.markAsFakeLine();
                    return fakeLine;
                })
                .collect(Collectors.toList());

        view.setSearchData(dataContext.getStops(), linesList);
        view.getSearchButton().addActionListener(e -> view.showSearchOverlay());

        view.setOnSearchSelect(stop -> {
            if (stop != null) {
                handleStopSelection(stop);
            }
        });
    }

    private void handleStopSelection(Stop stop) {
        if (stop.isFakeLine()) {
            handleLineSelection(stop);
        } else {
            MapOverlayManager.clearRoute();
            MapOverlayManager.clearBusRouteFilter();
            MapOverlayManager.setVisibleStops(Collections.singletonList(stop));
            centerOnStop(stop);
            showFloatingArrivals(stop);
            refreshMapOverlay();
        }
    }

    private void handleLineSelection(Stop fakeLine) {
        String lineName = fakeLine.getStopName();
        String[] parts = lineName.split(" - ", 2);
        String routeId = parts[0].trim();

        List<Stop> routeStops = dataContext.getRouteService().getStopsForRouteAndHeadsign(
                routeId,
                parts.length > 1 ? parts[1].trim() : null
        );

        if (routeStops.isEmpty()) {
            routeStops = dataContext.getRouteService().getStopsForRoute(routeId);
        }
        if (routeStops.isEmpty()) {
            return;
        }

        MapOverlayManager.clearVisibleStops();
        MapOverlayManager.setRoute(routeStops);
        MapOverlayManager.setBusRouteFilter(routeId);
        refreshMapOverlay();
        fitMapToRoute(routeStops);
        view.hideFloatingPanel();
    }

    private void centerOnStop(Stop stop) {
        if (stop.getStopLat() == 0.0 && stop.getStopLon() == 0.0) return;
        GeoPosition pos = new GeoPosition(stop.getStopLat(), stop.getStopLon());
        MapAnimator.flyTo(view.getMapViewer(), pos, 1, 2500, null);
    }

    private void fitMapToRoute(List<Stop> routeStops) {
        if (routeStops == null || routeStops.isEmpty()) return;

        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

        for (Stop s : routeStops) {
            minLat = Math.min(minLat, s.getStopLat());
            maxLat = Math.max(maxLat, s.getStopLat());
            minLon = Math.min(minLon, s.getStopLon());
            maxLon = Math.max(maxLon, s.getStopLon());
        }

        int middleIndex = routeStops.size() / 2;
        Stop middleStop = routeStops.get(middleIndex);
        GeoPosition centerPos = new GeoPosition(middleStop.getStopLat(), middleStop.getStopLon());

        double latDiff = maxLat - minLat;
        double lonDiff = maxLon - minLon;
        double maxDiff = Math.max(latDiff, lonDiff);

        int zoom;
        if (maxDiff > 0.3) zoom = 8;
        else if (maxDiff > 0.15) zoom = 7;
        else if (maxDiff > 0.08) zoom = 6;
        else if (maxDiff > 0.04) zoom = 5;
        else if (maxDiff > 0.02) zoom = 4;
        else zoom = 3;

        MapAnimator.flyTo(view.getMapViewer(), centerPos, zoom, 3000, null);
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
        realtimeScheduler.refreshMapOverlay(view, dataContext.getTrips(), mode);
    }

    private void startRealtimeUpdates() {
        realtimeScheduler.start(
                view,
                dataContext.getTrips(),
                dataContext.getStopTripMapper(),
                dataContext.getArrivalService(),
                () -> mode,
                ts -> currentFeedTs = ts
        );
    }
}


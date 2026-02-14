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

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import damose.data.loader.RoutesLoader;
import damose.model.ConnectionMode;
import damose.model.Route;
import damose.model.Stop;
import damose.model.Trip;
import damose.model.VehiclePosition;
import damose.model.VehicleType;
import damose.service.FavoritesService;
import damose.service.RealtimeService;
import damose.util.MemoryManager;
import damose.view.MainView;
import damose.view.component.RouteSidePanel;
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
    private volatile String activeRoutePanelId;
    private volatile List<Stop> activeRoutePanelStops = Collections.emptyList();
    private volatile Integer activeRoutePanelDirection;
    private volatile String activeRoutePanelName;

    public void start() {
        System.out.println("Starting application...");

        dataContext = dataLoader.load();

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
    }

    private void handleStopSelection(Stop stop, boolean fromSearch) {
        if (stop.isFakeLine()) {
            handleLineSelection(stop);
        } else {
            activeRoutePanelId = null;
            activeRoutePanelStops = Collections.emptyList();
            activeRoutePanelDirection = null;
            activeRoutePanelName = null;
            view.hideRouteSidePanel();
            MapOverlayManager.clearRoute();
            MapOverlayManager.clearBusRouteFilter();
            MapOverlayManager.clearBusDirectionFilter();
            MapOverlayManager.setVisibleStops(Collections.singletonList(stop));
            if (fromSearch) {
                centerOnStopWithBottomAnchor(stop);
            } else {
                centerOnStop(stop);
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
                if (routeStops.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        activeRoutePanelId = null;
                        activeRoutePanelStops = Collections.emptyList();
                        activeRoutePanelDirection = null;
                        activeRoutePanelName = null;
                        view.hideRouteSidePanel();
                        MapOverlayManager.clearBusDirectionFilter();
                    });
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    activeRoutePanelId = routeId;
                    activeRoutePanelStops = new ArrayList<>(routeStops);
                    activeRoutePanelDirection = initialDirection;
                    activeRoutePanelName = routeName;

                    Map<Integer, String> directionLabels = buildDirectionLabels(routeId, directions);
                    view.setRouteSidePanelDirections(directionLabels, initialDirection != null ? initialDirection : 0);
                    view.showRouteSidePanel(routeName, routeStops);

                    MapOverlayManager.clearVisibleStops();
                    MapOverlayManager.setRoute(routeStops);
                    MapOverlayManager.setBusRouteFilter(routeId);
                    MapOverlayManager.setBusDirectionFilter(initialDirection);
                    refreshMapOverlay();
                    fitMapToRoute(routeStops);
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
                    view.hideRouteSidePanel();
                    MapOverlayManager.clearBusDirectionFilter();
                });
            }
        }, "line-selection-" + routeId).start();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private Integer chooseInitialDirection(List<Integer> directions) {
        if (directions == null || directions.isEmpty()) return null;
        if (directions.contains(0)) return 0;
        return directions.get(0);
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
        String routeId = activeRoutePanelId;
        String routeName = activeRoutePanelName;
        if (routeId == null || routeName == null) return;

        new Thread(() -> {
            List<Stop> routeStops = dataContext.getRouteService().getStopsForRouteAndDirection(routeId, directionId);
            if (routeStops.isEmpty()) return;

            SwingUtilities.invokeLater(() -> {
                activeRoutePanelDirection = directionId;
                activeRoutePanelStops = new ArrayList<>(routeStops);
                view.showRouteSidePanel(routeName, routeStops);

                List<Integer> directions = dataContext.getRouteService().getDirectionsForRoute(routeId);
                view.setRouteSidePanelDirections(buildDirectionLabels(routeId, directions), directionId);

                MapOverlayManager.clearVisibleStops();
                MapOverlayManager.setRoute(routeStops);
                MapOverlayManager.setBusRouteFilter(routeId);
                MapOverlayManager.setBusDirectionFilter(directionId);
                refreshMapOverlay();
            });
        }, "route-direction-" + routeId + "-" + directionId).start();
    }

    private void centerOnStop(Stop stop) {
        if (stop.getStopLat() == 0.0 && stop.getStopLon() == 0.0) return;
        GeoPosition pos = new GeoPosition(stop.getStopLat(), stop.getStopLon());
        MapAnimator.flyTo(view.getMapViewer(), pos, 1, 2500, null);
    }

    private void centerOnStopWithBottomAnchor(Stop stop) {
        if (stop.getStopLat() == 0.0 && stop.getStopLon() == 0.0) return;

        JXMapViewer mapViewer = view.getMapViewer();
        GeoPosition stopPos = new GeoPosition(stop.getStopLat(), stop.getStopLon());
        int targetZoom = 1;
        GeoPosition targetCenter = computeBottomThirdCenter(mapViewer, stopPos, targetZoom);

        MapAnimator.flyTo(mapViewer, targetCenter, targetZoom, 2500, null);
    }

    private GeoPosition computeBottomThirdCenter(JXMapViewer mapViewer, GeoPosition stopPos, int zoom) {
        if (mapViewer == null || mapViewer.getTileFactory() == null) {
            return stopPos;
        }

        int mapHeight = mapViewer.getHeight();
        if (mapHeight <= 0) {
            return stopPos;
        }

        Point2D stopWorldPixel = mapViewer.getTileFactory().geoToPixel(stopPos, zoom);
        double desiredStopScreenY = mapHeight * (2.0 / 3.0);
        double centerWorldY = stopWorldPixel.getY() + (mapHeight / 2.0 - desiredStopScreenY);
        Point2D centerWorldPixel = new Point2D.Double(stopWorldPixel.getX(), centerWorldY);
        return mapViewer.getTileFactory().pixelToGeo(centerWorldPixel, zoom);
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
        if (routeId == null || routeStops == null || routeStops.size() < 2) {
            return;
        }

        Map<String, RouteSidePanel.VehicleMarker> byVehicle = new LinkedHashMap<>();
        for (VehiclePosition vp : positions) {
            if (vp == null || vp.getPosition() == null) continue;

            Trip trip = dataContext.getTripMatcher().matchByTripId(vp.getTripId());
            if (trip == null || !isSameRouteId(routeId, trip.getRouteId())) continue;
            if (directionFilter != null && trip.getDirectionId() != directionFilter) continue;

            double progress = computeRouteProgress(vp.getPosition(), routeStops);
            VehicleType vehicleType = resolveVehicleType(trip.getRouteId());
            String markerId = (vp.getVehicleId() == null || vp.getVehicleId().isBlank())
                    ? vp.getTripId()
                    : vp.getVehicleId();
            String routeCode = resolveRouteCode(trip.getRouteId());
            String vehicleKind = vehicleType == VehicleType.TRAM ? "TRAM" : "BUS";
            String markerTitle = vehicleKind;
            int progressPct = (int) Math.round(progress * 100.0);
            String markerDetails = "L:" + routeCode + "  ID:" + markerId + "  " + progressPct + "%";

            byVehicle.put(markerId, new RouteSidePanel.VehicleMarker(
                    progress, markerId, vehicleType, markerTitle, markerDetails));
        }

        List<RouteSidePanel.VehicleMarker> markers = new ArrayList<>(byVehicle.values());
        markers.sort(Comparator.comparingDouble(RouteSidePanel.VehicleMarker::getProgress));
        view.updateRouteSidePanelVehicles(markers);
    }

    private VehicleType resolveVehicleType(String routeId) {
        Route route = RoutesLoader.getRouteById(routeId);
        return route != null ? route.getVehicleType() : VehicleType.BUS;
    }

    private String resolveRouteCode(String routeId) {
        Route route = RoutesLoader.getRouteById(routeId);
        if (route == null) return routeId == null ? "" : routeId;
        String shortName = safe(route.getRouteShortName());
        return shortName.isEmpty() ? route.getRouteId() : shortName;
    }

    private static boolean isSameRouteId(String left, String right) {
        if (left == null || right == null) return false;
        String a = normalizeRouteId(left);
        String b = normalizeRouteId(right);
        return a.equalsIgnoreCase(b);
    }

    private static String normalizeRouteId(String routeId) {
        String trimmed = routeId == null ? "" : routeId.trim();
        if (trimmed.isEmpty()) return trimmed;
        if (!trimmed.chars().allMatch(Character::isDigit)) {
            return trimmed.toUpperCase();
        }

        int i = 0;
        while (i < trimmed.length() - 1 && trimmed.charAt(i) == '0') {
            i++;
        }
        return trimmed.substring(i);
    }

    private static double computeRouteProgress(GeoPosition pos, List<Stop> routeStops) {
        if (pos == null || routeStops == null || routeStops.size() < 2) return 0.0;

        double refLatRad = Math.toRadians(pos.getLatitude());
        double cosRef = Math.max(0.2, Math.cos(refLatRad));

        int n = routeStops.size();
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            Stop s = routeStops.get(i);
            xs[i] = s.getStopLon() * cosRef;
            ys[i] = s.getStopLat();
        }

        double[] prefixLen = new double[n];
        for (int i = 1; i < n; i++) {
            double dx = xs[i] - xs[i - 1];
            double dy = ys[i] - ys[i - 1];
            prefixLen[i] = prefixLen[i - 1] + Math.hypot(dx, dy);
        }

        double total = prefixLen[n - 1];
        if (total <= 0.0) return 0.0;

        double px = pos.getLongitude() * cosRef;
        double py = pos.getLatitude();
        double bestDist2 = Double.MAX_VALUE;
        double bestPathLen = 0.0;

        for (int i = 0; i < n - 1; i++) {
            double ax = xs[i];
            double ay = ys[i];
            double bx = xs[i + 1];
            double by = ys[i + 1];
            double dx = bx - ax;
            double dy = by - ay;
            double segLen2 = dx * dx + dy * dy;
            if (segLen2 <= 1e-12) continue;

            double t = ((px - ax) * dx + (py - ay) * dy) / segLen2;
            t = Math.max(0.0, Math.min(1.0, t));

            double qx = ax + t * dx;
            double qy = ay + t * dy;
            double dist2 = (px - qx) * (px - qx) + (py - qy) * (py - qy);

            if (dist2 < bestDist2) {
                bestDist2 = dist2;
                bestPathLen = prefixLen[i] + Math.sqrt(segLen2) * t;
            }
        }

        double progress = bestPathLen / total;
        return Math.max(0.0, Math.min(1.0, progress));
    }
}


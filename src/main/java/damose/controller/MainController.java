package damose.controller;

import java.awt.geom.Point2D;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jxmapviewer.viewer.GeoPosition;

import com.google.transit.realtime.GtfsRealtime;

import damose.data.loader.CalendarLoader;
import damose.data.loader.RoutesLoader;
import damose.data.loader.StopTimesLoader;
import damose.data.loader.StopsLoader;
import damose.data.loader.TripsLoader;
import damose.data.mapper.StopTripMapper;
import damose.data.mapper.TripMatcher;
import damose.data.model.Stop;
import damose.data.model.StopTime;
import damose.data.model.Trip;
import damose.data.model.TripServiceCalendar;
import damose.data.model.TripUpdateRecord;
import damose.data.model.VehiclePosition;
import damose.model.ConnectionMode;
import damose.service.ArrivalService;
import damose.service.FavoritesService;
import damose.service.GtfsParser;
import damose.service.RealtimeService;
import damose.service.RouteService;
import damose.service.ServiceQualityTracker;
import damose.ui.MainView;
import damose.ui.map.MapAnimator;
import damose.ui.map.MapOverlayManager;
import damose.util.MemoryManager;

/**
 * Main application controller.
 * Coordinates view, static GTFS data, and real-time updates.
 */
public class MainController {

    private List<Stop> stops;
    private List<Trip> trips;
    private List<StopTime> stopTimes;
    private TripMatcher matcher;
    private StopTripMapper stopTripMapper;
    private RouteService routeService;
    private ConnectionMode mode = ConnectionMode.ONLINE;
    private MainView view;

    private ArrivalService arrivalService;
    private Timer realtimeTimer;
    private long currentFeedTs = Instant.now().getEpochSecond();

    public void start() {
        System.out.println("Starting application...");

        // Load static data
        stops = StopsLoader.load();
        trips = TripsLoader.load();
        stopTimes = StopTimesLoader.load();
        RoutesLoader.load(); // Load routes for vehicle type detection

        System.out.println("Stops loaded: " + (stops == null ? 0 : stops.size()));
        System.out.println("Trips loaded: " + (trips == null ? 0 : trips.size()));

        matcher = new TripMatcher(trips);
        stopTripMapper = new StopTripMapper(stopTimes, matcher);
        routeService = new RouteService(trips, stopTimes, stops);

        // Load calendar_dates.txt
        TripServiceCalendar tripServiceCalendar;
        try {
            tripServiceCalendar = CalendarLoader.load();
        } catch (Exception e) {
            System.out.println("Could not load calendar_dates: " + e.getMessage());
            tripServiceCalendar = new TripServiceCalendar();
        }

        // Initialize ArrivalService
        arrivalService = new ArrivalService(matcher, stopTripMapper, tripServiceCalendar);

        // Initialize view
        view = new MainView();
        view.init();
        view.setAllStops(stops);

        setupSearchPanel();
        
        // Initialize favorites with both stops and lines
        FavoritesService.init(stops, linesList);
        setupStopClickListener();
        setupConnectionButton();
        setupFavoritesButton();
        setupFloatingPanelFavorite();
        setupBusToggleButton();

        view.addWaypointClickListener();
        MapOverlayManager.updateMap(view.getMapViewer(), Collections.emptyList(), Collections.emptyList(), trips);

        // Check for RT data availability and set initial mode
        // startRealtimeUpdates() is called after mode check completes
        checkInitialConnectionMode();
        
        // Start memory management
        MemoryManager.start();

        System.out.println("Application started successfully");
        System.out.println(MemoryManager.getMemoryInfo());
    }
    
    /**
     * Check if RT data is available at startup and set appropriate mode.
     * Starts realtime updates only after mode check completes.
     */
    private void checkInitialConnectionMode() {
        System.out.println("Checking RT data availability...");
        view.getConnectionButton().showConnecting();
        
        // Try to connect in background
        new Thread(() -> {
            RealtimeService.setMode(ConnectionMode.ONLINE);
            RealtimeService.startPolling();
            
            try {
                Thread.sleep(3000); // Give time for initial connection
            } catch (InterruptedException ignored) {}
            
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
                
                // Start realtime updates only after mode is properly determined
                startRealtimeUpdates();
            });
        }).start();
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
            // Dim the button when buses are hidden
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
                List<String> allTrips = arrivalService.getAllTripsForStopToday(stopId, mode, currentFeedTs);
                view.showAllTripsInPanel(allTrips);
            }
        });
    }
    
    private void showFavoritesDialog() {
        List<Stop> favorites = FavoritesService.getAllFavorites();
        // Always show the search with favorites tab, even if empty
        view.showFavoritesInSearch(favorites);
    }
    
    /**
     * Toggle between online and offline mode.
     */
    private void toggleConnectionMode() {
        ConnectionMode newMode = (mode == ConnectionMode.ONLINE) 
            ? ConnectionMode.OFFLINE 
            : ConnectionMode.ONLINE;
        
        System.out.println("Switching mode: " + mode + " -> " + newMode);
        
        // Show connecting animation
        view.getConnectionButton().showConnecting();
        
        if (newMode == ConnectionMode.ONLINE) {
            // Try to connect
            RealtimeService.setMode(ConnectionMode.ONLINE);
            RealtimeService.startPolling();
            
            // Check if connection succeeds after a delay
            new Thread(() -> {
                try {
                    Thread.sleep(2500); // Give time for connection
                    
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
                            
                            // Show notification
                            JOptionPane.showMessageDialog(
                                view.getMapViewer(),
                                "Dati Real-Time non disponibili.\nControlla la tua connessione.",
                                "Connessione non riuscita",
                                JOptionPane.WARNING_MESSAGE
                            );
                        }
                        refreshMapOverlay();
                    });
                } catch (InterruptedException ignored) {}
            }).start();
        } else {
            // Go offline immediately
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
    
    /**
     * Refresh the floating panel if it's currently visible to update RT/static data.
     */
    private void refreshFloatingPanelIfVisible() {
        String stopId = view.getFloatingPanelStopId();
        if (stopId != null && view.isFloatingPanelVisible()) {
            Stop stop = findStopById(stopId);
            if (stop != null) {
                List<String> arrivi = arrivalService.computeArrivalsForStop(stopId, mode, currentFeedTs);
                boolean isFavorite = FavoritesService.isFavorite(stopId);
                view.refreshFloatingPanel(stop.getStopName(), stopId, arrivi, isFavorite);
            }
        }
    }
    
    private Stop findStopById(String stopId) {
        if (stopId == null || stops == null) return null;
        for (Stop s : stops) {
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

    private List<Stop> linesList;
    
    private void setupSearchPanel() {
        // Prepare lines list
        linesList = trips.stream()
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

        view.setSearchData(stops, linesList);
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
            MapOverlayManager.clearBusRouteFilter(); // Show all buses when viewing a stop
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

        List<Stop> routeStops = routeService.getStopsForRouteAndHeadsign(routeId, 
            parts.length > 1 ? parts[1].trim() : null);

        if (routeStops.isEmpty()) {
            routeStops = routeService.getStopsForRoute(routeId);
        }

        if (routeStops.isEmpty()) {
            return;
        }

        // Clear any previously selected single stop
        MapOverlayManager.clearVisibleStops();
        MapOverlayManager.setRoute(routeStops);
        MapOverlayManager.setBusRouteFilter(routeId); // Only show buses of this route
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

        // Cinematic fly to center of route
        MapAnimator.flyTo(view.getMapViewer(), centerPos, zoom, 3000, null);
    }

    private void showFloatingArrivals(Stop stop) {
        List<String> arrivi = arrivalService.computeArrivalsForStop(stop.getStopId(), mode, currentFeedTs);
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
        List<VehiclePosition> positions = Collections.emptyList();
        
        if (mode == ConnectionMode.ONLINE) {
            GtfsRealtime.FeedMessage vpFeed = RealtimeService.getLatestVehiclePositions();
            if (vpFeed != null) {
                try {
                    positions = GtfsParser.parseVehiclePositions(vpFeed);
                } catch (Exception e) {
                    System.out.println("Error parsing vehicle positions: " + e.getMessage());
                }
            }
        }
        // In offline mode, no buses are shown

        final List<VehiclePosition> busPositions = positions;
        SwingUtilities.invokeLater(() -> MapOverlayManager.updateMap(
                view.getMapViewer(), Collections.emptyList(), busPositions, trips));
    }

    private void startRealtimeUpdates() {
        if (realtimeTimer != null) {
            realtimeTimer.cancel();
        }
        realtimeTimer = new Timer("realtime-updates", true);

        realtimeTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                GtfsRealtime.FeedMessage tuFeed = RealtimeService.getLatestTripUpdates();
                GtfsRealtime.FeedMessage vpFeed = RealtimeService.getLatestVehiclePositions();

                try {
                    long tsTU = (tuFeed != null && tuFeed.hasHeader() && tuFeed.getHeader().hasTimestamp())
                                ? tuFeed.getHeader().getTimestamp()
                                : Instant.now().getEpochSecond();
                    currentFeedTs = tsTU;
                } catch (Exception ignored) {}

                if (mode == ConnectionMode.ONLINE) {
                    try {
                        List<TripUpdateRecord> updates = GtfsParser.parseTripUpdates(tuFeed, stopTripMapper, currentFeedTs);
                        arrivalService.updateRealtimeArrivals(updates);
                    } catch (Exception ex) {
                        System.out.println("Error parsing TripUpdates RT: " + ex.getMessage());
                    }
                }

                List<VehiclePosition> computedPositions;
                try {
                    if (mode == ConnectionMode.ONLINE && vpFeed != null) {
                        computedPositions = GtfsParser.parseVehiclePositions(vpFeed);
                        System.out.println("Buses parsed: " + computedPositions.size());
                        
                        // Update service quality metrics
                        ServiceQualityTracker.getInstance().updateVehicleCount(computedPositions.size());
                    } else {
                        computedPositions = Collections.emptyList();
                        if (vpFeed == null) {
                            System.out.println("VehiclePositions feed is null");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error parsing VehiclePositions: " + e.getMessage());
                    computedPositions = Collections.emptyList();
                }

                final List<VehiclePosition> busPositions = computedPositions;
                SwingUtilities.invokeLater(() -> MapOverlayManager.updateMap(
                        view.getMapViewer(), Collections.emptyList(), busPositions, trips));
            }
        }, 0, 30_000);
    }
}


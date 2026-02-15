package damose.view.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import damose.config.AppConstants;
import damose.model.Stop;
import damose.model.Trip;
import damose.model.VehiclePosition;
import damose.model.BusWaypoint;
import damose.model.VehicleType;
import damose.view.render.RoutePainter;

/**
 * Map utility for map overlay manager.
 */
public class MapOverlayManager {

    private static final RoutePainter routePainter = new RoutePainter();
    private static final Color BUS_ROUTE_COLOR = new Color(48, 162, 236, 225);
    private static final Color BUS_ROUTE_OUTLINE_COLOR = new Color(0, 0, 0, 235);

    private static Set<String> currentStopIds = new HashSet<>();
    private static Set<String> currentBusIds = new HashSet<>();

    private static final List<Stop> routeStops = new ArrayList<>();
    private static final List<Stop> visibleStops = new ArrayList<>();
    private static final List<BusWaypoint> busWaypoints = new ArrayList<>();
    private static final TripLookupIndex tripLookupIndex = new TripLookupIndex();
    private static final VehicleOverlayRenderer vehicleOverlayRenderer = new VehicleOverlayRenderer();
    private static final StopOverlayRenderer stopOverlayRenderer = new StopOverlayRenderer();

    private static String busRouteFilter = null;
    private static Integer busDirectionFilter = null;
    private static String selectedVehicleMarkerId = null;

    private static boolean busesVisible = true;

    private static final Object lock = new Object();
    private static boolean initialized = false;
    private static JXMapViewer currentMap = null;

    private MapOverlayManager() {
    }

    private static void loadIcons() {
        vehicleOverlayRenderer.loadIcons();
        stopOverlayRenderer.loadIcons();
    }

    private static void initPainters(JXMapViewer mapViewer) {
        if (initialized && currentMap == mapViewer) return;

        loadIcons();

        mapViewer.setOverlayPainter((g, map, w, h) -> {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            synchronized (lock) {
                if (routePainter.hasRoute()) {
                    routePainter.paint(g2, map, w, h);
                }

                drawStops(g2, map);

                drawBuses(g2, map);
            }
        });

        initialized = true;
        currentMap = mapViewer;
    }

    private static void drawStops(Graphics2D g, JXMapViewer map) {
        stopOverlayRenderer.drawStops(g, map, visibleStops, routeStops);
    }

    private static void drawBuses(Graphics2D g, JXMapViewer map) {
        vehicleOverlayRenderer.drawVehicles(
                g,
                map,
                busWaypoints,
                busRouteFilter,
                busDirectionFilter,
                busesVisible,
                MapAnimator.isAnimating(),
                selectedVehicleMarkerId
        );
    }

    /**
     * Updates the bus route filter value.
     */
    public static void setBusRouteFilter(String routeId) {
        synchronized (lock) {
            busRouteFilter = routeId;
        }
        if (currentMap != null) {
            currentMap.repaint();
        }
    }

    /**
     * Returns the result of clearBusRouteFilter.
     */
    public static void clearBusRouteFilter() {
        setBusRouteFilter(null);
        setBusDirectionFilter(null);
    }

    /**
     * Updates the bus direction filter value.
     */
    public static void setBusDirectionFilter(Integer directionId) {
        synchronized (lock) {
            busDirectionFilter = directionId;
        }
        if (currentMap != null) {
            currentMap.repaint();
        }
    }

    /**
     * Returns the result of clearBusDirectionFilter.
     */
    public static void clearBusDirectionFilter() {
        setBusDirectionFilter(null);
    }

    /**
     * Updates the buses visible value.
     */
    public static void setBusesVisible(boolean visible) {
        synchronized (lock) {
            busesVisible = visible;
        }
        if (currentMap != null) {
            currentMap.repaint();
        }
    }

    /**
     * Returns the result of toggleBusesVisible.
     */
    public static boolean toggleBusesVisible() {
        synchronized (lock) {
            busesVisible = !busesVisible;
        }
        if (currentMap != null) {
            currentMap.repaint();
        }
        return busesVisible;
    }

    /**
     * Returns the result of areBusesVisible.
     */
    public static boolean areBusesVisible() {
        return busesVisible;
    }

    /**
     * Updates the selected vehicle marker id for map highlight.
     */
    public static void setSelectedVehicleMarkerId(String markerId) {
        synchronized (lock) {
            selectedVehicleMarkerId = trimToNull(markerId);
        }
        if (currentMap != null) {
            currentMap.repaint();
        }
    }

    /**
     * Clears selected vehicle marker highlight on map.
     */
    public static void clearSelectedVehicleMarkerId() {
        setSelectedVehicleMarkerId(null);
    }

    public static void updateMap(JXMapViewer mapViewer,
                                 List<Stop> allStops,
                                 List<VehiclePosition> busPositions,
                                 List<Trip> trips) {

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updateMap(mapViewer, allStops, busPositions, trips));
            return;
        }

        initPainters(mapViewer);
        boolean needsRepaint = false;

        synchronized (lock) {
            Set<String> showIds = new HashSet<>();
            for (Stop s : visibleStops) showIds.add(s.getStopId());
            for (Stop s : routeStops) showIds.add(s.getStopId());

            if (!showIds.equals(currentStopIds)) {
                currentStopIds = new HashSet<>(showIds);
                needsRepaint = true;
            }

            tripLookupIndex.ensureIndexed(trips);
            List<BusWaypoint> newBusWaypoints = new ArrayList<>();
            Set<String> newBusIds = new HashSet<>();
            Set<String> seenVehicleKeys = new HashSet<>();

            for (VehiclePosition vp : busPositions) {
                if (vp == null || vp.getPosition() == null) continue;

                String vehicleKey = trimToNull(vp.getVehicleId());
                if (vehicleKey == null) {
                    vehicleKey = trimToNull(vp.getTripId());
                }
                if (vehicleKey != null && !seenVehicleKeys.add(vehicleKey)) {
                    continue;
                }

                String vpRouteId = trimToNull(vp.getRouteId());
                Integer vpDirection = vp.getDirectionId() >= 0 ? vp.getDirectionId() : null;
                Trip trip = tripLookupIndex.findTrip(vp.getTripId(), vpRouteId, vpDirection);

                String headsign = (trip != null) ? trip.getTripHeadsign() : vp.getTripId();
                String routeId = vpRouteId != null ? vpRouteId : trimToNull((trip != null) ? trip.getRouteId() : null);
                int directionId = vpDirection != null ? vpDirection : (trip != null ? trip.getDirectionId() : -1);

                if (routeId == null) {
                    continue;
                }

                newBusWaypoints.add(new BusWaypoint(vp, headsign, routeId, directionId));
                if (vehicleKey != null) {
                    newBusIds.add(vehicleKey);
                }
            }

            if (!newBusWaypoints.isEmpty() || !currentBusIds.isEmpty()) {
                currentBusIds = newBusIds;
                busWaypoints.clear();
                busWaypoints.addAll(newBusWaypoints);
                needsRepaint = true;
            }
        }

        if (needsRepaint && currentMap != null) {
            currentMap.repaint();
        }
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Updates the visible stops value.
     */
    public static void setVisibleStops(List<Stop> stops) {
        synchronized (lock) {
            visibleStops.clear();
            if (stops != null) visibleStops.addAll(stops);
            currentStopIds.clear();
        }
    }

    /**
     * Returns the result of clearVisibleStops.
     */
    public static void clearVisibleStops() {
        synchronized (lock) {
            visibleStops.clear();
            currentStopIds.clear();
        }
    }

    /**
     * Updates the route value.
     */
    public static void setRoute(List<Stop> stops) {
        setRoute(stops, null);
    }

    /**
     * Updates the route value.
     */
    public static void setRoute(List<Stop> stops, List<GeoPosition> shapePath) {
        synchronized (lock) {
            routeStops.clear();
            currentStopIds.clear();

            if (stops == null || stops.size() < 2) {
                routePainter.clearRoute();
                return;
            }

            routeStops.addAll(stops);

            List<GeoPosition> positions = new ArrayList<>();
            if (shapePath != null && shapePath.size() >= 2) {
                positions.addAll(shapePath);
            } else {
                for (Stop stop : stops) {
                    positions.add(new GeoPosition(stop.getStopLat(), stop.getStopLon()));
                }
            }
            routePainter.setRoute(positions);
        }

        if (currentMap != null) {
            currentMap.repaint();
        }
    }

    /**
     * Updates route painter colors according to vehicle type.
     */
    public static void setRouteStyleForVehicleType(VehicleType vehicleType) {
        synchronized (lock) {
            if (vehicleType == VehicleType.BUS) {
                routePainter.setRouteColor(BUS_ROUTE_COLOR);
                routePainter.setOutlineColor(BUS_ROUTE_OUTLINE_COLOR);
            } else {
                routePainter.setRouteColor(AppConstants.ROUTE_COLOR);
                routePainter.setOutlineColor(AppConstants.ROUTE_OUTLINE_COLOR);
            }
        }
        if (currentMap != null) {
            currentMap.repaint();
        }
    }

    /**
     * Returns the result of clearRoute.
     */
    public static void clearRoute() {
        synchronized (lock) {
            routePainter.clearRoute();
            routeStops.clear();
            currentStopIds.clear();
        }
    }

    /**
     * Returns the result of hasActiveRoute.
     */
    public static boolean hasActiveRoute() {
        synchronized (lock) {
            return routePainter.hasRoute();
        }
    }

    /**
     * Returns the result of clearAll.
     */
    public static void clearAll() {
        clearRoute();
        clearVisibleStops();
    }
}


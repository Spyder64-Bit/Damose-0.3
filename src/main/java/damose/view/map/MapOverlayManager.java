package damose.view.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import damose.config.AppConstants;
import damose.data.mapper.TripIdUtils;
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

    private static final Map<String, List<Trip>> tripByExactId = new HashMap<>();
    private static final Map<String, List<Trip>> tripByNormalizedId = new HashMap<>();
    private static List<Trip> indexedTripsRef = null;

    private static String busRouteFilter = null;
    private static Integer busDirectionFilter = null;

    private static boolean busesVisible = true;

    private static final Object lock = new Object();
    private static boolean initialized = false;
    private static JXMapViewer currentMap = null;

    private static Image busIcon;
    private static Image busIconSmall;
    private static Image tramIcon;
    private static Image tramIconSmall;
    private static Image stopIcon;
    private static Image stopIconSmall;

    private MapOverlayManager() {
    }

    private static void loadIcons() {
        try {
            ImageIcon bus = new ImageIcon(MapOverlayManager.class.getResource("/sprites/bus.png"));
            busIcon = bus.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            busIconSmall = bus.getImage().getScaledInstance(26, 26, Image.SCALE_SMOOTH);
        } catch (Exception e) {
            System.out.println("Failed to load bus icon: " + e.getMessage());
        }

        try {
            ImageIcon tram = new ImageIcon(MapOverlayManager.class.getResource("/sprites/tram.png"));
            tramIcon = tram.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            tramIconSmall = tram.getImage().getScaledInstance(26, 26, Image.SCALE_SMOOTH);
        } catch (Exception e) {
            System.out.println("Failed to load tram icon: " + e.getMessage());
        }

        try {
            ImageIcon stop = new ImageIcon(MapOverlayManager.class.getResource("/sprites/stop.png"));
            stopIcon = stop.getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH);
            stopIconSmall = stop.getImage().getScaledInstance(22, 22, Image.SCALE_SMOOTH);
        } catch (Exception e) {
            System.out.println("Failed to load stop icon: " + e.getMessage());
        }
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
        if (visibleStops.isEmpty() && routeStops.isEmpty()) return;

        Rectangle2D viewport = map.getViewportBounds();
        int zoom = map.getZoom();

        int size;
        if (zoom >= 8) size = 10;
        else if (zoom >= 7) size = 14;
        else if (zoom >= 5) size = 22;
        else size = 36;

        Image icon = size <= 22 ? stopIconSmall : stopIcon;


        for (Stop stop : visibleStops) {
            if (stop == null) continue;

            GeoPosition pos = new GeoPosition(stop.getStopLat(), stop.getStopLon());
            Point2D worldPt = map.getTileFactory().geoToPixel(pos, zoom);
            int screenX = (int) (worldPt.getX() - viewport.getX());
            int screenY = (int) (worldPt.getY() - viewport.getY());

            if (screenX < -size || screenX > map.getWidth() + size ||
                screenY < -size || screenY > map.getHeight() + size) {
                continue;
            }

            if (icon != null) {
                g.drawImage(icon, screenX - size / 2, screenY - size / 2, size, size, null);
            } else {
                g.setColor(new Color(220, 50, 50));
                g.fillOval(screenX - size / 2, screenY - size / 2, size, size);
                g.setColor(Color.WHITE);
                g.drawOval(screenX - size / 2, screenY - size / 2, size, size);
            }
        }

        int minRouteDistancePx = routeStopMinDistancePx(zoom);
        List<Point2D> renderedRouteStops = new ArrayList<>();
        int lastIndex = routeStops.size() - 1;
        for (int i = 0; i < routeStops.size(); i++) {
            Stop stop = routeStops.get(i);
            if (stop == null) continue;

            boolean forceRender = (i == 0 || i == lastIndex);

            GeoPosition pos = new GeoPosition(stop.getStopLat(), stop.getStopLon());
            Point2D worldPt = map.getTileFactory().geoToPixel(pos, zoom);
            int screenX = (int) (worldPt.getX() - viewport.getX());
            int screenY = (int) (worldPt.getY() - viewport.getY());

            if (screenX < -size || screenX > map.getWidth() + size ||
                    screenY < -size || screenY > map.getHeight() + size) {
                continue;
            }

            if (!forceRender && minRouteDistancePx > 0
                    && isTooClose(screenX, screenY, renderedRouteStops, minRouteDistancePx)) {
                continue;
            }

            if (icon != null) {
                g.drawImage(icon, screenX - size / 2, screenY - size / 2, size, size, null);
            } else {
                g.setColor(new Color(220, 50, 50));
                g.fillOval(screenX - size / 2, screenY - size / 2, size, size);
                g.setColor(Color.WHITE);
                g.drawOval(screenX - size / 2, screenY - size / 2, size, size);
            }

            if (minRouteDistancePx > 0) {
                renderedRouteStops.add(new Point2D.Double(screenX, screenY));
            }
        }


    }

    private static void drawBuses(Graphics2D g, JXMapViewer map) {
        if (busWaypoints.isEmpty()) return;

        if (!busesVisible && busRouteFilter == null) return;

        Rectangle2D viewport = map.getViewportBounds();
        int zoom = map.getZoom();

        int size = (zoom > 5) ? 26 : 40;

        for (BusWaypoint wp : busWaypoints) {
            if (wp == null || wp.getPosition() == null) continue;

            if (busRouteFilter != null && !matchesRouteFilter(busRouteFilter, wp.getRouteId())) {
                continue;
            }
            if (busDirectionFilter != null && wp.getDirectionId() != busDirectionFilter) {
                continue;
            }

            Point2D worldPt = map.getTileFactory().geoToPixel(wp.getPosition(), zoom);
            int screenX = (int) (worldPt.getX() - viewport.getX());
            int screenY = (int) (worldPt.getY() - viewport.getY());

            if (screenX < -size || screenX > map.getWidth() + size ||
                screenY < -size || screenY > map.getHeight() + size) {
                continue;
            }

            Image icon = getVehicleIcon(wp.getVehicleType(), zoom);

            if (icon != null) {
                g.drawImage(icon, screenX - size / 2, screenY - size / 2, null);
            } else {
                g.setColor(wp.getVehicleType() == VehicleType.TRAM
                        ? new Color(255, 140, 0)
                        : new Color(0, 120, 255));
                g.fillOval(screenX - size / 2, screenY - size / 2, size, size);
                g.setColor(Color.WHITE);
                g.drawOval(screenX - size / 2, screenY - size / 2, size, size);
            }
        }
    }

    private static Image getVehicleIcon(VehicleType type, int zoom) {
        boolean small = zoom > 5;
        if (type == VehicleType.TRAM) {
            return small ? tramIconSmall : tramIcon;
        }
        return small ? busIconSmall : busIcon;
    }

    private static int routeStopMinDistancePx(int zoom) {
        if (zoom >= 8) return 48;
        if (zoom >= 7) return 34;
        if (zoom >= 6) return 24;
        return 0;
    }

    private static boolean isTooClose(int x, int y, List<Point2D> points, int minDistancePx) {
        int minDistance2 = minDistancePx * minDistancePx;
        for (Point2D p : points) {
            double dx = x - p.getX();
            double dy = y - p.getY();
            if ((dx * dx + dy * dy) < minDistance2) {
                return true;
            }
        }
        return false;
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

            ensureTripIndex(trips);
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
                Trip trip = findTrip(vp.getTripId(), vpRouteId, vpDirection);

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

    private static void ensureTripIndex(List<Trip> trips) {
        if (trips == null) {
            return;
        }
        if (indexedTripsRef == trips && !tripByExactId.isEmpty()) {
            return;
        }

        tripByExactId.clear();
        tripByNormalizedId.clear();
        indexedTripsRef = trips;

        for (Trip trip : trips) {
            if (trip == null) continue;
            String staticTripId = trip.getTripId();
            if (staticTripId == null || staticTripId.isBlank()) continue;

            tripByExactId.computeIfAbsent(staticTripId, k -> new ArrayList<>()).add(trip);
            String normalized = TripIdUtils.normalizeSimple(staticTripId);
            if (normalized != null && !normalized.isBlank()) {
                tripByNormalizedId.computeIfAbsent(normalized, k -> new ArrayList<>()).add(trip);
            }
        }
    }

    private static Trip findTrip(String tripId, String routeId, Integer directionId) {
        if (tripId == null || tripId.isBlank()) return null;

        Trip exact = chooseBestCandidate(tripByExactId.get(tripId), routeId, directionId);
        if (exact != null) {
            return exact;
        }

        Set<Trip> candidates = new LinkedHashSet<>();
        Set<String> rtVariants = TripIdUtils.generateVariants(tripId);
        for (String variant : rtVariants) {
            List<Trip> normalized = tripByNormalizedId.get(variant);
            if (normalized != null && !normalized.isEmpty()) {
                candidates.addAll(normalized);
            }
        }
        return chooseBestCandidate(new ArrayList<>(candidates), routeId, directionId);
    }

    private static Trip chooseBestCandidate(List<Trip> candidates, String routeId, Integer directionId) {
        if (candidates == null || candidates.isEmpty()) return null;

        String preferredRoute = trimToNull(routeId);
        if (preferredRoute != null) {
            List<Trip> routeMatches = new ArrayList<>();
            for (Trip t : candidates) {
                if (t == null || t.getRouteId() == null) continue;
                if (preferredRoute.equalsIgnoreCase(t.getRouteId().trim())) {
                    routeMatches.add(t);
                }
            }
            if (routeMatches.size() == 1) {
                return routeMatches.get(0);
            }
            if (routeMatches.size() > 1) {
                if (directionId != null && directionId >= 0) {
                    List<Trip> directional = new ArrayList<>();
                    for (Trip t : routeMatches) {
                        if (t != null && t.getDirectionId() == directionId) {
                            directional.add(t);
                        }
                    }
                    if (directional.size() == 1) {
                        return directional.get(0);
                    }
                }
                return null;
            }
            return null;
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        if (directionId != null && directionId >= 0) {
            List<Trip> directional = new ArrayList<>();
            for (Trip t : candidates) {
                if (t != null && t.getDirectionId() == directionId) {
                    directional.add(t);
                }
            }
            if (directional.size() == 1) {
                return directional.get(0);
            }
        }

        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean matchesRouteFilter(String filterRouteId, String candidateRouteId) {
        if (filterRouteId == null || candidateRouteId == null) return false;

        String filter = filterRouteId.trim();
        String candidate = candidateRouteId.trim();
        if (filter.isEmpty() || candidate.isEmpty()) return false;

        return filter.equalsIgnoreCase(candidate);
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


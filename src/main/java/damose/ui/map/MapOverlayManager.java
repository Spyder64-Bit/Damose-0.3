package damose.ui.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import damose.data.model.Stop;
import damose.data.model.Trip;
import damose.data.model.VehiclePosition;
import damose.model.BusWaypoint;
import damose.ui.render.RoutePainter;

/**
 * Manages map overlays including stops, buses, and routes.
 * All elements are drawn directly for reliability.
 */
public class MapOverlayManager {

    private static final RoutePainter routePainter = new RoutePainter();

    private static Set<String> currentStopIds = new HashSet<>();
    private static Set<String> currentBusIds = new HashSet<>();

    private static final List<Stop> routeStops = new ArrayList<>();
    private static final List<Stop> visibleStops = new ArrayList<>();
    private static final List<BusWaypoint> busWaypoints = new ArrayList<>();
    
    // Filter buses by route ID (null = show all)
    private static String busRouteFilter = null;
    
    // Global bus visibility (true = show, false = hide unless route filter is set)
    private static boolean busesVisible = true;

    private static final Object lock = new Object();
    private static boolean initialized = false;
    private static JXMapViewer currentMap = null;
    
    // Icons
    private static Image busIcon;
    private static Image busIconSmall;
    private static Image stopIcon;
    private static Image stopIconSmall;

    private MapOverlayManager() {
        // Utility class
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
                // Draw route line first (below stops)
                if (routePainter.hasRoute()) {
                    routePainter.paint(g2, map, w, h);
                }
                
                // Draw stops
                drawStops(g2, map);
                
                // Draw buses on top
                drawBuses(g2, map);
            }
        });

        initialized = true;
        currentMap = mapViewer;
    }
    
    private static void drawStops(Graphics2D g, JXMapViewer map) {
        List<Stop> allStops = new ArrayList<>();
        allStops.addAll(visibleStops);
        allStops.addAll(routeStops);
        
        if (allStops.isEmpty()) return;
        
        Rectangle2D viewport = map.getViewportBounds();
        int zoom = map.getZoom();
        
        Image icon = (zoom > 4) ? stopIconSmall : stopIcon;
        int size = (zoom > 4) ? 22 : 36;
        
        for (Stop stop : allStops) {
            if (stop == null) continue;
            
            GeoPosition pos = new GeoPosition(stop.getStopLat(), stop.getStopLon());
            Point2D worldPt = map.getTileFactory().geoToPixel(pos, zoom);
            int screenX = (int) (worldPt.getX() - viewport.getX());
            int screenY = (int) (worldPt.getY() - viewport.getY());
            
            // Skip if outside visible area
            if (screenX < -size || screenX > map.getWidth() + size ||
                screenY < -size || screenY > map.getHeight() + size) {
                continue;
            }
            
            if (icon != null) {
                g.drawImage(icon, screenX - size / 2, screenY - size / 2, null);
            } else {
                // Fallback: red circle
                g.setColor(new Color(220, 50, 50));
                g.fillOval(screenX - size / 2, screenY - size / 2, size, size);
                g.setColor(Color.WHITE);
                g.drawOval(screenX - size / 2, screenY - size / 2, size, size);
            }
        }
    }
    
    private static void drawBuses(Graphics2D g, JXMapViewer map) {
        if (busWaypoints.isEmpty()) return;
        
        // Don't draw if buses are hidden (unless a route filter is active)
        if (!busesVisible && busRouteFilter == null) return;
        
        Rectangle2D viewport = map.getViewportBounds();
        int zoom = map.getZoom();
        
        Image icon = (zoom > 5) ? busIconSmall : busIcon;
        int size = (zoom > 5) ? 26 : 40;
        
        for (BusWaypoint wp : busWaypoints) {
            if (wp == null || wp.getPosition() == null) continue;
            
            // Filter by route if set
            if (busRouteFilter != null && wp.getRouteId() != null) {
                if (!wp.getRouteId().equals(busRouteFilter)) {
                    continue;
                }
            }
            
            Point2D worldPt = map.getTileFactory().geoToPixel(wp.getPosition(), zoom);
            int screenX = (int) (worldPt.getX() - viewport.getX());
            int screenY = (int) (worldPt.getY() - viewport.getY());
            
            // Skip if outside visible area
            if (screenX < -size || screenX > map.getWidth() + size ||
                screenY < -size || screenY > map.getHeight() + size) {
                continue;
            }
            
            if (icon != null) {
                g.drawImage(icon, screenX - size / 2, screenY - size / 2, null);
            } else {
                // Fallback: blue circle
                g.setColor(new Color(0, 120, 255));
                g.fillOval(screenX - size / 2, screenY - size / 2, size, size);
                g.setColor(Color.WHITE);
                g.drawOval(screenX - size / 2, screenY - size / 2, size, size);
            }
        }
    }
    
    /**
     * Set route filter for buses. Only buses of this route will be shown.
     * @param routeId Route ID to filter by, or null to show all buses
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
     * Clear bus route filter to show all buses.
     */
    public static void clearBusRouteFilter() {
        setBusRouteFilter(null);
    }
    
    /**
     * Set global bus visibility.
     * @param visible true to show buses, false to hide (unless a route filter is active)
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
     * Toggle global bus visibility.
     * @return new visibility state
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
     * Check if buses are currently visible.
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
            // Check if stops changed
            Set<String> showIds = new HashSet<>();
            for (Stop s : visibleStops) showIds.add(s.getStopId());
            for (Stop s : routeStops) showIds.add(s.getStopId());

            if (!showIds.equals(currentStopIds)) {
                currentStopIds = new HashSet<>(showIds);
                needsRepaint = true;
            }

            // Update bus waypoints
            List<BusWaypoint> newBusWaypoints = new ArrayList<>();
            Set<String> newBusIds = new HashSet<>();
            
            for (VehiclePosition vp : busPositions) {
                Trip trip = findTrip(trips, vp.getTripId());
                String headsign = (trip != null) ? trip.getTripHeadsign() : vp.getTripId();
                String routeId = (trip != null) ? trip.getRouteId() : null;
                newBusWaypoints.add(new BusWaypoint(vp, headsign, routeId));
                if (vp.getVehicleId() != null) {
                    newBusIds.add(vp.getVehicleId());
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

    private static Trip findTrip(List<Trip> trips, String tripId) {
        if (tripId == null || trips == null) return null;
        for (Trip t : trips) {
            if (t.getTripId().equals(tripId)) return t;
        }
        return null;
    }

    public static void setVisibleStops(List<Stop> stops) {
        synchronized (lock) {
            visibleStops.clear();
            if (stops != null) visibleStops.addAll(stops);
            currentStopIds.clear();
        }
    }

    public static void clearVisibleStops() {
        synchronized (lock) {
            visibleStops.clear();
            currentStopIds.clear();
        }
    }

    public static void setRoute(List<Stop> stops) {
        synchronized (lock) {
            routeStops.clear();
            currentStopIds.clear();

            if (stops == null || stops.size() < 2) {
                routePainter.clearRoute();
                return;
            }

            routeStops.addAll(stops);

            List<GeoPosition> positions = new ArrayList<>();
            for (Stop stop : stops) {
                positions.add(new GeoPosition(stop.getStopLat(), stop.getStopLon()));
            }
            routePainter.setRoute(positions);
        }

        if (currentMap != null) {
            currentMap.repaint();
        }
    }

    public static void clearRoute() {
        synchronized (lock) {
            routePainter.clearRoute();
            routeStops.clear();
            currentStopIds.clear();
        }
    }

    public static boolean hasActiveRoute() {
        synchronized (lock) {
            return routePainter.hasRoute();
        }
    }

    public static void clearAll() {
        clearRoute();
        clearVisibleStops();
    }
}


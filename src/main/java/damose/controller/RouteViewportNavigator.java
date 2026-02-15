package damose.controller;

import java.awt.geom.Point2D;
import java.util.List;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import damose.model.Stop;
import damose.view.map.MapAnimator;

/**
 * Coordinates application flow for route viewport navigator.
 */
public final class RouteViewportNavigator {

    /**
     * Handles centerOnStop.
     */
    public void centerOnStop(JXMapViewer mapViewer, Stop stop) {
        if (!hasMapCoordinates(stop) || mapViewer == null) return;
        GeoPosition pos = new GeoPosition(stop.getStopLat(), stop.getStopLon());
        MapAnimator.flyTo(mapViewer, pos, 1, 2500, null);
    }

    /**
     * Handles centerOnStopWithBottomAnchor.
     */
    public void centerOnStopWithBottomAnchor(JXMapViewer mapViewer, Stop stop) {
        if (!hasMapCoordinates(stop) || mapViewer == null) return;

        GeoPosition stopPos = new GeoPosition(stop.getStopLat(), stop.getStopLon());
        int targetZoom = 1;
        GeoPosition targetCenter = computeBottomThirdCenter(mapViewer, stopPos, targetZoom);
        MapAnimator.flyTo(mapViewer, targetCenter, targetZoom, 2500, null);
    }

    /**
     * Handles focusOnVehicle.
     */
    public void focusOnVehicle(JXMapViewer mapViewer, GeoPosition vehiclePos, boolean animate) {
        if (mapViewer == null || vehiclePos == null) return;

        int zoom = mapViewer.getZoom();
        GeoPosition targetCenter = computeBottomThirdCenter(mapViewer, vehiclePos, zoom);
        if (animate) {
            MapAnimator.panTo(mapViewer, targetCenter, 850);
        } else {
            mapViewer.setCenterPosition(targetCenter);
            mapViewer.repaint();
        }
    }

    /**
     * Handles fitMapToRoute.
     */
    public void fitMapToRoute(JXMapViewer mapViewer, List<Stop> routeStops, boolean hasRoutePanel) {
        if (routeStops == null || routeStops.size() < 2) return;
        if (mapViewer == null || mapViewer.getTileFactory() == null) return;

        int mapWidth = Math.max(1, mapViewer.getWidth());
        int mapHeight = Math.max(1, mapViewer.getHeight());

        int outerPaddingPx = 38;
        int reservedRightPx = hasRoutePanel ? Math.min(300, Math.max(190, mapWidth / 5)) : 0;
        int usableWidth = Math.max(220, mapWidth - reservedRightPx - (outerPaddingPx * 2));
        int usableHeight = Math.max(180, mapHeight - (outerPaddingPx * 2));

        int targetZoom = findBestZoomForRoute(routeStops, mapViewer, usableWidth, usableHeight, 1, 17);
        if (targetZoom > 1) {
            Point2D tighterSpan = computeRouteBoundsSpan(routeStops, mapViewer, targetZoom - 1);
            if (tighterSpan != null
                    && tighterSpan.getX() <= usableWidth * 1.08
                    && tighterSpan.getY() <= usableHeight * 1.08) {
                targetZoom = targetZoom - 1;
            }
        }

        Point2D boundsCenter = computeRouteBoundsCenter(routeStops, mapViewer, targetZoom);
        if (boundsCenter == null) return;

        double visibleCenterX = outerPaddingPx + usableWidth / 2.0;
        double mapCenterX = mapWidth / 2.0;
        double shiftToVisibleArea = mapCenterX - visibleCenterX;

        Point2D targetCenterWorld = new Point2D.Double(
                boundsCenter.getX() + shiftToVisibleArea,
                boundsCenter.getY()
        );
        GeoPosition targetCenterGeo = mapViewer.getTileFactory().pixelToGeo(targetCenterWorld, targetZoom);
        MapAnimator.flyTo(mapViewer, targetCenterGeo, targetZoom, 3000, null);
    }

    private static boolean hasMapCoordinates(Stop stop) {
        return stop != null && !(stop.getStopLat() == 0.0 && stop.getStopLon() == 0.0);
    }

    private GeoPosition computeBottomThirdCenter(JXMapViewer mapViewer, GeoPosition stopPos, int zoom) {
        if (mapViewer.getTileFactory() == null) {
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

    private int findBestZoomForRoute(List<Stop> routeStops,
                                     JXMapViewer mapViewer,
                                     int maxSpanX,
                                     int maxSpanY,
                                     int minZoom,
                                     int maxZoom) {
        for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
            Point2D span = computeRouteBoundsSpan(routeStops, mapViewer, zoom);
            if (span == null) continue;

            if (span.getX() <= maxSpanX && span.getY() <= maxSpanY) {
                return zoom;
            }
        }
        return maxZoom;
    }

    private Point2D computeRouteBoundsCenter(List<Stop> routeStops, JXMapViewer mapViewer, int zoom) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (Stop stop : routeStops) {
            if (stop == null) continue;
            GeoPosition pos = new GeoPosition(stop.getStopLat(), stop.getStopLon());
            Point2D world = mapViewer.getTileFactory().geoToPixel(pos, zoom);
            minX = Math.min(minX, world.getX());
            minY = Math.min(minY, world.getY());
            maxX = Math.max(maxX, world.getX());
            maxY = Math.max(maxY, world.getY());
        }

        if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(maxX) || !Double.isFinite(maxY)) {
            return null;
        }

        return new Point2D.Double((minX + maxX) / 2.0, (minY + maxY) / 2.0);
    }

    private Point2D computeRouteBoundsSpan(List<Stop> routeStops, JXMapViewer mapViewer, int zoom) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (Stop stop : routeStops) {
            if (stop == null) continue;
            GeoPosition pos = new GeoPosition(stop.getStopLat(), stop.getStopLon());
            Point2D world = mapViewer.getTileFactory().geoToPixel(pos, zoom);
            minX = Math.min(minX, world.getX());
            minY = Math.min(minY, world.getY());
            maxX = Math.max(maxX, world.getX());
            maxY = Math.max(maxY, world.getY());
        }

        if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(maxX) || !Double.isFinite(maxY)) {
            return null;
        }

        return new Point2D.Double(maxX - minX, maxY - minY);
    }
}


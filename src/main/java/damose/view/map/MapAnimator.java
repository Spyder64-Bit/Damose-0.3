package damose.view.map;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import damose.config.AppConstants;

/**
 * Map utility for map animator.
 */
public final class MapAnimator {

    private static final int FRAME_INTERVAL_MS = 20;
    private static final int FLY_DURATION_MS = 2000;
    private static final int ZOOM_OUT_LEVELS = 1;
    private static final double LONG_HOP_DISTANCE_KM = 2.0;

    private static Timer activeTimer;
    private static Runnable onCompleteCallback;

    private MapAnimator() {
    }

    /**
     * Returns the result of flyTo.
     */
    public static void flyTo(JXMapViewer mapViewer, GeoPosition targetPos, int targetZoom) {
        flyTo(mapViewer, targetPos, targetZoom, FLY_DURATION_MS, null);
    }

    public static void flyTo(JXMapViewer mapViewer, GeoPosition targetPos, int targetZoom,
                             int durationMs, Runnable onComplete) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> flyTo(mapViewer, targetPos, targetZoom, durationMs, onComplete));
            return;
        }

        stopAnimation();

        GeoPosition startPos = mapViewer.getCenterPosition();
        int startZoom = mapViewer.getZoom();
        int finalZoom = targetZoom < 0 ? startZoom : targetZoom;

        if (isCloseEnough(startPos, targetPos) && startZoom == finalZoom) {
            mapViewer.setCenterPosition(targetPos);
            mapViewer.setZoom(finalZoom);
            mapViewer.repaint();
            if (onComplete != null) onComplete.run();
            return;
        }

        double distance = haversineDistance(startPos, targetPos);
        int pullBackZoom = computePullBackZoom(startZoom, finalZoom, distance);
        int effectiveDurationMs = computeDurationMs(durationMs, distance);

        final int midZoom = pullBackZoom;
        final long startTime = System.currentTimeMillis();
        onCompleteCallback = onComplete;

        activeTimer = new Timer(FRAME_INTERVAL_MS, null);
        activeTimer.setCoalesce(true);

        final int[] appliedZoom = {startZoom};
        final long[] lastZoomChangeMs = {System.currentTimeMillis()};

        activeTimer.addActionListener(e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double progress = Math.min(1.0, (double) elapsed / effectiveDurationMs);

            double smooth = smootherStep(progress);
            double centerT = smootherStep(smooth);

            double lat = lerp(startPos.getLatitude(), targetPos.getLatitude(), centerT);
            double lon = lerp(startPos.getLongitude(), targetPos.getLongitude(), centerT);

            double zoomFloat;
            if (smooth < 0.5) {
                double phase = smootherStep(smooth / 0.5);
                zoomFloat = lerp(startZoom, midZoom, phase);
            } else {
                double phase = smootherStep((smooth - 0.5) / 0.5);
                zoomFloat = lerp(midZoom, finalZoom, phase);
            }

            int minZoom = Math.min(Math.min(startZoom, finalZoom), midZoom);
            int maxZoom = Math.max(Math.max(startZoom, finalZoom), midZoom);
            int requestedZoom = (int) Math.round(Math.max(minZoom, Math.min(maxZoom, zoomFloat)));
            if (Math.abs(requestedZoom - appliedZoom[0]) > 1) {
                requestedZoom = appliedZoom[0] + Integer.signum(requestedZoom - appliedZoom[0]);
            }
            int zoom = appliedZoom[0];
            long now = System.currentTimeMillis();
            if (requestedZoom != appliedZoom[0] && (now - lastZoomChangeMs[0]) >= 120) {
                zoom = requestedZoom;
                appliedZoom[0] = requestedZoom;
                lastZoomChangeMs[0] = now;
            }
            if (progress >= 1.0) {
                zoom = finalZoom;
            }

            mapViewer.setCenterPosition(new GeoPosition(lat, lon));
            if (zoom != mapViewer.getZoom()) {
                mapViewer.setZoom(zoom);
            }

            if (progress >= 1.0) {
                finishAnimation(mapViewer, targetPos, finalZoom);
            }
        });

        activeTimer.start();
    }

    private static int computePullBackZoom(int startZoom, int finalZoom, double distanceKm) {
        if (distanceKm >= LONG_HOP_DISTANCE_KM) {
            return Math.max(finalZoom, AppConstants.ROME_OVERVIEW_ZOOM);
        }

        int maxZoom = Math.max(startZoom, finalZoom);
        int pullBackZoom = Math.min(maxZoom + ZOOM_OUT_LEVELS, AppConstants.MAX_ANIMATION_PULLBACK_ZOOM);

        if (distanceKm < 0.5) {
            pullBackZoom = Math.max(pullBackZoom - 1, maxZoom);
        }

        return pullBackZoom;
    }

    private static int computeDurationMs(int requestedDurationMs, double distanceKm) {
        int base = Math.max(700, requestedDurationMs);
        int distanceBonus = (int) Math.min(1100, Math.max(0, Math.round(distanceKm * 110.0)));
        int stretched = (int) Math.round((base + distanceBonus) * 1.22);
        return Math.min(4600, stretched);
    }

    /**
     * Returns the result of panTo.
     */
    public static void panTo(JXMapViewer mapViewer, GeoPosition targetPos, int durationMs) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> panTo(mapViewer, targetPos, durationMs));
            return;
        }

        stopAnimation();

        GeoPosition startPos = mapViewer.getCenterPosition();

        if (isCloseEnough(startPos, targetPos)) {
            mapViewer.setCenterPosition(targetPos);
            mapViewer.repaint();
            return;
        }

        final long startTime = System.currentTimeMillis();

        activeTimer = new Timer(FRAME_INTERVAL_MS, null);
        activeTimer.setCoalesce(true);

        activeTimer.addActionListener(e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double progress = Math.min(1.0, (double) elapsed / durationMs);
            double eased = smootherStep(smootherStep(progress));

            double lat = lerp(startPos.getLatitude(), targetPos.getLatitude(), eased);
            double lon = lerp(startPos.getLongitude(), targetPos.getLongitude(), eased);

            mapViewer.setCenterPosition(new GeoPosition(lat, lon));

            if (progress >= 1.0) {
                finishAnimation(mapViewer, targetPos, -1);
            }
        });

        activeTimer.start();
    }

    /**
     * Returns the result of animateTo.
     */
    public static void animateTo(JXMapViewer mapViewer, GeoPosition targetPos, int targetZoom) {
        flyTo(mapViewer, targetPos, targetZoom);
    }

    /**
     * Returns the result of animateTo.
     */
    public static void animateTo(JXMapViewer mapViewer, GeoPosition targetPos, int targetZoom, int durationMs) {
        flyTo(mapViewer, targetPos, targetZoom, durationMs, null);
    }

    /**
     * Returns the result of stopAnimation.
     */
    public static void stopAnimation() {
        if (activeTimer != null) {
            activeTimer.stop();
            activeTimer = null;
        }
        onCompleteCallback = null;
    }

    /**
     * Returns whether animating.
     */
    public static boolean isAnimating() {
        return activeTimer != null && activeTimer.isRunning();
    }

    private static void finishAnimation(JXMapViewer mapViewer, GeoPosition finalPos, int finalZoom) {
        stopAnimation();
        mapViewer.setCenterPosition(finalPos);
        if (finalZoom >= 0) {
            mapViewer.setZoom(finalZoom);
        }
        mapViewer.repaint();

        if (onCompleteCallback != null) {
            Runnable callback = onCompleteCallback;
            onCompleteCallback = null;
            callback.run();
        }
    }


    private static double smootherStep(double t) {
        double x = Math.max(0.0, Math.min(1.0, t));
        return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    private static boolean isCloseEnough(GeoPosition a, GeoPosition b) {
        double threshold = 0.00005;
        return Math.abs(a.getLatitude() - b.getLatitude()) < threshold
            && Math.abs(a.getLongitude() - b.getLongitude()) < threshold;
    }

    private static double haversineDistance(GeoPosition a, GeoPosition b) {
        double R = 6371;
        double dLat = Math.toRadians(b.getLatitude() - a.getLatitude());
        double dLon = Math.toRadians(b.getLongitude() - a.getLongitude());
        double lat1 = Math.toRadians(a.getLatitude());
        double lat2 = Math.toRadians(b.getLatitude());

        double x = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
        return R * c;
    }
}


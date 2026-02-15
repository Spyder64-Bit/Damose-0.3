package damose.view.map;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

/**
 * Map utility for map animator.
 */
public final class MapAnimator {

    private static final int FRAME_INTERVAL_MS = 33;
    private static final int FLY_DURATION_MS = 2000;
    private static final int ZOOM_OUT_LEVELS = 1;

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

        int maxZoom = Math.max(startZoom, finalZoom);
        int pullBackZoom = Math.min(maxZoom + ZOOM_OUT_LEVELS, 13);

        double distance = haversineDistance(startPos, targetPos);
        if (distance > 8.0) {
            pullBackZoom = Math.min(pullBackZoom + 1, 14);
        } else if (distance < 0.5) {
            pullBackZoom = Math.max(pullBackZoom - 1, maxZoom);
        }

        final int midZoom = pullBackZoom;
        final long startTime = System.currentTimeMillis();
        onCompleteCallback = onComplete;

        activeTimer = new Timer(FRAME_INTERVAL_MS, null);
        activeTimer.setCoalesce(true);

        activeTimer.addActionListener(e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double progress = Math.min(1.0, (double) elapsed / durationMs);

            double lat, lon;
            int zoom;

            if (progress < 0.35) {
                double phaseProgress = progress / 0.35;
                double eased = easeOutQuad(phaseProgress);

                lat = lerp(startPos.getLatitude(),
                          (startPos.getLatitude() + targetPos.getLatitude()) / 2, eased * 0.3);
                lon = lerp(startPos.getLongitude(),
                          (startPos.getLongitude() + targetPos.getLongitude()) / 2, eased * 0.3);
                zoom = (int) Math.round(lerp(startZoom, midZoom, eased));

            } else if (progress < 0.65) {
                double phaseProgress = (progress - 0.35) / 0.30;
                double eased = easeInOutSine(phaseProgress);

                double midLat = (startPos.getLatitude() + targetPos.getLatitude()) / 2;
                double midLon = (startPos.getLongitude() + targetPos.getLongitude()) / 2;

                lat = lerp(startPos.getLatitude() + (midLat - startPos.getLatitude()) * 0.3,
                          targetPos.getLatitude() - (targetPos.getLatitude() - midLat) * 0.3, eased);
                lon = lerp(startPos.getLongitude() + (midLon - startPos.getLongitude()) * 0.3,
                          targetPos.getLongitude() - (targetPos.getLongitude() - midLon) * 0.3, eased);
                zoom = midZoom;

            } else {
                double phaseProgress = (progress - 0.65) / 0.35;
                double eased = easeOutCubic(phaseProgress);

                double startLat = targetPos.getLatitude() -
                    (targetPos.getLatitude() - (startPos.getLatitude() + targetPos.getLatitude()) / 2) * 0.3;
                double startLon = targetPos.getLongitude() -
                    (targetPos.getLongitude() - (startPos.getLongitude() + targetPos.getLongitude()) / 2) * 0.3;

                lat = lerp(startLat, targetPos.getLatitude(), eased);
                lon = lerp(startLon, targetPos.getLongitude(), eased);
                zoom = (int) Math.round(lerp(midZoom, finalZoom, eased));
            }

            mapViewer.setCenterPosition(new GeoPosition(lat, lon));
            if (zoom != mapViewer.getZoom()) {
                mapViewer.setZoom(zoom);
            }
            mapViewer.repaint();

            if (progress >= 1.0) {
                finishAnimation(mapViewer, targetPos, finalZoom);
            }
        });

        activeTimer.start();
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
            double eased = easeOutCubic(progress);

            double lat = lerp(startPos.getLatitude(), targetPos.getLatitude(), eased);
            double lon = lerp(startPos.getLongitude(), targetPos.getLongitude(), eased);

            mapViewer.setCenterPosition(new GeoPosition(lat, lon));
            mapViewer.repaint();

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


    private static double easeOutCubic(double t) {
        return 1 - Math.pow(1 - t, 3);
    }

    private static double easeOutQuad(double t) {
        return 1 - (1 - t) * (1 - t);
    }

    private static double easeInOutSine(double t) {
        return -(Math.cos(Math.PI * t) - 1) / 2;
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


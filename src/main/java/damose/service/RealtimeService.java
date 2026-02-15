package damose.service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;

import com.google.transit.realtime.GtfsRealtime;

import damose.config.AppConstants;
import damose.model.ConnectionMode;

/**
 * Provides service logic for realtime service.
 */
public class RealtimeService {

    private static GtfsRealtime.FeedMessage latestVehiclePositions;
    private static GtfsRealtime.FeedMessage latestTripUpdates;

    private static Timer timer;
    private static ConnectionMode mode = ConnectionMode.ONLINE;
    private static Runnable onDataReceived;
    private static boolean dataReceivedOnce = false;
    private static volatile long lastSuccessfulFetchEpochSeconds = Long.MIN_VALUE;
    private static volatile int consecutiveFailures = 0;

    private RealtimeService() {
    }

    /**
     * Updates the mode value.
     */
    public static synchronized void setMode(ConnectionMode newMode) {
        mode = newMode;
        if (newMode == ConnectionMode.OFFLINE) {
            clearRealtimeCache();
            resetHealthState();
            dataReceivedOnce = false;
        } else {
            dataReceivedOnce = false;
            consecutiveFailures = 0;
        }
    }

    /**
     * Returns the mode.
     */
    public static ConnectionMode getMode() {
        return mode;
    }

    /**
     * Returns the result of startPolling.
     */
    public static synchronized void startPolling() {
        stopPolling();
        timer = new Timer("GTFSRealtimeUpdater", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            /**
             * Handles run.
             */
            public void run() {
                if (mode == ConnectionMode.ONLINE) {
                    fetchRealtimeFeeds();
                }
            }
        }, 0, AppConstants.RT_UPDATE_INTERVAL_MS);
    }

    /**
     * Returns the result of stopPolling.
     */
    public static synchronized void stopPolling() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (mode == ConnectionMode.OFFLINE) {
            clearRealtimeCache();
        }
    }

    /**
     * Returns the result of fetchRealtimeFeeds.
     */
    public static void fetchRealtimeFeeds() {
        boolean hadSuccessfulFetch = false;
        try {
            GtfsRealtime.FeedMessage parsed = fetchFeedFromUrl(AppConstants.VEHICLE_POSITIONS_URL);
            if (parsed != null) {
                latestVehiclePositions = parsed;
                hadSuccessfulFetch = true;
                System.out.println("VehiclePositions updated: header.ts="
                        + (parsed.hasHeader() && parsed.getHeader().hasTimestamp()
                        ? parsed.getHeader().getTimestamp() : "n/a"));
            }
        } catch (Exception e) {
            System.out.println("Error fetching VehiclePositions: " + e.getMessage());
        }

        try {
            GtfsRealtime.FeedMessage parsed = fetchFeedFromUrl(AppConstants.TRIP_UPDATES_URL);
            if (parsed != null) {
                latestTripUpdates = parsed;
                hadSuccessfulFetch = true;
                System.out.println("TripUpdates updated: header.ts="
                        + (parsed.hasHeader() && parsed.getHeader().hasTimestamp()
                        ? parsed.getHeader().getTimestamp() : "n/a"));
            }
        } catch (Exception e) {
            System.out.println("Error fetching TripUpdates: " + e.getMessage());
        }

        if (hadSuccessfulFetch) {
            lastSuccessfulFetchEpochSeconds = Instant.now().getEpochSecond();
            consecutiveFailures = 0;
            notifyDataReceived();
        } else {
            consecutiveFailures++;
        }
    }

    private static GtfsRealtime.FeedMessage fetchFeedFromUrl(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(AppConstants.HTTP_CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(AppConstants.HTTP_READ_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) damose-bus-tracker/1.0");
            conn.setRequestProperty("Accept", "application/x-protobuf, application/octet-stream, */*");
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            if (code != 200) {
                System.out.println("HTTP error: " + code + " for " + urlStr);
                return null;
            }

            if (conn.getContentLengthLong() == 0) {
                return null;
            }

            try (InputStream in = conn.getInputStream()) {
                return GtfsRealtime.FeedMessage.parseFrom(in);
            }
        } catch (java.io.IOException ex) {
            System.out.println("Error fetching/parsing from " + urlStr + ": " + ex.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Returns the latest vehicle positions.
     */
    public static GtfsRealtime.FeedMessage getLatestVehiclePositions() {
        return latestVehiclePositions;
    }

    /**
     * Returns the latest trip updates.
     */
    public static GtfsRealtime.FeedMessage getLatestTripUpdates() {
        return latestTripUpdates;
    }

    /**
     * Returns the result of hasRealTimeData.
     */
    public static boolean hasRealTimeData() {
        return latestTripUpdates != null || latestVehiclePositions != null;
    }

    /**
     * Returns last successful realtime fetch epoch-second.
     */
    public static long getLastSuccessfulFetchEpochSeconds() {
        return lastSuccessfulFetchEpochSeconds;
    }

    /**
     * Returns consecutive realtime fetch failures.
     */
    public static int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    /**
     * Returns whether realtime feed is considered healthy.
     */
    public static boolean isRealtimeHealthy(long nowEpochSeconds, long staleAfterSeconds) {
        if (mode != ConnectionMode.ONLINE) {
            return false;
        }
        long last = lastSuccessfulFetchEpochSeconds;
        if (last == Long.MIN_VALUE) {
            return false;
        }
        if (staleAfterSeconds <= 0) {
            return true;
        }
        long ageSeconds = nowEpochSeconds - last;
        return ageSeconds >= 0 && ageSeconds <= staleAfterSeconds;
    }

    /**
     * Registers callback for data received.
     */
    public static void setOnDataReceived(Runnable callback) {
        onDataReceived = callback;
    }

    private static void notifyDataReceived() {
        if (!dataReceivedOnce && hasRealTimeData()) {
            dataReceivedOnce = true;
            if (onDataReceived != null) {
                onDataReceived.run();
            }
        }
    }

    private static void clearRealtimeCache() {
        latestVehiclePositions = null;
        latestTripUpdates = null;
    }

    private static void resetHealthState() {
        lastSuccessfulFetchEpochSeconds = Long.MIN_VALUE;
        consecutiveFailures = 0;
    }
}


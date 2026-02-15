package damose.controller;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import com.google.transit.realtime.GtfsRealtime;

import damose.config.AppConstants;
import damose.data.mapper.StopTripMapper;
import damose.model.ConnectionMode;
import damose.model.Trip;
import damose.model.TripUpdateRecord;
import damose.model.VehiclePosition;
import damose.service.ArrivalService;
import damose.service.GtfsParser;
import damose.service.RealtimeService;
import damose.service.ServiceQualityTracker;
import damose.view.MainView;
import damose.view.map.MapOverlayManager;

/**
 * Coordinates application flow for realtime update scheduler.
 */
public final class RealtimeUpdateScheduler {

    private Timer timer;
    private volatile long lastTripUpdatesFeedTs = Long.MIN_VALUE;
    private volatile long lastVehiclePositionsFeedTs = Long.MIN_VALUE;
    private volatile List<VehiclePosition> lastVehiclePositions = Collections.emptyList();

    public void start(MainView view,
                      List<Trip> trips,
                      StopTripMapper stopTripMapper,
                      ArrivalService arrivalService,
                      Supplier<ConnectionMode> modeSupplier,
                      Consumer<Long> feedTimestampConsumer,
                      Consumer<List<VehiclePosition>> vehiclePositionsConsumer,
                      Consumer<Boolean> realtimeHealthConsumer) {
        stop();
        timer = new Timer("realtime-updates", true);
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            /**
             * Handles run.
             */
            public void run() {
                runCycle(view, trips, stopTripMapper, arrivalService, modeSupplier,
                        feedTimestampConsumer, vehiclePositionsConsumer, realtimeHealthConsumer);
            }
        }, 0, 30_000);
    }

    /**
     * Handles stop.
     */
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        lastTripUpdatesFeedTs = Long.MIN_VALUE;
        lastVehiclePositionsFeedTs = Long.MIN_VALUE;
        lastVehiclePositions = Collections.emptyList();
    }

    public void refreshMapOverlay(MainView view, List<Trip> trips, ConnectionMode mode,
                                  Consumer<List<VehiclePosition>> vehiclePositionsConsumer) {
        List<VehiclePosition> positions = Collections.emptyList();

        if (mode == ConnectionMode.ONLINE) {
            GtfsRealtime.FeedMessage vpFeed = RealtimeService.getLatestVehiclePositions();
            if (vpFeed != null) {
                try {
                    positions = getOrParseVehiclePositions(vpFeed);
                } catch (Exception e) {
                    System.out.println("Error parsing vehicle positions: " + e.getMessage());
                }
            }
        }

        final List<VehiclePosition> busPositions = positions;
        SwingUtilities.invokeLater(() -> {
            MapOverlayManager.updateMap(view.getMapViewer(), Collections.emptyList(), busPositions, trips);
            if (vehiclePositionsConsumer != null) {
                vehiclePositionsConsumer.accept(busPositions);
            }
        });
    }

    private void runCycle(MainView view,
                          List<Trip> trips,
                          StopTripMapper stopTripMapper,
                          ArrivalService arrivalService,
                          Supplier<ConnectionMode> modeSupplier,
                          Consumer<Long> feedTimestampConsumer,
                          Consumer<List<VehiclePosition>> vehiclePositionsConsumer,
                          Consumer<Boolean> realtimeHealthConsumer) {
        GtfsRealtime.FeedMessage tuFeed = RealtimeService.getLatestTripUpdates();
        GtfsRealtime.FeedMessage vpFeed = RealtimeService.getLatestVehiclePositions();

        long feedTs;
        try {
            feedTs = (tuFeed != null && tuFeed.hasHeader() && tuFeed.getHeader().hasTimestamp())
                    ? tuFeed.getHeader().getTimestamp()
                    : Instant.now().getEpochSecond();
        } catch (Exception ignored) {
            feedTs = Instant.now().getEpochSecond();
        }
        feedTimestampConsumer.accept(feedTs);

        ConnectionMode mode = modeSupplier.get();
        if (realtimeHealthConsumer != null) {
            long nowEpoch = Instant.now().getEpochSecond();
            boolean healthy = mode == ConnectionMode.ONLINE
                    && RealtimeService.isRealtimeHealthy(nowEpoch, AppConstants.RT_STALE_THRESHOLD_SECONDS);
            realtimeHealthConsumer.accept(healthy);
        }

        if (mode == ConnectionMode.ONLINE) {
            try {
                long tripFeedTs = extractFeedTimestamp(tuFeed, feedTs);
                if (tripFeedTs != lastTripUpdatesFeedTs) {
                    List<TripUpdateRecord> updates = GtfsParser.parseTripUpdates(tuFeed, stopTripMapper, feedTs);
                    arrivalService.updateRealtimeArrivals(updates, feedTs);
                    lastTripUpdatesFeedTs = tripFeedTs;
                }
            } catch (Exception ex) {
                System.out.println("Error parsing TripUpdates RT: " + ex.getMessage());
            }
        }

        List<VehiclePosition> computedPositions;
        try {
            if (mode == ConnectionMode.ONLINE && vpFeed != null) {
                computedPositions = getOrParseVehiclePositions(vpFeed);
                System.out.println("Buses parsed: " + computedPositions.size());
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
        SwingUtilities.invokeLater(() -> {
            MapOverlayManager.updateMap(view.getMapViewer(), Collections.emptyList(), busPositions, trips);
            if (vehiclePositionsConsumer != null) {
                vehiclePositionsConsumer.accept(busPositions);
            }
        });
    }

    private List<VehiclePosition> getOrParseVehiclePositions(GtfsRealtime.FeedMessage vpFeed) {
        long feedTs = extractFeedTimestamp(vpFeed, Long.MIN_VALUE);
        if (feedTs == lastVehiclePositionsFeedTs) {
            return lastVehiclePositions;
        }

        List<VehiclePosition> parsed = GtfsParser.parseVehiclePositions(vpFeed);
        lastVehiclePositionsFeedTs = feedTs;
        lastVehiclePositions = parsed != null ? parsed : Collections.emptyList();
        return lastVehiclePositions;
    }

    private static long extractFeedTimestamp(GtfsRealtime.FeedMessage feed, long fallback) {
        if (feed == null || !feed.hasHeader() || !feed.getHeader().hasTimestamp()) {
            return fallback;
        }
        return feed.getHeader().getTimestamp();
    }
}


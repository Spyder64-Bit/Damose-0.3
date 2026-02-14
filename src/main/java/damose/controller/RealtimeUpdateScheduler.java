package damose.controller;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import com.google.transit.realtime.GtfsRealtime;

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

public final class RealtimeUpdateScheduler {

    private Timer timer;

    public void start(MainView view,
                      List<Trip> trips,
                      StopTripMapper stopTripMapper,
                      ArrivalService arrivalService,
                      Supplier<ConnectionMode> modeSupplier,
                      Consumer<Long> feedTimestampConsumer,
                      Consumer<List<VehiclePosition>> vehiclePositionsConsumer) {
        stop();
        timer = new Timer("realtime-updates", true);
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                runCycle(view, trips, stopTripMapper, arrivalService, modeSupplier,
                        feedTimestampConsumer, vehiclePositionsConsumer);
            }
        }, 0, 30_000);
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void refreshMapOverlay(MainView view, List<Trip> trips, ConnectionMode mode,
                                  Consumer<List<VehiclePosition>> vehiclePositionsConsumer) {
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
                          Consumer<List<VehiclePosition>> vehiclePositionsConsumer) {
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
        if (mode == ConnectionMode.ONLINE) {
            try {
                List<TripUpdateRecord> updates = GtfsParser.parseTripUpdates(tuFeed, stopTripMapper, feedTs);
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
}


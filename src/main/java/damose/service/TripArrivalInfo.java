package damose.service;

import java.time.LocalTime;

/**
 * Arrival tuple for a specific trip at a stop.
 */
final class TripArrivalInfo {
    final String routeId;
    final String headsign;
    final LocalTime arrivalTime;
    final long scheduledEpoch;
    Long predictedEpoch;

    TripArrivalInfo(String routeId,
                    String headsign,
                    LocalTime arrivalTime,
                    long scheduledEpoch,
                    Long predictedEpoch) {
        this.routeId = routeId;
        this.headsign = headsign;
        this.arrivalTime = arrivalTime;
        this.scheduledEpoch = scheduledEpoch;
        this.predictedEpoch = predictedEpoch;
    }
}

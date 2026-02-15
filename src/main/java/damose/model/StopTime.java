package damose.model;

import java.time.LocalTime;

/**
 * Domain model for stop time.
 */
public class StopTime {

    private final String tripId;
    private final LocalTime arrivalTime;
    private final LocalTime departureTime;
    private final String stopId;
    private final int stopSequence;
    private final String stopHeadsign;
    private final int pickupType;
    private final int dropOffType;
    private final double shapeDistTraveled;
    private final int timepoint;

    public StopTime(String tripId, LocalTime arrivalTime, LocalTime departureTime,
                    String stopId, int stopSequence, String stopHeadsign,
                    int pickupType, int dropOffType, double shapeDistTraveled, int timepoint) {
        this.tripId = tripId;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.stopId = stopId;
        this.stopSequence = stopSequence;
        this.stopHeadsign = stopHeadsign;
        this.pickupType = pickupType;
        this.dropOffType = dropOffType;
        this.shapeDistTraveled = shapeDistTraveled;
        this.timepoint = timepoint;
    }

    /**
     * Returns the trip id.
     */
    public String getTripId() {
        return tripId;
    }

    /**
     * Returns the arrival time.
     */
    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    /**
     * Returns the departure time.
     */
    public LocalTime getDepartureTime() {
        return departureTime;
    }

    /**
     * Returns the stop id.
     */
    public String getStopId() {
        return stopId;
    }

    /**
     * Returns the stop sequence.
     */
    public int getStopSequence() {
        return stopSequence;
    }

    /**
     * Returns the stop headsign.
     */
    public String getStopHeadsign() {
        return stopHeadsign;
    }

    /**
     * Returns the pickup type.
     */
    public int getPickupType() {
        return pickupType;
    }

    /**
     * Returns the drop off type.
     */
    public int getDropOffType() {
        return dropOffType;
    }

    /**
     * Returns the shape dist traveled.
     */
    public double getShapeDistTraveled() {
        return shapeDistTraveled;
    }

    /**
     * Returns the timepoint.
     */
    public int getTimepoint() {
        return timepoint;
    }

    @Override
    /**
     * Returns the result of toString.
     */
    public String toString() {
        return tripId + " @ " + stopId + " ???????? " + arrivalTime;
    }
}


package damose.model;

/**
 * Domain model for trip update record.
 */
public class TripUpdateRecord {

    private final String tripId;
    private final String routeId;
    private final String stopId;
    private final long arrivalEpochSeconds;

    public TripUpdateRecord(String tripId, String routeId, String stopId, long arrivalEpochSeconds) {
        this.tripId = tripId;
        this.routeId = routeId;
        this.stopId = stopId;
        this.arrivalEpochSeconds = arrivalEpochSeconds;
    }

    /**
     * Returns the trip id.
     */
    public String getTripId() {
        return tripId;
    }

    /**
     * Returns the route id.
     */
    public String getRouteId() {
        return routeId;
    }

    /**
     * Returns the stop id.
     */
    public String getStopId() {
        return stopId;
    }

    /**
     * Returns the arrival epoch seconds.
     */
    public long getArrivalEpochSeconds() {
        return arrivalEpochSeconds;
    }

    @Override
    /**
     * Returns the result of toString.
     */
    public String toString() {
        return "TripUpdate{tripId='" + tripId + "', routeId='" + routeId
                + "', stopId='" + stopId + "', arrival=" + arrivalEpochSeconds + "}";
    }
}


package damose.model;

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

    public String getTripId() {
        return tripId;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getStopId() {
        return stopId;
    }

    public long getArrivalEpochSeconds() {
        return arrivalEpochSeconds;
    }

    @Override
    public String toString() {
        return "TripUpdate{tripId='" + tripId + "', routeId='" + routeId
                + "', stopId='" + stopId + "', arrival=" + arrivalEpochSeconds + "}";
    }
}

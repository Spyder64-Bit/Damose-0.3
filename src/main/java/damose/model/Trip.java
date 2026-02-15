package damose.model;

/**
 * Domain model for trip.
 */
public class Trip {

    private final String routeId;
    private final String serviceId;
    private final String tripId;
    private final String tripHeadsign;
    private final String tripShortName;
    private final int directionId;
    private final String shapeId;

    public Trip(String routeId, String serviceId, String tripId,
                String tripHeadsign, String tripShortName, int directionId, String shapeId) {
        this.routeId = routeId;
        this.serviceId = serviceId;
        this.tripId = tripId;
        this.tripHeadsign = tripHeadsign;
        this.tripShortName = tripShortName;
        this.directionId = directionId;
        this.shapeId = shapeId;
    }

    /**
     * Returns the route id.
     */
    public String getRouteId() {
        return routeId;
    }

    /**
     * Returns the service id.
     */
    public String getServiceId() {
        return serviceId;
    }

    /**
     * Returns the trip id.
     */
    public String getTripId() {
        return tripId;
    }

    /**
     * Returns the trip headsign.
     */
    public String getTripHeadsign() {
        return tripHeadsign;
    }

    /**
     * Returns the trip short name.
     */
    public String getTripShortName() {
        return tripShortName;
    }

    /**
     * Returns the direction id.
     */
    public int getDirectionId() {
        return directionId;
    }

    /**
     * Returns the shape id.
     */
    public String getShapeId() {
        return shapeId;
    }

    @Override
    /**
     * Returns the result of toString.
     */
    public String toString() {
        return "Trip{routeId='" + routeId + "', tripId='" + tripId + "', headsign='" + tripHeadsign + "'}";
    }

    @Override
    /**
     * Returns the result of equals.
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trip trip = (Trip) o;
        return tripId != null ? tripId.equals(trip.tripId) : trip.tripId == null;
    }

    @Override
    /**
     * Returns the result of hashCode.
     */
    public int hashCode() {
        return tripId != null ? tripId.hashCode() : 0;
    }
}


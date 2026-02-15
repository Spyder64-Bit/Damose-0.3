package damose.model;

import org.jxmapviewer.viewer.GeoPosition;

/**
 * Domain model for vehicle position.
 */
public class VehiclePosition {

    private final String tripId;
    private final String vehicleId;
    private final GeoPosition position;
    private final int stopSequence;
    private final String routeId;
    private final int directionId;
    private final String occupancyInfo;
    private final int occupancyPercentage;
    private final String currentStopId;

    public VehiclePosition(String tripId, String vehicleId, GeoPosition position, int stopSequence) {
        this(tripId, vehicleId, position, stopSequence, null, -1, null, -1, null);
    }

    public VehiclePosition(String tripId, String vehicleId, GeoPosition position, int stopSequence,
                           String routeId, int directionId) {
        this(tripId, vehicleId, position, stopSequence, routeId, directionId, null, -1, null);
    }

    public VehiclePosition(String tripId, String vehicleId, GeoPosition position, int stopSequence,
                           String routeId, int directionId,
                           String occupancyInfo, int occupancyPercentage, String currentStopId) {
        this.tripId = tripId;
        this.vehicleId = vehicleId;
        this.position = position;
        this.stopSequence = stopSequence;
        this.routeId = routeId;
        this.directionId = directionId;
        this.occupancyInfo = occupancyInfo;
        this.occupancyPercentage = occupancyPercentage;
        this.currentStopId = currentStopId;
    }

    /**
     * Returns the trip id.
     */
    public String getTripId() {
        return tripId;
    }

    /**
     * Returns the vehicle id.
     */
    public String getVehicleId() {
        return vehicleId;
    }

    /**
     * Returns the position.
     */
    public GeoPosition getPosition() {
        return position;
    }

    /**
     * Returns the stop sequence.
     */
    public int getStopSequence() {
        return stopSequence;
    }

    /**
     * Returns the route id.
     */
    public String getRouteId() {
        return routeId;
    }

    /**
     * Returns the direction id.
     */
    public int getDirectionId() {
        return directionId;
    }

    /**
     * Returns the occupancy info.
     */
    public String getOccupancyInfo() {
        return occupancyInfo;
    }

    /**
     * Returns the occupancy percentage.
     */
    public int getOccupancyPercentage() {
        return occupancyPercentage;
    }

    /**
     * Returns the current stop id.
     */
    public String getCurrentStopId() {
        return currentStopId;
    }

    @Override
    /**
     * Returns the result of toString.
     */
    public String toString() {
        return "Bus " + vehicleId + " trip=" + tripId +
               " route=" + routeId +
               " dir=" + directionId +
               " pos=" + position.getLatitude() + "," + position.getLongitude() +
               " stopSeq=" + stopSequence +
               " occ=" + occupancyInfo +
               " occPct=" + occupancyPercentage;
    }
}


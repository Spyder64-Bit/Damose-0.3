package damose.model;

import org.jxmapviewer.viewer.DefaultWaypoint;

import damose.data.loader.RoutesLoader;


/**
 * Domain model for bus waypoint.
 */
public class BusWaypoint extends DefaultWaypoint {

    private final String tripId;
    private final String tripHeadsign;
    private final String vehicleId;
    private final String routeId;
    private final int directionId;
    private final VehicleType vehicleType;

    public BusWaypoint(VehiclePosition vp, String tripHeadsign, String routeId, int directionId) {
        super(vp.getPosition());
        this.tripId = vp.getTripId();
        this.vehicleId = vp.getVehicleId();
        this.tripHeadsign = tripHeadsign;
        this.routeId = routeId;
        this.directionId = directionId;


        Route route = RoutesLoader.getRouteById(routeId);
        if (route != null) {
            this.vehicleType = route.getVehicleType();
        } else {
            this.vehicleType = VehicleType.BUS;
        }
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
     * Returns the vehicle id.
     */
    public String getVehicleId() {
        return vehicleId;
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
     * Returns the vehicle type.
     */
    public VehicleType getVehicleType() {
        return vehicleType;
    }

    @Override
    /**
     * Returns the result of toString.
     */
    public String toString() {
        return vehicleType.getEmoji() + " " + routeId + " - " + tripHeadsign + " (" + vehicleId + ")";
    }
}


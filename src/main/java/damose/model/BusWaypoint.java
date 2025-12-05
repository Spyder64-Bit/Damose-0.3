package damose.model;

import org.jxmapviewer.viewer.DefaultWaypoint;

import damose.data.loader.RoutesLoader;
import damose.data.model.Route;
import damose.data.model.VehiclePosition;

/**
 * Waypoint representing a vehicle (bus, tram, metro) on the map.
 */
public class BusWaypoint extends DefaultWaypoint {

    private final String tripId;
    private final String tripHeadsign;
    private final String vehicleId;
    private final String routeId;
    private final VehicleType vehicleType;

    public BusWaypoint(VehiclePosition vp, String tripHeadsign, String routeId) {
        super(vp.getPosition());
        this.tripId = vp.getTripId();
        this.vehicleId = vp.getVehicleId();
        this.tripHeadsign = tripHeadsign;
        this.routeId = routeId;
        
        // Determine vehicle type from route
        Route route = RoutesLoader.getRouteById(routeId);
        if (route != null) {
            this.vehicleType = route.getVehicleType();
        } else {
            this.vehicleType = VehicleType.BUS; // Default to bus
        }
    }

    public String getTripId() {
        return tripId;
    }

    public String getTripHeadsign() {
        return tripHeadsign;
    }

    public String getVehicleId() {
        return vehicleId;
    }
    
    public String getRouteId() {
        return routeId;
    }
    
    public VehicleType getVehicleType() {
        return vehicleType;
    }

    @Override
    public String toString() {
        return vehicleType.getEmoji() + " " + routeId + " - " + tripHeadsign + " (" + vehicleId + ")";
    }
}

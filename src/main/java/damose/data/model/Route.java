package damose.data.model;

import damose.model.VehicleType;

/**
 * Represents a route from GTFS routes.txt.
 */
public class Route {
    
    private final String routeId;
    private final String agencyId;
    private final String routeShortName;
    private final String routeLongName;
    private final VehicleType vehicleType;
    private final String routeColor;
    private final String routeTextColor;
    
    public Route(String routeId, String agencyId, String routeShortName, 
                 String routeLongName, int routeType, String routeColor, String routeTextColor) {
        this.routeId = routeId;
        this.agencyId = agencyId;
        this.routeShortName = routeShortName;
        this.routeLongName = routeLongName;
        this.vehicleType = VehicleType.fromGtfsCode(routeType);
        this.routeColor = routeColor;
        this.routeTextColor = routeTextColor;
    }
    
    public String getRouteId() {
        return routeId;
    }
    
    public String getAgencyId() {
        return agencyId;
    }
    
    public String getRouteShortName() {
        return routeShortName;
    }
    
    public String getRouteLongName() {
        return routeLongName;
    }
    
    public VehicleType getVehicleType() {
        return vehicleType;
    }
    
    public String getRouteColor() {
        return routeColor;
    }
    
    public String getRouteTextColor() {
        return routeTextColor;
    }
    
    /**
     * Get display name: short name if available, otherwise route ID.
     */
    public String getDisplayName() {
        if (routeShortName != null && !routeShortName.isEmpty()) {
            return routeShortName;
        }
        return routeId;
    }
    
    @Override
    public String toString() {
        return vehicleType.getEmoji() + " " + getDisplayName() + " (" + vehicleType.getDisplayName() + ")";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return routeId != null ? routeId.equals(route.routeId) : route.routeId == null;
    }
    
    @Override
    public int hashCode() {
        return routeId != null ? routeId.hashCode() : 0;
    }
}


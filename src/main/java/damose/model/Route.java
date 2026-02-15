package damose.model;

/**
 * Domain model for route.
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

    /**
     * Returns the route id.
     */
    public String getRouteId() {
        return routeId;
    }

    /**
     * Returns the agency id.
     */
    public String getAgencyId() {
        return agencyId;
    }

    /**
     * Returns the route short name.
     */
    public String getRouteShortName() {
        return routeShortName;
    }

    /**
     * Returns the route long name.
     */
    public String getRouteLongName() {
        return routeLongName;
    }

    /**
     * Returns the vehicle type.
     */
    public VehicleType getVehicleType() {
        return vehicleType;
    }

    /**
     * Returns the route color.
     */
    public String getRouteColor() {
        return routeColor;
    }

    /**
     * Returns the route text color.
     */
    public String getRouteTextColor() {
        return routeTextColor;
    }

    /**
     * Returns the display name.
     */
    public String getDisplayName() {
        if (routeShortName != null && !routeShortName.isEmpty()) {
            return routeShortName;
        }
        return routeId;
    }

    @Override
    /**
     * Returns the result of toString.
     */
    public String toString() {
        return vehicleType.getEmoji() + " " + getDisplayName() + " (" + vehicleType.getDisplayName() + ")";
    }

    @Override
    /**
     * Returns the result of equals.
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return routeId != null ? routeId.equals(route.routeId) : route.routeId == null;
    }

    @Override
    /**
     * Returns the result of hashCode.
     */
    public int hashCode() {
        return routeId != null ? routeId.hashCode() : 0;
    }
}


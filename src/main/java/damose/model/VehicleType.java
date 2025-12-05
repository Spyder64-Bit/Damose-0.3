package damose.model;

import java.awt.Color;

/**
 * GTFS route types representing different vehicle types.
 */
public enum VehicleType {
    
    TRAM(0, "Tram", "tram.png", new Color(255, 69, 0)),      // Orange-red
    METRO(1, "Metro", "metro.png", new Color(0, 123, 255)),  // Blue
    RAIL(2, "Rail", "rail.png", new Color(139, 69, 19)),     // Brown
    BUS(3, "Bus", "bus.png", new Color(76, 175, 80)),        // Green
    FERRY(4, "Ferry", "ferry.png", new Color(0, 188, 212)),  // Cyan
    CABLE_CAR(5, "Cable Car", "cable.png", new Color(156, 39, 176)), // Purple
    GONDOLA(6, "Gondola", "gondola.png", new Color(233, 30, 99)),    // Pink
    FUNICULAR(7, "Funicular", "funicular.png", new Color(121, 85, 72)), // Brown
    UNKNOWN(-1, "Unknown", "bus.png", new Color(158, 158, 158)); // Gray
    
    private final int gtfsCode;
    private final String displayName;
    private final String iconFileName;
    private final Color color;
    
    VehicleType(int gtfsCode, String displayName, String iconFileName, Color color) {
        this.gtfsCode = gtfsCode;
        this.displayName = displayName;
        this.iconFileName = iconFileName;
        this.color = color;
    }
    
    public int getGtfsCode() {
        return gtfsCode;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getIconFileName() {
        return iconFileName;
    }
    
    public Color getColor() {
        return color;
    }
    
    /**
     * Get VehicleType from GTFS route_type code.
     */
    public static VehicleType fromGtfsCode(int code) {
        for (VehicleType type : values()) {
            if (type.gtfsCode == code) {
                return type;
            }
        }
        return UNKNOWN;
    }
    
    /**
     * Get VehicleType from GTFS route_type code string.
     */
    public static VehicleType fromGtfsCode(String codeStr) {
        if (codeStr == null || codeStr.trim().isEmpty()) {
            return UNKNOWN;
        }
        try {
            return fromGtfsCode(Integer.parseInt(codeStr.trim()));
        } catch (NumberFormatException e) {
            return UNKNOWN;
        }
    }
    
    /**
     * Get emoji representation for UI display.
     */
    public String getEmoji() {
        return switch (this) {
            case TRAM -> "🚋";
            case METRO -> "🚇";
            case RAIL -> "🚆";
            case BUS -> "🚌";
            case FERRY -> "⛴️";
            case CABLE_CAR -> "🚠";
            case GONDOLA -> "🚡";
            case FUNICULAR -> "🚞";
            default -> "🚍";
        };
    }
}


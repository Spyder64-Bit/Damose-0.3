package damose.model;

import java.awt.Color;

public enum VehicleType {
    
    TRAM(0, "Tram", "tram.png", new Color(255, 69, 0)), // Nota in italiano
    METRO(1, "Metro", "metro.png", new Color(0, 123, 255)), // Nota in italiano
    RAIL(2, "Rail", "rail.png", new Color(139, 69, 19)), // Nota in italiano
    BUS(3, "Bus", "bus.png", new Color(76, 175, 80)), // Nota in italiano
    FERRY(4, "Ferry", "ferry.png", new Color(0, 188, 212)), // Nota in italiano
    CABLE_CAR(5, "Cable Car", "cable.png", new Color(156, 39, 176)), // Nota in italiano
    GONDOLA(6, "Gondola", "gondola.png", new Color(233, 30, 99)), // Nota in italiano
    FUNICULAR(7, "Funicular", "funicular.png", new Color(121, 85, 72)), // Nota in italiano
    UNKNOWN(-1, "Unknown", "bus.png", new Color(158, 158, 158)); // Nota in italiano
    
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
    
    public static VehicleType fromGtfsCode(int code) {
        for (VehicleType type : values()) {
            if (type.gtfsCode == code) {
                return type;
            }
        }
        return UNKNOWN;
    }
    
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
    
    public String getEmoji() {
        return switch (this) {
            case TRAM -> "ðŸš‹";
            case METRO -> "ðŸš‡";
            case RAIL -> "ðŸš†";
            case BUS -> "ðŸšŒ";
            case FERRY -> "â›´ï¸";
            case CABLE_CAR -> "ðŸš ";
            case GONDOLA -> "ðŸš¡";
            case FUNICULAR -> "ðŸšž";
            default -> "ðŸš";
        };
    }
}












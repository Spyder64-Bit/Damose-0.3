package damose.model;

import java.awt.Color;

/**
 * Domain model for vehicle type.
 */
public enum VehicleType {

    TRAM(0, "Tram", "tram.png", new Color(255, 69, 0)),
    METRO(1, "Metro", "metro.png", new Color(0, 123, 255)),
    RAIL(2, "Rail", "rail.png", new Color(139, 69, 19)),
    BUS(3, "Bus", "bus.png", new Color(76, 175, 80)),
    FERRY(4, "Ferry", "ferry.png", new Color(0, 188, 212)),
    CABLE_CAR(5, "Cable Car", "cable.png", new Color(156, 39, 176)),
    GONDOLA(6, "Gondola", "gondola.png", new Color(233, 30, 99)),
    FUNICULAR(7, "Funicular", "funicular.png", new Color(121, 85, 72)),
    UNKNOWN(-1, "Unknown", "bus.png", new Color(158, 158, 158));

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

    /**
     * Returns the gtfs code.
     */
    public int getGtfsCode() {
        return gtfsCode;
    }

    /**
     * Returns the display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the icon file name.
     */
    public String getIconFileName() {
        return iconFileName;
    }

    /**
     * Returns the color.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Returns the result of fromGtfsCode.
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
     * Returns the result of fromGtfsCode.
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
     * Returns the emoji.
     */
    public String getEmoji() {
        return switch (this) {
            case TRAM -> "\uD83D\uDE8B";
            case METRO -> "\uD83D\uDE87";
            case RAIL -> "\uD83D\uDE86";
            case BUS -> "\uD83D\uDE8C";
            case FERRY -> "\u26F4";
            case CABLE_CAR -> "\uD83D\uDEA0";
            case GONDOLA -> "\uD83D\uDEA1";
            case FUNICULAR -> "\uD83D\uDE9E";
            default -> "\u2753";
        };
    }
}


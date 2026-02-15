package damose.model;

/**
 * Domain model for stop.
 */
public class Stop {

    private final String stopId;
    private final String stopCode;
    private final String stopName;
    private final double stopLat;
    private final double stopLon;

    private boolean isFakeLine = false;

    public Stop(String stopId, String stopCode, String stopName, double stopLat, double stopLon) {
        this.stopId = stopId;
        this.stopCode = stopCode;
        this.stopName = stopName;
        this.stopLat = stopLat;
        this.stopLon = stopLon;
    }

    /**
     * Returns the stop id.
     */
    public String getStopId() {
        return stopId;
    }

    /**
     * Returns the stop code.
     */
    public String getStopCode() {
        return stopCode;
    }

    /**
     * Returns the stop name.
     */
    public String getStopName() {
        return stopName;
    }

    /**
     * Returns the stop lat.
     */
    public double getStopLat() {
        return stopLat;
    }

    /**
     * Returns the stop lon.
     */
    public double getStopLon() {
        return stopLon;
    }

    /**
     * Handles markAsFakeLine.
     */
    public void markAsFakeLine() {
        this.isFakeLine = true;
    }

    /**
     * Returns whether fake line.
     */
    public boolean isFakeLine() {
        return isFakeLine;
    }

    @Override
    /**
     * Returns the result of toString.
     */
    public String toString() {
        return isFakeLine ? stopName : stopId + " - " + stopName;
    }

    @Override
    /**
     * Returns the result of equals.
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stop stop = (Stop) o;
        return stopId != null ? stopId.equals(stop.stopId) : stop.stopId == null;
    }

    @Override
    /**
     * Returns the result of hashCode.
     */
    public int hashCode() {
        return stopId != null ? stopId.hashCode() : 0;
    }
}


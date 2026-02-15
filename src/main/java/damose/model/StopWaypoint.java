package damose.model;

import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;


/**
 * Domain model for stop waypoint.
 */
public class StopWaypoint extends DefaultWaypoint {

    private final Stop stop;

    public StopWaypoint(Stop stop) {
        super(new GeoPosition(stop.getStopLat(), stop.getStopLon()));
        this.stop = stop;
    }

    /**
     * Returns the stop.
     */
    public Stop getStop() {
        return stop;
    }

    @Override
    /**
     * Returns the result of toString.
     */
    public String toString() {
        return stop.getStopName() + " (" + stop.getStopId() + ")";
    }
}


package damose.service;

import java.util.Collections;
import java.util.List;

import damose.data.model.VehiclePosition;

/**
 * Static simulator for offline mode.
 * In offline mode, no bus positions are shown (static schedule only).
 */
public final class StaticSimulator {

    private StaticSimulator() {
        // Utility class
    }

    /**
     * Returns empty list in offline mode - no simulated buses.
     * Real-time bus positions require online connection.
     */
    public static List<VehiclePosition> simulateAllTrips() {
        // Don't simulate buses - it's too heavy and not useful
        // Offline mode shows static schedule data only
        return Collections.emptyList();
    }
}


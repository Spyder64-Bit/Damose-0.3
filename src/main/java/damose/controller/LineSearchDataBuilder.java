package damose.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import damose.model.Route;
import damose.model.Stop;

/**
 * Builds searchable line entries from GTFS route definitions.
 */
final class LineSearchDataBuilder {

    private LineSearchDataBuilder() {
    }

    static List<Stop> build(Map<String, Route> routesById) {
        if (routesById == null || routesById.isEmpty()) {
            return List.of();
        }

        return routesById.values().stream()
                .map(route -> {
                    String shortName = safe(route.getRouteShortName());
                    String longName = safe(route.getRouteLongName());
                    String displayName = shortName.isEmpty() ? route.getRouteId() : shortName;
                    if (!longName.isEmpty() && !longName.equalsIgnoreCase(displayName)) {
                        displayName = displayName + " - " + longName;
                    }

                    Stop line = new Stop(
                            route.getRouteId(),
                            String.valueOf(route.getVehicleType().getGtfsCode()),
                            displayName,
                            0.0,
                            0.0
                    );
                    line.markAsFakeLine();
                    return line;
                })
                .sorted(Comparator.comparing(Stop::getStopName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

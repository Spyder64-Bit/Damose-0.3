package damose.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jxmapviewer.viewer.GeoPosition;

import damose.model.Stop;
import damose.model.StopTime;
import damose.model.Trip;

/**
 * Provides service logic for route service.
 */
public class RouteService {

    private final List<Trip> trips;
    private final List<StopTime> stopTimes;
    private final Map<String, Stop> stopsById;
    private final Map<String, List<GeoPosition>> shapesById;
    private final Map<String, List<Trip>> tripsByRouteId;
    private final Map<String, Integer> stopCountByTripId;

    public RouteService(List<Trip> trips,
                        List<StopTime> stopTimes,
                        List<Stop> stops,
                        Map<String, List<GeoPosition>> shapesById) {
        this.trips = trips;
        this.stopTimes = stopTimes;
        this.stopsById = stops.stream()
                .collect(Collectors.toMap(Stop::getStopId, s -> s, (a, b) -> a));
        this.shapesById = shapesById != null ? shapesById : Collections.emptyMap();
        this.tripsByRouteId = buildTripsByRouteId(trips);
        this.stopCountByTripId = buildStopCountByTripId(stopTimes);
    }

    /**
     * Returns the result of findRepresentativeTrip.
     */
    public Trip findRepresentativeTrip(String routeId, String headsign) {
        return findTripsByRouteId(routeId).stream()
                .filter(t -> headsign == null || t.getTripHeadsign().equalsIgnoreCase(headsign))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the result of findTripsByRouteId.
     */
    public List<Trip> findTripsByRouteId(String routeId) {
        if (routeId == null) return Collections.emptyList();

        String target = routeId.trim();
        if (target.isEmpty()) return Collections.emptyList();

        List<Trip> exact = tripsByRouteId.get(target);
        if (exact != null && !exact.isEmpty()) {
            return exact;
        }

        for (Map.Entry<String, List<Trip>> entry : tripsByRouteId.entrySet()) {
            String key = entry.getKey();
            if (key != null && target.equalsIgnoreCase(key.trim())) {
                return entry.getValue();
            }
        }
        return Collections.emptyList();
    }

    /**
     * Returns the stops for trip.
     */
    public List<Stop> getStopsForTrip(String tripId) {
        if (tripId == null) return Collections.emptyList();

        List<StopTime> tripStopTimes = stopTimes.stream()
                .filter(st -> st.getTripId().equals(tripId))
                .sorted(Comparator.comparingInt(StopTime::getStopSequence))
                .collect(Collectors.toList());

        List<Stop> orderedStops = new ArrayList<>();
        for (StopTime st : tripStopTimes) {
            Stop stop = stopsById.get(st.getStopId());
            if (stop != null) {
                orderedStops.add(stop);
            }
        }

        return orderedStops;
    }

    /**
     * Returns the stops for route.
     */
    public List<Stop> getStopsForRoute(String routeId) {
        if (routeId == null) return Collections.emptyList();

        List<Trip> routeTrips = findTripsByRouteId(routeId);
        if (routeTrips.isEmpty()) return Collections.emptyList();

        Trip bestTrip = chooseBestTrip(routeTrips);
        if (bestTrip == null) return Collections.emptyList();
        return getStopsForTrip(bestTrip.getTripId());
    }

    /**
     * Returns the stops for route and direction.
     */
    public List<Stop> getStopsForRouteAndDirection(String routeId, int directionId) {
        if (routeId == null) return Collections.emptyList();

        List<Trip> routeTrips = findTripsByRouteId(routeId).stream()
                .filter(t -> t.getDirectionId() == directionId)
                .collect(Collectors.toList());

        if (routeTrips.isEmpty()) return Collections.emptyList();

        Trip bestTrip = chooseBestTrip(routeTrips);
        if (bestTrip == null) return Collections.emptyList();
        return getStopsForTrip(bestTrip.getTripId());
    }

    /**
     * Returns the shape for route.
     */
    public List<GeoPosition> getShapeForRoute(String routeId) {
        if (routeId == null) return Collections.emptyList();

        List<Trip> routeTrips = findTripsByRouteId(routeId);
        if (routeTrips.isEmpty()) return Collections.emptyList();

        Trip bestShapeTrip = chooseBestTripForShape(routeTrips);
        if (bestShapeTrip != null) {
            return getShapeForTrip(bestShapeTrip);
        }

        Trip bestTrip = chooseBestTrip(routeTrips);
        return bestTrip == null ? Collections.emptyList() : getShapeForTrip(bestTrip);
    }

    /**
     * Returns the shape for route and direction.
     */
    public List<GeoPosition> getShapeForRouteAndDirection(String routeId, int directionId) {
        if (routeId == null) return Collections.emptyList();

        List<Trip> routeTrips = findTripsByRouteId(routeId).stream()
                .filter(t -> t.getDirectionId() == directionId)
                .collect(Collectors.toList());

        if (routeTrips.isEmpty()) return Collections.emptyList();

        Trip bestShapeTrip = chooseBestTripForShape(routeTrips);
        if (bestShapeTrip != null) {
            return getShapeForTrip(bestShapeTrip);
        }

        Trip bestTrip = chooseBestTrip(routeTrips);
        return bestTrip == null ? Collections.emptyList() : getShapeForTrip(bestTrip);
    }

    /**
     * Returns the stops for route and headsign.
     */
    public List<Stop> getStopsForRouteAndHeadsign(String routeId, String headsign) {
        Trip trip = findRepresentativeTrip(routeId, headsign);
        if (trip == null) return Collections.emptyList();
        return getStopsForTrip(trip.getTripId());
    }

    /**
     * Returns the headsigns for route.
     */
    public List<String> getHeadsignsForRoute(String routeId) {
        return findTripsByRouteId(routeId).stream()
                .map(Trip::getTripHeadsign)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Returns the directions for route.
     */
    public List<Integer> getDirectionsForRoute(String routeId) {
        return findTripsByRouteId(routeId).stream()
                .map(Trip::getDirectionId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Returns the representative headsign for route and direction.
     */
    public String getRepresentativeHeadsignForRouteAndDirection(String routeId, int directionId) {
        return findTripsByRouteId(routeId).stream()
                .filter(t -> t.getDirectionId() == directionId)
                .map(Trip::getTripHeadsign)
                .filter(h -> h != null && !h.isBlank())
                .findFirst()
                .orElse("");
    }

    private static Map<String, List<Trip>> buildTripsByRouteId(List<Trip> trips) {
        Map<String, List<Trip>> map = new HashMap<>();
        for (Trip trip : trips) {
            if (trip == null || trip.getRouteId() == null) continue;
            String routeId = trip.getRouteId().trim();
            if (routeId.isEmpty()) continue;
            map.computeIfAbsent(routeId, k -> new ArrayList<>()).add(trip);
        }
        return map;
    }

    private static Map<String, Integer> buildStopCountByTripId(List<StopTime> stopTimes) {
        Map<String, Integer> map = new HashMap<>();
        for (StopTime stopTime : stopTimes) {
            if (stopTime == null || stopTime.getTripId() == null) continue;
            map.merge(stopTime.getTripId(), 1, Integer::sum);
        }
        return map;
    }

    private Trip chooseBestTrip(List<Trip> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;

        Trip bestTrip = null;
        int maxStops = -1;
        for (Trip trip : candidates) {
            int count = stopCountByTripId.getOrDefault(trip.getTripId(), 0);
            if (count > maxStops) {
                maxStops = count;
                bestTrip = trip;
            }
        }
        return bestTrip;
    }

    private Trip chooseBestTripForShape(List<Trip> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;

        Trip bestTrip = null;
        int bestShapePoints = -1;
        int bestStops = -1;

        for (Trip trip : candidates) {
            int shapePoints = getShapePointCount(trip);
            if (shapePoints < 2) continue;

            int stopCount = stopCountByTripId.getOrDefault(trip.getTripId(), 0);
            if (shapePoints > bestShapePoints
                    || (shapePoints == bestShapePoints && stopCount > bestStops)) {
                bestShapePoints = shapePoints;
                bestStops = stopCount;
                bestTrip = trip;
            }
        }
        return bestTrip;
    }

    private List<GeoPosition> getShapeForTrip(Trip trip) {
        if (trip == null) return Collections.emptyList();
        String shapeId = trip.getShapeId();
        if (shapeId == null || shapeId.isBlank()) return Collections.emptyList();

        List<GeoPosition> shape = shapesById.get(shapeId.trim());
        if (shape == null || shape.size() < 2) {
            return Collections.emptyList();
        }
        return new ArrayList<>(shape);
    }

    private int getShapePointCount(Trip trip) {
        if (trip == null || trip.getShapeId() == null || trip.getShapeId().isBlank()) {
            return 0;
        }
        List<GeoPosition> shape = shapesById.get(trip.getShapeId().trim());
        return shape == null ? 0 : shape.size();
    }

}


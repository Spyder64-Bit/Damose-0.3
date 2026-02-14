package damose.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import damose.model.Stop;
import damose.model.StopTime;
import damose.model.Trip;

public class RouteService {

    private final List<Trip> trips;
    private final List<StopTime> stopTimes;
    private final Map<String, Stop> stopsById;
    private final Map<String, List<Trip>> tripsByRouteId;
    private final Map<String, Integer> stopCountByTripId;

    public RouteService(List<Trip> trips, List<StopTime> stopTimes, List<Stop> stops) {
        this.trips = trips;
        this.stopTimes = stopTimes;
        this.stopsById = stops.stream()
                .collect(Collectors.toMap(Stop::getStopId, s -> s, (a, b) -> a));
        this.tripsByRouteId = buildTripsByRouteId(trips);
        this.stopCountByTripId = buildStopCountByTripId(stopTimes);
    }

    public Trip findRepresentativeTrip(String routeId, String headsign) {
        return findTripsByRouteId(routeId).stream()
                .filter(t -> headsign == null || t.getTripHeadsign().equalsIgnoreCase(headsign))
                .findFirst()
                .orElse(null);
    }

    public List<Trip> findTripsByRouteId(String routeId) {
        if (routeId == null) return Collections.emptyList();

        List<Trip> exact = tripsByRouteId.get(routeId);
        if (exact != null && !exact.isEmpty()) {
            return exact;
        }

        String normalizedTarget = normalizeNumericRouteId(routeId);
        if (normalizedTarget == null) {
            return Collections.emptyList();
        }

        for (Map.Entry<String, List<Trip>> entry : tripsByRouteId.entrySet()) {
            if (normalizedTarget.equals(normalizeNumericRouteId(entry.getKey()))) {
                return entry.getValue();
            }
        }
        return Collections.emptyList();
    }

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

    public List<Stop> getStopsForRoute(String routeId) {
        if (routeId == null) return Collections.emptyList();

        List<Trip> routeTrips = findTripsByRouteId(routeId);
        if (routeTrips.isEmpty()) return Collections.emptyList();

        Trip bestTrip = chooseBestTrip(routeTrips);
        if (bestTrip == null) return Collections.emptyList();
        return getStopsForTrip(bestTrip.getTripId());
    }

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

    public List<Stop> getStopsForRouteAndHeadsign(String routeId, String headsign) {
        Trip trip = findRepresentativeTrip(routeId, headsign);
        if (trip == null) return Collections.emptyList();
        return getStopsForTrip(trip.getTripId());
    }

    public List<String> getHeadsignsForRoute(String routeId) {
        return findTripsByRouteId(routeId).stream()
                .map(Trip::getTripHeadsign)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<Integer> getDirectionsForRoute(String routeId) {
        return findTripsByRouteId(routeId).stream()
                .map(Trip::getDirectionId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

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
            map.computeIfAbsent(trip.getRouteId(), k -> new ArrayList<>()).add(trip);
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

    private static String normalizeNumericRouteId(String routeId) {
        if (routeId == null) return null;
        String trimmed = routeId.trim();
        if (trimmed.isEmpty()) return null;
        if (!trimmed.chars().allMatch(Character::isDigit)) {
            return trimmed.toUpperCase();
        }

        int i = 0;
        while (i < trimmed.length() - 1 && trimmed.charAt(i) == '0') {
            i++;
        }
        return trimmed.substring(i);
    }
}


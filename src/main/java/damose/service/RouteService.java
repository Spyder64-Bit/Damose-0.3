package damose.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    public RouteService(List<Trip> trips, List<StopTime> stopTimes, List<Stop> stops) {
        this.trips = trips;
        this.stopTimes = stopTimes;
        this.stopsById = stops.stream()
                .collect(Collectors.toMap(Stop::getStopId, s -> s, (a, b) -> a));
    }

    public Trip findRepresentativeTrip(String routeId, String headsign) {
        return trips.stream()
                .filter(t -> t.getRouteId().equalsIgnoreCase(routeId))
                .filter(t -> headsign == null || t.getTripHeadsign().equalsIgnoreCase(headsign))
                .findFirst()
                .orElse(null);
    }

    public List<Trip> findTripsByRouteId(String routeId) {
        return trips.stream()
                .filter(t -> t.getRouteId().equalsIgnoreCase(routeId))
                .collect(Collectors.toList());
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

        String bestTripId = null;
        int maxStops = 0;

        for (Trip trip : routeTrips) {
            long count = stopTimes.stream()
                    .filter(st -> st.getTripId().equals(trip.getTripId()))
                    .count();
            if (count > maxStops) {
                maxStops = (int) count;
                bestTripId = trip.getTripId();
            }
        }

        if (bestTripId == null) return Collections.emptyList();
        return getStopsForTrip(bestTripId);
    }

    public List<Stop> getStopsForRouteAndHeadsign(String routeId, String headsign) {
        Trip trip = findRepresentativeTrip(routeId, headsign);
        if (trip == null) return Collections.emptyList();
        return getStopsForTrip(trip.getTripId());
    }

    public List<String> getHeadsignsForRoute(String routeId) {
        return trips.stream()
                .filter(t -> t.getRouteId().equalsIgnoreCase(routeId))
                .map(Trip::getTripHeadsign)
                .distinct()
                .collect(Collectors.toList());
    }
}


package damose.data.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import damose.model.StopTime;
import damose.model.Trip;

/**
 * Data mapping logic for stop trip mapper.
 */
public class StopTripMapper {

    private final Map<String, List<StopTime>> stopToTrips = new HashMap<>();
    private final Map<String, Map<Integer, String>> tripSeqToStop = new HashMap<>();
    private final Map<String, List<StopTime>> tripToStopTimes = new HashMap<>();
    private final Set<String> knownStopIds = new HashSet<>();
    private final TripMatcher matcher;

    public StopTripMapper(List<StopTime> stopTimes, TripMatcher matcher) {
        this.matcher = matcher;

        for (StopTime st : stopTimes) {
            String stopId = st.getStopId();
            String rawTripId = st.getTripId();
            String normTripId = normalizeTripId(rawTripId);
            int seq = st.getStopSequence();

            if (stopId != null) knownStopIds.add(stopId);

            stopToTrips.computeIfAbsent(stopId, k -> new ArrayList<>()).add(st);

            tripSeqToStop
                    .computeIfAbsent(normTripId, k -> new HashMap<>())
                    .put(seq, stopId);

            tripToStopTimes.computeIfAbsent(normTripId, k -> new ArrayList<>()).add(st);
        }

        for (List<StopTime> list : stopToTrips.values()) {
            list.sort(Comparator.comparing(StopTime::getArrivalTime, Comparator.nullsLast(Comparator.naturalOrder())));
        }
        for (List<StopTime> list : tripToStopTimes.values()) {
            list.sort(Comparator.comparingInt(StopTime::getStopSequence));
        }

        System.out.println("StopTripMapper initialized: stopToTrips=" + stopToTrips.size() +
                          " tripSeqToStop=" + tripSeqToStop.size());
    }

    /**
     * Returns the trips for stop.
     */
    public List<Trip> getTripsForStop(String stopId) {
        List<StopTime> times = stopToTrips.getOrDefault(stopId, Collections.emptyList());
        if (times.isEmpty() || matcher == null) return Collections.emptyList();

        Set<Trip> distinctTrips = new LinkedHashSet<>();
        for (StopTime st : times) {
            Trip trip = matcher.matchByTripId(st.getTripId());
            if (trip != null) distinctTrips.add(trip);
        }
        if (distinctTrips.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(distinctTrips);
    }

    /**
     * Returns whether known stop id.
     */
    public boolean isKnownStopId(String stopId) {
        return stopId != null && knownStopIds.contains(stopId);
    }

    /**
     * Returns the stop id by trip and sequence.
     */
    public String getStopIdByTripAndSequence(String tripId, int sequence) {
        if (tripId == null) return null;

        String norm = normalizeTripId(tripId);
        Map<Integer, String> seqMap = tripSeqToStop.get(norm);
        if (seqMap != null) {
            String s = seqMap.get(sequence);
            if (s != null) {
                return s;
            }
        }

        String simple = TripIdUtils.normalizeSimple(tripId);
        if (simple != null && !simple.equals(norm)) {
            seqMap = tripSeqToStop.get(simple);
            if (seqMap != null) {
                String s = seqMap.get(sequence);
                if (s != null) {
                    return s;
                }
            }
        }

        return null;
    }

    /**
     * Returns the stop times for stop.
     */
    public List<StopTime> getStopTimesForStop(String stopId) {
        return stopToTrips.getOrDefault(stopId, Collections.emptyList());
    }

    /**
     * Returns the stop times for trip.
     */
    public List<StopTime> getStopTimesForTrip(String tripId) {
        if (tripId == null) return Collections.emptyList();

        String norm = normalizeTripId(tripId);
        List<StopTime> direct = tripToStopTimes.get(norm);
        if (direct != null) {
            return direct;
        }

        String simple = TripIdUtils.normalizeSimple(tripId);
        if (simple != null && !simple.equals(norm)) {
            List<StopTime> fallback = tripToStopTimes.get(simple);
            if (fallback != null) {
                return fallback;
            }
        }

        return Collections.emptyList();
    }

    private String normalizeTripId(String id) {
        if (id == null) return null;
        String normalized = TripIdUtils.normalizeSimple(id);
        return normalized != null ? normalized : id.trim();
    }

}


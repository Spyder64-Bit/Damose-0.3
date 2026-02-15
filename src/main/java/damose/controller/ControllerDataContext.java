package damose.controller;

import java.util.List;

import damose.data.mapper.StopTripMapper;
import damose.data.mapper.TripMatcher;
import damose.model.Stop;
import damose.model.StopTime;
import damose.model.Trip;
import damose.service.ArrivalService;
import damose.service.RouteService;

/**
 * Coordinates application flow for controller data context.
 */
public final class ControllerDataContext {

    private final List<Stop> stops;
    private final List<Trip> trips;
    private final List<StopTime> stopTimes;
    private final TripMatcher tripMatcher;
    private final StopTripMapper stopTripMapper;
    private final RouteService routeService;
    private final ArrivalService arrivalService;

    public ControllerDataContext(List<Stop> stops,
                                 List<Trip> trips,
                                 List<StopTime> stopTimes,
                                 TripMatcher tripMatcher,
                                 StopTripMapper stopTripMapper,
                                 RouteService routeService,
                                 ArrivalService arrivalService) {
        this.stops = stops;
        this.trips = trips;
        this.stopTimes = stopTimes;
        this.tripMatcher = tripMatcher;
        this.stopTripMapper = stopTripMapper;
        this.routeService = routeService;
        this.arrivalService = arrivalService;
    }

    /**
     * Returns the stops.
     */
    public List<Stop> getStops() {
        return stops;
    }

    /**
     * Returns the trips.
     */
    public List<Trip> getTrips() {
        return trips;
    }

    /**
     * Returns the stop times.
     */
    public List<StopTime> getStopTimes() {
        return stopTimes;
    }

    /**
     * Returns the trip matcher.
     */
    public TripMatcher getTripMatcher() {
        return tripMatcher;
    }

    /**
     * Returns the stop trip mapper.
     */
    public StopTripMapper getStopTripMapper() {
        return stopTripMapper;
    }

    /**
     * Returns the route service.
     */
    public RouteService getRouteService() {
        return routeService;
    }

    /**
     * Returns the arrival service.
     */
    public ArrivalService getArrivalService() {
        return arrivalService;
    }
}


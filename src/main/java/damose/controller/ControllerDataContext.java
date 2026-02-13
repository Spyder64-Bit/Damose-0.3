package damose.controller;

import java.util.List;

import damose.data.mapper.StopTripMapper;
import damose.data.mapper.TripMatcher;
import damose.model.Stop;
import damose.model.StopTime;
import damose.model.Trip;
import damose.service.ArrivalService;
import damose.service.RouteService;

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

    public List<Stop> getStops() {
        return stops;
    }

    public List<Trip> getTrips() {
        return trips;
    }

    public List<StopTime> getStopTimes() {
        return stopTimes;
    }

    public TripMatcher getTripMatcher() {
        return tripMatcher;
    }

    public StopTripMapper getStopTripMapper() {
        return stopTripMapper;
    }

    public RouteService getRouteService() {
        return routeService;
    }

    public ArrivalService getArrivalService() {
        return arrivalService;
    }
}


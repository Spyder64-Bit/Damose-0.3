package damose.controller;

import java.util.List;

import damose.data.loader.CalendarLoader;
import damose.data.loader.RoutesLoader;
import damose.data.loader.StopTimesLoader;
import damose.data.loader.StopsLoader;
import damose.data.loader.TripsLoader;
import damose.data.mapper.StopTripMapper;
import damose.data.mapper.TripMatcher;
import damose.model.Stop;
import damose.model.StopTime;
import damose.model.Trip;
import damose.model.TripServiceCalendar;
import damose.service.ArrivalService;
import damose.service.RouteService;

public final class ControllerDataLoader {

    public ControllerDataContext load() {
        System.out.println("Caricamento dati statici...");

        List<Stop> stops = StopsLoader.load();
        List<Trip> trips = TripsLoader.load();
        List<StopTime> stopTimes = StopTimesLoader.load();
        RoutesLoader.load(); 

        System.out.println("Stops loaded: " + (stops == null ? 0 : stops.size()));
        System.out.println("Trips loaded: " + (trips == null ? 0 : trips.size()));

        TripMatcher matcher = new TripMatcher(trips);
        StopTripMapper stopTripMapper = new StopTripMapper(stopTimes, matcher);
        RouteService routeService = new RouteService(trips, stopTimes, stops);

        TripServiceCalendar calendar;
        try {
            calendar = CalendarLoader.load();
        } catch (Exception e) {
            System.out.println("Could not load calendar_dates: " + e.getMessage());
            calendar = new TripServiceCalendar();
        }

        ArrivalService arrivalService = new ArrivalService(matcher, stopTripMapper, calendar);

        return new ControllerDataContext(
                stops,
                trips,
                stopTimes,
                matcher,
                stopTripMapper,
                routeService,
                arrivalService
        );
    }
}


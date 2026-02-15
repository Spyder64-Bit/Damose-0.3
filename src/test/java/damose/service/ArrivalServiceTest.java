package damose.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import damose.data.mapper.StopTripMapper;
import damose.data.mapper.TripMatcher;
import damose.model.ConnectionMode;
import damose.model.StopTime;
import damose.model.Trip;
import damose.model.TripServiceCalendar;
import damose.model.TripUpdateRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ArrivalService")
class ArrivalServiceTest {

    @Test
    @DisplayName("should not reuse one realtime prediction for multiple scheduled trips of same route")
    void shouldNotReuseSingleRouteFallbackAcrossMultipleTrips() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);

        Trip t1 = new Trip("88", "SVC", "TRIP_8833", "MARLIANA", "88", 0, "S1");
        Trip t2 = new Trip("88", "SVC", "TRIP_8857", "LABIA", "88", 0, "S2");
        List<Trip> trips = List.of(t1, t2);

        StopTime st1 = new StopTime("TRIP_8833", LocalTime.of(8, 33), LocalTime.of(8, 33),
                "STOP_1", 1, "", 0, 0, 0, 1);
        StopTime st2 = new StopTime("TRIP_8857", LocalTime.of(8, 57), LocalTime.of(8, 57),
                "STOP_1", 1, "", 0, 0, 0, 1);
        List<StopTime> stopTimes = List.of(st1, st2);

        TripMatcher matcher = new TripMatcher(trips);
        StopTripMapper mapper = new StopTripMapper(stopTimes, matcher);
        TripServiceCalendar calendar = new TripServiceCalendar();
        calendar.addServiceDate("SVC", today);

        ArrivalService service = new ArrivalService(matcher, mapper, calendar);

        long feedTs = today.atTime(12, 0).atZone(zone).toEpochSecond();
        long singleRealtimeEpoch = today.atTime(8, 47).atZone(zone).toEpochSecond();

        service.updateRealtimeArrivals(List.of(
                new TripUpdateRecord("UNMATCHED_RT_TRIP", "88", "STOP_1", singleRealtimeEpoch)
        ));

        List<String> tripsToday = service.getAllTripsForStopToday("STOP_1", ConnectionMode.ONLINE, feedTs);

        assertEquals(2, tripsToday.size());
        assertTrue(tripsToday.get(0).startsWith("08:33 | 88"));
        assertTrue(tripsToday.get(1).startsWith("08:57 | 88"));

        assertFalse(tripsToday.get(0).contains("["));
        assertTrue(tripsToday.get(1).contains("[-10 min]"));
        assertTrue(tripsToday.stream().noneMatch(s -> s.contains("[+14 min]")));
    }
}

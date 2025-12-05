package damose.service;

import damose.data.model.Stop;
import damose.data.model.StopTime;
import damose.data.model.Trip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RouteService.
 */
@DisplayName("RouteService")
class RouteServiceTest {

    private RouteService routeService;
    private List<Trip> trips;
    private List<StopTime> stopTimes;
    private List<Stop> stops;

    @BeforeEach
    void setUp() {
        // Create test data
        trips = Arrays.asList(
            new Trip("64", "S1", "T64_1", "Termini → Colosseo", "64", 0, "SH1"),
            new Trip("64", "S1", "T64_2", "Colosseo → Termini", "64", 1, "SH2"),
            new Trip("75", "S1", "T75_1", "Piazza Venezia", "75", 0, "SH3"),
            new Trip("75", "S1", "T75_2", "Termini", "75", 1, "SH4")
        );

        stops = Arrays.asList(
            new Stop("S1", "C1", "Termini", 41.90, 12.50),
            new Stop("S2", "C2", "Repubblica", 41.91, 12.51),
            new Stop("S3", "C3", "Colosseo", 41.89, 12.49),
            new Stop("S4", "C4", "Piazza Venezia", 41.89, 12.48)
        );

        LocalTime t1 = LocalTime.of(8, 0);
        LocalTime t2 = LocalTime.of(8, 10);
        LocalTime t3 = LocalTime.of(8, 20);

        stopTimes = Arrays.asList(
            // Trip T64_1: S1 -> S2 -> S3
            new StopTime("T64_1", t1, t1, "S1", 1, "", 0, 0, 0, 1),
            new StopTime("T64_1", t2, t2, "S2", 2, "", 0, 0, 0, 1),
            new StopTime("T64_1", t3, t3, "S3", 3, "", 0, 0, 0, 1),
            // Trip T64_2: S3 -> S2 -> S1
            new StopTime("T64_2", t1, t1, "S3", 1, "", 0, 0, 0, 1),
            new StopTime("T64_2", t2, t2, "S2", 2, "", 0, 0, 0, 1),
            new StopTime("T64_2", t3, t3, "S1", 3, "", 0, 0, 0, 1),
            // Trip T75_1: S1 -> S4 (shorter trip)
            new StopTime("T75_1", t1, t1, "S1", 1, "", 0, 0, 0, 1),
            new StopTime("T75_1", t2, t2, "S4", 2, "", 0, 0, 0, 1)
        );

        routeService = new RouteService(trips, stopTimes, stops);
    }

    @Nested
    @DisplayName("findRepresentativeTrip()")
    class FindRepresentativeTripTests {

        @Test
        @DisplayName("should find trip by routeId")
        void shouldFindTripByRouteId() {
            Trip result = routeService.findRepresentativeTrip("64", null);
            assertNotNull(result);
            assertEquals("64", result.getRouteId());
        }

        @Test
        @DisplayName("should find trip by routeId and headsign")
        void shouldFindTripByRouteIdAndHeadsign() {
            Trip result = routeService.findRepresentativeTrip("64", "Colosseo → Termini");
            assertNotNull(result);
            assertEquals("T64_2", result.getTripId());
        }

        @Test
        @DisplayName("should return null for unknown routeId")
        void shouldReturnNullForUnknownRouteId() {
            Trip result = routeService.findRepresentativeTrip("999", null);
            assertNull(result);
        }

        @Test
        @DisplayName("should be case insensitive")
        void shouldBeCaseInsensitive() {
            Trip result = routeService.findRepresentativeTrip("64", "termini → colosseo");
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("findTripsByRouteId()")
    class FindTripsByRouteIdTests {

        @Test
        @DisplayName("should find all trips for route")
        void shouldFindAllTripsForRoute() {
            List<Trip> result = routeService.findTripsByRouteId("64");
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("should return empty list for unknown route")
        void shouldReturnEmptyListForUnknownRoute() {
            List<Trip> result = routeService.findTripsByRouteId("999");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getStopsForTrip()")
    class GetStopsForTripTests {

        @Test
        @DisplayName("should return ordered stops for trip")
        void shouldReturnOrderedStopsForTrip() {
            List<Stop> result = routeService.getStopsForTrip("T64_1");
            assertEquals(3, result.size());
            assertEquals("S1", result.get(0).getStopId());
            assertEquals("S2", result.get(1).getStopId());
            assertEquals("S3", result.get(2).getStopId());
        }

        @Test
        @DisplayName("should return empty list for null tripId")
        void shouldReturnEmptyListForNullTripId() {
            List<Stop> result = routeService.getStopsForTrip(null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty list for unknown tripId")
        void shouldReturnEmptyListForUnknownTripId() {
            List<Stop> result = routeService.getStopsForTrip("UNKNOWN");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getStopsForRoute()")
    class GetStopsForRouteTests {

        @Test
        @DisplayName("should return stops using trip with most stops")
        void shouldReturnStopsUsingTripWithMostStops() {
            List<Stop> result = routeService.getStopsForRoute("64");
            assertEquals(3, result.size()); // T64_1 and T64_2 both have 3 stops
        }

        @Test
        @DisplayName("should return empty list for null routeId")
        void shouldReturnEmptyListForNullRouteId() {
            List<Stop> result = routeService.getStopsForRoute(null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty list for unknown routeId")
        void shouldReturnEmptyListForUnknownRouteId() {
            List<Stop> result = routeService.getStopsForRoute("UNKNOWN");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getStopsForRouteAndHeadsign()")
    class GetStopsForRouteAndHeadsignTests {

        @Test
        @DisplayName("should return stops for specific direction")
        void shouldReturnStopsForSpecificDirection() {
            List<Stop> outbound = routeService.getStopsForRouteAndHeadsign("64", "Termini → Colosseo");
            List<Stop> inbound = routeService.getStopsForRouteAndHeadsign("64", "Colosseo → Termini");
            
            assertEquals(3, outbound.size());
            assertEquals(3, inbound.size());
            
            // Different order for different directions
            assertEquals("S1", outbound.get(0).getStopId());
            assertEquals("S3", inbound.get(0).getStopId());
        }

        @Test
        @DisplayName("should return empty list for unknown headsign")
        void shouldReturnEmptyListForUnknownHeadsign() {
            List<Stop> result = routeService.getStopsForRouteAndHeadsign("64", "Unknown Direction");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getHeadsignsForRoute()")
    class GetHeadsignsForRouteTests {

        @Test
        @DisplayName("should return all headsigns for route")
        void shouldReturnAllHeadsignsForRoute() {
            List<String> result = routeService.getHeadsignsForRoute("64");
            assertEquals(2, result.size());
            assertTrue(result.contains("Termini → Colosseo"));
            assertTrue(result.contains("Colosseo → Termini"));
        }

        @Test
        @DisplayName("should return distinct headsigns")
        void shouldReturnDistinctHeadsigns() {
            List<String> result = routeService.getHeadsignsForRoute("75");
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("should return empty list for unknown route")
        void shouldReturnEmptyListForUnknownRoute() {
            List<String> result = routeService.getHeadsignsForRoute("UNKNOWN");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle empty trips list")
        void shouldHandleEmptyTripsList() {
            RouteService emptyService = new RouteService(
                Collections.emptyList(), 
                stopTimes, 
                stops
            );
            assertTrue(emptyService.findTripsByRouteId("64").isEmpty());
        }

        @Test
        @DisplayName("should handle empty stopTimes list")
        void shouldHandleEmptyStopTimesList() {
            RouteService emptyService = new RouteService(
                trips, 
                Collections.emptyList(), 
                stops
            );
            assertTrue(emptyService.getStopsForTrip("T64_1").isEmpty());
        }

        @Test
        @DisplayName("should handle empty stops list")
        void shouldHandleEmptyStopsList() {
            RouteService emptyService = new RouteService(
                trips, 
                stopTimes, 
                Collections.emptyList()
            );
            assertTrue(emptyService.getStopsForTrip("T64_1").isEmpty());
        }
    }
}


package damose.data.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Trip model.
 */
@DisplayName("Trip")
class TripTest {

    private Trip trip;

    @BeforeEach
    void setUp() {
        trip = new Trip("64", "SERVICE_A", "TRIP001", "Termini → Colosseo", "64A", 0, "SHAPE001");
    }

    @Nested
    @DisplayName("Constructor and Getters")
    class ConstructorAndGettersTests {

        @Test
        @DisplayName("should create trip with correct values")
        void shouldCreateTripWithCorrectValues() {
            assertEquals("64", trip.getRouteId());
            assertEquals("SERVICE_A", trip.getServiceId());
            assertEquals("TRIP001", trip.getTripId());
            assertEquals("Termini → Colosseo", trip.getTripHeadsign());
            assertEquals("64A", trip.getTripShortName());
            assertEquals(0, trip.getDirectionId());
            assertEquals("SHAPE001", trip.getShapeId());
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("should include route ID, trip ID and headsign")
        void shouldIncludeMainFields() {
            String str = trip.toString();
            assertTrue(str.contains("64"));
            assertTrue(str.contains("TRIP001"));
            assertTrue(str.contains("Termini → Colosseo"));
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself() {
            assertEquals(trip, trip);
        }

        @Test
        @DisplayName("should be equal to trip with same tripId")
        void shouldBeEqualToTripWithSameTripId() {
            Trip sameTrip = new Trip("DIFFERENT", "DIFFERENT", "TRIP001", "Different", "X", 1, "Y");
            assertEquals(trip, sameTrip);
        }

        @Test
        @DisplayName("should not be equal to trip with different tripId")
        void shouldNotBeEqualToTripWithDifferentTripId() {
            Trip differentTrip = new Trip("64", "SERVICE_A", "TRIP002", "Termini → Colosseo", "64A", 0, "SHAPE001");
            assertNotEquals(trip, differentTrip);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            assertNotEquals(null, trip);
        }

        @Test
        @DisplayName("should have same hashCode for equal trips")
        void shouldHaveSameHashCodeForEqualTrips() {
            Trip sameTrip = new Trip("DIFFERENT", "DIFFERENT", "TRIP001", "Different", "X", 1, "Y");
            assertEquals(trip.hashCode(), sameTrip.hashCode());
        }
    }

    @Nested
    @DisplayName("Direction ID")
    class DirectionIdTests {

        @Test
        @DisplayName("should handle direction 0 (outbound)")
        void shouldHandleDirectionZero() {
            Trip outbound = new Trip("64", "S", "T1", "Out", "", 0, "");
            assertEquals(0, outbound.getDirectionId());
        }

        @Test
        @DisplayName("should handle direction 1 (inbound)")
        void shouldHandleDirectionOne() {
            Trip inbound = new Trip("64", "S", "T1", "In", "", 1, "");
            assertEquals(1, inbound.getDirectionId());
        }
    }
}


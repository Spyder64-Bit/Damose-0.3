package damose.data.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StopTime")
class StopTimeTest {

    private StopTime stopTime;
    private LocalTime arrivalTime;
    private LocalTime departureTime;

    @BeforeEach
    void setUp() {
        arrivalTime = LocalTime.of(8, 30, 0);
        departureTime = LocalTime.of(8, 31, 0);
        stopTime = new StopTime(
            "TRIP001",
            arrivalTime,
            departureTime,
            "STOP001",
            5,
            "Colosseo",
            0,
            0,
            1500.5,
            1
        );
    }

    @Nested
    @DisplayName("Constructor and Getters")
    class ConstructorAndGettersTests {

        @Test
        @DisplayName("should create stop time with correct values")
        void shouldCreateStopTimeWithCorrectValues() {
            assertEquals("TRIP001", stopTime.getTripId());
            assertEquals(arrivalTime, stopTime.getArrivalTime());
            assertEquals(departureTime, stopTime.getDepartureTime());
            assertEquals("STOP001", stopTime.getStopId());
            assertEquals(5, stopTime.getStopSequence());
            assertEquals("Colosseo", stopTime.getStopHeadsign());
            assertEquals(0, stopTime.getPickupType());
            assertEquals(0, stopTime.getDropOffType());
            assertEquals(1500.5, stopTime.getShapeDistTraveled(), 0.01);
            assertEquals(1, stopTime.getTimepoint());
        }
    }

    @Nested
    @DisplayName("Time Handling")
    class TimeHandlingTests {

        @Test
        @DisplayName("should handle null arrival time")
        void shouldHandleNullArrivalTime() {
            StopTime st = new StopTime("T1", null, departureTime, "S1", 1, "", 0, 0, 0, 0);
            assertNull(st.getArrivalTime());
        }

        @Test
        @DisplayName("should handle null departure time")
        void shouldHandleNullDepartureTime() {
            StopTime st = new StopTime("T1", arrivalTime, null, "S1", 1, "", 0, 0, 0, 0);
            assertNull(st.getDepartureTime());
        }

        @Test
        @DisplayName("should handle midnight time")
        void shouldHandleMidnight() {
            LocalTime midnight = LocalTime.MIDNIGHT;
            StopTime st = new StopTime("T1", midnight, midnight, "S1", 1, "", 0, 0, 0, 0);
            assertEquals(LocalTime.of(0, 0), st.getArrivalTime());
        }

        @Test
        @DisplayName("should handle late night time (23:59)")
        void shouldHandleLateNight() {
            LocalTime lateNight = LocalTime.of(23, 59, 59);
            StopTime st = new StopTime("T1", lateNight, lateNight, "S1", 1, "", 0, 0, 0, 0);
            assertEquals(23, st.getArrivalTime().getHour());
            assertEquals(59, st.getArrivalTime().getMinute());
        }
    }

    @Nested
    @DisplayName("Stop Sequence")
    class StopSequenceTests {

        @Test
        @DisplayName("should handle first stop (sequence 1)")
        void shouldHandleFirstStop() {
            StopTime first = new StopTime("T1", arrivalTime, departureTime, "S1", 1, "", 0, 0, 0, 0);
            assertEquals(1, first.getStopSequence());
        }

        @Test
        @DisplayName("should handle large sequence number")
        void shouldHandleLargeSequence() {
            StopTime last = new StopTime("T1", arrivalTime, departureTime, "S1", 100, "", 0, 0, 0, 0);
            assertEquals(100, last.getStopSequence());
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("should include trip ID, stop ID and arrival time")
        void shouldIncludeMainFields() {
            String str = stopTime.toString();
            assertTrue(str.contains("TRIP001"));
            assertTrue(str.contains("STOP001"));
            assertTrue(str.contains("08:30"));
        }
    }

    @Nested
    @DisplayName("Pickup and DropOff Types")
    class PickupDropOffTests {

        @Test
        @DisplayName("should handle regular pickup (0)")
        void shouldHandleRegularPickup() {
            assertEquals(0, stopTime.getPickupType());
        }

        @Test
        @DisplayName("should handle no pickup available (1)")
        void shouldHandleNoPickup() {
            StopTime st = new StopTime("T1", arrivalTime, departureTime, "S1", 1, "", 1, 0, 0, 0);
            assertEquals(1, st.getPickupType());
        }

        @Test
        @DisplayName("should handle phone-ahead pickup (2)")
        void shouldHandlePhoneAheadPickup() {
            StopTime st = new StopTime("T1", arrivalTime, departureTime, "S1", 1, "", 2, 0, 0, 0);
            assertEquals(2, st.getPickupType());
        }
    }
}












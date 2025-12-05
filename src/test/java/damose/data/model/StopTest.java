package damose.data.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Stop model.
 */
@DisplayName("Stop")
class StopTest {

    private Stop stop;

    @BeforeEach
    void setUp() {
        stop = new Stop("STOP001", "S001", "Termini Station", 41.9010, 12.5010);
    }

    @Nested
    @DisplayName("Constructor and Getters")
    class ConstructorAndGettersTests {

        @Test
        @DisplayName("should create stop with correct values")
        void shouldCreateStopWithCorrectValues() {
            assertEquals("STOP001", stop.getStopId());
            assertEquals("S001", stop.getStopCode());
            assertEquals("Termini Station", stop.getStopName());
            assertEquals(41.9010, stop.getStopLat(), 0.0001);
            assertEquals(12.5010, stop.getStopLon(), 0.0001);
        }

        @Test
        @DisplayName("should not be fake line by default")
        void shouldNotBeFakeLineByDefault() {
            assertFalse(stop.isFakeLine());
        }
    }

    @Nested
    @DisplayName("Fake Line Feature")
    class FakeLineTests {

        @Test
        @DisplayName("should mark stop as fake line")
        void shouldMarkAsFakeLine() {
            stop.markAsFakeLine();
            assertTrue(stop.isFakeLine());
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("should format as 'id - name' for real stop")
        void shouldFormatAsIdNameForRealStop() {
            assertEquals("STOP001 - Termini Station", stop.toString());
        }

        @Test
        @DisplayName("should format as name only for fake line")
        void shouldFormatAsNameOnlyForFakeLine() {
            stop.markAsFakeLine();
            assertEquals("Termini Station", stop.toString());
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself() {
            assertEquals(stop, stop);
        }

        @Test
        @DisplayName("should be equal to stop with same ID")
        void shouldBeEqualToStopWithSameId() {
            Stop sameStop = new Stop("STOP001", "DIFFERENT", "Different Name", 0.0, 0.0);
            assertEquals(stop, sameStop);
        }

        @Test
        @DisplayName("should not be equal to stop with different ID")
        void shouldNotBeEqualToStopWithDifferentId() {
            Stop differentStop = new Stop("STOP002", "S001", "Termini Station", 41.9010, 12.5010);
            assertNotEquals(stop, differentStop);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            assertNotEquals(null, stop);
        }

        @Test
        @DisplayName("should have same hashCode for equal stops")
        void shouldHaveSameHashCodeForEqualStops() {
            Stop sameStop = new Stop("STOP001", "DIFFERENT", "Different Name", 0.0, 0.0);
            assertEquals(stop.hashCode(), sameStop.hashCode());
        }
    }
}


package damose.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VehicleType enum.
 */
@DisplayName("VehicleType")
class VehicleTypeTest {

    @Nested
    @DisplayName("fromGtfsCode(int)")
    class FromGtfsCodeIntTests {

        @ParameterizedTest
        @CsvSource({
            "0, TRAM",
            "1, METRO",
            "2, RAIL",
            "3, BUS",
            "4, FERRY",
            "5, CABLE_CAR",
            "6, GONDOLA",
            "7, FUNICULAR"
        })
        @DisplayName("should return correct type for valid GTFS codes")
        void shouldReturnCorrectTypeForValidCodes(int code, VehicleType expected) {
            assertEquals(expected, VehicleType.fromGtfsCode(code));
        }

        @Test
        @DisplayName("should return UNKNOWN for invalid code")
        void shouldReturnUnknownForInvalidCode() {
            assertEquals(VehicleType.UNKNOWN, VehicleType.fromGtfsCode(99));
            assertEquals(VehicleType.UNKNOWN, VehicleType.fromGtfsCode(-5));
        }
    }

    @Nested
    @DisplayName("fromGtfsCode(String)")
    class FromGtfsCodeStringTests {

        @ParameterizedTest
        @CsvSource({
            "0, TRAM",
            "1, METRO",
            "3, BUS",
            "'  3  ', BUS"
        })
        @DisplayName("should parse valid string codes")
        void shouldParseValidStringCodes(String code, VehicleType expected) {
            assertEquals(expected, VehicleType.fromGtfsCode(code));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "abc", "not-a-number"})
        @DisplayName("should return UNKNOWN for invalid strings")
        void shouldReturnUnknownForInvalidStrings(String code) {
            assertEquals(VehicleType.UNKNOWN, VehicleType.fromGtfsCode(code));
        }
    }

    @Nested
    @DisplayName("Properties")
    class PropertiesTests {

        @Test
        @DisplayName("BUS should have correct properties")
        void busShouldHaveCorrectProperties() {
            VehicleType bus = VehicleType.BUS;
            assertEquals(3, bus.getGtfsCode());
            assertEquals("Bus", bus.getDisplayName());
            assertEquals("bus.png", bus.getIconFileName());
            assertNotNull(bus.getColor());
            assertEquals("🚌", bus.getEmoji());
        }

        @Test
        @DisplayName("TRAM should have correct properties")
        void tramShouldHaveCorrectProperties() {
            VehicleType tram = VehicleType.TRAM;
            assertEquals(0, tram.getGtfsCode());
            assertEquals("Tram", tram.getDisplayName());
            assertEquals("tram.png", tram.getIconFileName());
            assertEquals("🚋", tram.getEmoji());
        }

        @Test
        @DisplayName("METRO should have correct properties")
        void metroShouldHaveCorrectProperties() {
            VehicleType metro = VehicleType.METRO;
            assertEquals(1, metro.getGtfsCode());
            assertEquals("Metro", metro.getDisplayName());
            assertEquals("metro.png", metro.getIconFileName());
            assertEquals("🚇", metro.getEmoji());
        }
    }

    @Nested
    @DisplayName("All Types Have Required Properties")
    class AllTypesTests {

        @Test
        @DisplayName("all vehicle types should have non-null color")
        void allTypesShouldHaveColor() {
            for (VehicleType type : VehicleType.values()) {
                assertNotNull(type.getColor(), type + " should have a color");
            }
        }

        @Test
        @DisplayName("all vehicle types should have non-empty emoji")
        void allTypesShouldHaveEmoji() {
            for (VehicleType type : VehicleType.values()) {
                assertNotNull(type.getEmoji(), type + " should have an emoji");
                assertFalse(type.getEmoji().isEmpty(), type + " emoji should not be empty");
            }
        }

        @Test
        @DisplayName("all vehicle types should have display name")
        void allTypesShouldHaveDisplayName() {
            for (VehicleType type : VehicleType.values()) {
                assertNotNull(type.getDisplayName(), type + " should have display name");
                assertFalse(type.getDisplayName().isEmpty());
            }
        }

        @Test
        @DisplayName("all vehicle types should have icon filename")
        void allTypesShouldHaveIconFileName() {
            for (VehicleType type : VehicleType.values()) {
                assertNotNull(type.getIconFileName(), type + " should have icon filename");
                assertTrue(type.getIconFileName().endsWith(".png"));
            }
        }
    }

    @Nested
    @DisplayName("Rome Transit Types")
    class RomeTransitTests {

        @Test
        @DisplayName("Rome has BUS routes (type 3)")
        void romeShouldHaveBusRoutes() {
            assertEquals(VehicleType.BUS, VehicleType.fromGtfsCode(3));
        }

        @Test
        @DisplayName("Rome has TRAM routes (type 0)")
        void romeShouldHaveTramRoutes() {
            assertEquals(VehicleType.TRAM, VehicleType.fromGtfsCode(0));
        }

        @Test
        @DisplayName("Rome has METRO routes (type 1)")
        void romeShouldHaveMetroRoutes() {
            assertEquals(VehicleType.METRO, VehicleType.fromGtfsCode(1));
        }
    }
}


package damose.ui.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GeoUtils.
 */
@DisplayName("GeoUtils")
class GeoUtilsTest {

    @Nested
    @DisplayName("haversine()")
    class HaversineTests {

        @Test
        @DisplayName("should return 0 for same coordinates")
        void shouldReturnZeroForSameCoordinates() {
            double distance = GeoUtils.haversine(41.9028, 12.4964, 41.9028, 12.4964);
            assertEquals(0.0, distance, 0.001);
        }

        @Test
        @DisplayName("should calculate correct distance between Rome and Vatican")
        void shouldCalculateDistanceRomeToVatican() {
            // Rome center to Vatican (~1.5 km)
            double distance = GeoUtils.haversine(41.9028, 12.4964, 41.9029, 12.4534);
            assertTrue(distance > 3.0 && distance < 5.0, 
                "Distance should be roughly 3-5 km, was: " + distance);
        }

        @Test
        @DisplayName("should calculate correct distance between Rome and Milan")
        void shouldCalculateDistanceRomeToMilan() {
            // Rome (41.9028, 12.4964) to Milan (45.4642, 9.1900) - ~480 km
            double distance = GeoUtils.haversine(41.9028, 12.4964, 45.4642, 9.1900);
            assertTrue(distance > 450 && distance < 520, 
                "Distance should be roughly 480 km, was: " + distance);
        }

        @ParameterizedTest
        @CsvSource({
            "0.0, 0.0, 0.0, 1.0, 111.0",      // 1 degree longitude at equator ~111 km
            "0.0, 0.0, 1.0, 0.0, 111.0",      // 1 degree latitude ~111 km
        })
        @DisplayName("should calculate roughly 111 km per degree")
        void shouldCalculate111KmPerDegree(double lat1, double lon1, double lat2, double lon2, double expected) {
            double distance = GeoUtils.haversine(lat1, lon1, lat2, lon2);
            assertEquals(expected, distance, 5.0); // Allow 5 km tolerance
        }

        @Test
        @DisplayName("should handle negative coordinates")
        void shouldHandleNegativeCoordinates() {
            // Southern hemisphere
            double distance = GeoUtils.haversine(-33.8688, 151.2093, -37.8136, 144.9631);
            assertTrue(distance > 700 && distance < 800, 
                "Sydney to Melbourne should be ~700-800 km, was: " + distance);
        }

        @Test
        @DisplayName("should handle crossing equator")
        void shouldHandleCrossingEquator() {
            double distance = GeoUtils.haversine(10.0, 0.0, -10.0, 0.0);
            assertTrue(distance > 2200 && distance < 2300,
                "20 degrees of latitude should be ~2220 km, was: " + distance);
        }

        @Test
        @DisplayName("should handle crossing prime meridian")
        void shouldHandleCrossingPrimeMeridian() {
            double distance = GeoUtils.haversine(51.5074, -5.0, 51.5074, 5.0);
            assertTrue(distance > 600 && distance < 800,
                "Distance should be roughly 700 km, was: " + distance);
        }

        @Test
        @DisplayName("should be symmetric")
        void shouldBeSymmetric() {
            double d1 = GeoUtils.haversine(41.9028, 12.4964, 45.4642, 9.1900);
            double d2 = GeoUtils.haversine(45.4642, 9.1900, 41.9028, 12.4964);
            assertEquals(d1, d2, 0.001);
        }
    }

    @Nested
    @DisplayName("Distance Calculations for Rome")
    class RomeDistanceTests {

        // Rome coordinates
        private static final double TERMINI_LAT = 41.9010;
        private static final double TERMINI_LON = 12.5010;
        private static final double COLOSSEO_LAT = 41.8902;
        private static final double COLOSSEO_LON = 12.4922;
        private static final double VATICANO_LAT = 41.9029;
        private static final double VATICANO_LON = 12.4534;

        @Test
        @DisplayName("should calculate Termini to Colosseo (~1.5 km)")
        void shouldCalculateTerminiToColosseo() {
            double distance = GeoUtils.haversine(
                TERMINI_LAT, TERMINI_LON, 
                COLOSSEO_LAT, COLOSSEO_LON
            );
            assertTrue(distance > 1.0 && distance < 2.5,
                "Termini to Colosseo should be ~1.5 km, was: " + distance);
        }

        @Test
        @DisplayName("should calculate Termini to Vaticano (~3.5 km)")
        void shouldCalculateTerminiToVaticano() {
            double distance = GeoUtils.haversine(
                TERMINI_LAT, TERMINI_LON,
                VATICANO_LAT, VATICANO_LON
            );
            assertTrue(distance > 3.0 && distance < 5.0,
                "Termini to Vaticano should be ~3.5 km, was: " + distance);
        }
    }
}


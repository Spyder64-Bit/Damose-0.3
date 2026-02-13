package damose.data.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TripIdUtils")
class TripIdUtilsTest {

    @Nested
    @DisplayName("normalizeSimple()")
    class NormalizeSimpleTests {

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNullForNullInput() {
            assertNull(TripIdUtils.normalizeSimple(null));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("should return null for empty or whitespace input")
        void shouldReturnNullForEmptyInput(String input) {
            assertNull(TripIdUtils.normalizeSimple(input));
        }

        @ParameterizedTest
        @CsvSource({
            "0#4930-11, 4930-11",
            "1#ABC123, abc123",
            "12#test-trip, test-trip"
        })
        @DisplayName("should remove digit# prefix")
        void shouldRemoveDigitHashPrefix(String input, String expected) {
            assertEquals(expected, TripIdUtils.normalizeSimple(input));
        }

        @ParameterizedTest
        @CsvSource({
            "agency:TRIP123, trip123",
            "AGENCY:test, test",
            "Agency:Mixed, mixed"
        })
        @DisplayName("should remove agency: prefix")
        void shouldRemoveAgencyPrefix(String input, String expected) {
            assertEquals(expected, TripIdUtils.normalizeSimple(input));
        }

        @ParameterizedTest
        @CsvSource({
            "trip:12345, 12345",
            "TRIP:ABC, abc"
        })
        @DisplayName("should remove trip: prefix")
        void shouldRemoveTripPrefix(String input, String expected) {
            assertEquals(expected, TripIdUtils.normalizeSimple(input));
        }

        @Test
        @DisplayName("should convert to lowercase")
        void shouldConvertToLowercase() {
            assertEquals("abc123", TripIdUtils.normalizeSimple("ABC123"));
            assertEquals("mixedcase", TripIdUtils.normalizeSimple("MixedCase"));
        }

        @Test
        @DisplayName("should keep letters, numbers, dash and underscore")
        void shouldKeepValidCharacters() {
            assertEquals("abc-123_def", TripIdUtils.normalizeSimple("ABC-123_DEF"));
        }

        @Test
        @DisplayName("should remove special characters")
        void shouldRemoveSpecialCharacters() {
            assertEquals("abc123", TripIdUtils.normalizeSimple("ABC!@#123"));
            assertEquals("test", TripIdUtils.normalizeSimple("test?!"));
        }

        @Test
        @DisplayName("should trim leading/trailing separators")
        void shouldTrimSeparators() {
            assertEquals("test", TripIdUtils.normalizeSimple("-test-"));
            assertEquals("test", TripIdUtils.normalizeSimple("_test_"));
            assertEquals("test", TripIdUtils.normalizeSimple("...test..."));
        }

        @Test
        @DisplayName("should preserve meaningful trailing digits")
        void shouldPreserveMeaningfulTrailingDigits() {
            assertEquals("4900", TripIdUtils.normalizeSimple("4900"));
            assertEquals("line-120", TripIdUtils.normalizeSimple("LINE-120"));
        }

    }

    @Nested
    @DisplayName("generateVariants()")
    class GenerateVariantsTests {

        @Test
        @DisplayName("should return empty set for null input")
        void shouldReturnEmptySetForNull() {
            Set<String> variants = TripIdUtils.generateVariants(null);
            assertTrue(variants.isEmpty());
        }

        @Test
        @DisplayName("should include normalized form")
        void shouldIncludeNormalizedForm() {
            Set<String> variants = TripIdUtils.generateVariants("0#TEST-123");
            assertTrue(variants.contains("test-123"));
        }

        @Test
        @DisplayName("should include variant without separators")
        void shouldIncludeVariantWithoutSeparators() {
            Set<String> variants = TripIdUtils.generateVariants("test-123_abc");
            assertTrue(variants.contains("test123abc"));
        }

        @Test
        @DisplayName("should include variant with dash replaced by underscore")
        void shouldIncludeDashToUnderscoreVariant() {
            Set<String> variants = TripIdUtils.generateVariants("test-123");
            assertTrue(variants.contains("test_123"));
        }

        @Test
        @DisplayName("should include variant with underscore replaced by dash")
        void shouldIncludeUnderscoreToDashVariant() {
            Set<String> variants = TripIdUtils.generateVariants("test_123");
            assertTrue(variants.contains("test-123"));
        }

        @Test
        @DisplayName("should handle dots in trip ID")
        void shouldHandleDots() {
            Set<String> variants = TripIdUtils.generateVariants("test.123");
            assertTrue(variants.contains("test-123"));
            assertTrue(variants.contains("test_123"));
            assertTrue(variants.contains("test123"));
        }

        @Test
        @DisplayName("should generate multiple variants for complex ID")
        void shouldGenerateMultipleVariants() {
            Set<String> variants = TripIdUtils.generateVariants("0#LINE-123_A");
            assertFalse(variants.isEmpty());
            assertTrue(variants.size() >= 2);
        }
    }

    @Nested
    @DisplayName("normalizeOrEmpty()")
    class NormalizeOrEmptyTests {

        @Test
        @DisplayName("should return empty string for null")
        void shouldReturnEmptyForNull() {
            assertEquals("", TripIdUtils.normalizeOrEmpty(null));
        }

        @Test
        @DisplayName("should return empty string for empty input")
        void shouldReturnEmptyForEmptyInput() {
            assertEquals("", TripIdUtils.normalizeOrEmpty(""));
            assertEquals("", TripIdUtils.normalizeOrEmpty("   "));
        }

        @Test
        @DisplayName("should return normalized value for valid input")
        void shouldReturnNormalizedValue() {
            assertEquals("test123", TripIdUtils.normalizeOrEmpty("TEST123"));
        }
    }
}












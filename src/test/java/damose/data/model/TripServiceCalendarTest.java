package damose.data.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TripServiceCalendar")
class TripServiceCalendarTest {

    private TripServiceCalendar calendar;

    @BeforeEach
    void setUp() {
        calendar = new TripServiceCalendar();
    }

    @Nested
    @DisplayName("addServiceDate()")
    class AddServiceDateTests {

        @Test
        @DisplayName("should add service date")
        void shouldAddServiceDate() {
            LocalDate date = LocalDate.of(2024, 12, 5);
            calendar.addServiceDate("SERVICE_A", date);
            
            assertTrue(calendar.serviceRunsOnDate("SERVICE_A", date));
        }

        @Test
        @DisplayName("should add multiple dates for same service")
        void shouldAddMultipleDatesForSameService() {
            LocalDate date1 = LocalDate.of(2024, 12, 5);
            LocalDate date2 = LocalDate.of(2024, 12, 6);
            
            calendar.addServiceDate("SERVICE_A", date1);
            calendar.addServiceDate("SERVICE_A", date2);
            
            assertTrue(calendar.serviceRunsOnDate("SERVICE_A", date1));
            assertTrue(calendar.serviceRunsOnDate("SERVICE_A", date2));
        }

        @Test
        @DisplayName("should add dates for different services")
        void shouldAddDatesForDifferentServices() {
            LocalDate date = LocalDate.of(2024, 12, 5);
            
            calendar.addServiceDate("SERVICE_A", date);
            calendar.addServiceDate("SERVICE_B", date);
            
            assertTrue(calendar.serviceRunsOnDate("SERVICE_A", date));
            assertTrue(calendar.serviceRunsOnDate("SERVICE_B", date));
        }

        @Test
        @DisplayName("should handle duplicate additions")
        void shouldHandleDuplicateAdditions() {
            LocalDate date = LocalDate.of(2024, 12, 5);
            
            calendar.addServiceDate("SERVICE_A", date);
            calendar.addServiceDate("SERVICE_A", date); // Nota in italiano
            
            assertEquals(1, calendar.serviceCount());
            assertTrue(calendar.serviceRunsOnDate("SERVICE_A", date));
        }
    }

    @Nested
    @DisplayName("removeServiceDate()")
    class RemoveServiceDateTests {

        @Test
        @DisplayName("should remove service date")
        void shouldRemoveServiceDate() {
            LocalDate date = LocalDate.of(2024, 12, 5);
            calendar.addServiceDate("SERVICE_A", date);
            
            calendar.removeServiceDate("SERVICE_A", date);
            
            assertFalse(calendar.serviceRunsOnDate("SERVICE_A", date));
        }

        @Test
        @DisplayName("should handle removing non-existent date")
        void shouldHandleRemovingNonExistentDate() {
            LocalDate date = LocalDate.of(2024, 12, 5);
            
            assertDoesNotThrow(() -> 
                calendar.removeServiceDate("SERVICE_A", date)
            );
        }

        @Test
        @DisplayName("should remove service when all dates removed")
        void shouldRemoveServiceWhenAllDatesRemoved() {
            LocalDate date = LocalDate.of(2024, 12, 5);
            calendar.addServiceDate("SERVICE_A", date);
            
            calendar.removeServiceDate("SERVICE_A", date);
            
            assertEquals(0, calendar.serviceCount());
        }

        @Test
        @DisplayName("should only remove specified date")
        void shouldOnlyRemoveSpecifiedDate() {
            LocalDate date1 = LocalDate.of(2024, 12, 5);
            LocalDate date2 = LocalDate.of(2024, 12, 6);
            calendar.addServiceDate("SERVICE_A", date1);
            calendar.addServiceDate("SERVICE_A", date2);
            
            calendar.removeServiceDate("SERVICE_A", date1);
            
            assertFalse(calendar.serviceRunsOnDate("SERVICE_A", date1));
            assertTrue(calendar.serviceRunsOnDate("SERVICE_A", date2));
        }
    }

    @Nested
    @DisplayName("serviceRunsOnDate()")
    class ServiceRunsOnDateTests {

        @Test
        @DisplayName("should return false for unknown service")
        void shouldReturnFalseForUnknownService() {
            assertFalse(calendar.serviceRunsOnDate("UNKNOWN", LocalDate.now()));
        }

        @Test
        @DisplayName("should return false for date not in service")
        void shouldReturnFalseForDateNotInService() {
            LocalDate date1 = LocalDate.of(2024, 12, 5);
            LocalDate date2 = LocalDate.of(2024, 12, 6);
            
            calendar.addServiceDate("SERVICE_A", date1);
            
            assertFalse(calendar.serviceRunsOnDate("SERVICE_A", date2));
        }

        @Test
        @DisplayName("should return true for date in service")
        void shouldReturnTrueForDateInService() {
            LocalDate date = LocalDate.of(2024, 12, 5);
            calendar.addServiceDate("SERVICE_A", date);
            
            assertTrue(calendar.serviceRunsOnDate("SERVICE_A", date));
        }
    }

    @Nested
    @DisplayName("serviceCount()")
    class ServiceCountTests {

        @Test
        @DisplayName("should return 0 for empty calendar")
        void shouldReturnZeroForEmptyCalendar() {
            assertEquals(0, calendar.serviceCount());
        }

        @Test
        @DisplayName("should count unique services")
        void shouldCountUniqueServices() {
            LocalDate date = LocalDate.of(2024, 12, 5);
            
            calendar.addServiceDate("SERVICE_A", date);
            calendar.addServiceDate("SERVICE_B", date);
            calendar.addServiceDate("SERVICE_C", date);
            
            assertEquals(3, calendar.serviceCount());
        }

        @Test
        @DisplayName("should not count service with no dates")
        void shouldNotCountServiceWithNoDates() {
            LocalDate date = LocalDate.of(2024, 12, 5);
            calendar.addServiceDate("SERVICE_A", date);
            calendar.removeServiceDate("SERVICE_A", date);
            
            assertEquals(0, calendar.serviceCount());
        }
    }

    @Nested
    @DisplayName("Real-world Scenarios")
    class RealWorldScenarioTests {

        @Test
        @DisplayName("should handle weekday service")
        void shouldHandleWeekdayService() {
            for (int day = 2; day <= 6; day++) { // Nota in italiano
                calendar.addServiceDate("WEEKDAY", LocalDate.of(2024, 12, day));
            }
            
            assertTrue(calendar.serviceRunsOnDate("WEEKDAY", LocalDate.of(2024, 12, 2)));
            assertTrue(calendar.serviceRunsOnDate("WEEKDAY", LocalDate.of(2024, 12, 6)));
            assertFalse(calendar.serviceRunsOnDate("WEEKDAY", LocalDate.of(2024, 12, 7)));
        }

        @Test
        @DisplayName("should handle holiday exception")
        void shouldHandleHolidayException() {
            LocalDate christmas = LocalDate.of(2024, 12, 25);
            LocalDate boxingDay = LocalDate.of(2024, 12, 26);
            
            calendar.addServiceDate("HOLIDAY", christmas);
            calendar.addServiceDate("HOLIDAY", boxingDay);
            
            assertTrue(calendar.serviceRunsOnDate("HOLIDAY", christmas));
            assertTrue(calendar.serviceRunsOnDate("HOLIDAY", boxingDay));
            assertFalse(calendar.serviceRunsOnDate("HOLIDAY", LocalDate.of(2024, 12, 24)));
        }
    }
}









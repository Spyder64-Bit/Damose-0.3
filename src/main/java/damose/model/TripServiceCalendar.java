package damose.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Domain model for trip service calendar.
 */
public class TripServiceCalendar {

    private final Map<String, Set<LocalDate>> serviceDates = new HashMap<>();

    /**
     * Handles addServiceDate.
     */
    public void addServiceDate(String serviceId, LocalDate date) {
        serviceDates.computeIfAbsent(serviceId, k -> new HashSet<>()).add(date);
    }

    /**
     * Handles removeServiceDate.
     */
    public void removeServiceDate(String serviceId, LocalDate date) {
        Set<LocalDate> dates = serviceDates.get(serviceId);
        if (dates != null) {
            dates.remove(date);
            if (dates.isEmpty()) {
                serviceDates.remove(serviceId);
            }
        }
    }

    /**
     * Returns the result of serviceRunsOnDate.
     */
    public boolean serviceRunsOnDate(String serviceId, LocalDate date) {
        Set<LocalDate> dates = serviceDates.get(serviceId);
        return dates != null && dates.contains(date);
    }

    /**
     * Returns the result of serviceCount.
     */
    public int serviceCount() {
        return serviceDates.size();
    }
}


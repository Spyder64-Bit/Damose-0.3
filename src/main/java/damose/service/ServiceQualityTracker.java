package damose.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides service logic for service quality tracker.
 */
public class ServiceQualityTracker {

    private static final ServiceQualityTracker INSTANCE = new ServiceQualityTracker();

    private final AtomicInteger activeVehicles = new AtomicInteger(0);
    private final AtomicInteger totalUpdatesToday = new AtomicInteger(0);
    private final AtomicLong lastUpdateTimestamp = new AtomicLong(0);

    private final ConcurrentHashMap<String, Long> tripDelays = new ConcurrentHashMap<>();
    private final AtomicInteger onTimeCount = new AtomicInteger(0);
    private final AtomicInteger delayedCount = new AtomicInteger(0);
    private final AtomicInteger earlyCount = new AtomicInteger(0);

    private final List<Integer> vehicleHistory = new ArrayList<>();
    private final List<Double> delayHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 10;

    private ServiceQualityTracker() {}

    /**
     * Returns the instance.
     */
    public static ServiceQualityTracker getInstance() {
        return INSTANCE;
    }

    /**
     * Handles updateVehicleCount.
     */
    public void updateVehicleCount(int count) {
        activeVehicles.set(count);
        lastUpdateTimestamp.set(Instant.now().getEpochSecond());
        totalUpdatesToday.incrementAndGet();

        synchronized (vehicleHistory) {
            vehicleHistory.add(count);
            if (vehicleHistory.size() > MAX_HISTORY) {
                vehicleHistory.remove(0);
            }
        }
    }

    /**
     * Handles recordTripDelay.
     */
    public void recordTripDelay(String tripId, long delaySeconds) {
        tripDelays.put(tripId, delaySeconds);

        if (delaySeconds > 300) {
            delayedCount.incrementAndGet();
        } else if (delaySeconds < -120) {
            earlyCount.incrementAndGet();
        } else {
            onTimeCount.incrementAndGet();
        }

        double avgDelay = getAverageDelayMinutes();
        synchronized (delayHistory) {
            delayHistory.add(avgDelay);
            if (delayHistory.size() > MAX_HISTORY) {
                delayHistory.remove(0);
            }
        }
    }

    /**
     * Returns the active vehicles.
     */
    public int getActiveVehicles() {
        return activeVehicles.get();
    }

    /**
     * Returns the average delay minutes.
     */
    public double getAverageDelayMinutes() {
        if (tripDelays.isEmpty()) return 0.0;
        double sum = 0.0;
        for (Long delay : tripDelays.values()) sum += delay;
        return (sum / tripDelays.size()) / 60.0;
    }

    /**
     * Returns the on time percentage.
     */
    public double getOnTimePercentage() {
        int total = onTimeCount.get() + delayedCount.get() + earlyCount.get();
        if (total == 0) return 100.0;
        return (onTimeCount.get() * 100.0) / total;
    }

    /**
     * Returns the seconds since last update.
     */
    public long getSecondsSinceLastUpdate() {
        long lastTs = lastUpdateTimestamp.get();
        if (lastTs == 0) return -1;
        return Instant.now().getEpochSecond() - lastTs;
    }

    /**
     * Returns the last update time.
     */
    public String getLastUpdateTime() {
        long lastTs = lastUpdateTimestamp.get();
        if (lastTs == 0) return "N/A";
        Instant instant = Instant.ofEpochSecond(lastTs);
        java.time.LocalTime localTime = instant.atZone(java.time.ZoneId.systemDefault()).toLocalTime();
        return String.format("%02d:%02d:%02d", localTime.getHour(), localTime.getMinute(), localTime.getSecond());
    }

    /**
     * Returns the total updates today.
     */
    public int getTotalUpdatesToday() {
        return totalUpdatesToday.get();
    }

    /**
     * Returns the vehicle history.
     */
    public List<Integer> getVehicleHistory() {
        synchronized (vehicleHistory) {
            return new ArrayList<>(vehicleHistory);
        }
    }

    /**
     * Returns the delay history.
     */
    public List<Double> getDelayHistory() {
        synchronized (delayHistory) {
            return new ArrayList<>(delayHistory);
        }
    }

    public enum ServiceStatus {
        GOOD("Green dot", "Servizio regolare", new java.awt.Color(76, 175, 80)),
        MODERATE("Yellow dot", "Lievi ritardi", new java.awt.Color(255, 193, 7)),
        POOR("Red dot", "Forti ritardi", new java.awt.Color(244, 67, 54)),
        DEGRADED("Orange dot", "Servizio ridotto", new java.awt.Color(255, 152, 0)),
        UNKNOWN("White dot", "Dati non disponibili", new java.awt.Color(158, 158, 158));

        private final String label;
        private final String description;
        private final java.awt.Color color;

        ServiceStatus(String label, String description, java.awt.Color color) {
            this.label = label;
            this.description = description;
            this.color = color;
        }

        public String getLabel() { return label; }
        public String getDescription() { return description; }
        public java.awt.Color getColor() { return color; }
    }

    /**
     * Returns the service status.
     */
    public ServiceStatus getServiceStatus() {
        double onTime = getOnTimePercentage();
        int vehicles = activeVehicles.get();
        long secondsSinceUpdate = getSecondsSinceLastUpdate();

        if (secondsSinceUpdate < 0 || secondsSinceUpdate > 300) return ServiceStatus.UNKNOWN;
        if (vehicles < 10) return ServiceStatus.DEGRADED;
        if (onTime >= 80) return ServiceStatus.GOOD;
        if (onTime >= 60) return ServiceStatus.MODERATE;
        return ServiceStatus.POOR;
    }

    /**
     * Handles resetDaily.
     */
    public void resetDaily() {
        totalUpdatesToday.set(0);
        onTimeCount.set(0);
        delayedCount.set(0);
        earlyCount.set(0);
        tripDelays.clear();
        synchronized (vehicleHistory) { vehicleHistory.clear(); }
        synchronized (delayHistory) { delayHistory.clear(); }
    }
}


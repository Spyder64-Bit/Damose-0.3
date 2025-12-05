package damose.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks service quality metrics for the transit network.
 */
public class ServiceQualityTracker {
    
    private static final ServiceQualityTracker INSTANCE = new ServiceQualityTracker();
    
    // Metrics
    private final AtomicInteger activeVehicles = new AtomicInteger(0);
    private final AtomicInteger totalUpdatesToday = new AtomicInteger(0);
    private final AtomicLong lastUpdateTimestamp = new AtomicLong(0);
    
    // Delay tracking
    private final ConcurrentHashMap<String, Long> tripDelays = new ConcurrentHashMap<>();
    private final AtomicInteger onTimeCount = new AtomicInteger(0);
    private final AtomicInteger delayedCount = new AtomicInteger(0);
    private final AtomicInteger earlyCount = new AtomicInteger(0);
    
    // Historical data (last 10 measurements)
    private final List<Integer> vehicleHistory = new ArrayList<>();
    private final List<Double> delayHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 10;
    
    private ServiceQualityTracker() {}
    
    public static ServiceQualityTracker getInstance() {
        return INSTANCE;
    }
    
    /**
     * Update metrics with new vehicle count.
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
     * Record a trip's delay.
     * @param tripId Trip identifier
     * @param delaySeconds Delay in seconds (positive = late, negative = early)
     */
    public void recordTripDelay(String tripId, long delaySeconds) {
        tripDelays.put(tripId, delaySeconds);
        
        // Update on-time/delayed counters
        if (delaySeconds > 300) { // More than 5 min late
            delayedCount.incrementAndGet();
        } else if (delaySeconds < -120) { // More than 2 min early
            earlyCount.incrementAndGet();
        } else {
            onTimeCount.incrementAndGet();
        }
        
        // Update average delay history
        double avgDelay = getAverageDelayMinutes();
        synchronized (delayHistory) {
            delayHistory.add(avgDelay);
            if (delayHistory.size() > MAX_HISTORY) {
                delayHistory.remove(0);
            }
        }
    }
    
    /**
     * Get number of active vehicles.
     */
    public int getActiveVehicles() {
        return activeVehicles.get();
    }
    
    /**
     * Get average delay in minutes.
     */
    public double getAverageDelayMinutes() {
        if (tripDelays.isEmpty()) return 0.0;
        
        double sum = 0;
        for (Long delay : tripDelays.values()) {
            sum += delay;
        }
        return (sum / tripDelays.size()) / 60.0;
    }
    
    /**
     * Get on-time performance percentage.
     */
    public double getOnTimePercentage() {
        int total = onTimeCount.get() + delayedCount.get() + earlyCount.get();
        if (total == 0) return 100.0;
        return (onTimeCount.get() * 100.0) / total;
    }
    
    /**
     * Get time since last update in seconds.
     */
    public long getSecondsSinceLastUpdate() {
        long lastTs = lastUpdateTimestamp.get();
        if (lastTs == 0) return -1;
        return Instant.now().getEpochSecond() - lastTs;
    }
    
    /**
     * Get the last update time as a formatted string.
     */
    public String getLastUpdateTime() {
        long lastTs = lastUpdateTimestamp.get();
        if (lastTs == 0) return "N/A";
        
        Instant instant = Instant.ofEpochSecond(lastTs);
        java.time.LocalTime localTime = instant.atZone(java.time.ZoneId.systemDefault()).toLocalTime();
        return String.format("%02d:%02d:%02d", 
            localTime.getHour(), localTime.getMinute(), localTime.getSecond());
    }
    
    /**
     * Get number of updates today.
     */
    public int getTotalUpdatesToday() {
        return totalUpdatesToday.get();
    }
    
    /**
     * Get historical vehicle counts.
     */
    public List<Integer> getVehicleHistory() {
        synchronized (vehicleHistory) {
            return new ArrayList<>(vehicleHistory);
        }
    }
    
    /**
     * Get historical average delays.
     */
    public List<Double> getDelayHistory() {
        synchronized (delayHistory) {
            return new ArrayList<>(delayHistory);
        }
    }
    
    /**
     * Get service status based on metrics.
     */
    public ServiceStatus getServiceStatus() {
        double onTime = getOnTimePercentage();
        int vehicles = activeVehicles.get();
        long secondsSinceUpdate = getSecondsSinceLastUpdate();
        
        if (secondsSinceUpdate < 0 || secondsSinceUpdate > 300) {
            return ServiceStatus.UNKNOWN;
        }
        if (vehicles < 10) {
            return ServiceStatus.DEGRADED;
        }
        if (onTime >= 80) {
            return ServiceStatus.GOOD;
        }
        if (onTime >= 60) {
            return ServiceStatus.MODERATE;
        }
        return ServiceStatus.POOR;
    }
    
    /**
     * Reset all counters (for new day).
     */
    public void resetDaily() {
        totalUpdatesToday.set(0);
        onTimeCount.set(0);
        delayedCount.set(0);
        earlyCount.set(0);
        tripDelays.clear();
        synchronized (vehicleHistory) {
            vehicleHistory.clear();
        }
        synchronized (delayHistory) {
            delayHistory.clear();
        }
    }
    
    /**
     * Service status levels.
     */
    public enum ServiceStatus {
        GOOD("🟢", "Servizio regolare", new java.awt.Color(76, 175, 80)),
        MODERATE("🟡", "Lievi ritardi", new java.awt.Color(255, 193, 7)),
        POOR("🔴", "Forti ritardi", new java.awt.Color(244, 67, 54)),
        DEGRADED("🟠", "Servizio ridotto", new java.awt.Color(255, 152, 0)),
        UNKNOWN("⚪", "Dati non disponibili", new java.awt.Color(158, 158, 158));
        
        private final String emoji;
        private final String description;
        private final java.awt.Color color;
        
        ServiceStatus(String emoji, String description, java.awt.Color color) {
            this.emoji = emoji;
            this.description = description;
            this.color = color;
        }
        
        public String getEmoji() { return emoji; }
        public String getDescription() { return description; }
        public java.awt.Color getColor() { return color; }
    }
}


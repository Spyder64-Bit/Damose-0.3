package damose.util;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Memory management utility for the application.
 * Periodically cleans up unused memory and monitors heap usage.
 */
public final class MemoryManager {

    private static final long GC_INTERVAL_MS = 60_000; // Run every 60 seconds
    private static final double MEMORY_THRESHOLD = 0.75; // Trigger GC at 75% heap usage
    
    private static Timer gcTimer;
    private static boolean enabled = false;

    private MemoryManager() {
        // Utility class
    }

    /**
     * Start the automatic memory management.
     */
    public static synchronized void start() {
        if (enabled) return;
        
        enabled = true;
        gcTimer = new Timer("MemoryManager", true);
        
        gcTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                performMemoryCheck();
            }
        }, GC_INTERVAL_MS, GC_INTERVAL_MS);
        
        System.out.println("MemoryManager: Started (interval: " + (GC_INTERVAL_MS / 1000) + "s)");
    }

    /**
     * Stop the automatic memory management.
     */
    public static synchronized void stop() {
        if (gcTimer != null) {
            gcTimer.cancel();
            gcTimer = null;
        }
        enabled = false;
        System.out.println("MemoryManager: Stopped");
    }

    /**
     * Perform a memory check and trigger GC if needed.
     */
    public static void performMemoryCheck() {
        Runtime runtime = Runtime.getRuntime();
        
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double usageRatio = (double) usedMemory / maxMemory;
        
        if (usageRatio > MEMORY_THRESHOLD) {
            System.out.println("MemoryManager: High memory usage (" + 
                formatBytes(usedMemory) + "/" + formatBytes(maxMemory) + 
                " = " + String.format("%.1f%%", usageRatio * 100) + "), triggering GC...");
            
            runGC();
            
            // Report after GC
            freeMemory = runtime.freeMemory();
            totalMemory = runtime.totalMemory();
            usedMemory = totalMemory - freeMemory;
            System.out.println("MemoryManager: After GC: " + 
                formatBytes(usedMemory) + "/" + formatBytes(maxMemory));
        }
    }

    /**
     * Manually trigger garbage collection.
     */
    public static void runGC() {
        System.gc();
    }

    /**
     * Get current memory usage info.
     */
    public static String getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return String.format("Memory: %s / %s (%.1f%%)", 
            formatBytes(usedMemory), 
            formatBytes(maxMemory),
            (double) usedMemory / maxMemory * 100);
    }

    /**
     * Get used memory in bytes.
     */
    public static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Get max available memory in bytes.
     */
    public static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}


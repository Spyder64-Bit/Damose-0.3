package damose.util;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Utility support for memory manager.
 */
public final class MemoryManager {

    private static final long GC_INTERVAL_MS = 60_000;
    private static final double MEMORY_THRESHOLD = 0.75;

    private static Timer gcTimer;
    private static boolean enabled = false;

    private MemoryManager() {
    }

    /**
     * Returns the result of start.
     */
    public static synchronized void start() {
        if (enabled) return;

        enabled = true;
        gcTimer = new Timer("MemoryManager", true);

        gcTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            /**
             * Handles run.
             */
            public void run() {
                performMemoryCheck();
            }
        }, GC_INTERVAL_MS, GC_INTERVAL_MS);

        System.out.println("MemoryManager: Started (interval: " + (GC_INTERVAL_MS / 1000) + "s)");
    }

    /**
     * Returns the result of stop.
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
     * Returns the result of performMemoryCheck.
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

            freeMemory = runtime.freeMemory();
            totalMemory = runtime.totalMemory();
            usedMemory = totalMemory - freeMemory;
            System.out.println("MemoryManager: After GC: " +
                formatBytes(usedMemory) + "/" + formatBytes(maxMemory));
        }
    }

    /**
     * Returns the result of runGC.
     */
    public static void runGC() {
        System.gc();
    }

    /**
     * Returns the memory info.
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
     * Returns the used memory.
     */
    public static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Returns the max memory.
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


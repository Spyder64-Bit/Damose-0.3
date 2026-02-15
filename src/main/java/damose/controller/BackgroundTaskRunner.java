package damose.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Small wrapper around a daemon cached thread pool for background tasks.
 */
final class BackgroundTaskRunner {

    private final ExecutorService executor;

    BackgroundTaskRunner(String namePrefix) {
        AtomicInteger counter = new AtomicInteger(0);
        String prefix = (namePrefix == null || namePrefix.isBlank()) ? "bg-task" : namePrefix;
        this.executor = Executors.newCachedThreadPool(runnable -> {
            Thread t = new Thread(runnable, prefix + "-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    void run(Runnable task) {
        if (task == null) {
            return;
        }
        executor.execute(task);
    }
}

package damose.app;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

import damose.config.AppConstants;
import damose.controller.MainController;
import damose.database.DatabaseManager;
import damose.database.SessionManager;
import damose.model.ConnectionMode;
import damose.service.RealtimeService;
import damose.view.dialog.LoadingDialog;
import damose.view.dialog.LoginDialog;

/**
 * Application bootstrap for damose app.
 */
public class DamoseApp {

    private static LoadingDialog loadingDialog;
    private static final AtomicBoolean loadingFinalized = new AtomicBoolean(false);
    private static final AtomicBoolean appStarted = new AtomicBoolean(false);
    private static final AtomicBoolean rtReady = new AtomicBoolean(false);

    /**
     * Returns the result of main.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginDialog loginDialog = new LoginDialog(null);

            loginDialog.setOnComplete(user -> {
                if (loginDialog.wasCancelled()) {
                    System.out.println("Login cancelled, exiting...");
                    System.exit(0);
                    return;
                }

                SessionManager.setCurrentUser(user);

                if (user != null) {
                    System.out.println("Logged in as: " + user.getUsername());
                } else {
                    System.out.println("Continuing without account");
                }

                startLoadingProcess();
            });

            loginDialog.setVisible(true);
        });

        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseManager::close));
    }

    private static void startLoadingProcess() {
        loadingFinalized.set(false);
        appStarted.set(false);
        rtReady.set(false);
        loadingDialog = new LoadingDialog(null);
        loadingDialog.setVisible(true);

        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> loadingDialog.stepInitStart());

                DatabaseManager.initialize();
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.stepInitDone();
                    loadingDialog.stepStaticStart();
                    loadingDialog.stepStaticProgress("GTFS statico");
                    loadingDialog.stepStaticDone(8000, 15000);
                    loadingDialog.stepRTStart(AppConstants.RT_TIMEOUT_SECONDS);
                });

                startApplicationInBackground();
                startRealtimeWarmup();

            } catch (Exception e) {
                System.err.println("Loading flow error: " + e.getMessage());
                SwingUtilities.invokeLater(() -> loadingDialog.dispose());
            }
        }, "LoadingThread").start();
    }

    private static void startApplicationInBackground() {
        SwingUtilities.invokeLater(() -> loadingDialog.stepAppStart());
        new Thread(() -> {
            try {
                MainController controller = new MainController();
                controller.start();
                appStarted.set(true);
                tryFinalizeLoading();
            } catch (Exception e) {
                System.err.println("Error starting application: " + e.getMessage());
                e.printStackTrace(System.err);
                loadingFinalized.set(true);
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    System.exit(1);
                });
            }
        }, "AppStartThread").start();
    }

    private static void startRealtimeWarmup() {
        RealtimeService.setOnDataReceived(() -> markRtReady(true));
        RealtimeService.setMode(ConnectionMode.ONLINE);

        new Thread(RealtimeService::fetchRealtimeFeeds, "RealtimeInitialFetch").start();

        Timer timeoutCheck = new Timer(AppConstants.RT_TIMEOUT_SECONDS * 1000 + 500, e -> {
            ((Timer) e.getSource()).stop();
            if (!rtReady.get()) {
                markRtReady(false);
            }
        });
        timeoutCheck.setRepeats(false);
        timeoutCheck.start();
    }

    private static void markRtReady(boolean success) {
        if (!rtReady.compareAndSet(false, true)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (success) {
                loadingDialog.stepRTDone();
            } else {
                loadingDialog.stepRTTimeout();
            }
        });
        tryFinalizeLoading();
    }

    private static void tryFinalizeLoading() {
        if (!appStarted.get() || !rtReady.get()) {
            return;
        }
        if (!loadingFinalized.compareAndSet(false, true)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            loadingDialog.stepAppDone();
            loadingDialog.setProgress(100, "Pronto!");

            Timer closeTimer = new Timer(220, e -> {
                ((Timer) e.getSource()).stop();
                loadingDialog.dispose();
            });
            closeTimer.setRepeats(false);
            closeTimer.start();
        });
    }
}


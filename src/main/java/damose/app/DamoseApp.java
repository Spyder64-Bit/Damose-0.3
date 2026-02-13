package damose.app;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import damose.config.AppConstants;
import damose.controller.MainController;
import damose.database.DatabaseManager;
import damose.database.SessionManager;
import damose.model.ConnectionMode;
import damose.service.RealtimeService;
import damose.view.dialog.LoadingDialog;
import damose.view.dialog.LoginDialog;

public class DamoseApp {

    private static LoadingDialog loadingDialog;

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
        loadingDialog = new LoadingDialog(null);
        loadingDialog.setVisible(true);

        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> loadingDialog.stepInitStart());
                Thread.sleep(300);

                DatabaseManager.initialize();
                SwingUtilities.invokeLater(() -> loadingDialog.stepInitDone());
                Thread.sleep(200);

                SwingUtilities.invokeLater(() -> loadingDialog.stepStaticStart());

                SwingUtilities.invokeLater(() -> loadingDialog.stepStaticProgress("stops.txt"));
                Thread.sleep(400);
                SwingUtilities.invokeLater(() -> loadingDialog.stepStaticProgress("trips.txt"));
                Thread.sleep(400);
                SwingUtilities.invokeLater(() -> loadingDialog.stepStaticProgress("stop_times.txt"));
                Thread.sleep(500);
                SwingUtilities.invokeLater(() -> loadingDialog.stepStaticProgress("calendar_dates.txt"));
                Thread.sleep(300);

                SwingUtilities.invokeLater(() -> loadingDialog.stepStaticDone(8000, 15000));
                Thread.sleep(300);

                SwingUtilities.invokeLater(() -> loadingDialog.stepRTStart(AppConstants.RT_TIMEOUT_SECONDS));

                RealtimeService.setOnDataReceived(() -> {
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.stepRTDone();
                        finishLoading();
                    });
                });

                RealtimeService.setMode(ConnectionMode.ONLINE);
                RealtimeService.fetchRealtimeFeeds();

                Timer timeoutCheck = new Timer(AppConstants.RT_TIMEOUT_SECONDS * 1000 + 500, e -> {
                    ((Timer) e.getSource()).stop();
                    if (!loadingDialog.isDataReceived()) {
                        SwingUtilities.invokeLater(() -> finishLoading());
                    }
                });
                timeoutCheck.setRepeats(false);
                timeoutCheck.start();

            } catch (InterruptedException e) {
                System.err.println("Loading thread interrupted: " + e.getMessage());
            }
        }, "LoadingThread").start();
    }

    private static void finishLoading() {
        loadingDialog.stepAppStart();

        new Thread(() -> {
            try {
                MainController controller = new MainController();
                controller.start();

                SwingUtilities.invokeLater(() -> {
                    loadingDialog.stepAppDone();
                    loadingDialog.setProgress(100, "Pronto!");

                    Timer closeTimer = new Timer(300, e -> {
                        ((Timer) e.getSource()).stop();
                        loadingDialog.dispose();
                    });
                    closeTimer.setRepeats(false);
                    closeTimer.start();
                });
            } catch (Exception e) {
                System.err.println("Error starting application: " + e.getMessage());
                e.printStackTrace(System.err);
                SwingUtilities.invokeLater(() -> loadingDialog.dispose());
            }
        }, "AppStartThread").start();
    }
}

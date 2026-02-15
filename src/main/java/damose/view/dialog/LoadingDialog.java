package damose.view.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import damose.config.AppConstants;

/**
 * Core class for loading dialog.
 */
public class LoadingDialog extends JFrame {

    private final JLabel statusLabel;
    private final JLabel detailLabel;
    private final ProgressBarPanel progressPanel;
    private final StepIndicator[] steps;

    private Timer countdownTimer;
    private int secondsRemaining;
    private boolean dataReceived = false;
    private Runnable onComplete;

    public LoadingDialog(JFrame parent) {
        super("Damose - Caricamento");
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(480, 400);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/sprites/icon.png"));
            List<Image> icons = new ArrayList<>();
            icons.add(icon.getImage());
            setIconImages(icons);
        } catch (Exception e) {
            System.out.println("Could not load app icon: " + e.getMessage());
        }

        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppConstants.BG_DARK);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(AppConstants.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(36, 44, 36, 44));

        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel();
        titleRow.setOpaque(false);
        titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));
        titleRow.setAlignmentX(CENTER_ALIGNMENT);

        try {
            ImageIcon rawIcon = new ImageIcon(getClass().getResource("/sprites/icon.png"));
            Image scaled = rawIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            JLabel iconLabel = new JLabel(new ImageIcon(scaled));
            titleRow.add(iconLabel);
            titleRow.add(Box.createHorizontalStrut(12));
        } catch (Exception e) {
        }

        JLabel titleLabel = new JLabel("DAMOSE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        titleLabel.setForeground(AppConstants.TEXT_PRIMARY);
        titleRow.add(titleLabel);

        headerPanel.add(titleRow);

        JLabel subtitleLabel = new JLabel("Rome Bus Tracker");
        subtitleLabel.setFont(AppConstants.FONT_SUBTITLE);
        subtitleLabel.setForeground(AppConstants.TEXT_MUTED);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(new EmptyBorder(32, 0, 20, 0));

        steps = new StepIndicator[4];
        steps[0] = new StepIndicator("Inizializzazione");
        steps[1] = new StepIndicator("Caricamento dati GTFS");
        steps[2] = new StepIndicator("Connessione Real-Time");
        steps[3] = new StepIndicator("Avvio applicazione");

        for (int i = 0; i < steps.length; i++) {
            centerPanel.add(steps[i]);
            if (i < steps.length - 1) {
                centerPanel.add(Box.createVerticalStrut(14));
            }
        }

        centerPanel.add(Box.createVerticalStrut(28));

        progressPanel = new ProgressBarPanel();
        progressPanel.setAlignmentX(CENTER_ALIGNMENT);
        centerPanel.add(progressPanel);

        centerPanel.add(Box.createVerticalStrut(20));

        statusLabel = new JLabel("Preparazione...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        statusLabel.setForeground(AppConstants.TEXT_PRIMARY);
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        centerPanel.add(statusLabel);

        detailLabel = new JLabel(" ", SwingConstants.CENTER);
        detailLabel.setFont(AppConstants.FONT_SUBTITLE);
        detailLabel.setForeground(AppConstants.TEXT_MUTED);
        detailLabel.setAlignmentX(CENTER_ALIGNMENT);
        centerPanel.add(Box.createVerticalStrut(6));
        centerPanel.add(detailLabel);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        setContentPane(mainPanel);
        setShape(new RoundRectangle2D.Double(0, 0, 480, 400, 24, 24));
    }

    private class StepIndicator extends JPanel {
        private final JLabel iconLabel;
        private final JLabel textLabel;
        private State state = State.PENDING;
        private Timer animationTimer;
        private java.awt.image.BufferedImage loadingImageCache;

        enum State { PENDING, ACTIVE, DONE, WARNING }

        StepIndicator(String text) {
            setOpaque(false);
            setLayout(new BorderLayout(10, 0));
            setAlignmentX(CENTER_ALIGNMENT);

            iconLabel = new JLabel("o");
            iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            iconLabel.setForeground(AppConstants.TEXT_MUTED);
            iconLabel.setPreferredSize(new Dimension(28, 28));
            iconLabel.setHorizontalAlignment(JLabel.CENTER);
            iconLabel.setVerticalAlignment(JLabel.CENTER);

            textLabel = new JLabel(text);
            textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            textLabel.setForeground(AppConstants.TEXT_SECONDARY);

            add(iconLabel, BorderLayout.WEST);
            add(textLabel, BorderLayout.CENTER);
        }

        void setState(State newState) {
            this.state = newState;

            if (animationTimer != null) {
                animationTimer.stop();
                animationTimer = null;
            }

            SwingUtilities.invokeLater(() -> {
                switch (state) {
                    case PENDING:
                        iconLabel.setIcon(null);
                        iconLabel.setText("o");
                        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
                        iconLabel.setForeground(AppConstants.TEXT_MUTED);
                        textLabel.setForeground(AppConstants.TEXT_SECONDARY);
                        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
                        break;
                    case ACTIVE:
                        try {
                            ImageIcon gifIcon = new ImageIcon(getClass().getResource("/sprites/loading.gif"));
                            Image scaled = gifIcon.getImage().getScaledInstance(28, 28, Image.SCALE_DEFAULT);
                            ImageIcon scaledGif = new ImageIcon(scaled);
                            iconLabel.setText(null);
                            iconLabel.setIcon(scaledGif);
                        } catch (Exception e) {
                            System.out.println("Failed to load loading.gif: " + e.getMessage());
                            iconLabel.setIcon(null);
                            iconLabel.setText(">");
                            iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
                            iconLabel.setForeground(AppConstants.ACCENT);
                        }
                        textLabel.setForeground(AppConstants.TEXT_PRIMARY);
                        textLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
                        break;
                    case DONE:
                        try {
                            java.net.URL url = getClass().getResource("/sprites/tick.png");
                            if (url != null) {
                                ImageIcon tickIcon = new ImageIcon(url);
                                java.awt.MediaTracker tracker = new java.awt.MediaTracker(iconLabel);
                                tracker.addImage(tickIcon.getImage(), 0);
                                tracker.waitForAll();

                                Image scaled = tickIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                                ImageIcon scaledIcon = new ImageIcon(scaled);
                                iconLabel.setIcon(scaledIcon);
                                iconLabel.setText(null);
                            } else {
                                throw new Exception("tick.png not found");
                            }
                        } catch (Exception e) {
                            System.out.println("Failed to load tick.png: " + e.getMessage());
                            e.printStackTrace();
                            iconLabel.setIcon(null);
                            iconLabel.setText("v");
                            iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
                            iconLabel.setForeground(AppConstants.SUCCESS_COLOR);
                        }
                        textLabel.setForeground(AppConstants.SUCCESS_COLOR);
                        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
                        break;
                    case WARNING:
                        iconLabel.setIcon(null);
                        iconLabel.setText("!");
                        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
                        iconLabel.setForeground(AppConstants.WARNING_COLOR);
                        textLabel.setForeground(AppConstants.WARNING_COLOR);
                        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
                        break;
                }
            });
        }

        private void startLoadingAnimation() {
            if (loadingImageCache == null) {
                return;
            }

            final int[] rotation = {0};
            animationTimer = new Timer(50, e -> {
                if (state != State.ACTIVE) {
                    return;
                }

                rotation[0] = (rotation[0] + 15) % 360;
                try {
                    java.awt.image.BufferedImage rotated = rotateImage(loadingImageCache, rotation[0]);
                    iconLabel.setIcon(new ImageIcon(rotated));
                } catch (Exception ex) {
                    System.out.println("Error rotating image: " + ex.getMessage());
                }
            });
            animationTimer.start();
        }

        private java.awt.image.BufferedImage rotateImage(java.awt.image.BufferedImage img, int angle) {
            double radians = Math.toRadians(angle);
            double sin = Math.abs(Math.sin(radians));
            double cos = Math.abs(Math.cos(radians));

            int w = img.getWidth();
            int h = img.getHeight();
            int newWidth = (int) Math.floor(w * cos + h * sin);
            int newHeight = (int) Math.floor(h * cos + w * sin);

            java.awt.image.BufferedImage rotated = new java.awt.image.BufferedImage(
                newWidth, newHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g2d = rotated.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.translate((newWidth - w) / 2.0, (newHeight - h) / 2.0);
            g2d.rotate(radians, w / 2.0, h / 2.0);
            g2d.drawImage(img, 0, 0, null);
            g2d.dispose();

            return rotated;
        }

        void setText(String text) {
            SwingUtilities.invokeLater(() -> textLabel.setText(text));
        }
    }

    private class ProgressBarPanel extends JPanel {
        private int progress = 0;

        ProgressBarPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(392, 14));
            setMaximumSize(new Dimension(392, 14));
        }

        void setProgress(int value) {
            this.progress = Math.max(0, Math.min(100, value));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = h;

            g2.setColor(AppConstants.PROGRESS_BG);
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            if (progress > 0) {
                int fillWidth = (int) (w * progress / 100.0);
                if (fillWidth > arc) {
                    g2.setColor(AppConstants.ACCENT);
                    g2.fillRoundRect(0, 0, fillWidth, h, arc, arc);
                }
            }

            g2.dispose();
        }
    }

    /**
     * Updates the progress value.
     */
    public void setProgress(int percent, String status) {
        SwingUtilities.invokeLater(() -> {
            progressPanel.setProgress(percent);
            statusLabel.setText(status);
        });
    }

    /**
     * Updates the detail value.
     */
    public void setDetail(String detail) {
        SwingUtilities.invokeLater(() -> detailLabel.setText(detail != null ? detail : " "));
    }

    /**
     * Handles stepInitStart.
     */
    public void stepInitStart() {
        steps[0].setState(StepIndicator.State.ACTIVE);
        setProgress(5, "Inizializzazione sistema...");
    }

    /**
     * Handles stepInitDone.
     */
    public void stepInitDone() {
        steps[0].setState(StepIndicator.State.DONE);
        setProgress(15, "Sistema pronto");
    }

    /**
     * Handles stepStaticStart.
     */
    public void stepStaticStart() {
        steps[1].setState(StepIndicator.State.ACTIVE);
        setProgress(20, "Caricamento dati GTFS...");
    }

    /**
     * Handles stepStaticProgress.
     */
    public void stepStaticProgress(String item) {
        setDetail("Lettura " + item);
    }

    /**
     * Handles stepStaticDone.
     */
    public void stepStaticDone(int stopsCount, int tripsCount) {
        steps[1].setState(StepIndicator.State.DONE);
        setProgress(50, "Dati GTFS caricati");
        setDetail(String.format("%,d fermate  |  %,d viaggi", stopsCount, tripsCount));
    }

    /**
     * Handles stepRTStart.
     */
    public void stepRTStart(int timeoutSeconds) {
        steps[2].setState(StepIndicator.State.ACTIVE);
        setProgress(55, "Connessione feed real-time...");
        this.secondsRemaining = timeoutSeconds;

        countdownTimer = new Timer(1000, e -> {
            secondsRemaining--;
            setDetail("Attesa risposta... " + secondsRemaining + "s");

            if (secondsRemaining <= 0 && !dataReceived) {
                countdownTimer.stop();
                stepRTTimeout();
            }
        });
        setDetail("Attesa risposta... " + secondsRemaining + "s");
        countdownTimer.start();
    }

    /**
     * Handles stepRTDone.
     */
    public void stepRTDone() {
        dataReceived = true;
        if (countdownTimer != null) countdownTimer.stop();
        steps[2].setState(StepIndicator.State.DONE);
        setProgress(80, "Dati real-time ricevuti");
        setDetail(null);
    }

    /**
     * Handles stepRTTimeout.
     */
    public void stepRTTimeout() {
        if (countdownTimer != null) countdownTimer.stop();
        steps[2].setState(StepIndicator.State.WARNING);
        steps[2].setText("Real-Time non disponibile");
        setProgress(75, "Modalita' offline");
        setDetail("Verranno usati solo dati statici");
    }

    /**
     * Handles stepAppStart.
     */
    public void stepAppStart() {
        steps[3].setState(StepIndicator.State.ACTIVE);
        setProgress(90, "Avvio interfaccia...");
        setDetail(null);
    }

    /**
     * Handles stepAppDone.
     */
    public void stepAppDone() {
        steps[3].setState(StepIndicator.State.DONE);
        setProgress(100, "Pronto!");
        setDetail(null);
    }

    /**
     * Registers callback for complete.
     */
    public void setOnComplete(Runnable callback) {
        this.onComplete = callback;
    }

    /**
     * Handles completeAndClose.
     */
    public void completeAndClose() {
        stepAppDone();
        Timer closeTimer = new Timer(700, e -> {
            ((Timer) e.getSource()).stop();
            dispose();
            if (onComplete != null) {
                SwingUtilities.invokeLater(onComplete);
            }
        });
        closeTimer.setRepeats(false);
        closeTimer.start();
    }

    /**
     * Returns whether data received.
     */
    public boolean isDataReceived() {
        return dataReceived;
    }

    /**
     * Handles closeNow.
     */
    public void closeNow() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        dispose();
    }
}


package damose.view.component;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import damose.model.ConnectionMode;

/**
 * UI component for connection button.
 */
public class ConnectionButton extends JButton {

    public static final int BUTTON_WIDTH = 48;
    public static final int BUTTON_HEIGHT = 48;
    private static final int ANIMATION_INTERVAL_MS = 50;
    private static final int WIFI_ICON_SIZE = 40;
    private static final int NOWIFI_ICON_SIZE = 40;
    private static final int CONNECTING_ICON_SIZE = 36;

    private final Icon wifiIcon;
    private final Icon noWifiIcon;
    private final Icon connectingIcon;

    private ConnectionMode currentMode = ConnectionMode.ONLINE;
    private boolean isConnecting = false;
    private Runnable onModeToggle;
    private Timer animationTimer;

    public ConnectionButton() {
        wifiIcon = loadScaledIcon("/sprites/wifi.png", WIFI_ICON_SIZE, Image.SCALE_SMOOTH);
        noWifiIcon = loadScaledIcon("/sprites/nowifi.png", NOWIFI_ICON_SIZE, Image.SCALE_SMOOTH);

        connectingIcon = loadScaledIcon("/sprites/connecting.gif", CONNECTING_ICON_SIZE, Image.SCALE_DEFAULT);

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        setMargin(new java.awt.Insets(0, 0, 0, 0));
        setRolloverEnabled(false);

        getModel().setArmed(false);
        getModel().setPressed(false);
        getModel().setSelected(false);

        setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        setMinimumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        setMaximumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));

        updateIcon();

        addActionListener(e -> {
            if (!isConnecting && onModeToggle != null) {
                onModeToggle.run();
            }
        });
    }

    private Icon loadScaledIcon(String path, int size, int scaleHint) {
        URL url = getClass().getResource(path);
        if (url == null) {
            System.err.println("Missing resource: " + path);
            return null;
        }
        ImageIcon imageIcon = new ImageIcon(url);
        Image scaled = imageIcon.getImage().getScaledInstance(size, size, scaleHint);
        return new ImageIcon(scaled);
    }

    /**
     * Registers callback for mode toggle.
     */
    public void setOnModeToggle(Runnable callback) {
        this.onModeToggle = callback;
    }

    /**
     * Handles showConnecting.
     */
    public void showConnecting() {
        SwingUtilities.invokeLater(() -> {
            isConnecting = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Connessione in corso...");

            if (connectingIcon != null) {
                setIcon(connectingIcon);
                setDisabledIcon(connectingIcon);
                startAnimationTimer();
                repaint();
            }
        });
    }

    private void startAnimationTimer() {
        stopAnimationTimer();
        animationTimer = new Timer(ANIMATION_INTERVAL_MS, e -> repaint());
        animationTimer.setInitialDelay(0);
        animationTimer.setCoalesce(true);
        animationTimer.start();
    }

    private void stopAnimationTimer() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
    }

    /**
     * Updates the mode value.
     */
    public void setMode(ConnectionMode mode) {
        SwingUtilities.invokeLater(() -> {
            this.currentMode = mode;
            isConnecting = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            stopAnimationTimer();
            updateIcon();
        });
    }

    private void updateIcon() {
        if (currentMode == ConnectionMode.ONLINE) {
            setIcon(wifiIcon);
            setDisabledIcon(wifiIcon);
            setToolTipText("Online - Clicca per andare Offline");
        } else {
            setIcon(noWifiIcon);
            setDisabledIcon(noWifiIcon);
            setToolTipText("Offline - Clicca per andare Online");
        }
    }

    /**
     * Returns the mode.
     */
    public ConnectionMode getMode() {
        return currentMode;
    }

    /**
     * Updates the online value.
     */
    public void setOnline() {
        setMode(ConnectionMode.ONLINE);
    }

    /**
     * Updates the offline value.
     */
    public void setOffline() {
        setMode(ConnectionMode.OFFLINE);
    }
}


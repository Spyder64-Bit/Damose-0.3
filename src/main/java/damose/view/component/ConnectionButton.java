package damose.view.component;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import damose.model.ConnectionMode;

public class ConnectionButton extends JButton {

    private static final int TARGET_WIDTH = 160;

    private final ImageIcon wifiIcon;
    private final ImageIcon noWifiIcon;
    private final ImageIcon connectingIcon;

    private ConnectionMode currentMode = ConnectionMode.ONLINE;
    private boolean isConnecting = false;
    private Runnable onModeToggle;

    private Timer animationTimer;

    public ConnectionButton() {

        wifiIcon = loadScaledIcon("/sprites/wifi.png");
        noWifiIcon = loadScaledIcon("/sprites/nowifi.png");
        connectingIcon = loadScaledIcon("/sprites/connecting.png");

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

        if (wifiIcon != null) {
            int width = wifiIcon.getIconWidth();
            int height = wifiIcon.getIconHeight();
            
            setPreferredSize(new Dimension(width, height));
            setMinimumSize(new Dimension(width, height));
            setMaximumSize(new Dimension(width, height));
        }

        updateIcon();

        addActionListener(e -> {
            if (!isConnecting && onModeToggle != null) {
                onModeToggle.run();
            }
        });
    }

    private ImageIcon loadScaledIcon(String path) {
        URL url = getClass().getResource(path);
        if (url == null) {
            System.err.println("Missing resource: " + path);
            return null;
        }

        ImageIcon raw = new ImageIcon(url);
        int originalWidth = raw.getIconWidth();
        int originalHeight = raw.getIconHeight();
        
        System.out.println("Loading " + path + ": original size = " + originalWidth + "x" + originalHeight);

        int scaledHeight = (int) ((double) TARGET_WIDTH / originalWidth * originalHeight);
        
        System.out.println("Scaling to " + TARGET_WIDTH + "x" + scaledHeight);

        Image scaled = raw.getImage()
                .getScaledInstance(TARGET_WIDTH, scaledHeight, Image.SCALE_SMOOTH);

        ImageIcon result = new ImageIcon(scaled);
        System.out.println("Final icon size = " + result.getIconWidth() + "x" + result.getIconHeight());
        return result;
    }

    public void setOnModeToggle(Runnable callback) {
        this.onModeToggle = callback;
    }

    public void showConnecting() {
        SwingUtilities.invokeLater(() -> {
            isConnecting = true;
            setEnabled(false);
            setToolTipText("Connessione in corso...");

            if (connectingIcon != null) {
                setIcon(connectingIcon);
                startAnimationTimer();
            }
        });
    }

    private void startAnimationTimer() {
        stopAnimationTimer();
        animationTimer = new Timer(50, e -> repaint());
        animationTimer.start();
    }

    private void stopAnimationTimer() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
    }

    public void setMode(ConnectionMode mode) {
        SwingUtilities.invokeLater(() -> {
            this.currentMode = mode;
            isConnecting = false;
            setEnabled(true);
            stopAnimationTimer();
            updateIcon();
        });
    }

    private void updateIcon() {
        if (currentMode == ConnectionMode.ONLINE) {
            setIcon(wifiIcon);
            setToolTipText("Online - Clicca per passare offline");
        } else {
            setIcon(noWifiIcon);
            setToolTipText("Offline - Clicca per connetterti");
        }
    }

    public ConnectionMode getMode() {
        return currentMode;
    }

    public void setOnline() {
        setMode(ConnectionMode.ONLINE);
    }

    public void setOffline() {
        setMode(ConnectionMode.OFFLINE);
    }
}


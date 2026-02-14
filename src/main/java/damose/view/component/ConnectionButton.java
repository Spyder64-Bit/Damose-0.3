package damose.view.component;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import damose.model.ConnectionMode;

public class ConnectionButton extends JButton {

    public static final int BUTTON_WIDTH = 44;
    public static final int BUTTON_HEIGHT = 44;
    private static final int ANIMATION_INTERVAL_MS = 50;

    private static final double WIFI_ZOOM = 1.8;
    private static final double NOWIFI_ZOOM = 1.8;
    private static final double CONNECTING_ZOOM = 4.8;

    private final Image wifiImage;
    private final Image noWifiImage;
    private final Image connectingImage;

    private final Icon wifiIcon;
    private final Icon noWifiIcon;
    private final Icon connectingIcon;

    private ConnectionMode currentMode = ConnectionMode.ONLINE;
    private boolean isConnecting = false;
    private Runnable onModeToggle;
    private Timer animationTimer;

    public ConnectionButton() {
        wifiImage = loadImage("/sprites/wifi.png");
        noWifiImage = loadImage("/sprites/nowifi.png");
        connectingImage = loadImage("/sprites/connecting.gif");

        wifiIcon = createFittedIcon(wifiImage, WIFI_ZOOM);
        noWifiIcon = createFittedIcon(noWifiImage, NOWIFI_ZOOM);
        connectingIcon = createFittedIcon(connectingImage, CONNECTING_ZOOM);

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

    private Image loadImage(String path) {
        URL url = getClass().getResource(path);
        if (url == null) {
            System.err.println("Missing resource: " + path);
            return null;
        }

        return new ImageIcon(url).getImage();
    }

    private Icon createFittedIcon(Image image, double zoom) {
        if (image == null) return null;
        return new FitToBoundsIcon(image, BUTTON_WIDTH, BUTTON_HEIGHT, zoom);
    }

    private static final class FitToBoundsIcon implements Icon {
        private final Image image;
        private final int maxWidth;
        private final int maxHeight;
        private final double zoom;

        private FitToBoundsIcon(Image image, int maxWidth, int maxHeight, double zoom) {
            this.image = image;
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
            this.zoom = zoom;
        }

        @Override
        public int getIconWidth() {
            return maxWidth;
        }

        @Override
        public int getIconHeight() {
            return maxHeight;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            int imgW = image.getWidth(c);
            int imgH = image.getHeight(c);
            if (imgW <= 0 || imgH <= 0) {
                return;
            }

            double scale = Math.min((double) maxWidth / imgW, (double) maxHeight / imgH) * zoom;
            int drawW = Math.max(1, (int) Math.round(imgW * scale));
            int drawH = Math.max(1, (int) Math.round(imgH * scale));

            int drawX = x + (maxWidth - drawW) / 2;
            int drawY = y + (maxHeight - drawH) / 2;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            java.awt.Shape oldClip = g2.getClip();
            g2.setClip(x, y, maxWidth, maxHeight);
            g2.drawImage(image, drawX, drawY, drawW, drawH, c);
            g2.setClip(oldClip);
            g2.dispose();
        }
    }

    public void setOnModeToggle(Runnable callback) {
        this.onModeToggle = callback;
    }

    public void showConnecting() {
        SwingUtilities.invokeLater(() -> {
            isConnecting = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
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
            setToolTipText("Online - Clicca per passare offline");
        } else {
            setIcon(noWifiIcon);
            setDisabledIcon(noWifiIcon);
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


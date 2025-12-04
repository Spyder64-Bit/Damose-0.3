package damose.ui.component;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import damose.model.ConnectionMode;

/**
 * Button that displays connection status and allows toggling between online/offline modes.
 * Shows wifi.png when online, nowifi.png when offline, and connecting.gif while connecting.
 */
public class ConnectionButton extends JButton {

    private static final int ICON_SIZE = 36;
    
    private final ImageIcon wifiIcon;
    private final ImageIcon noWifiIcon;
    private ImageIcon connectingIcon;
    
    private ConnectionMode currentMode = ConnectionMode.ONLINE;
    private boolean isConnecting = false;
    private Runnable onModeToggle;
    
    // Timer for GIF animation refresh
    private Timer animationTimer;

    public ConnectionButton() {
        // Load icons
        wifiIcon = loadScaledIcon("/sprites/wifi.png");
        noWifiIcon = loadScaledIcon("/sprites/nowifi.png");
        loadConnectingGif();
        
        // Setup button appearance
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setPreferredSize(new Dimension(ICON_SIZE + 8, ICON_SIZE + 8));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("Connesso - Clicca per passare offline");
        
        // Set initial icon
        updateIcon();
        
        // Toggle mode on click
        addActionListener(e -> {
            if (!isConnecting && onModeToggle != null) {
                onModeToggle.run();
            }
        });
        
        // Hover effect
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isConnecting) {
                    setOpaque(true);
                    repaint();
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                setOpaque(false);
                repaint();
            }
        });
    }
    
    private ImageIcon loadScaledIcon(String path) {
        try {
            ImageIcon raw = new ImageIcon(getClass().getResource(path));
            Image scaled = raw.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            System.out.println("Could not load icon: " + path + " - " + e.getMessage());
            return null;
        }
    }
    
    private void loadConnectingGif() {
        try {
            URL gifUrl = getClass().getResource("/sprites/connecting.gif");
            if (gifUrl != null) {
                // Load GIF without scaling to preserve animation
                connectingIcon = new ImageIcon(gifUrl);
                // Set this button as the image observer to receive animation updates
                connectingIcon.setImageObserver(this);
            }
        } catch (Exception e) {
            System.out.println("Could not load GIF: " + e.getMessage());
            connectingIcon = null;
        }
    }
    
    /**
     * Set the callback to run when mode should be toggled.
     */
    public void setOnModeToggle(Runnable callback) {
        this.onModeToggle = callback;
    }
    
    /**
     * Show connecting animation.
     */
    public void showConnecting() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::showConnecting);
            return;
        }
        
        isConnecting = true;
        setEnabled(false);
        setToolTipText("Connessione in corso...");
        
        if (connectingIcon != null) {
            setIcon(connectingIcon);
            // Start animation timer to force repaints
            startAnimationTimer();
        }
    }
    
    private void startAnimationTimer() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        // Repaint every 50ms to ensure GIF animates
        animationTimer = new Timer(50, e -> repaint());
        animationTimer.start();
    }
    
    private void stopAnimationTimer() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
            animationTimer = null;
        }
    }
    
    /**
     * Set the current connection mode and update the icon.
     */
    public void setMode(ConnectionMode mode) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setMode(mode));
            return;
        }
        
        this.currentMode = mode;
        finishModeChange();
    }
    
    private void finishModeChange() {
        isConnecting = false;
        setEnabled(true);
        stopAnimationTimer();
        updateIcon();
    }
    
    private void updateIcon() {
        if (currentMode == ConnectionMode.ONLINE) {
            if (wifiIcon != null) setIcon(wifiIcon);
            setToolTipText("Online - Clicca per passare offline");
        } else {
            if (noWifiIcon != null) setIcon(noWifiIcon);
            setToolTipText("Offline - Clicca per connetterti");
        }
    }
    
    /**
     * Force update to offline state (e.g., when connection fails).
     */
    public void setOffline() {
        setMode(ConnectionMode.OFFLINE);
    }
    
    /**
     * Force update to online state.
     */
    public void setOnline() {
        setMode(ConnectionMode.ONLINE);
    }
    
    /**
     * Get current connection mode.
     */
    public ConnectionMode getMode() {
        return currentMode;
    }
}


package damose.view.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;

import damose.config.AppConstants;

/**
 * UI component for info overlay.
 */
public class InfoOverlay extends JPanel {

    private final JPanel contentPanel;
    private JTextPane aboutPane;
    private Timer aboutGlitchTimer;
    private boolean aboutGlitchPhase;
    private static final String GITHUB_URL = "https://github.com/Spyder64-Bit";

    public InfoOverlay() {
        setLayout(null);
        setOpaque(false);
        setFocusable(true);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(AppConstants.BG_MEDIUM);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppConstants.BORDER_COLOR, 1),
                new EmptyBorder(14, 14, 14, 14)
        ));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Info e Legenda");
        title.setForeground(AppConstants.TEXT_PRIMARY);
        title.setFont(AppConstants.FONT_TITLE);
        header.add(title, BorderLayout.WEST);

        JLabel close = new JLabel("X");
        close.setForeground(AppConstants.TEXT_SECONDARY);
        close.setFont(AppConstants.FONT_BUTTON);
        close.setBorder(new EmptyBorder(2, 8, 2, 8));
        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                closeOverlay();
            }

            @Override
            /**
             * Handles mouseEntered.
             */
            public void mouseEntered(MouseEvent e) {
                close.setForeground(AppConstants.ERROR_COLOR);
            }

            @Override
            /**
             * Handles mouseExited.
             */
            public void mouseExited(MouseEvent e) {
                close.setForeground(AppConstants.TEXT_SECONDARY);
            }
        });
        header.add(close, BorderLayout.EAST);
        contentPanel.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(AppConstants.BG_MEDIUM);
        body.setBorder(new EmptyBorder(10, 2, 4, 2));

        body.add(createSectionTitle("Legenda"));
        body.add(createLegendRow("/sprites/lente.png", 26, "Pulsante cerca fermate/linee", Image.SCALE_SMOOTH));
        body.add(createLegendRow("/sprites/star.png", 24, "Pulsante preferiti", Image.SCALE_SMOOTH));
        body.add(createLegendRow("/sprites/bus1.png", 24, "Mostra/Nascondi autobus", Image.SCALE_SMOOTH));
        body.add(createLegendRow("/sprites/wifi.png", 24, "Stato online - Clicca per andare Offline", Image.SCALE_SMOOTH));
        body.add(createLegendRow("/sprites/nowifi.png", 24, "Stato offline - Clicca per andare Online", Image.SCALE_SMOOTH));
        body.add(createLegendRow("/sprites/connecting.gif", 24, "Connessione in corso", Image.SCALE_DEFAULT));
        body.add(createLegendRow("/sprites/info.png", 24, "Apre questo pannello info", Image.SCALE_SMOOTH));
        body.add(createLegendRow("/sprites/bus.png", 22, "Marker autobus", Image.SCALE_SMOOTH));
        body.add(createLegendRow("/sprites/tram.png", 22, "Marker tram", Image.SCALE_SMOOTH));
        body.add(createLegendRow("/sprites/stop.png", 22, "Marker fermata", Image.SCALE_SMOOTH));
        body.add(Box.createVerticalStrut(12));

        body.add(createSectionTitle("About Me"));
        body.add(createAboutHeader());
        body.add(Box.createVerticalStrut(8));
        aboutPane = new JTextPane();
        aboutPane.setContentType("text/html");
        aboutPane.setEditable(false);
        aboutPane.setFocusable(false);
        aboutPane.setBackground(AppConstants.BG_LIGHT);
        aboutPane.setForeground(AppConstants.TEXT_PRIMARY);
        aboutPane.setFont(AppConstants.FONT_BODY);
        aboutPane.setBorder(new EmptyBorder(12, 12, 12, 12));
        aboutPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        aboutPane.setPreferredSize(new Dimension(560, 130));
        aboutPane.setMinimumSize(new Dimension(560, 130));
        aboutPane.setMaximumSize(new Dimension(560, 130));
        aboutPane.setText(buildAboutHtml(false));
        aboutPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getURL() != null) {
                openExternalLink(e.getURL().toString());
            }
        });
        body.add(aboutPane);

        JScrollPane scrollPane = new JScrollPane(body);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(AppConstants.BG_MEDIUM);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        add(contentPanel);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!contentPanel.getBounds().contains(e.getPoint())) {
                    closeOverlay();
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    closeOverlay();
                }
            }
        });
    }

    /**
     * Handles showInfo.
     */
    public void showInfo() {
        setVisible(true);
        layoutPanel();
        startAboutGlitchEffect();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void closeOverlay() {
        stopAboutGlitchEffect();
        setVisible(false);
    }

    private void layoutPanel() {
        int panelW = 620;
        int panelH = Math.min(520, Math.max(360, getHeight() - 80));
        int x = Math.max(18, (getWidth() - panelW) / 6);
        int y = Math.max(18, (getHeight() - panelH) / 5);
        contentPanel.setBounds(x, y, panelW, panelH);
    }

    @Override
    /**
     * Updates the bounds value.
     */
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (contentPanel != null && isVisible()) {
            layoutPanel();
        }
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 125));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    @Override
    /**
     * Returns whether opaque.
     */
    public boolean isOpaque() {
        return false;
    }

    private JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(AppConstants.ACCENT);
        label.setFont(AppConstants.FONT_BUTTON);
        label.setBorder(new EmptyBorder(0, 0, 8, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JPanel createLegendRow(String iconPath, int iconSize, String text, int scaleHint) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(AppConstants.BG_MEDIUM);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(24, 24));
        iconLabel.setMinimumSize(new Dimension(24, 24));
        iconLabel.setMaximumSize(new Dimension(24, 24));
        iconLabel.setHorizontalAlignment(JLabel.CENTER);
        iconLabel.setVerticalAlignment(JLabel.CENTER);
        ImageIcon icon = loadScaledIcon(iconPath, iconSize, scaleHint);
        if (icon != null) {
            iconLabel.setIcon(icon);
        }

        JLabel textLabel = new JLabel(text);
        textLabel.setForeground(AppConstants.TEXT_PRIMARY);
        textLabel.setFont(AppConstants.FONT_BODY);

        row.add(iconLabel);
        row.add(Box.createHorizontalStrut(8));
        row.add(textLabel);
        return row;
    }

    private JPanel createAboutHeader() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(AppConstants.BG_MEDIUM);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel avatar = new JLabel();
        avatar.setPreferredSize(new Dimension(72, 72));
        avatar.setMinimumSize(new Dimension(72, 72));
        avatar.setMaximumSize(new Dimension(72, 72));
        avatar.setBorder(BorderFactory.createLineBorder(AppConstants.BORDER_COLOR, 1, true));
        avatar.setHorizontalAlignment(JLabel.CENTER);
        avatar.setVerticalAlignment(JLabel.CENTER);

        ImageIcon fallback = loadScaledIcon("/sprites/icon.png", 72, Image.SCALE_SMOOTH);
        if (fallback != null) {
            avatar.setIcon(fallback);
        }
        avatar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        avatar.setToolTipText("Apri profilo GitHub");
        avatar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openExternalLink(GITHUB_URL);
            }
        });

        row.add(avatar);
        row.add(Box.createHorizontalGlue());

        new Thread(() -> {
            ImageIcon remote = loadGithubAvatarIcon(72);
            if (remote != null) {
                SwingUtilities.invokeLater(() -> avatar.setIcon(remote));
            }
        }, "github-avatar-loader").start();

        return row;
    }

    private ImageIcon loadGithubAvatarIcon(int size) {
        try {
            URL url = new URL("https://github.com/Spyder64-Bit.png?size=200");
            BufferedImage img = ImageIO.read(url);
            if (img == null) {
                return null;
            }
            Image scaled = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }

    private void openExternalLink(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ignored) {
        }
    }

    private ImageIcon loadScaledIcon(String path, int size, int scaleHint) {
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(path));
            Image scaled = icon.getImage().getScaledInstance(size, size, scaleHint);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildAboutHtml(boolean glitch) {
        String font = "Segoe UI";
        String textColor = glitch ? "#DCEEFF" : "#E5E7ED";
        String opening = "<html><body style='font-family:" + font
                + "; font-size:12px; color:" + textColor
                + "; margin:0; width:520px; word-wrap:break-word; overflow-wrap:anywhere;'>";
        return opening
                + "ciao! sono spyder.<br/>"
                + "damose &egrave; nato a roma, tra tazze di caff&egrave; e nirvana in sottofondo.<br/>"
                + "&egrave; il mio primo progetto serio e spero che sia una porta su tante altre cose.<br/>"
                + "controlla il mio github -&gt; <a href='https://github.com/Spyder64-Bit'>spyder64-bit</a>"
                + "</body></html>";
    }

    private void startAboutGlitchEffect() {
        if (aboutPane == null) {
            return;
        }
        if (aboutGlitchTimer == null) {
            aboutGlitchTimer = new Timer(700, e -> {
                aboutGlitchPhase = !aboutGlitchPhase;
                aboutPane.setText(buildAboutHtml(aboutGlitchPhase));
            });
        }
        if (!aboutGlitchTimer.isRunning()) {
            aboutGlitchTimer.start();
        }
    }

    private void stopAboutGlitchEffect() {
        if (aboutGlitchTimer != null) {
            aboutGlitchTimer.stop();
        }
        aboutGlitchPhase = false;
        if (aboutPane != null) {
            aboutPane.setText(buildAboutHtml(false));
        }
    }
}


package damose.view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import damose.config.AppConstants;
import damose.view.component.ConnectionButton;

/**
 * Builds the left-side map controls panel and exposes created widgets.
 */
final class MapControlsBuilder {

    private MapControlsBuilder() {
    }

    static Widgets build(Class<?> resourceOwner, Runnable onInfoAction) {
        JPanel panel = createOverlayCardPanel();
        panel.setLayout(null);

        JButton searchButton = createMapControlIconButton(
                resourceOwner, "/sprites/lente.png", 44, 5, 5, "Cerca fermate e linee"
        );
        panel.add(searchButton);

        JButton favoritesButton = createMapControlIconButton(
                resourceOwner, "/sprites/star.png", 40, 5, 60, "Fermate preferite"
        );
        panel.add(favoritesButton);

        JButton busToggleButton = createMapControlIconButton(
                resourceOwner, "/sprites/bus1.png", 40, 5, 115, "Mostra/Nascondi autobus"
        );
        panel.add(busToggleButton);

        ConnectionButton connectionButton = new ConnectionButton();
        connectionButton.setBounds(
                (58 - ConnectionButton.BUTTON_WIDTH) / 2,
                170,
                ConnectionButton.BUTTON_WIDTH,
                ConnectionButton.BUTTON_HEIGHT
        );
        panel.add(connectionButton);

        JButton infoButton = createInfoButton(resourceOwner);
        infoButton.setBounds(5, 225, 48, 48);
        if (onInfoAction != null) {
            infoButton.addActionListener(e -> onInfoAction.run());
        }
        panel.add(infoButton);

        return new Widgets(panel, searchButton, favoritesButton, busToggleButton, connectionButton, infoButton);
    }

    static final class Widgets {
        private final JPanel panel;
        private final JButton searchButton;
        private final JButton favoritesButton;
        private final JButton busToggleButton;
        private final ConnectionButton connectionButton;
        private final JButton infoButton;

        private Widgets(JPanel panel,
                        JButton searchButton,
                        JButton favoritesButton,
                        JButton busToggleButton,
                        ConnectionButton connectionButton,
                        JButton infoButton) {
            this.panel = panel;
            this.searchButton = searchButton;
            this.favoritesButton = favoritesButton;
            this.busToggleButton = busToggleButton;
            this.connectionButton = connectionButton;
            this.infoButton = infoButton;
        }

        JPanel panel() {
            return panel;
        }

        JButton searchButton() {
            return searchButton;
        }

        JButton favoritesButton() {
            return favoritesButton;
        }

        JButton busToggleButton() {
            return busToggleButton;
        }

        ConnectionButton connectionButton() {
            return connectionButton;
        }

        JButton infoButton() {
            return infoButton;
        }
    }

    private static JPanel createOverlayCardPanel() {
        JPanel panel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                g2.setColor(AppConstants.OVERLAY_CARD_BG);
                g2.fillRoundRect(0, 0, w, h, AppConstants.OVERLAY_CARD_ARC, AppConstants.OVERLAY_CARD_ARC);

                g2.setColor(AppConstants.OVERLAY_CARD_BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, AppConstants.OVERLAY_CARD_ARC, AppConstants.OVERLAY_CARD_ARC);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    private static JButton createInfoButton(Class<?> resourceOwner) {
        JButton button = new JButton();
        Image infoImage = ImageResourceLoader.loadTrimmedImage(resourceOwner, "/sprites/info.png");
        if (infoImage != null) {
            Image scaledInfo = infoImage.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            button.setIcon(new ImageIcon(scaledInfo));
        } else {
            button.setText("i");
            button.setFont(new Font("Segoe UI", Font.BOLD, 22));
            button.setForeground(new Color(220, 220, 230));
        }
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(false);
        button.setToolTipText("Legenda e informazioni");
        return button;
    }

    private static JButton createMapControlIconButton(Class<?> resourceOwner,
                                                      String iconPath,
                                                      int iconSize,
                                                      int x,
                                                      int y,
                                                      String tooltip) {
        ImageIcon icon = new ImageIcon(resourceOwner.getResource(iconPath));
        Image scaled = icon.getImage().getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
        JButton button = new JButton(new ImageIcon(scaled));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBounds(x, y, 48, 48);
        button.setToolTipText(tooltip);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}

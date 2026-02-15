package damose.view.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;

import damose.config.AppConstants;
import damose.model.Stop;
import damose.model.VehicleType;
import damose.service.FavoritesService;

/**
 * List cell renderer used by search results.
 */
final class SearchStopCellRenderer extends JPanel implements ListCellRenderer<Stop> {

    private final JLabel nameLabel;
    private final JLabel idLabel;
    private final JLabel typeLabel;
    private final JLabel starLabel;
    private final ImageIcon yellowStarIcon;
    private final ImageIcon busTypeIcon;
    private final ImageIcon tramTypeIcon;

    SearchStopCellRenderer() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 14, 10, 14));
        setOpaque(true);

        yellowStarIcon = new ImageIcon(createYellowStar(16));
        busTypeIcon = loadTypeIcon("/sprites/bus.png");
        tramTypeIcon = loadTypeIcon("/sprites/tram.png");

        typeLabel = new JLabel();
        typeLabel.setBorder(new EmptyBorder(0, 0, 0, 8));
        typeLabel.setPreferredSize(new Dimension(22, 22));
        add(typeLabel, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        nameLabel = new JLabel();
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        idLabel = new JLabel();
        idLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        textPanel.add(nameLabel);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(idLabel);

        add(textPanel, BorderLayout.CENTER);

        starLabel = new JLabel();
        starLabel.setBorder(new EmptyBorder(0, 8, 0, 4));
        starLabel.setPreferredSize(new Dimension(24, 24));
        add(starLabel, BorderLayout.EAST);
    }

    @Override
    public java.awt.Component getListCellRendererComponent(JList<? extends Stop> list,
                                                           Stop value,
                                                           int index,
                                                           boolean isSelected,
                                                           boolean cellHasFocus) {
        String name = value.getStopName();
        if (name.length() > 38) name = name.substring(0, 38) + "...";
        nameLabel.setText(name);

        boolean isFavorite;
        if (value.isFakeLine()) {
            VehicleType vehicleType = VehicleType.fromGtfsCode(value.getStopCode());
            boolean isTram = vehicleType == VehicleType.TRAM;
            idLabel.setText(isTram ? "Linea tram" : "Linea bus");
            typeLabel.setIcon(isTram ? tramTypeIcon : busTypeIcon);
            isFavorite = FavoritesService.isLineFavorite(value.getStopId());
        } else {
            idLabel.setText("Stop ID: " + value.getStopId());
            typeLabel.setIcon(null);
            isFavorite = FavoritesService.isFavorite(value.getStopId());
        }

        starLabel.setIcon(isFavorite ? yellowStarIcon : null);

        if (isSelected) {
            setBackground(AppConstants.ACCENT);
            nameLabel.setForeground(Color.WHITE);
            idLabel.setForeground(new Color(220, 220, 220));
        } else {
            setBackground(AppConstants.LIST_BG);
            nameLabel.setForeground(AppConstants.TEXT_PRIMARY);
            idLabel.setForeground(AppConstants.TEXT_SECONDARY);
        }

        return this;
    }

    private static ImageIcon loadTypeIcon(String path) {
        return IconLoader.loadScaled(SearchStopCellRenderer.class, path, 18);
    }

    private static Image createYellowStar(int size) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        int[] xPoints = new int[10];
        int[] yPoints = new int[10];
        double angleStep = Math.PI / 5;
        int cx = size / 2;
        int cy = size / 2;
        int outerR = size / 2 - 1;
        int innerR = size / 4;

        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * angleStep;
            int r = (i % 2 == 0) ? outerR : innerR;
            xPoints[i] = (int) (cx + r * Math.cos(angle));
            yPoints[i] = (int) (cy + r * Math.sin(angle));
        }

        g2.setColor(new Color(255, 200, 50));
        g2.fillPolygon(xPoints, yPoints, 10);
        g2.setColor(new Color(200, 150, 0));
        g2.drawPolygon(xPoints, yPoints, 10);
        g2.dispose();
        return img;
    }
}

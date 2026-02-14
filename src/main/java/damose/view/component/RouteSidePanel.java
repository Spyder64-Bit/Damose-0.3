package damose.view.component;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import damose.config.AppConstants;
import damose.model.Stop;
import damose.model.VehicleType;

public class RouteSidePanel extends JPanel {

    public static final class VehicleMarker {
        private final double progress;
        private final String vehicleId;
        private final VehicleType vehicleType;
        private final String titleText;
        private final String detailText;

        public VehicleMarker(double progress, String vehicleId, VehicleType vehicleType,
                             String titleText, String detailText) {
            this.progress = Math.max(0.0, Math.min(1.0, progress));
            this.vehicleId = vehicleId == null ? "" : vehicleId;
            this.vehicleType = vehicleType == null ? VehicleType.BUS : vehicleType;
            this.titleText = titleText == null ? "" : titleText.trim();
            this.detailText = detailText == null ? "" : detailText.trim();
        }

        public double getProgress() {
            return progress;
        }

        public String getVehicleId() {
            return vehicleId;
        }

        public VehicleType getVehicleType() {
            return vehicleType;
        }

        public String getTitleText() {
            return titleText;
        }

        public String getDetailText() {
            return detailText;
        }
    }

    private final JLabel routeLabel;
    private final JLabel metaLabel;
    private final JButton closeButton;
    private final JButton directionSwitchButton;
    private final TimelineCanvas timelineCanvas;
    private final JScrollPane scrollPane;

    private final List<Stop> routeStops = new ArrayList<>();
    private final List<VehicleMarker> vehicleMarkers = new ArrayList<>();
    private final Map<Integer, String> directionLabels = new LinkedHashMap<>();
    private Runnable onClose;
    private IntConsumer onDirectionSelected;
    private int selectedDirection = 0;
    private static final int ROUTE_TITLE_MAX_CHARS = 29;

    public RouteSidePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(8, 10, 8, 10));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        routeLabel = new JLabel("Linea");
        routeLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        routeLabel.setForeground(AppConstants.TEXT_PRIMARY);
        routeLabel.setAlignmentX(LEFT_ALIGNMENT);

        closeButton = new JButton("X");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        closeButton.setForeground(AppConstants.TEXT_PRIMARY);
        closeButton.setFocusPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.setToolTipText("Chiudi ricerca linea");
        closeButton.addActionListener(e -> {
            setVisible(false);
            if (onClose != null) {
                onClose.run();
            }
        });

        titleRow.add(routeLabel, BorderLayout.WEST);
        titleRow.add(closeButton, BorderLayout.EAST);

        metaLabel = new JLabel("0 fermate - 0 veicoli");
        metaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        metaLabel.setForeground(AppConstants.TEXT_SECONDARY);
        metaLabel.setAlignmentX(LEFT_ALIGNMENT);

        header.add(titleRow);
        header.add(Box.createVerticalStrut(2));
        header.add(metaLabel);
        header.add(Box.createVerticalStrut(8));

        directionSwitchButton = new JButton("Direzione");
        directionSwitchButton.setFocusPainted(false);
        directionSwitchButton.setBorderPainted(false);
        directionSwitchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        directionSwitchButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        directionSwitchButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        directionSwitchButton.setContentAreaFilled(true);
        directionSwitchButton.setOpaque(true);
        directionSwitchButton.setBackground(new Color(28, 28, 34, 220));
        directionSwitchButton.setForeground(AppConstants.TEXT_PRIMARY);
        directionSwitchButton.setAlignmentX(LEFT_ALIGNMENT);
        directionSwitchButton.setVisible(false);
        directionSwitchButton.addActionListener(e -> showDirectionMenu());
        header.add(directionSwitchButton);
        add(header, BorderLayout.NORTH);

        timelineCanvas = new TimelineCanvas();
        timelineCanvas.setOpaque(false);

        scrollPane = new JScrollPane(timelineCanvas);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void setOnDirectionSelected(IntConsumer callback) {
        this.onDirectionSelected = callback;
    }

    public void setDirectionOptions(Map<Integer, String> options, int selectedDirection) {
        directionLabels.clear();
        if (options != null) {
            directionLabels.putAll(options);
        }
        this.selectedDirection = selectedDirection;
        rebuildDirectionSwitch();
    }

    public void setRoute(String routeName, List<Stop> stops) {
        String normalizedName = routeName == null || routeName.isBlank() ? "Linea" : routeName.trim();
        routeLabel.setText(ellipsize(normalizedName, ROUTE_TITLE_MAX_CHARS));
        routeLabel.setToolTipText(normalizedName);
        routeStops.clear();
        if (stops != null) {
            routeStops.addAll(stops);
        }
        vehicleMarkers.clear();
        updateMeta();
        timelineCanvas.revalidate();
        timelineCanvas.repaint();
        scrollPane.getVerticalScrollBar().setValue(0);
    }

    public void setVehicleMarkers(List<VehicleMarker> markers) {
        vehicleMarkers.clear();
        if (markers != null) {
            vehicleMarkers.addAll(markers);
        }
        updateMeta();
        timelineCanvas.repaint();
    }

    private void updateMeta() {
        metaLabel.setText(routeStops.size() + " fermate - " + vehicleMarkers.size() + " veicoli");
    }

    private static String ellipsize(String text, int maxChars) {
        if (text == null) return "";
        if (maxChars < 4 || text.length() <= maxChars) return text;
        return text.substring(0, maxChars - 3) + "...";
    }

    private static String normalizeDirectionLabel(String label) {
        if (label == null) return "Direzione";
        String value = label.trim();
        if (value.isEmpty()) return "Direzione";

        if (value.matches("(?i)^dir\\s*\\d+\\s*-\\s*.+$")) {
            int dash = value.indexOf('-');
            value = dash >= 0 && dash + 1 < value.length() ? value.substring(dash + 1).trim() : value;
        } else if (value.matches("^\\d+\\s*-\\s*.+$")) {
            int dash = value.indexOf('-');
            value = dash >= 0 && dash + 1 < value.length() ? value.substring(dash + 1).trim() : value;
        } else if (value.matches("(?i)^dir\\s*\\d+$")) {
            value = "Direzione";
        }

        return value.isEmpty() ? "Direzione" : value;
    }

    private void rebuildDirectionSwitch() {
        if (directionLabels.size() <= 1) {
            directionSwitchButton.setVisible(false);
            directionSwitchButton.setToolTipText(null);
            return;
        }

        if (!directionLabels.containsKey(selectedDirection)) {
            selectedDirection = directionLabels.keySet().iterator().next();
        }

        String selectedLabel = normalizeDirectionLabel(directionLabels.get(selectedDirection));
        directionSwitchButton.setText(ellipsize(selectedLabel, 26) + " v");
        directionSwitchButton.setToolTipText(selectedLabel);
        directionSwitchButton.setVisible(true);
    }

    private void showDirectionMenu() {
        if (directionLabels.size() <= 1) return;

        JPopupMenu menu = new JPopupMenu();
        for (Map.Entry<Integer, String> entry : directionLabels.entrySet()) {
            int directionId = entry.getKey();
            String text = normalizeDirectionLabel(entry.getValue());
            String prefix = directionId == selectedDirection ? "> " : "  ";
            JMenuItem item = new JMenuItem(prefix + ellipsize(text, 42));
            item.addActionListener(e -> {
                if (selectedDirection == directionId) return;
                selectedDirection = directionId;
                rebuildDirectionSwitch();
                if (onDirectionSelected != null) {
                    onDirectionSelected.accept(directionId);
                }
            });
            menu.add(item);
        }
        menu.show(directionSwitchButton, 0, directionSwitchButton.getHeight());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(AppConstants.OVERLAY_CARD_BG);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                AppConstants.OVERLAY_CARD_ARC, AppConstants.OVERLAY_CARD_ARC);
        g2.setColor(AppConstants.OVERLAY_CARD_BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                AppConstants.OVERLAY_CARD_ARC, AppConstants.OVERLAY_CARD_ARC);
        g2.dispose();
    }

    @Override
    public boolean isOpaque() {
        return false;
    }

    private final class TimelineCanvas extends JPanel {
        private static final int LEFT_PADDING = 6;
        private static final int LINE_X = 124;
        private static final int STOPS_TEXT_X = 140;
        private static final int TOP_PADDING = 24;
        private static final int BOTTOM_PADDING = 24;
        private static final int ROW_HEIGHT = 26;
        private static final int STOP_DOT_SIZE = 7;
        private static final int VEHICLE_ICON_SIZE = 18;
        private static final int VEHICLE_LIST_ROW_HEIGHT = 28;

        private final ImageIcon busIcon;
        private final ImageIcon tramIcon;

        private TimelineCanvas() {
            busIcon = loadIcon("/sprites/bus.png", VEHICLE_ICON_SIZE);
            tramIcon = loadIcon("/sprites/tram.png", VEHICLE_ICON_SIZE);
        }

        private ImageIcon loadIcon(String path, int size) {
            try {
                ImageIcon raw = new ImageIcon(getClass().getResource(path));
                Image scaled = raw.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public Dimension getPreferredSize() {
            int stopCount = Math.max(2, routeStops.size());
            int routeH = TOP_PADDING + BOTTOM_PADDING + (stopCount - 1) * ROW_HEIGHT;
            int listH = TOP_PADDING + BOTTOM_PADDING + Math.max(0, vehicleMarkers.size() - 1) * VEHICLE_LIST_ROW_HEIGHT;
            return new Dimension(320, Math.max(routeH, listH));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int stopCount = Math.max(2, routeStops.size());
            int topY = TOP_PADDING;
            int bottomY = TOP_PADDING + (stopCount - 1) * ROW_HEIGHT;

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawLine(LINE_X, topY, LINE_X, bottomY);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            for (int i = 0; i < routeStops.size(); i++) {
                int y = TOP_PADDING + i * ROW_HEIGHT;
                g2.setColor(Color.WHITE);
                g2.fillOval(LINE_X - STOP_DOT_SIZE / 2, y - STOP_DOT_SIZE / 2, STOP_DOT_SIZE, STOP_DOT_SIZE);

                String stopName = routeStops.get(i).getStopName();
                if (stopName.length() > 34) {
                    stopName = stopName.substring(0, 34) + "...";
                }
                g2.setColor(AppConstants.TEXT_PRIMARY);
                g2.drawString(stopName, STOPS_TEXT_X, y + 4);
            }

            List<VehicleMarker> markers = vehicleMarkers.isEmpty()
                    ? Collections.emptyList()
                    : new ArrayList<>(vehicleMarkers);

            for (int i = 0; i < markers.size(); i++) {
                VehicleMarker marker = markers.get(i);

                int yOnRoute = topY + (int) Math.round(marker.getProgress() * (bottomY - topY));
                int drawX = LINE_X - VEHICLE_ICON_SIZE / 2;
                int drawY = yOnRoute - VEHICLE_ICON_SIZE / 2;

                g2.setColor(new Color(255, 255, 255, 220));
                g2.fillOval(drawX - 2, drawY - 2, VEHICLE_ICON_SIZE + 4, VEHICLE_ICON_SIZE + 4);

                ImageIcon icon = marker.getVehicleType() == VehicleType.TRAM ? tramIcon : busIcon;
                if (icon != null) {
                    icon.paintIcon(this, g2, drawX, drawY);
                } else {
                    g2.setColor(marker.getVehicleType() == VehicleType.TRAM
                            ? new Color(255, 140, 0)
                            : new Color(76, 175, 80));
                    g2.fillOval(drawX, drawY, VEHICLE_ICON_SIZE, VEHICLE_ICON_SIZE);
                }

                String title = ellipsize(marker.getTitleText(), 12);
                String details = ellipsize(marker.getDetailText(), 20);
                if (!title.isBlank() || !details.isBlank()) {
                    Font titleFont = new Font("Segoe UI", Font.BOLD, 9);
                    Font detailsFont = new Font("Segoe UI", Font.PLAIN, 9);

                    g2.setFont(titleFont);
                    int titleW = g2.getFontMetrics().stringWidth(title);
                    g2.setFont(detailsFont);
                    int detailW = g2.getFontMetrics().stringWidth(details);

                    int maxBoxW = LINE_X - LEFT_PADDING - 14;
                    int boxW = Math.min(maxBoxW, Math.max(titleW, detailW) + 20);
                    int boxH = 24;
                    int boxX = LEFT_PADDING;
                    int boxY = yOnRoute - (boxH / 2);
                    boxY = Math.max(4, Math.min(getHeight() - boxH - 4, boxY));

                    g2.setColor(new Color(20, 20, 24, 210));
                    g2.fillRoundRect(boxX, boxY, boxW, boxH, 8, 8);
                    g2.setColor(AppConstants.OVERLAY_CARD_BORDER);
                    g2.drawRoundRect(boxX, boxY, boxW, boxH, 8, 8);

                    int connectorY = boxY + boxH / 2;
                    g2.setColor(new Color(210, 210, 220, 170));
                    g2.drawLine(boxX + boxW, connectorY, drawX - 3, yOnRoute);

                    int iconListX = boxX + 4;
                    int iconListY = boxY + 6;
                    if (icon != null) {
                        icon.paintIcon(this, g2, iconListX, iconListY);
                    } else {
                        g2.setColor(marker.getVehicleType() == VehicleType.TRAM
                                ? new Color(255, 140, 0)
                                : new Color(76, 175, 80));
                        g2.fillOval(iconListX, iconListY, 12, 12);
                    }

                    g2.setFont(titleFont);
                    g2.setColor(new Color(190, 190, 210));
                    g2.drawString(title, boxX + 18, boxY + 10);

                    g2.setFont(detailsFont);
                    g2.setColor(AppConstants.TEXT_PRIMARY);
                    g2.drawString(details, boxX + 18, boxY + 20);
                }
            }

            g2.setColor(AppConstants.TEXT_SECONDARY);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.drawString("Start", LEFT_PADDING + 2, topY - 8);
            g2.drawString("End", LEFT_PADDING + 2, bottomY + 16);

            g2.dispose();
        }
    }
}

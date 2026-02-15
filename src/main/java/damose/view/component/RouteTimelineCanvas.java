package damose.view.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import damose.config.AppConstants;
import damose.model.Stop;
import damose.model.VehicleType;

/**
 * Timeline canvas that renders route stops and tracked vehicles.
 */
final class RouteTimelineCanvas extends JPanel {
    private static final int LINE_X = 26;
    private static final int STOPS_TEXT_X = 42;
    private static final int TOP_PADDING = 24;
    private static final int BOTTOM_PADDING = 24;
    private static final int ROW_HEIGHT = 26;
    private static final int STOP_DOT_SIZE = 7;
    private static final int VEHICLE_ICON_SIZE = 18;
    private static final int VEHICLE_ICON_SIZE_SELECTED = 24;

    private final ImageIcon busIcon;
    private final ImageIcon tramIcon;
    private final ImageIcon busIconSelected;
    private final ImageIcon tramIconSelected;
    private final List<CircularHitArea<RouteSidePanel.VehicleMarker>> markerHitAreas = new ArrayList<>();
    private final List<JButton> stopButtons = new ArrayList<>();

    private final List<Stop> routeStops;
    private final List<RouteSidePanel.VehicleMarker> vehicleMarkers;
    private final Supplier<String> selectedVehicleMarkerIdSupplier;
    private final Consumer<String> selectedVehicleMarkerIdUpdater;
    private final Supplier<Consumer<RouteSidePanel.VehicleMarker>> vehicleSelectedCallbackSupplier;
    private final Supplier<Consumer<Stop>> stopSelectedCallbackSupplier;
    private final IntSupplier viewportWidthSupplier;

    RouteTimelineCanvas(List<Stop> routeStops,
                        List<RouteSidePanel.VehicleMarker> vehicleMarkers,
                        Supplier<String> selectedVehicleMarkerIdSupplier,
                        Consumer<String> selectedVehicleMarkerIdUpdater,
                        Supplier<Consumer<RouteSidePanel.VehicleMarker>> vehicleSelectedCallbackSupplier,
                        Supplier<Consumer<Stop>> stopSelectedCallbackSupplier,
                        IntSupplier viewportWidthSupplier) {
        this.routeStops = routeStops;
        this.vehicleMarkers = vehicleMarkers;
        this.selectedVehicleMarkerIdSupplier = selectedVehicleMarkerIdSupplier;
        this.selectedVehicleMarkerIdUpdater = selectedVehicleMarkerIdUpdater;
        this.vehicleSelectedCallbackSupplier = vehicleSelectedCallbackSupplier;
        this.stopSelectedCallbackSupplier = stopSelectedCallbackSupplier;
        this.viewportWidthSupplier = viewportWidthSupplier;

        setLayout(null);
        busIcon = IconLoader.loadScaled(getClass(), "/sprites/bus.png", VEHICLE_ICON_SIZE);
        tramIcon = IconLoader.loadScaled(getClass(), "/sprites/tram.png", VEHICLE_ICON_SIZE);
        busIconSelected = IconLoader.loadScaled(getClass(), "/sprites/bus.png", VEHICLE_ICON_SIZE_SELECTED);
        tramIconSelected = IconLoader.loadScaled(getClass(), "/sprites/tram.png", VEHICLE_ICON_SIZE_SELECTED);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                refreshStopButtons();
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                RouteSidePanel.VehicleMarker marker = findMarkerAt(e.getX(), e.getY());
                Consumer<RouteSidePanel.VehicleMarker> callback = vehicleSelectedCallbackSupplier.get();
                if (marker == null || callback == null) {
                    return;
                }
                selectedVehicleMarkerIdUpdater.accept(marker.getVehicleId());
                repaint();
                callback.accept(marker);
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                RouteSidePanel.VehicleMarker marker = findMarkerAt(e.getX(), e.getY());
                if (marker != null) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });
    }

    void refreshStopButtons() {
        for (JButton button : stopButtons) {
            remove(button);
        }
        stopButtons.clear();

        if (routeStops.isEmpty()) {
            revalidate();
            repaint();
            return;
        }

        int buttonX = Math.max(0, STOPS_TEXT_X - 4);
        int buttonWidth = Math.max(90, getWidth() - buttonX - 6);
        for (int i = 0; i < routeStops.size(); i++) {
            Stop stop = routeStops.get(i);
            int y = TOP_PADDING + i * ROW_HEIGHT;

            JButton button = new JButton();
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setToolTipText(stop.getStopName());
            button.setBounds(buttonX, y - 11, buttonWidth, 22);
            button.addActionListener(e -> {
                Consumer<Stop> callback = stopSelectedCallbackSupplier.get();
                if (callback != null) {
                    callback.accept(stop);
                }
            });
            stopButtons.add(button);
            add(button);
        }

        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int stopCount = Math.max(2, routeStops.size());
        int routeH = TOP_PADDING + BOTTOM_PADDING + (stopCount - 1) * ROW_HEIGHT;
        int viewportW = viewportWidthSupplier.getAsInt();
        int width = viewportW > 0 ? viewportW - 4 : 220;
        return new Dimension(Math.max(190, width), routeH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        markerHitAreas.clear();

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
            if (stopName.length() > 22) {
                stopName = stopName.substring(0, 22) + "...";
            }
            g2.setColor(AppConstants.TEXT_PRIMARY);
            g2.drawString(stopName, STOPS_TEXT_X, y + 4);
        }

        List<RouteSidePanel.VehicleMarker> markers = new ArrayList<>(vehicleMarkers);
        String selectedVehicleMarkerId = selectedVehicleMarkerIdSupplier.get();
        for (RouteSidePanel.VehicleMarker marker : markers) {
            boolean selected = selectedVehicleMarkerId != null
                    && selectedVehicleMarkerId.equalsIgnoreCase(marker.getVehicleId());
            int iconSize = selected ? VEHICLE_ICON_SIZE_SELECTED : VEHICLE_ICON_SIZE;

            int yOnRoute = topY + (int) Math.round(marker.getProgress() * (bottomY - topY));
            int drawX = LINE_X - iconSize / 2;
            int drawY = yOnRoute - iconSize / 2;

            g2.setColor(selected ? new Color(255, 255, 255, 245) : new Color(255, 255, 255, 220));
            g2.fillOval(drawX - 2, drawY - 2, iconSize + 4, iconSize + 4);

            ImageIcon icon;
            if (marker.getVehicleType() == VehicleType.TRAM) {
                icon = selected ? tramIconSelected : tramIcon;
            } else {
                icon = selected ? busIconSelected : busIcon;
            }
            if (icon != null) {
                icon.paintIcon(this, g2, drawX, drawY);
            } else {
                g2.setColor(marker.getVehicleType() == VehicleType.TRAM
                        ? new Color(255, 140, 0)
                        : new Color(76, 175, 80));
                g2.fillOval(drawX, drawY, iconSize, iconSize);
            }
            markerHitAreas.add(new CircularHitArea<>(marker, LINE_X, yOnRoute, iconSize / 2 + 6));
        }

        g2.dispose();
    }

    private RouteSidePanel.VehicleMarker findMarkerAt(int x, int y) {
        CircularHitArea<RouteSidePanel.VehicleMarker> best = null;
        double bestDist = Double.MAX_VALUE;
        for (CircularHitArea<RouteSidePanel.VehicleMarker> area : markerHitAreas) {
            if (!area.contains(x, y)) continue;
            double dist = area.distance2(x, y);
            if (dist < bestDist) {
                best = area;
                bestDist = dist;
            }
        }
        return best != null ? best.payload() : null;
    }
}

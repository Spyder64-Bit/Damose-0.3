package damose.view.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import damose.config.AppConstants;
import damose.model.Stop;
import damose.model.VehicleType;

/**
 * UI component for route side panel.
 */
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

        /**
         * Returns the progress.
         */
        public double getProgress() {
            return progress;
        }

        /**
         * Returns the vehicle id.
         */
        public String getVehicleId() {
            return vehicleId;
        }

        /**
         * Returns the vehicle type.
         */
        public VehicleType getVehicleType() {
            return vehicleType;
        }

        /**
         * Returns the title text.
         */
        public String getTitleText() {
            return titleText;
        }

        /**
         * Returns the detail text.
         */
        public String getDetailText() {
            return detailText;
        }
    }

    private final JLabel routeLabel;
    private final JLabel metaLabel;
    private final JButton closeButton;
    private final JButton directionSwitchButton;
    private final JPanel titleRow;
    private final RouteTimelineCanvas timelineCanvas;
    private final JScrollPane scrollPane;
    private final JPanel scrollContent;

    private final List<Stop> routeStops = new ArrayList<>();
    private final List<VehicleMarker> vehicleMarkers = new ArrayList<>();
    private final Map<Integer, String> directionLabels = new LinkedHashMap<>();
    private String selectedVehicleMarkerId;
    private Runnable onClose;
    private IntConsumer onDirectionSelected;
    private Consumer<VehicleMarker> onVehicleMarkerSelected;
    private Consumer<Stop> onStopSelected;
    private int selectedDirection = 0;
    private String routeFullName = "Linea";
    private static final Color DIRECTION_BTN_BG = new Color(34, 40, 52, 230);
    private static final Color DIRECTION_BTN_BG_HOVER = new Color(44, 52, 68, 235);
    private static final Color DIRECTION_BTN_BORDER = new Color(84, 96, 116, 220);

    public RouteSidePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(0, 0, 0, 0));

        titleRow = new JPanel(new BorderLayout(8, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(LEFT_ALIGNMENT);

        routeLabel = new JLabel("Linea");
        routeLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        routeLabel.setForeground(AppConstants.TEXT_PRIMARY);
        routeLabel.setAlignmentX(LEFT_ALIGNMENT);
        routeLabel.setHorizontalAlignment(JLabel.LEFT);
        routeLabel.setBorder(new EmptyBorder(0, 0, 0, 4));

        closeButton = new JButton("X");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        closeButton.setForeground(AppConstants.TEXT_PRIMARY);
        closeButton.setFocusPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.setPreferredSize(new Dimension(26, 26));
        closeButton.setMinimumSize(new Dimension(26, 26));
        closeButton.setMaximumSize(new Dimension(26, 26));
        closeButton.setToolTipText("Chiudi ricerca linea");
        closeButton.addActionListener(e -> {
            setVisible(false);
            if (onClose != null) {
                onClose.run();
            }
        });

        titleRow.add(routeLabel, BorderLayout.CENTER);
        titleRow.add(closeButton, BorderLayout.EAST);
        titleRow.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateRouteLabelForWidth();
            }
        });

        metaLabel = new JLabel("0 fermate - 0 veicoli");
        metaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        metaLabel.setForeground(AppConstants.TEXT_SECONDARY);
        metaLabel.setAlignmentX(LEFT_ALIGNMENT);
        metaLabel.setHorizontalAlignment(JLabel.LEFT);
        metaLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, metaLabel.getPreferredSize().height));

        header.add(titleRow);
        header.add(Box.createVerticalStrut(2));
        header.add(metaLabel);
        header.add(Box.createVerticalStrut(8));

        directionSwitchButton = new JButton("Direzione");
        directionSwitchButton.setFocusPainted(false);
        directionSwitchButton.setBorderPainted(true);
        directionSwitchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        directionSwitchButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
        directionSwitchButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DIRECTION_BTN_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        directionSwitchButton.setContentAreaFilled(true);
        directionSwitchButton.setOpaque(true);
        directionSwitchButton.setBackground(DIRECTION_BTN_BG);
        directionSwitchButton.setForeground(AppConstants.TEXT_PRIMARY);
        directionSwitchButton.setAlignmentX(LEFT_ALIGNMENT);
        directionSwitchButton.setHorizontalAlignment(JButton.LEFT);
        directionSwitchButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        directionSwitchButton.setVisible(false);
        directionSwitchButton.addActionListener(e -> showDirectionMenu());
        directionSwitchButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                directionSwitchButton.setBackground(DIRECTION_BTN_BG_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                directionSwitchButton.setBackground(DIRECTION_BTN_BG);
            }
        });
        header.add(directionSwitchButton);

        timelineCanvas = new RouteTimelineCanvas(
                routeStops,
                vehicleMarkers,
                () -> selectedVehicleMarkerId,
                markerId -> selectedVehicleMarkerId = markerId,
                () -> onVehicleMarkerSelected,
                () -> onStopSelected,
                this::getTimelineViewportWidth
        );
        timelineCanvas.setOpaque(false);
        timelineCanvas.setAlignmentX(LEFT_ALIGNMENT);

        scrollContent = new ScrollContentPanel();
        scrollContent.setOpaque(false);
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setBorder(new EmptyBorder(8, 10, 8, 10));
        scrollContent.add(header);
        scrollContent.add(Box.createVerticalStrut(6));
        scrollContent.add(timelineCanvas);

        scrollPane = new JScrollPane(scrollContent);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Registers callback for close.
     */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /**
     * Registers callback for direction selected.
     */
    public void setOnDirectionSelected(IntConsumer callback) {
        this.onDirectionSelected = callback;
    }

    /**
     * Registers callback for vehicle marker selected.
     */
    public void setOnVehicleMarkerSelected(Consumer<VehicleMarker> callback) {
        this.onVehicleMarkerSelected = callback;
    }

    /**
     * Registers callback for stop selected.
     */
    public void setOnStopSelected(Consumer<Stop> callback) {
        this.onStopSelected = callback;
    }

    /**
     * Updates the direction options value.
     */
    public void setDirectionOptions(Map<Integer, String> options, int selectedDirection) {
        directionLabels.clear();
        if (options != null) {
            directionLabels.putAll(options);
        }
        this.selectedDirection = selectedDirection;
        rebuildDirectionSwitch();
    }

    /**
     * Updates the route value.
     */
    public void setRoute(String routeName, List<Stop> stops) {
        String normalizedName = routeName == null || routeName.isBlank() ? "Linea" : routeName.trim();
        routeFullName = normalizedName;
        updateRouteLabelForWidth();
        routeStops.clear();
        if (stops != null) {
            routeStops.addAll(stops);
        }
        vehicleMarkers.clear();
        selectedVehicleMarkerId = null;
        updateMeta();
        timelineCanvas.refreshStopButtons();
        timelineCanvas.revalidate();
        timelineCanvas.repaint();
        scrollPane.getVerticalScrollBar().setValue(0);
    }

    /**
     * Updates the vehicle markers value.
     */
    public void setVehicleMarkers(List<VehicleMarker> markers) {
        vehicleMarkers.clear();
        if (markers != null) {
            vehicleMarkers.addAll(markers);
        }
        if (selectedVehicleMarkerId != null
                && vehicleMarkers.stream().noneMatch(m -> selectedVehicleMarkerId.equalsIgnoreCase(m.getVehicleId()))) {
            selectedVehicleMarkerId = null;
        }
        updateMeta();
        timelineCanvas.repaint();
    }

    private void updateMeta() {
        metaLabel.setText(routeStops.size() + " fermate - " + vehicleMarkers.size() + " veicoli");
    }

    private int getTimelineViewportWidth() {
        return scrollPane != null ? scrollPane.getViewport().getWidth() : 0;
    }

    private void updateRouteLabelForWidth() {
        String normalized = routeFullName == null || routeFullName.isBlank() ? "Linea" : routeFullName.trim();
        int availableWidth = titleRow.getWidth() - closeButton.getPreferredSize().width - 14;
        if (availableWidth <= 30) {
            routeLabel.setText(RouteTextUtils.ellipsize(normalized, 18));
        } else {
            FontMetrics fm = routeLabel.getFontMetrics(routeLabel.getFont());
            routeLabel.setText(RouteTextUtils.ellipsizeToWidth(normalized, fm, availableWidth));
        }
        routeLabel.setToolTipText(normalized);
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

        String selectedLabel = RouteTextUtils.normalizeDirectionLabel(directionLabels.get(selectedDirection));
        directionSwitchButton.setText("v " + RouteTextUtils.ellipsize(selectedLabel, 22));
        directionSwitchButton.setToolTipText(selectedLabel);
        directionSwitchButton.setVisible(true);
    }

    private void showDirectionMenu() {
        if (directionLabels.size() <= 1) return;

        JPopupMenu menu = new JPopupMenu();
        for (Map.Entry<Integer, String> entry : directionLabels.entrySet()) {
            int directionId = entry.getKey();
            String text = RouteTextUtils.normalizeDirectionLabel(entry.getValue());
            JMenuItem item = new JMenuItem(RouteTextUtils.ellipsize(text, 36));
            if (directionId == selectedDirection) {
                item.setIcon(new StatusDotIcon(new Color(99, 210, 99)));
                item.setFont(item.getFont().deriveFont(Font.BOLD));
            } else {
                item.setIcon(new StatusDotIcon(new Color(80, 80, 86)));
            }
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
    /**
     * Returns whether opaque.
     */
    public boolean isOpaque() {
        return false;
    }

    private static final class ScrollContentPanel extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(visibleRect.height - 28, 60);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

}


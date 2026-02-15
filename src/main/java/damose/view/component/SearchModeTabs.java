package damose.view.component;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import damose.config.AppConstants;

/**
 * Mode-tab bar used by SearchOverlay.
 */
final class SearchModeTabs {

    private final JPanel panel;
    private final JLabel stopsModeBtn;
    private final JLabel linesModeBtn;
    private final JLabel favoritesModeBtn;
    private SearchOverlayMode currentMode = SearchOverlayMode.STOPS;
    private Consumer<SearchOverlayMode> onModeChanged;

    SearchModeTabs() {
        panel = new JPanel();
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 0, 12, 0));

        stopsModeBtn = createModeButton("Fermate");
        linesModeBtn = createModeButton("Linee");
        favoritesModeBtn = createModeButton("Preferiti");

        installModeClick(stopsModeBtn, SearchOverlayMode.STOPS);
        installModeClick(linesModeBtn, SearchOverlayMode.LINES);
        installModeClick(favoritesModeBtn, SearchOverlayMode.FAVORITES);

        panel.add(stopsModeBtn);
        panel.add(Box.createHorizontalStrut(6));
        panel.add(linesModeBtn);
        panel.add(Box.createHorizontalStrut(6));
        panel.add(favoritesModeBtn);
        updateStyles();
    }

    JPanel panel() {
        return panel;
    }

    SearchOverlayMode currentMode() {
        return currentMode;
    }

    void setOnModeChanged(Consumer<SearchOverlayMode> callback) {
        this.onModeChanged = callback;
    }

    void setCurrentMode(SearchOverlayMode mode) {
        if (mode == null || mode == currentMode) {
            return;
        }
        currentMode = mode;
        updateStyles();
        if (onModeChanged != null) {
            onModeChanged.accept(mode);
        }
    }

    void cycleMode() {
        setCurrentMode(currentMode.next());
    }

    private void installModeClick(JLabel label, SearchOverlayMode mode) {
        label.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                setCurrentMode(mode);
            }
        });
    }

    private JLabel createModeButton(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setOpaque(true);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setBorder(new EmptyBorder(8, 20, 8, 20));
        return label;
    }

    private void updateStyles() {
        stopsModeBtn.setBackground(AppConstants.BG_FIELD);
        stopsModeBtn.setForeground(AppConstants.TEXT_SECONDARY);
        linesModeBtn.setBackground(AppConstants.BG_FIELD);
        linesModeBtn.setForeground(AppConstants.TEXT_SECONDARY);
        favoritesModeBtn.setBackground(AppConstants.BG_FIELD);
        favoritesModeBtn.setForeground(AppConstants.TEXT_SECONDARY);

        JLabel selected = switch (currentMode) {
            case STOPS -> stopsModeBtn;
            case LINES -> linesModeBtn;
            case FAVORITES -> favoritesModeBtn;
        };
        selected.setBackground(AppConstants.ACCENT);
        selected.setForeground(Color.WHITE);
    }
}

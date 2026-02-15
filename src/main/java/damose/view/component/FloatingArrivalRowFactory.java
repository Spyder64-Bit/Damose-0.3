package damose.view.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import damose.config.AppConstants;

/**
 * Builds row labels used inside FloatingArrivalPanel.
 */
final class FloatingArrivalRowFactory {

    private FloatingArrivalRowFactory() {
    }

    static JLabel createNoTripsLabel(Font font) {
        JLabel noData = new JLabel("Nessun passaggio programmato");
        noData.setForeground(Color.WHITE);
        noData.setFont(font);
        noData.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        return noData;
    }

    static JLabel createTripRowLabel(String trip, Font font) {
        String html = "<html><body style='width:" + (AppConstants.FLOATING_PANEL_WIDTH - 60)
                + "px'>" + (trip == null ? "" : trip) + "</body></html>";
        JLabel label = new JLabel(html);
        label.setForeground(Color.WHITE);
        label.setFont(font);
        label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        label.setMaximumSize(new Dimension(AppConstants.FLOATING_PANEL_WIDTH - 40, 70));
        label.setVerticalTextPosition(JLabel.TOP);
        return label;
    }

    static JLabel createArrivalRowLabel(String rawText, boolean compactRowsMode, int wrapWidth, Font font) {
        ParsedRow parsed = parse(rawText);
        String html = compactRowsMode
                ? PanelTextFormatter.toCompactWrappedHtml(parsed.text, 32)
                : PanelTextFormatter.toWrappedHtml(parsed.text, wrapWidth);

        JLabel label = new JLabel(html);
        label.setForeground(Color.WHITE);
        label.setFont(font);
        int verticalPadding = compactRowsMode ? 3 : 8;
        label.setBorder(BorderFactory.createEmptyBorder(verticalPadding, 2, verticalPadding, 2));
        label.setIcon(new DotIcon(10, parsed.dotColor));
        label.setIconTextGap(compactRowsMode ? 8 : 12);
        label.setMaximumSize(new Dimension(AppConstants.FLOATING_PANEL_WIDTH - 36, Integer.MAX_VALUE));
        label.setVerticalTextPosition(JLabel.TOP);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static ParsedRow parse(String text) {
        String displayText = text == null ? "" : text;
        Color dotColor = null;

        if (displayText.startsWith("[DOT_RED]")) {
            dotColor = AppConstants.ERROR_COLOR;
            displayText = displayText.substring("[DOT_RED]".length()).trim();
        } else if (displayText.startsWith("[DOT_GREEN]")) {
            dotColor = AppConstants.SUCCESS_COLOR;
            displayText = displayText.substring("[DOT_GREEN]".length()).trim();
        } else if (displayText.startsWith("[DOT_GRAY]")) {
            dotColor = AppConstants.TEXT_SECONDARY;
            displayText = displayText.substring("[DOT_GRAY]".length()).trim();
        }

        if (dotColor == null) {
            dotColor = inferDotColor(displayText);
        }

        return new ParsedRow(displayText, dotColor);
    }

    private static Color inferDotColor(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("ritardo")) {
            return AppConstants.ERROR_COLOR;
        }
        if (lower.contains("anticipo") || lower.contains("in orario")) {
            return AppConstants.SUCCESS_COLOR;
        }
        if (lower.contains("statico")) {
            return AppConstants.TEXT_SECONDARY;
        }
        return Color.WHITE;
    }

    private static final class ParsedRow {
        private final String text;
        private final Color dotColor;

        private ParsedRow(String text, Color dotColor) {
            this.text = text;
            this.dotColor = dotColor;
        }
    }
}

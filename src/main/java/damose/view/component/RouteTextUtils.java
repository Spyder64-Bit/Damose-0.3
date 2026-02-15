package damose.view.component;

import java.awt.FontMetrics;

/**
 * Text helpers used by route side panel.
 */
final class RouteTextUtils {

    private RouteTextUtils() {
    }

    static String ellipsizeToWidth(String text, FontMetrics fm, int maxWidth) {
        if (text == null || text.isEmpty()) return "";
        if (fm.stringWidth(text) <= maxWidth) return text;
        String suffix = "...";
        int suffixW = fm.stringWidth(suffix);
        if (suffixW >= maxWidth) return suffix;
        int end = text.length();
        while (end > 0 && fm.stringWidth(text.substring(0, end)) + suffixW > maxWidth) {
            end--;
        }
        return end <= 0 ? suffix : text.substring(0, end) + suffix;
    }

    static String ellipsize(String text, int maxChars) {
        if (text == null) return "";
        if (maxChars < 4 || text.length() <= maxChars) return text;
        return text.substring(0, maxChars - 3) + "...";
    }

    static String normalizeDirectionLabel(String label) {
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
}

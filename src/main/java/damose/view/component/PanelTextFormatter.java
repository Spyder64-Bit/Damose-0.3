package damose.view.component;

/**
 * Text formatting helper for floating panel labels.
 */
final class PanelTextFormatter {

    private PanelTextFormatter() {
    }

    static String toWrappedHtml(String text, int widthPx) {
        int safeWidth = Math.max(120, widthPx);
        String safeText = injectBreakHints(escapeHtml(text == null ? "" : text));
        return "<html><body style='width:" + safeWidth
                + "px;word-wrap:break-word;overflow-wrap:anywhere;word-break:break-word;margin:0;padding:0;'>"
                + safeText + "</body></html>";
    }

    static String toCompactWrappedHtml(String text, int maxCharsPerLine) {
        int lineLimit = Math.max(18, maxCharsPerLine);
        String raw = text == null ? "" : text.trim();
        if (raw.isEmpty()) {
            return "<html></html>";
        }

        StringBuilder html = new StringBuilder("<html>");
        StringBuilder line = new StringBuilder();

        for (String word : raw.split("\\s+")) {
            if (word.isEmpty()) continue;

            String token = word;
            while (token.length() > lineLimit) {
                if (line.length() > 0) {
                    html.append(escapeHtml(line.toString())).append("<br>");
                    line.setLength(0);
                }
                html.append(escapeHtml(token.substring(0, lineLimit - 1))).append("-<br>");
                token = token.substring(lineLimit - 1);
            }

            int projected = line.length() == 0 ? token.length() : (line.length() + 1 + token.length());
            if (projected > lineLimit && line.length() > 0) {
                html.append(escapeHtml(line.toString())).append("<br>");
                line.setLength(0);
            }

            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(token);
        }

        if (line.length() > 0) {
            html.append(escapeHtml(line.toString()));
        }
        html.append("</html>");
        return html.toString();
    }

    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String injectBreakHints(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text
                .replace("/", "/<wbr>")
                .replace("-", "-<wbr>")
                .replace(",", ",<wbr> ")
                .replace(": ", ":<wbr> ");
    }
}

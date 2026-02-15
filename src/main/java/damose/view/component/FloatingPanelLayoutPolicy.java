package damose.view.component;

/**
 * Layout policy for FloatingArrivalPanel content and scroll sizing.
 */
final class FloatingPanelLayoutPolicy {

    private static final int HEADER_HEIGHT = 50;
    private static final int FOOTER_HEIGHT = 40;
    private static final int PADDING = 10;
    private static final int ARRIVAL_ROW_HEIGHT = 44;
    private static final int ARRIVAL_COMPACT_ROW_HEIGHT = 32;
    private static final int TRIP_ROW_HEIGHT = 30;
    private static final int TRIP_MIN_SCROLL_HEIGHT = 80;
    private static final int TRIP_MAX_SCROLL_HEIGHT = 150;

    private FloatingPanelLayoutPolicy() {
    }

    static SizeTarget forArrivals(int rows, boolean compactRowsMode) {
        int rowHeight = compactRowsMode ? ARRIVAL_COMPACT_ROW_HEIGHT : ARRIVAL_ROW_HEIGHT;
        int safeRows = Math.max(rows, 1);
        int scrollHeight = Math.max(safeRows * rowHeight, rowHeight);
        int contentHeight = HEADER_HEIGHT + scrollHeight + FOOTER_HEIGHT + PADDING;
        return new SizeTarget(contentHeight, scrollHeight);
    }

    static SizeTarget forTrips(int rows) {
        int safeRows = Math.max(rows, 1);
        int scrollHeight = Math.max(safeRows * TRIP_ROW_HEIGHT, TRIP_ROW_HEIGHT);
        scrollHeight = Math.min(Math.max(scrollHeight, TRIP_MIN_SCROLL_HEIGHT), TRIP_MAX_SCROLL_HEIGHT);
        int contentHeight = HEADER_HEIGHT + scrollHeight + FOOTER_HEIGHT + PADDING;
        return new SizeTarget(contentHeight, scrollHeight);
    }

    static final class SizeTarget {
        final int contentHeight;
        final int scrollHeight;

        SizeTarget(int contentHeight, int scrollHeight) {
            this.contentHeight = contentHeight;
            this.scrollHeight = scrollHeight;
        }
    }
}

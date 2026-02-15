package damose.view.component;

/**
 * Calculates floating panel content and scroll sizing constraints.
 */
final class FloatingPanelSizeCalculator {

    private FloatingPanelSizeCalculator() {
    }

    static int resolvePanelMaxHeight(int parentHeight, int fallbackMaxHeight) {
        int parentHeightCap = Math.max(220, parentHeight - 20);
        return Math.min(fallbackMaxHeight, parentHeightCap);
    }

    static SizeResult clamp(int contentHeight,
                            int scrollHeight,
                            int minScrollHeight,
                            int maxPanelHeight) {
        int fixedSectionHeight = Math.max(0, contentHeight - scrollHeight);
        int maxScrollHeight = Math.max(minScrollHeight, maxPanelHeight - fixedSectionHeight);
        int clampedScrollHeight = Math.min(Math.max(scrollHeight, minScrollHeight), maxScrollHeight);
        int clampedContentHeight = fixedSectionHeight + clampedScrollHeight;
        return new SizeResult(clampedContentHeight, clampedScrollHeight);
    }

    static final class SizeResult {
        final int contentHeight;
        final int scrollHeight;

        SizeResult(int contentHeight, int scrollHeight) {
            this.contentHeight = contentHeight;
            this.scrollHeight = scrollHeight;
        }
    }
}

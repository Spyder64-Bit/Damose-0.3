package damose.view.component;

/**
 * Available modes for the search overlay.
 */
enum SearchOverlayMode {
    STOPS,
    LINES,
    FAVORITES;

    SearchOverlayMode next() {
        return switch (this) {
            case STOPS -> LINES;
            case LINES -> FAVORITES;
            case FAVORITES -> STOPS;
        };
    }
}

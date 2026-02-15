package damose.view;

import java.util.List;
import java.util.function.Consumer;

import javax.swing.JLayeredPane;

import damose.model.Stop;
import damose.view.component.InfoOverlay;
import damose.view.component.SearchOverlay;

/**
 * Coordinates search/info popup overlays and related callbacks.
 */
final class SearchOverlaySection {

    private SearchOverlay searchOverlay;
    private InfoOverlay infoOverlay;
    private List<Stop> pendingStops;
    private List<Stop> pendingLines;
    private Consumer<Stop> onSearchSelect;
    private Runnable onFavoritesLoginRequired;
    private List<Stop> pendingFavorites;

    void initialize(JLayeredPane hostPane, int width, int height) {
        searchOverlay = new SearchOverlay();
        searchOverlay.setVisible(false);
        searchOverlay.setBounds(0, 0, width, height);
        hostPane.add(searchOverlay, JLayeredPane.POPUP_LAYER);
        applyPendingSearchState();

        infoOverlay = new InfoOverlay();
        infoOverlay.setVisible(false);
        infoOverlay.setBounds(0, 0, width, height);
        hostPane.add(infoOverlay, JLayeredPane.POPUP_LAYER);
    }

    void updateBounds(int width, int height) {
        if (searchOverlay != null) {
            searchOverlay.setBounds(0, 0, width, height);
        }
        if (infoOverlay != null) {
            infoOverlay.setBounds(0, 0, width, height);
        }
    }

    void showSearchOverlay() {
        if (searchOverlay != null) {
            searchOverlay.showSearch();
        }
    }

    void showInfoOverlay() {
        if (infoOverlay != null) {
            infoOverlay.showInfo();
        }
    }

    void setSearchData(List<Stop> stops, List<Stop> lines) {
        pendingStops = stops;
        pendingLines = lines;
        if (searchOverlay != null) {
            searchOverlay.setData(stops, lines);
        }
    }

    void setOnSearchSelect(Consumer<Stop> callback) {
        onSearchSelect = callback;
        if (searchOverlay != null) {
            searchOverlay.setOnSelect(callback);
        }
    }

    void setOnFavoritesLoginRequired(Runnable callback) {
        onFavoritesLoginRequired = callback;
        if (searchOverlay != null) {
            searchOverlay.setOnFavoritesLoginRequired(callback);
        }
    }

    void showFavorites(List<Stop> favorites) {
        pendingFavorites = favorites;
        if (searchOverlay != null) {
            searchOverlay.showFavorites(favorites);
        }
    }

    private void applyPendingSearchState() {
        if (searchOverlay == null) {
            return;
        }
        if (pendingStops != null || pendingLines != null) {
            searchOverlay.setData(pendingStops, pendingLines);
        }
        if (onSearchSelect != null) {
            searchOverlay.setOnSelect(onSearchSelect);
        }
        if (onFavoritesLoginRequired != null) {
            searchOverlay.setOnFavoritesLoginRequired(onFavoritesLoginRequired);
        }
        if (pendingFavorites != null) {
            searchOverlay.showFavorites(pendingFavorites);
        }
    }
}

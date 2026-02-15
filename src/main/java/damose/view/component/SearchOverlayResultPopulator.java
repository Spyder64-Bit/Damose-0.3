package damose.view.component;

import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;

import damose.model.Stop;

/**
 * Populates SearchOverlay list model with filtered/ranked results.
 */
final class SearchOverlayResultPopulator {

    private SearchOverlayResultPopulator() {
    }

    static void populate(DefaultListModel<Stop> listModel,
                         List<Stop> source,
                         SearchOverlayMode mode,
                         String query) {
        listModel.clear();

        int limit = mode == SearchOverlayMode.FAVORITES ? 100 : 50;
        if (mode == SearchOverlayMode.LINES && !query.isEmpty()) {
            List<Stop> matches = new ArrayList<>();
            for (Stop stop : source) {
                if (SearchResultMatcher.matchesQuery(stop, query)) {
                    matches.add(stop);
                }
            }
            matches.sort((a, b) -> SearchResultMatcher.compareLineResults(a, b, query));
            for (int i = 0; i < Math.min(limit, matches.size()); i++) {
                listModel.addElement(matches.get(i));
            }
            return;
        }

        int added = 0;
        for (Stop stop : source) {
            if (!SearchResultMatcher.matchesQuery(stop, query)) {
                continue;
            }
            listModel.addElement(stop);
            added++;
            if (added >= limit) {
                break;
            }
        }
    }
}

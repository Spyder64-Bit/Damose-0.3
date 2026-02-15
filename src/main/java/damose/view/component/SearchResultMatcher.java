package damose.view.component;

import damose.model.Stop;

/**
 * Matching and ranking utilities for search overlay results.
 */
final class SearchResultMatcher {

    private SearchResultMatcher() {
    }

    static boolean matchesQuery(Stop stop, String query) {
        if (stop == null) return false;
        if (query == null || query.isEmpty()) return true;

        String name = stop.getStopName();
        String id = stop.getStopId();
        String nameLower = name == null ? "" : name.toLowerCase();
        String idLower = id == null ? "" : id.toLowerCase();
        return nameLower.contains(query) || idLower.contains(query);
    }

    static int compareLineResults(Stop a, Stop b, String query) {
        int scoreA = lineMatchScore(a, query);
        int scoreB = lineMatchScore(b, query);
        if (scoreA != scoreB) {
            return Integer.compare(scoreA, scoreB);
        }

        String idA = a.getStopId() == null ? "" : a.getStopId().trim();
        String idB = b.getStopId() == null ? "" : b.getStopId().trim();
        if (idA.length() != idB.length()) {
            return Integer.compare(idA.length(), idB.length());
        }

        int idCmp = String.CASE_INSENSITIVE_ORDER.compare(idA, idB);
        if (idCmp != 0) {
            return idCmp;
        }

        String nameA = a.getStopName() == null ? "" : a.getStopName().trim();
        String nameB = b.getStopName() == null ? "" : b.getStopName().trim();
        return String.CASE_INSENSITIVE_ORDER.compare(nameA, nameB);
    }

    private static int lineMatchScore(Stop line, String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        String id = line.getStopId() == null ? "" : line.getStopId().trim().toLowerCase();
        String name = line.getStopName() == null ? "" : line.getStopName().trim().toLowerCase();
        if (q.isEmpty()) return 99;

        if (id.equals(q)) return 0;
        if (id.startsWith(q)) return 1;
        if (id.contains(q)) return 2;
        if (name.startsWith(q)) return 3;
        if (name.contains(q)) return 4;
        return 5;
    }
}

package damose.data.mapper;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class TripIdUtils {

    private TripIdUtils() {
    }

    public static String normalizeSimple(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        while (true) {
            String lower = s.toLowerCase();
            if (lower.startsWith("agency:")) {
                s = s.substring("agency:".length()).trim();
                continue;
            }
            if (lower.startsWith("trip:")) {
                s = s.substring("trip:".length()).trim();
                continue;
            }
            int colon = s.indexOf(':');
            if (colon > 0 && colon < 6) {
                s = s.substring(colon + 1).trim();
                continue;
            }
            break;
        }

        while (s.matches("^\\d+#.*")) {
            s = s.replaceFirst("^\\d+#", "").trim();
        }

        s = s.trim();
        s = s.replaceAll("[^A-Za-z0-9_\\-\\.]", "");
        s = s.replaceAll("^[\\-_.]+", "");
        s = s.replaceAll("[\\-_.]+$", "");
        s = s.toLowerCase();

        return s.isEmpty() ? null : s;
    }

    public static Set<String> generateVariants(String rawTripId) {
        Set<String> out = new HashSet<>();
        if (rawTripId == null) return out;

        String norm = normalizeSimple(rawTripId);

        if (norm == null) {
            String fallback = rawTripId.trim().toLowerCase();
            if (!fallback.isEmpty()) {
                out.add(fallback);
            }
            return out;
        }

        out.add(norm);

        String noSep = norm.replaceAll("[-_\\.]", "");
        if (!noSep.isEmpty()) out.add(noSep);

        if (norm.contains("-")) {
            out.add(norm.replace('-', '_'));
        }

        if (norm.contains("_")) {
            out.add(norm.replace('_', '-'));
        }

        if (norm.contains(".")) {
            out.add(norm.replace('.', '-'));
            out.add(norm.replace('.', '_'));
            out.add(norm.replace(".", ""));
        }

        out.removeIf(Objects::isNull);
        out.removeIf(String::isEmpty);

        return out;
    }

    public static String normalizeOrEmpty(String raw) {
        String n = normalizeSimple(raw);
        return n == null ? "" : n;
    }
}

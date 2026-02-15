package damose.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import damose.data.mapper.TripIdUtils;

/**
 * Utility methods for arrival matching and id normalization.
 */
final class ArrivalMatchingUtils {

    private ArrivalMatchingUtils() {
    }

    static String normalizeTripKey(String rawTripId) {
        if (rawTripId == null) return null;
        String simple = TripIdUtils.normalizeSimple(rawTripId);
        if (simple != null) {
            simple = simple.trim();
        }
        return simple;
    }

    static Set<String> generateStopIdVariants(String rawStopId) {
        Set<String> out = new HashSet<>();
        if (rawStopId == null) return out;

        String trimmed = rawStopId.trim();
        if (trimmed.isEmpty()) return out;

        out.add(trimmed);

        String normalized = normalizeStopIdForMatch(trimmed);
        if (normalized != null && !normalized.isEmpty()) {
            out.add(normalized);
        }

        String digitsOnly = trimmed.replaceAll("\\D", "");
        if (digitsOnly.length() >= 4) {
            out.add(digitsOnly);
        }

        if (normalized != null) {
            String normDigits = normalized.replaceAll("\\D", "");
            if (normDigits.length() >= 4) {
                out.add(normDigits);
            }
        }

        return out;
    }

    static String normalizeStopIdForMatch(String rawStopId) {
        if (rawStopId == null) return null;
        String s = rawStopId.trim();
        if (s.isEmpty()) return null;

        while (true) {
            String lower = s.toLowerCase();
            if (lower.startsWith("stop:")) {
                s = s.substring("stop:".length()).trim();
                continue;
            }
            int colon = s.indexOf(':');
            if (colon > 0 && colon < 6) {
                s = s.substring(colon + 1).trim();
                continue;
            }
            break;
        }

        s = s.replaceFirst("^\\d+#", "");
        s = s.replaceFirst("[_:]\\d+$", "");
        return s.trim();
    }

    static Set<String> generateRouteIdVariants(String rawRouteId) {
        Set<String> out = new HashSet<>();
        if (rawRouteId == null) return out;

        String trimmed = rawRouteId.trim();
        if (trimmed.isEmpty()) return out;
        out.add(trimmed);

        String upper = trimmed.toUpperCase();
        out.add(upper);

        String lower = trimmed.toLowerCase();
        if (lower.startsWith("route:")) {
            String bare = trimmed.substring("route:".length()).trim();
            if (!bare.isEmpty()) {
                out.add(bare);
                out.add(bare.toUpperCase());
            }
        }

        return out;
    }

    static long computeScheduledEpochForFeed(LocalTime scheduled, long feedEpochSeconds) {
        if (scheduled == null) return -1;
        ZoneId zone = ZoneId.systemDefault();
        Instant feedInstant = Instant.ofEpochSecond(feedEpochSeconds);
        LocalDate feedDate = feedInstant.atZone(zone).toLocalDate();

        long best = -1;
        long bestDiff = Long.MAX_VALUE;
        for (int delta = -1; delta <= 1; delta++) {
            LocalDate candidateDate = feedDate.plusDays(delta);
            Instant candInstant = scheduled.atDate(candidateDate).atZone(zone).toInstant();
            long diff = Math.abs(candInstant.getEpochSecond() - feedEpochSeconds);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = candInstant.getEpochSecond();
            }
        }
        return best;
    }
}

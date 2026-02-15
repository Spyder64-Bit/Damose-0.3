package damose.service;

import damose.config.AppConstants;

/**
 * Formatter for arrival/trip rows shown in panels.
 */
final class ArrivalFormattingSupport {

    private ArrivalFormattingSupport() {
    }

    static String formatTripInfo(TripArrivalInfo info) {
        String timeStr = String.format("%02d:%02d", info.arrivalTime.getHour(), info.arrivalTime.getMinute());
        String headsign = (info.headsign != null && !info.headsign.isEmpty()) ? info.headsign : "";

        if (headsign.length() > 30) {
            headsign = headsign.substring(0, 27) + "...";
        }

        if (info.predictedEpoch != null) {
            long delayMin = (info.predictedEpoch - info.scheduledEpoch) / 60;
            String rtStatus;
            if (delayMin > 1) {
                rtStatus = "+" + delayMin + " min";
            } else if (delayMin < -1) {
                rtStatus = "-" + Math.abs(delayMin) + " min";
            } else {
                rtStatus = "OK";
            }
            return timeStr + " | " + info.routeId + " " + headsign + " [" + rtStatus + "]";
        }
        return timeStr + " | " + info.routeId + " " + headsign;
    }

    static String formatArrivalInfo(RouteArrivalInfo info, long nowEpochSeconds) {
        if (info.predictedEpoch != null) {
            long diffFromNowMin = Math.max(0, (info.predictedEpoch - nowEpochSeconds) / 60);
            long delayMin = (info.predictedEpoch - info.scheduledEpoch) / 60;
            String dotPrefix;
            if (delayMin > 1) {
                dotPrefix = "[DOT_RED] ";
            } else if (delayMin < -1) {
                dotPrefix = "[DOT_GREEN] ";
            } else {
                dotPrefix = "[DOT_GRAY] ";
            }

            String etaText;
            if (diffFromNowMin <= AppConstants.IN_ARRIVO_THRESHOLD_MIN) {
                etaText = "In arrivo";
            } else {
                etaText = diffFromNowMin + " min";
            }
            return dotPrefix + info.routeId + " - " + etaText + " - " + formatRealtimeOffsetText(delayMin);
        }

        long diffStaticMin = Math.max(0, (info.scheduledEpoch - nowEpochSeconds) / 60);
        if (diffStaticMin <= AppConstants.IN_ARRIVO_THRESHOLD_MIN) {
            return "[DOT_GRAY] " + info.routeId + " - In arrivo - orario programmato";
        }
        return "[DOT_GRAY] " + info.routeId + " - " + diffStaticMin + " min - orario programmato";
    }

    private static String formatRealtimeOffsetText(long delayMin) {
        if (delayMin > 1) {
            return "ritardo " + delayMin + " min";
        }
        if (delayMin < -1) {
            return "anticipo " + Math.abs(delayMin) + " min";
        }
        return "in orario";
    }
}

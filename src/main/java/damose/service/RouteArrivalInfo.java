package damose.service;

/**
 * Best arrival candidate per route.
 */
final class RouteArrivalInfo {
    final String routeId;
    final long scheduledEpoch;
    final Long predictedEpoch;

    RouteArrivalInfo(String routeId, long scheduledEpoch, Long predictedEpoch) {
        this.routeId = routeId;
        this.scheduledEpoch = scheduledEpoch;
        this.predictedEpoch = predictedEpoch;
    }

    long sortKey() {
        if (predictedEpoch == null) {
            return scheduledEpoch;
        }
        return predictedEpoch;
    }
}

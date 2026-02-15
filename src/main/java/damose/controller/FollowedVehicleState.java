package damose.controller;

/**
 * Mutable state for currently followed vehicle marker.
 */
final class FollowedVehicleState {

    private volatile String markerId;
    private volatile int missCount;

    void follow(String markerId) {
        this.markerId = markerId;
        this.missCount = 0;
    }

    void clear() {
        markerId = null;
        missCount = 0;
    }

    String markerId() {
        return markerId;
    }

    void resetMisses() {
        missCount = 0;
    }

    boolean incrementMissAndReached(int threshold) {
        missCount++;
        return missCount >= threshold;
    }
}

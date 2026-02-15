package damose.view.component;

/**
 * Circular hit area model for pointer interaction.
 */
final class CircularHitArea<T> {

    private final T payload;
    private final int centerX;
    private final int centerY;
    private final int radius;

    CircularHitArea(T payload, int centerX, int centerY, int radius) {
        this.payload = payload;
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = Math.max(8, radius);
    }

    T payload() {
        return payload;
    }

    boolean contains(int x, int y) {
        return distance2(x, y) <= radius * radius;
    }

    double distance2(int x, int y) {
        int dx = x - centerX;
        int dy = y - centerY;
        return (dx * dx) + (dy * dy);
    }
}

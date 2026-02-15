package damose.controller;

import java.util.Collections;
import java.util.List;

import damose.model.Stop;

/**
 * Mutable state container for currently selected route panel context.
 */
final class RoutePanelState {

    private volatile String routeId;
    private volatile List<Stop> routeStops = Collections.emptyList();
    private volatile Integer direction;
    private volatile String routeName;
    private volatile boolean circular;

    void apply(String routeId, String routeName, List<Stop> routeStops, Integer direction, boolean circular) {
        this.routeId = routeId;
        this.routeName = routeName;
        this.routeStops = routeStops == null ? Collections.emptyList() : List.copyOf(routeStops);
        this.direction = direction;
        this.circular = circular;
    }

    void reset() {
        routeId = null;
        routeStops = Collections.emptyList();
        direction = null;
        routeName = null;
        circular = false;
    }

    String routeId() {
        return routeId;
    }

    List<Stop> routeStops() {
        return routeStops;
    }

    Integer direction() {
        return direction;
    }

    String routeName() {
        return routeName;
    }

    boolean isCircular() {
        return circular;
    }

    boolean hasRoute() {
        return routeId != null;
    }
}

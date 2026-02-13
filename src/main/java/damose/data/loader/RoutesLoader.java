package damose.data.loader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import damose.model.Route;

public final class RoutesLoader {
    
    private static final String ROUTES_PATH = "/gtfs_static/routes.txt";
    private static Map<String, Route> routesById = new HashMap<>();
    
    private RoutesLoader() {
    }
    
    public static List<Route> load() {
        return load(ROUTES_PATH);
    }
    
    public static List<Route> load(String resourcePath) {
        List<Route> routes = new ArrayList<>();
        routesById.clear();
        
        try (InputStream in = RoutesLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                System.err.println("RoutesLoader: resource not found: " + resourcePath);
                return routes;
            }
            
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String header = br.readLine();
                if (header == null) return routes;
                
                String line;
                while ((line = br.readLine()) != null) {
                    Route route = parseLine(line);
                    if (route != null) {
                        routes.add(route);
                        routesById.put(route.getRouteId(), route);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading routes.txt: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("Routes loaded: " + routes.size());
        return routes;
    }
    
    private static Route parseLine(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        
        List<String> fields = CsvParser.parseLine(line);
        if (fields.size() < 5) return null;
        
        try {
            String routeId = safeGet(fields, 0).trim();
            String agencyId = safeGet(fields, 1).trim();
            String shortName = safeGet(fields, 2).trim();
            String longName = safeGet(fields, 3).trim();
            int routeType = parseRouteType(safeGet(fields, 4).trim());
            String routeColor = safeGet(fields, 6).trim();
            String textColor = safeGet(fields, 7).trim();
            
            return new Route(routeId, agencyId, shortName, longName, routeType, routeColor, textColor);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static int parseRouteType(String s) {
        if (s == null || s.isEmpty()) return 3; // Nota in italiano
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 3;
        }
    }
    
    private static String safeGet(List<String> list, int idx) {
        return idx < list.size() ? list.get(idx) : "";
    }
    
    public static Route getRouteById(String routeId) {
        return routesById.get(routeId);
    }
    
    public static Map<String, Route> getRoutesById() {
        return routesById;
    }
}












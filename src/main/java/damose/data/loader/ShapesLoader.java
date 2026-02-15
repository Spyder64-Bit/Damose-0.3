package damose.data.loader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jxmapviewer.viewer.GeoPosition;

import damose.config.AppConstants;

/**
 * Static data loader for shapes loader.
 */
public final class ShapesLoader {

    private ShapesLoader() {
    }

    /**
     * Returns the result of load.
     */
    public static Map<String, List<GeoPosition>> load() {
        return load(AppConstants.GTFS_SHAPES_PATH);
    }

    /**
     * Returns the result of load.
     */
    public static Map<String, List<GeoPosition>> load(String resourcePath) {
        Map<String, List<ShapeRow>> rowsByShapeId = new HashMap<>();

        try (InputStream in = ShapesLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                System.err.println("ShapesLoader: resource not found: " + resourcePath);
                return Collections.emptyMap();
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line = br.readLine();
                if (line == null) return Collections.emptyMap();

                boolean headerConsumed = line.toLowerCase().contains("shape_id");
                if (!headerConsumed) {
                    processLine(line, rowsByShapeId);
                }

                while ((line = br.readLine()) != null) {
                    processLine(line, rowsByShapeId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading shapes.txt: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyMap();
        }

        Map<String, List<GeoPosition>> shapesById = new HashMap<>(rowsByShapeId.size());
        for (Map.Entry<String, List<ShapeRow>> entry : rowsByShapeId.entrySet()) {
            List<ShapeRow> rows = entry.getValue();
            rows.sort(Comparator.comparingInt(ShapeRow::sequence));

            List<GeoPosition> points = new ArrayList<>(rows.size());
            GeoPosition prev = null;
            for (ShapeRow row : rows) {
                GeoPosition current = new GeoPosition(row.lat(), row.lon());
                if (prev == null
                        || Math.abs(prev.getLatitude() - current.getLatitude()) > 0.0000001
                        || Math.abs(prev.getLongitude() - current.getLongitude()) > 0.0000001) {
                    points.add(current);
                    prev = current;
                }
            }
            if (points.size() >= 2) {
                shapesById.put(entry.getKey(), points);
            }
        }

        System.out.println("Shapes loaded: " + shapesById.size());
        return shapesById;
    }

    private static void processLine(String line, Map<String, List<ShapeRow>> rowsByShapeId) {
        if (line == null || line.trim().isEmpty()) return;

        List<String> fields = CsvParser.parseLine(line);
        if (fields.size() < 4) return;

        String shapeId = safeGet(fields, 0).trim();
        if (shapeId.isEmpty()) return;

        double lat = parseDouble(safeGet(fields, 1));
        double lon = parseDouble(safeGet(fields, 2));
        int seq = parseInt(safeGet(fields, 3));

        rowsByShapeId.computeIfAbsent(shapeId, k -> new ArrayList<>())
                .add(new ShapeRow(lat, lon, seq));
    }

    private static String safeGet(List<String> fields, int idx) {
        return idx < fields.size() ? fields.get(idx) : "";
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private record ShapeRow(double lat, double lon, int sequence) {
    }
}

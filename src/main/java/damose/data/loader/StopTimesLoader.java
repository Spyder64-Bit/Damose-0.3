package damose.data.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import damose.config.AppConstants;
import damose.model.StopTime;

/**
 * Static data loader for stop times loader.
 */
public final class StopTimesLoader {

    private StopTimesLoader() {
    }

    /**
     * Returns the result of load.
     */
    public static List<StopTime> load() {
        return load(AppConstants.GTFS_STOP_TIMES_PATH);
    }

    /**
     * Returns the result of load.
     */
    public static List<StopTime> load(String resourcePath) {
        List<StopTime> result = new ArrayList<>();

        try (InputStream in = StopTimesLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                System.err.println("StopTimesLoader: resource not found: " + resourcePath);
                return result;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                boolean firstLine = true;

                while ((line = br.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }

                    List<String> parts = parseCsvLineFast(line);
                    if (parts.size() < 5) continue;

                    String tripId = safeGet(parts, 0).trim().intern();
                    LocalTime arrival = parseTime(safeGet(parts, 1).trim());
                    LocalTime departure = parseTime(safeGet(parts, 2).trim());
                    String stopId = safeGet(parts, 3).trim().intern();
                    int stopSequence = parseInt(safeGet(parts, 4).trim());
                    String stopHeadsign = safeGet(parts, 5).trim();
                    if (!stopHeadsign.isEmpty()) stopHeadsign = stopHeadsign.intern();
                    int pickupType = parseInt(safeGet(parts, 6).trim());
                    int dropOffType = parseInt(safeGet(parts, 7).trim());
                    double shapeDistTraveled = parseDouble(safeGet(parts, 8).trim());
                    int timepoint = parseInt(safeGet(parts, 9).trim());

                    StopTime st = new StopTime(tripId, arrival, departure, stopId,
                            stopSequence, stopHeadsign, pickupType, dropOffType,
                            shapeDistTraveled, timepoint);

                    result.add(st);
                }

                System.out.println("StopTimes loaded: " + result.size());
            }
        } catch (IOException e) {
            System.err.println("Error loading stop_times: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    private static LocalTime parseTime(String s) {
        try {
            if (s == null || s.isEmpty()) return null;

            String[] parts = s.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int sec = Integer.parseInt(parts[2]);

            h = h % 24;

            return LocalTime.of(h, m, sec);
        } catch (Exception e) {
            return null;
        }
    }

    private static int parseInt(String s) {
        try {
            return (s == null || s.isEmpty()) ? 0 : Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static double parseDouble(String s) {
        try {
            return (s == null || s.isEmpty()) ? 0.0 : Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String safeGet(List<String> fields, int idx) {
        return idx < fields.size() ? fields.get(idx) : "";
    }

    private static List<String> parseCsvLineFast(String line) {
        if (line == null) return List.of();

        if (line.indexOf('"') < 0) {
            List<String> out = new ArrayList<>(12);
            int start = 0;
            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) == ',') {
                    out.add(line.substring(start, i));
                    start = i + 1;
                }
            }
            out.add(line.substring(start));
            return out;
        }

        return CsvParser.parseLine(line);
    }
}


package damose.service;

import java.util.ArrayList;
import java.util.List;

import org.jxmapviewer.viewer.GeoPosition;

import com.google.transit.realtime.GtfsRealtime;

import damose.data.mapper.StopTripMapper;
import damose.data.mapper.TripIdUtils;
import damose.model.TripUpdateRecord;
import damose.model.VehiclePosition;

public final class GtfsParser {

    private GtfsParser() {
    }

    public static List<TripUpdateRecord> parseTripUpdates(GtfsRealtime.FeedMessage feed,
                                                          StopTripMapper stopTripMapper,
                                                          Long feedHeaderTs) {
        List<TripUpdateRecord> updates = new ArrayList<>();
        if (feed == null) return updates;

        for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
            if (!entity.hasTripUpdate()) continue;

            GtfsRealtime.TripUpdate tu = entity.getTripUpdate();
            String rawTripId = (tu.hasTrip() && tu.getTrip().hasTripId()) 
                    ? tu.getTrip().getTripId() : null;
            String rawRouteId = (tu.hasTrip() && tu.getTrip().hasRouteId())
                    ? tu.getTrip().getRouteId() : null;
            String simple = TripIdUtils.normalizeSimple(rawTripId);

            for (GtfsRealtime.TripUpdate.StopTimeUpdate stu : tu.getStopTimeUpdateList()) {
                if (stu.hasScheduleRelationship()) {
                    GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship rel = 
                            stu.getScheduleRelationship();
                    if (rel == GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED
                        || rel == GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.NO_DATA) {
                        continue;
                    }
                }

                String stopId = stu.hasStopId() ? normalizeStopId(stu.getStopId()) : null;
                boolean hasUsableStopId = stopId != null && !stopId.isBlank();
                boolean isKnownStopId = hasUsableStopId
                        && stopTripMapper != null
                        && stopTripMapper.isKnownStopId(stopId);

                if ((!hasUsableStopId || !isKnownStopId)
                        && stu.hasStopSequence()
                        && stopTripMapper != null
                        && rawTripId != null) {
                    int seq = stu.getStopSequence();
                    String mapped = stopTripMapper.getStopIdByTripAndSequence(rawTripId, seq);
                    if (mapped == null || mapped.isBlank()) {
                        mapped = stopTripMapper.getStopIdByTripAndSequence(simple, seq);
                    }
                    if (mapped != null && !mapped.isBlank()) {
                        stopId = mapped;
                    }
                }

                long rawTime = -1;
                if (stu.hasArrival() && stu.getArrival().hasTime()) {
                    rawTime = stu.getArrival().getTime();
                } else if (stu.hasDeparture() && stu.getDeparture().hasTime()) {
                    rawTime = stu.getDeparture().getTime();
                }

                long arrivalEpoch = normalizeEpoch(rawTime);
                if (stopId != null && !stopId.isBlank() && arrivalEpoch > 0) {
                    updates.add(new TripUpdateRecord(rawTripId, rawRouteId, stopId, arrivalEpoch));
                }
            }
        }

        return updates;
    }

    public static List<VehiclePosition> parseVehiclePositions(GtfsRealtime.FeedMessage feed) {
        List<VehiclePosition> positions = new ArrayList<>();
        if (feed == null) return positions;

        for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
            if (!entity.hasVehicle()) continue;

            GtfsRealtime.VehiclePosition vehicle = entity.getVehicle();

            String tripId = (vehicle.hasTrip() && vehicle.getTrip().hasTripId()) 
                    ? vehicle.getTrip().getTripId() : null;
            String vehicleId = (vehicle.hasVehicle() && vehicle.getVehicle().hasId()) 
                    ? vehicle.getVehicle().getId() : null;

            if (!vehicle.hasPosition()) {
                continue;
            }

            double lat = vehicle.getPosition().getLatitude();
            double lon = vehicle.getPosition().getLongitude();
            int stopSeq = vehicle.hasCurrentStopSequence() ? vehicle.getCurrentStopSequence() : -1;

            if (!Double.isFinite(lat) || !Double.isFinite(lon)) {
                continue;
            }

            if (Math.abs(lat) > 90 || Math.abs(lon) > 180) {
                double latC = lat / 1_000_000.0;
                double lonC = lon / 1_000_000.0;
                if (Math.abs(latC) <= 90 && Math.abs(lonC) <= 180) {
                    lat = latC;
                    lon = lonC;
                }
            }

            if (Math.abs(lat) > 90 || Math.abs(lon) > 180) {
                continue;
            }

            if (lat == 0.0 && lon == 0.0) {
                continue;
            }

            positions.add(new VehiclePosition(
                tripId,
                vehicleId,
                new GeoPosition(lat, lon),
                stopSeq
            ));
        }

        return positions;
    }

    private static String normalizeStopId(String stopId) {
        if (stopId == null) return null;
        String normalized = stopId.trim();

        while (true) {
            String lower = normalized.toLowerCase();
            if (lower.startsWith("stop:")) {
                normalized = normalized.substring("stop:".length()).trim();
                continue;
            }
            int colon = normalized.indexOf(':');
            if (colon > 0 && colon < 6) {
                normalized = normalized.substring(colon + 1).trim();
                continue;
            }
            break;
        }

        normalized = normalized.replaceFirst("^\\d+#", "");

        normalized = normalized.replaceFirst("[_:]\\d+$", "");
        return normalized;
    }

    private static long normalizeEpoch(long raw) {
        if (raw <= 0) return -1;

        if (raw >= 1_000_000_000_000L) {
            return raw / 1000L;
        }

        if (raw >= 1_000_000_000L) {
            return raw;
        }

        return -1;
    }
}


package com.trackpro.alert.rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackpro.alert.AlertSeverity;
import com.trackpro.alert.AlertType;
import com.trackpro.alert.dto.AlertEvent;
import com.trackpro.model.DeviceEntity;
import com.trackpro.model.GeofenceEntity;
import com.trackpro.telemetry.DeviceFrame;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Checks every active geofence for the vehicle's org. Fires ENTER when transitioning
 * from outside→inside, EXIT when inside→outside. Uses JTS for polygons and Haversine for circles.
 */
@Component
public class GeofenceRule implements AlertRule {

    private static final Logger log = LoggerFactory.getLogger(GeofenceRule.class);
    private static final GeometryFactory GEO_FACTORY = new GeometryFactory();
    private static final double EARTH_RADIUS_M = 6_371_000;

    private final ObjectMapper objectMapper;

    public GeofenceRule(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AlertType getType() {
        return AlertType.GEOFENCE_ENTER;
    }

    @Override
    public List<AlertEvent> evaluate(DeviceFrame current, DeviceFrame previous, DeviceEntity device, AlertRuleContext context) {
        List<AlertEvent> events = new ArrayList<>();
        if (current == null || current.latitude() == null || current.longitude() == null) return events;
        if (device.getOrganisation() == null) return events;

        UUID orgId = device.getOrganisation().getId();
        UUID deviceId = device.getId();
        Instant now = current.eventTime() != null ? current.eventTime() : Instant.now();

        for (GeofenceEntity geofence : context.activeGeofencesForOrg(orgId)) {
            boolean currentlyInside = isInside(current.latitude(), current.longitude(), geofence);
            Optional<Boolean> previouslyInside = context.cache().getGeofenceInside(geofence.getId(), deviceId);

            if (previouslyInside.isPresent()) {
                boolean wasInside = previouslyInside.get();
                if (!wasInside && currentlyInside) {
                    events.add(buildEvent(current, device, geofence, AlertType.GEOFENCE_ENTER, now));
                } else if (wasInside && !currentlyInside) {
                    events.add(buildEvent(current, device, geofence, AlertType.GEOFENCE_EXIT, now));
                }
            }
            context.cache().setGeofenceInside(geofence.getId(), deviceId, currentlyInside);
        }
        return events;
    }

    private boolean isInside(double lat, double lon, GeofenceEntity geofence) {
        if (geofence.getGeofenceType() == GeofenceEntity.GeofenceType.CIRCLE) {
            return haversineM(lat, lon, geofence.getCenterLat(), geofence.getCenterLng()) <= geofence.getRadiusM();
        }
        return polygonContains(lat, lon, geofence.getGeometryJson());
    }

    private boolean polygonContains(double lat, double lon, String geometryJson) {
        try {
            JsonNode node = objectMapper.readTree(geometryJson);
            JsonNode coords = node.get("coordinates");
            if (coords == null || !coords.isArray()) return false;

            Coordinate[] ring = new Coordinate[coords.size() + 1];
            for (int i = 0; i < coords.size(); i++) {
                JsonNode pt = coords.get(i);
                ring[i] = new Coordinate(pt.get(0).asDouble(), pt.get(1).asDouble());
            }
            ring[coords.size()] = ring[0]; // close ring
            Polygon polygon = GEO_FACTORY.createPolygon(ring);
            Point point = GEO_FACTORY.createPoint(new Coordinate(lon, lat));
            return polygon.contains(point);
        } catch (JsonProcessingException e) {
            log.warn("Invalid geofence geometry: {}", e.getMessage());
            return false;
        }
    }

    private double haversineM(double lat1, double lon1, Double lat2, Double lon2) {
        if (lat2 == null || lon2 == null) return Double.MAX_VALUE;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    private AlertEvent buildEvent(DeviceFrame current, DeviceEntity device, GeofenceEntity geofence,
                                   AlertType type, Instant now) {
        AlertSeverity severity = geofence.getSeverity() == GeofenceEntity.GeofenceSeverity.HIGH
                ? AlertSeverity.HIGH
                : geofence.getSeverity() == GeofenceEntity.GeofenceSeverity.MEDIUM
                ? AlertSeverity.MEDIUM
                : AlertSeverity.LOW;
        return AlertEvent.builder()
                .deviceId(device.getId())
                .imei(device.getImei())
                .orgId(device.getOrganisation().getId())
                .alertType(type)
                .severity(severity)
                .message(String.format("%s geofence '%s'", type == AlertType.GEOFENCE_ENTER ? "Entered" : "Exited", geofence.getName()))
                .alertTime(now)
                .latitude(current.latitude())
                .longitude(current.longitude())
                .relatedGeofenceId(geofence.getId())
                .relatedGeofenceName(geofence.getName())
                .build();
    }
}

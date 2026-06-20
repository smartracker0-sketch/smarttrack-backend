package com.trackpro.alert.dto;

import com.trackpro.alert.AlertSeverity;
import com.trackpro.alert.AlertType;
import java.time.Instant;
import java.util.UUID;

/**
 * Normalised alert event produced by the alert rule engine.
 * This is what gets saved to PostgreSQL, published to MQTT,
 * and broadcast over WebSocket STOMP.
 */
public record AlertEvent(
        UUID id,
        UUID deviceId,
        String imei,
        UUID orgId,
        AlertType alertType,
        AlertSeverity severity,
        String message,
        Instant alertTime,
        Double latitude,
        Double longitude,
        Double speedKph,
        Long durationSeconds,
        UUID relatedGeofenceId,
        String relatedGeofenceName,
        Long odometerM
) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID id;
        private UUID deviceId;
        private String imei;
        private UUID orgId;
        private AlertType alertType;
        private AlertSeverity severity;
        private String message;
        private Instant alertTime;
        private Double latitude;
        private Double longitude;
        private Double speedKph;
        private Long durationSeconds;
        private UUID relatedGeofenceId;
        private String relatedGeofenceName;
        private Long odometerM;

        public Builder id(UUID v)                          { this.id = v; return this; }
        public Builder deviceId(UUID v)                  { this.deviceId = v; return this; }
        public Builder imei(String v)                    { this.imei = v; return this; }
        public Builder orgId(UUID v)                     { this.orgId = v; return this; }
        public Builder alertType(AlertType v)            { this.alertType = v; return this; }
        public Builder severity(AlertSeverity v)         { this.severity = v; return this; }
        public Builder message(String v)                 { this.message = v; return this; }
        public Builder alertTime(Instant v)              { this.alertTime = v; return this; }
        public Builder latitude(Double v)                { this.latitude = v; return this; }
        public Builder longitude(Double v)               { this.longitude = v; return this; }
        public Builder speedKph(Double v)                { this.speedKph = v; return this; }
        public Builder durationSeconds(Long v)           { this.durationSeconds = v; return this; }
        public Builder relatedGeofenceId(UUID v)         { this.relatedGeofenceId = v; return this; }
        public Builder relatedGeofenceName(String v)     { this.relatedGeofenceName = v; return this; }
        public Builder odometerM(Long v)                 { this.odometerM = v; return this; }

        public AlertEvent build() {
            return new AlertEvent(id, deviceId, imei, orgId, alertType, severity,
                    message, alertTime, latitude, longitude, speedKph,
                    durationSeconds, relatedGeofenceId, relatedGeofenceName, odometerM);
        }
    }
}

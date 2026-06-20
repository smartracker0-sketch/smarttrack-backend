package com.trackpro.model;

import com.trackpro.alert.AlertSeverity;
import com.trackpro.alert.AlertType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "device_alerts")
public class DeviceAlert {

    @Id @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceEntity device;

    @Column(nullable = false)
    private Instant alertTime;

    @Column(nullable = false)
    private Instant receivedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AlertSeverity severity = AlertSeverity.MEDIUM;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private boolean acknowledged = false;

    private Instant ackAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ack_by")
    private UserEntity ackBy;

    private Double latitude;
    private Double longitude;
    private Double speedKph;

    private Long durationSeconds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_geofence_id")
    private GeofenceEntity relatedGeofence;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    public UUID getId() { return id; }
    public DeviceEntity getDevice() { return device; }
    public void setDevice(DeviceEntity device) { this.device = device; }
    public Instant getAlertTime() { return alertTime; }
    public void setAlertTime(Instant alertTime) { this.alertTime = alertTime; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }
    public AlertSeverity getSeverity() { return severity; }
    public void setSeverity(AlertSeverity severity) { this.severity = severity; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
    public Instant getAckAt() { return ackAt; }
    public void setAckAt(Instant ackAt) { this.ackAt = ackAt; }
    public UserEntity getAckBy() { return ackBy; }
    public void setAckBy(UserEntity ackBy) { this.ackBy = ackBy; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getSpeedKph() { return speedKph; }
    public void setSpeedKph(Double speedKph) { this.speedKph = speedKph; }
    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
    public GeofenceEntity getRelatedGeofence() { return relatedGeofence; }
    public void setRelatedGeofence(GeofenceEntity relatedGeofence) { this.relatedGeofence = relatedGeofence; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}

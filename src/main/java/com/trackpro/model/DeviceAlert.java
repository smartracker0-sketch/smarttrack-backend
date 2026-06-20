package com.trackpro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(nullable = false, length = 64)
    private String alertType;

    @Column(nullable = false, length = 32)
    private String severity = "WARNING";

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

    public UUID getId() { return id; }
    public DeviceEntity getDevice() { return device; }
    public void setDevice(DeviceEntity device) { this.device = device; }
    public Instant getAlertTime() { return alertTime; }
    public void setAlertTime(Instant alertTime) { this.alertTime = alertTime; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
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
}

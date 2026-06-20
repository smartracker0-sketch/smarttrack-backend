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
@Table(name = "telemetry_events")
public class TelemetryEvent {

    @Id @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceEntity device;

    @Column(nullable = false)
    private Instant eventTime;

    @Column(nullable = false)
    private Instant receivedAt = Instant.now();

    private Double latitude;
    private Double longitude;
    @Column(name = "altitude_m")
    private Double altitudeM;
    @Column(name = "speed_kph")
    private Double speedKph;
    @Column(name = "heading_deg")
    private Double headingDeg;
    @Column(name = "accuracy_m")
    private Double accuracyM;
    private Integer satellites;
    private Boolean ignition;
    @Column(name = "voltage_mv")
    private Integer voltageMv;
    @Column(name = "gsm_signal")
    private Integer gsmSignal;
    @Column(name = "odometer_m")
    private Long odometerM;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    public UUID getId() { return id; }
    public DeviceEntity getDevice() { return device; }
    public void setDevice(DeviceEntity device) { this.device = device; }
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getAltitudeM() { return altitudeM; }
    public void setAltitudeM(Double altitudeM) { this.altitudeM = altitudeM; }
    public Double getSpeedKph() { return speedKph; }
    public void setSpeedKph(Double speedKph) { this.speedKph = speedKph; }
    public Double getHeadingDeg() { return headingDeg; }
    public void setHeadingDeg(Double headingDeg) { this.headingDeg = headingDeg; }
    public Double getAccuracyM() { return accuracyM; }
    public void setAccuracyM(Double accuracyM) { this.accuracyM = accuracyM; }
    public Integer getSatellites() { return satellites; }
    public void setSatellites(Integer satellites) { this.satellites = satellites; }
    public Boolean getIgnition() { return ignition; }
    public void setIgnition(Boolean ignition) { this.ignition = ignition; }
    public Integer getVoltageMv() { return voltageMv; }
    public void setVoltageMv(Integer voltageMv) { this.voltageMv = voltageMv; }
    public Integer getGsmSignal() { return gsmSignal; }
    public void setGsmSignal(Integer gsmSignal) { this.gsmSignal = gsmSignal; }
    public Long getOdometerM() { return odometerM; }
    public void setOdometerM(Long odometerM) { this.odometerM = odometerM; }
    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }
}

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
@Table(name = "fuel_readings")
public class FuelReading {

    @Id @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceEntity device;

    @Column(nullable = false)
    private Instant eventTime;

    @Column(nullable = false)
    private Instant receivedAt = Instant.now();

    @Column(name = "fuel_level_pct")
    private Double fuelLevelPct;
    @Column(name = "fuel_liters")
    private Double fuelLiters;
    @Column(name = "temperature_c")
    private Double temperatureC;

    @Column(name = "tank_id", length = 32)
    private String tankId;

    public UUID getId() { return id; }
    public DeviceEntity getDevice() { return device; }
    public void setDevice(DeviceEntity device) { this.device = device; }
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    public Double getFuelLevelPct() { return fuelLevelPct; }
    public void setFuelLevelPct(Double fuelLevelPct) { this.fuelLevelPct = fuelLevelPct; }
    public Double getFuelLiters() { return fuelLiters; }
    public void setFuelLiters(Double fuelLiters) { this.fuelLiters = fuelLiters; }
    public Double getTemperatureC() { return temperatureC; }
    public void setTemperatureC(Double temperatureC) { this.temperatureC = temperatureC; }
    public String getTankId() { return tankId; }
    public void setTankId(String tankId) { this.tankId = tankId; }
}

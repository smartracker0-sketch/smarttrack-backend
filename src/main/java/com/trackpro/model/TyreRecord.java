package com.trackpro.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tyre_records")
public class TyreRecord {

    @Id @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private OrganisationEntity organisation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private DeviceEntity device;

    @Column(name = "vehicle_plate", length = 32)
    private String vehiclePlate;

    @Column(nullable = false, length = 50)
    private String position;

    @Column(length = 100)
    private String brand;

    @Column(name = "serial_no", length = 100)
    private String serialNo;

    @Column(nullable = false, length = 20)
    private String status = "GOOD";

    @Column(name = "pressure_psi", precision = 5, scale = 1)
    private BigDecimal pressurePsi;

    @Column(name = "installed_at")
    private LocalDate installedAt;

    @Column(name = "next_check_date")
    private LocalDate nextCheckDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public OrganisationEntity getOrganisation() { return organisation; }
    public void setOrganisation(OrganisationEntity organisation) { this.organisation = organisation; }
    public DeviceEntity getDevice() { return device; }
    public void setDevice(DeviceEntity device) { this.device = device; }
    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getPressurePsi() { return pressurePsi; }
    public void setPressurePsi(BigDecimal pressurePsi) { this.pressurePsi = pressurePsi; }
    public LocalDate getInstalledAt() { return installedAt; }
    public void setInstalledAt(LocalDate installedAt) { this.installedAt = installedAt; }
    public LocalDate getNextCheckDate() { return nextCheckDate; }
    public void setNextCheckDate(LocalDate nextCheckDate) { this.nextCheckDate = nextCheckDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public UserEntity getCreatedBy() { return createdBy; }
    public void setCreatedBy(UserEntity createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}

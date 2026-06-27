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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "devices")
public class DeviceEntity {
    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private UserEntity owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private OrganisationEntity organisation;

    @Column(nullable = false, unique = true, length = 32)
    private String imei;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 64)
    private String deviceType = "GPS Tracker";

    @Column(nullable = false, length = 32)
    private String firmware = "unknown";

    @Column(length = 64)
    private String simCard;

    @Column(length = 64)
    private String serialNo;

    @Column(length = 64)
    private String vehiclePlate;

    @Column(nullable = false, length = 32)
    private String status = "Unassigned";

    private Integer speedLimitKmh;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = 20)
    private String simNumber;

    @Column(length = 64)
    private String simApn;

    @Column(length = 64)
    private String manufacturer;

    @Column(nullable = false, length = 30)
    private String activationStatus = "UNACTIVATED";

    @Column(nullable = false)
    private int activationAttempts = 0;

    private Instant activationAttemptedAt;

    private Instant activationConfirmedAt;

    @Column(columnDefinition = "TEXT")
    private String lastSmsReply;

    @Column(nullable = false)
    private boolean serverConfigured = false;

    @Column(nullable = false)
    private boolean apnConfigured = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UserEntity getOwner() { return owner; }
    public void setOwner(UserEntity owner) { this.owner = owner; }

    public OrganisationEntity getOrganisation() { return organisation; }
    public void setOrganisation(OrganisationEntity organisation) { this.organisation = organisation; }

    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getFirmware() { return firmware; }
    public void setFirmware(String firmware) { this.firmware = firmware; }

    public String getSimCard() { return simCard; }
    public void setSimCard(String simCard) { this.simCard = simCard; }

    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }

    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getSpeedLimitKmh() { return speedLimitKmh; }
    public void setSpeedLimitKmh(Integer speedLimitKmh) { this.speedLimitKmh = speedLimitKmh; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getSimNumber() { return simNumber; }
    public void setSimNumber(String simNumber) { this.simNumber = simNumber; }

    public String getSimApn() { return simApn; }
    public void setSimApn(String simApn) { this.simApn = simApn; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getActivationStatus() { return activationStatus; }
    public void setActivationStatus(String activationStatus) { this.activationStatus = activationStatus; }

    public int getActivationAttempts() { return activationAttempts; }
    public void setActivationAttempts(int activationAttempts) { this.activationAttempts = activationAttempts; }

    public Instant getActivationAttemptedAt() { return activationAttemptedAt; }
    public void setActivationAttemptedAt(Instant activationAttemptedAt) { this.activationAttemptedAt = activationAttemptedAt; }

    public Instant getActivationConfirmedAt() { return activationConfirmedAt; }
    public void setActivationConfirmedAt(Instant activationConfirmedAt) { this.activationConfirmedAt = activationConfirmedAt; }

    public String getLastSmsReply() { return lastSmsReply; }
    public void setLastSmsReply(String lastSmsReply) { this.lastSmsReply = lastSmsReply; }

    public boolean isServerConfigured() { return serverConfigured; }
    public void setServerConfigured(boolean serverConfigured) { this.serverConfigured = serverConfigured; }

    public boolean isApnConfigured() { return apnConfigured; }
    public void setApnConfigured(boolean apnConfigured) { this.apnConfigured = apnConfigured; }

    public Instant getCreatedAt() { return createdAt; }
}

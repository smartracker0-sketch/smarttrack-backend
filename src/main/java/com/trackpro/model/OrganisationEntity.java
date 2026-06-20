package com.trackpro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "organisations")
public class OrganisationEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 32)
    private String plan = "Starter";

    @Column(nullable = false, length = 32)
    private String status = "Active";

    @Column(nullable = false, length = 320)
    private String adminEmail;

    @Column(nullable = false)
    private int vehicleLimit = 10;

    private Integer defaultSpeedLimitKmh;
    private Integer idleThresholdMinutes;
    private Integer idleEscalationMinutes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }

    public int getVehicleLimit() { return vehicleLimit; }
    public void setVehicleLimit(int vehicleLimit) { this.vehicleLimit = vehicleLimit; }

    public Integer getDefaultSpeedLimitKmh() { return defaultSpeedLimitKmh; }
    public void setDefaultSpeedLimitKmh(Integer defaultSpeedLimitKmh) { this.defaultSpeedLimitKmh = defaultSpeedLimitKmh; }

    public Integer getIdleThresholdMinutes() { return idleThresholdMinutes; }
    public void setIdleThresholdMinutes(Integer idleThresholdMinutes) { this.idleThresholdMinutes = idleThresholdMinutes; }

    public Integer getIdleEscalationMinutes() { return idleEscalationMinutes; }
    public void setIdleEscalationMinutes(Integer idleEscalationMinutes) { this.idleEscalationMinutes = idleEscalationMinutes; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

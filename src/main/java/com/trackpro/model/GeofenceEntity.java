package com.trackpro.model;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "geofences")
public class GeofenceEntity {

    @Id @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private OrganisationEntity organisation;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GeofenceType geofenceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GeofenceSeverity severity = GeofenceSeverity.LOW;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String geometryJson;

    private Double centerLat;
    private Double centerLng;
    private Double radiusM;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public enum GeofenceType { CIRCLE, POLYGON }
    public enum GeofenceSeverity { LOW, MEDIUM, HIGH }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public OrganisationEntity getOrganisation() { return organisation; }
    public void setOrganisation(OrganisationEntity organisation) { this.organisation = organisation; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public GeofenceType getGeofenceType() { return geofenceType; }
    public void setGeofenceType(GeofenceType geofenceType) { this.geofenceType = geofenceType; }
    public GeofenceSeverity getSeverity() { return severity; }
    public void setSeverity(GeofenceSeverity severity) { this.severity = severity; }
    public String getGeometryJson() { return geometryJson; }
    public void setGeometryJson(String geometryJson) { this.geometryJson = geometryJson; }
    public Double getCenterLat() { return centerLat; }
    public void setCenterLat(Double centerLat) { this.centerLat = centerLat; }
    public Double getCenterLng() { return centerLng; }
    public void setCenterLng(Double centerLng) { this.centerLng = centerLng; }
    public Double getRadiusM() { return radiusM; }
    public void setRadiusM(Double radiusM) { this.radiusM = radiusM; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
}

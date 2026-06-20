package com.trackpro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String displayName;

    @Column(length = 1024)
    private String avatarUrl;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "score_total", precision = 5, scale = 2)
    private java.math.BigDecimal scoreTotal = java.math.BigDecimal.valueOf(100.00);

    @Column(name = "score_band", length = 20)
    private String scoreBand = "EXCELLENT";

    @Column(name = "total_trips_scored")
    private Integer totalTripsScored = 0;

    @Column(name = "last_scored_at")
    private Instant lastScoredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private com.trackpro.model.OrganisationEntity organisation;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new LinkedHashSet<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public java.math.BigDecimal getScoreTotal() { return scoreTotal; }
    public void setScoreTotal(java.math.BigDecimal scoreTotal) { this.scoreTotal = scoreTotal; }
    public String getScoreBand() { return scoreBand; }
    public void setScoreBand(String scoreBand) { this.scoreBand = scoreBand; }
    public Integer getTotalTripsScored() { return totalTripsScored; }
    public void setTotalTripsScored(Integer totalTripsScored) { this.totalTripsScored = totalTripsScored; }
    public Instant getLastScoredAt() { return lastScoredAt; }
    public void setLastScoredAt(Instant lastScoredAt) { this.lastScoredAt = lastScoredAt; }

    public Set<RoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleEntity> roles) {
        this.roles = roles;
    }

    public com.trackpro.model.OrganisationEntity getOrganisation() {
        return organisation;
    }

    public void setOrganisation(com.trackpro.model.OrganisationEntity organisation) {
        this.organisation = organisation;
    }
}

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
@Table(name = "driver_scores")
public class DriverScore {

    @Id @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private UserEntity driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id")
    private OrganisationEntity organisation;

    @Column(nullable = false)
    private int score;

    @Column(columnDefinition = "TEXT")
    private String breakdown;

    @Column(name = "clean_trip_bonus", nullable = false)
    private boolean cleanTripBonus = false;

    @Column(name = "streak_bonus", nullable = false)
    private boolean streakBonus = false;

    @Column(name = "adjusted_score")
    private Integer adjustedScore;

    @Column(name = "adjustment_reason")
    private String adjustmentReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public TripEntity getTrip() { return trip; }
    public void setTrip(TripEntity trip) { this.trip = trip; }
    public UserEntity getDriver() { return driver; }
    public void setDriver(UserEntity driver) { this.driver = driver; }
    public OrganisationEntity getOrganisation() { return organisation; }
    public void setOrganisation(OrganisationEntity organisation) { this.organisation = organisation; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public String getBreakdown() { return breakdown; }
    public void setBreakdown(String breakdown) { this.breakdown = breakdown; }
    public boolean isCleanTripBonus() { return cleanTripBonus; }
    public void setCleanTripBonus(boolean cleanTripBonus) { this.cleanTripBonus = cleanTripBonus; }
    public boolean isStreakBonus() { return streakBonus; }
    public void setStreakBonus(boolean streakBonus) { this.streakBonus = streakBonus; }
    public Integer getAdjustedScore() { return adjustedScore; }
    public void setAdjustedScore(Integer adjustedScore) { this.adjustedScore = adjustedScore; }
    public String getAdjustmentReason() { return adjustmentReason; }
    public void setAdjustmentReason(String adjustmentReason) { this.adjustmentReason = adjustmentReason; }
    public Instant getCreatedAt() { return createdAt; }
}

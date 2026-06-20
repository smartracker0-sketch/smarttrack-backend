package com.trackpro.scorecard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trackpro.scorecard")
public record ScorecardConfig(
        int startingScore,
        Deductions deductions,
        Bonuses bonuses,
        RollingAverage rollingAverage,
        NightDriving nightDriving,
        Bands bands
) {

    public record Deductions(
            int harshBraking,
            int harshAcceleration,
            int overspeedMedium,
            int overspeedHigh,
            int idleExceeded,
            int ignitionAnomaly,
            int restrictedGeofenceEntry,
            int nightDrivingPer10Min
    ) {}

    public record Bonuses(
            int cleanTrip,
            int smoothStreakPerTrip,
            int smoothStreakMinTrips
    ) {}

    public record RollingAverage(
            int tripWindow,
            double decayFactor
    ) {}

    public record NightDriving(
            boolean enabled,
            int startHour,
            int endHour
    ) {}

    public record Bands(
            int excellent,
            int good,
            int fair,
            int poor
    ) {}

    public ScorecardConfig {
        if (deductions == null) {
            deductions = new Deductions(3, 3, 5, 10, 2, 15, 8, 1);
        }
        if (bonuses == null) {
            bonuses = new Bonuses(5, 2, 5);
        }
        if (rollingAverage == null) {
            rollingAverage = new RollingAverage(30, 0.95);
        }
        if (nightDriving == null) {
            nightDriving = new NightDriving(false, 23, 5);
        }
        if (bands == null) {
            bands = new Bands(90, 75, 60, 40);
        }
    }
}

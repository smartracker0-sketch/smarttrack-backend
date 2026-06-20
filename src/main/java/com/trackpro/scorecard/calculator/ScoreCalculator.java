package com.trackpro.scorecard.calculator;

import com.trackpro.alert.AlertSeverity;
import com.trackpro.alert.AlertType;
import com.trackpro.scorecard.config.ScorecardConfig;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

@Component
public class ScoreCalculator {

    private final ScorecardConfig config;

    public ScoreCalculator(ScorecardConfig config) {
        this.config = config;
    }

    public int deductionFor(AlertType type, AlertSeverity severity, boolean restrictedGeofence) {
        return switch (type) {
            case HARSH_BRAKE -> config.deductions().harshBraking();
            case HARSH_ACCEL -> config.deductions().harshAcceleration();
            case OVERSPEED -> severity == AlertSeverity.HIGH
                    ? config.deductions().overspeedHigh()
                    : config.deductions().overspeedMedium();
            case IDLE_EXCEEDED -> config.deductions().idleExceeded();
            case IGNITION_ANOMALY -> config.deductions().ignitionAnomaly();
            case GEOFENCE_ENTER -> restrictedGeofence ? config.deductions().restrictedGeofenceEntry() : 0;
            default -> 0;
        };
    }

    public int nightDrivingPenalty(ZonedDateTime ts) {
        if (!config.nightDriving().enabled()) return 0;
        int start = config.nightDriving().startHour();
        int end = config.nightDriving().endHour();
        int hour = ts.getHour();
        boolean inWindow = start <= end
                ? hour >= start && hour < end
                : hour >= start || hour < end;
        return inWindow ? config.deductions().nightDrivingPer10Min() : 0;
    }

    public int cleanTripBonus() {
        return config.bonuses().cleanTrip();
    }

    public int smoothStreakBonus() {
        return config.bonuses().smoothStreakPerTrip();
    }
}

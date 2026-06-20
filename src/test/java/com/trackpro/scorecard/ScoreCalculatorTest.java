package com.trackpro.scorecard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.trackpro.alert.AlertSeverity;
import com.trackpro.alert.AlertType;
import com.trackpro.scorecard.calculator.ScoreCalculator;
import com.trackpro.scorecard.calculator.ScoreBandResolver;
import com.trackpro.scorecard.config.ScorecardConfig;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class ScoreCalculatorTest {

    private final ScorecardConfig config = new ScorecardConfig(100, null, null, null, null, null);
    private final ScoreCalculator calculator = new ScoreCalculator(config);
    private final ScoreBandResolver bandResolver = new ScoreBandResolver(config);

    @Test
    void harshBrakingDeductsDefault() {
        assertEquals(3, calculator.deductionFor(AlertType.HARSH_BRAKE, AlertSeverity.MEDIUM, false));
    }

    @Test
    void overspeedHighDeductsMoreThanMedium() {
        assertEquals(10, calculator.deductionFor(AlertType.OVERSPEED, AlertSeverity.HIGH, false));
        assertEquals(5, calculator.deductionFor(AlertType.OVERSPEED, AlertSeverity.MEDIUM, false));
    }

    @Test
    void geofenceRestrictedOnlyDeducts() {
        assertEquals(8, calculator.deductionFor(AlertType.GEOFENCE_ENTER, AlertSeverity.HIGH, true));
        assertEquals(0, calculator.deductionFor(AlertType.GEOFENCE_ENTER, AlertSeverity.HIGH, false));
    }

    @Test
    void bandResolverWorks() {
        assertEquals(ScoreBandResolver.Band.EXCELLENT, bandResolver.resolve(95));
        assertEquals(ScoreBandResolver.Band.GOOD, bandResolver.resolve(80));
        assertEquals(ScoreBandResolver.Band.FAIR, bandResolver.resolve(65));
        assertEquals(ScoreBandResolver.Band.POOR, bandResolver.resolve(50));
        assertEquals(ScoreBandResolver.Band.CRITICAL, bandResolver.resolve(20));
    }

    @Test
    void nightDrivingPenaltyDisabledByDefault() {
        assertEquals(0, calculator.nightDrivingPenalty(ZonedDateTime.now()));
    }
}

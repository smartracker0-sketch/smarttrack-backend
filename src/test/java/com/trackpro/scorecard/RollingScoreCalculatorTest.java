package com.trackpro.scorecard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.trackpro.scorecard.calculator.RollingScoreCalculator;
import com.trackpro.scorecard.config.ScorecardConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class RollingScoreCalculatorTest {

    private final ScorecardConfig config = new ScorecardConfig(100, null, null, null, null, null);
    private final RollingScoreCalculator calc = new RollingScoreCalculator(config);

    @Test
    void emptyScoresDefaultToStarting() {
        assertEquals(100.0, calc.calculate(List.of(), 0), 0.01);
    }

    @Test
    void singleTripReturnsTripScore() {
        assertEquals(85.0, calc.calculate(List.of(85), 1), 0.01);
    }

    @Test
    void recentScoresWeightedHeavily() {
        List<Integer> scores = List.of(90, 80, 70, 60, 50);
        double overall = calc.calculate(scores, 5);
        assertEquals(82.7, overall, 1.0);
    }
}

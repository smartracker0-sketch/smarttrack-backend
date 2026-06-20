package com.trackpro.scorecard.calculator;

import com.trackpro.scorecard.config.ScorecardConfig;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RollingScoreCalculator {

    private final ScorecardConfig config;

    public RollingScoreCalculator(ScorecardConfig config) {
        this.config = config;
    }

    public double calculate(List<Integer> scores, int totalTrips) {
        if (scores == null || scores.isEmpty()) return config.startingScore();
        int window = Math.min(scores.size(), config.rollingAverage().tripWindow());
        double decay = config.rollingAverage().decayFactor();
        double weightedSum = 0;
        double weightSum = 0;
        for (int i = 0; i < window; i++) {
            double weight = Math.pow(decay, i);
            weightedSum += scores.get(i) * weight;
            weightSum += weight;
        }
        return weightSum == 0 ? config.startingScore() : weightedSum / weightSum;
    }
}

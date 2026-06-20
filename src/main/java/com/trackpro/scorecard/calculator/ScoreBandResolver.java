package com.trackpro.scorecard.calculator;

import com.trackpro.scorecard.config.ScorecardConfig;
import org.springframework.stereotype.Component;

@Component
public class ScoreBandResolver {

    public enum Band { EXCELLENT, GOOD, FAIR, POOR, CRITICAL }

    private final ScorecardConfig config;

    public ScoreBandResolver(ScorecardConfig config) {
        this.config = config;
    }

    public Band resolve(int score) {
        var bands = config.bands();
        if (score >= bands.excellent()) return Band.EXCELLENT;
        if (score >= bands.good()) return Band.GOOD;
        if (score >= bands.fair()) return Band.FAIR;
        if (score >= bands.poor()) return Band.POOR;
        return Band.CRITICAL;
    }

    public String colour(Band band) {
        return switch (band) {
            case EXCELLENT -> "green";
            case GOOD -> "lightgreen";
            case FAIR -> "amber";
            case POOR -> "orange";
            case CRITICAL -> "red";
        };
    }
}

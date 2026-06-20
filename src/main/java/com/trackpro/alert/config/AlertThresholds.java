package com.trackpro.alert.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trackpro.alerts")
public record AlertThresholds(
        Overspeed overspeed,
        HarshDriving harshDriving,
        Idle idle,
        DeviceOffline deviceOffline,
        Fuel fuel
) {

    public record Overspeed(
            int defaultLimitKmh,
            int sustainedReadings,
            double sustainedSeconds,
            int debounceMinutes
    ) {}

    public record HarshDriving(
            double brakingThresholdMs2,
            double accelerationThresholdMs2
    ) {}

    public record Idle(
            int thresholdMinutes,
            int escalationMinutes,
            int debounceMinutes
    ) {}

    public record DeviceOffline(
            int thresholdMinutes,
            int escalationMinutes
    ) {}

    public record Fuel(
            int lowFuelPercent,
            double drainageThresholdPercent,
            int drainageWindowMinutes
    ) {}

    public AlertThresholds {
        if (overspeed == null) {
            overspeed = new Overspeed(100, 3, 10.0, 5);
        }
        if (harshDriving == null) {
            harshDriving = new HarshDriving(-6.5, 4.5);
        }
        if (idle == null) {
            idle = new Idle(10, 30, 30);
        }
        if (deviceOffline == null) {
            deviceOffline = new DeviceOffline(5, 60);
        }
        if (fuel == null) {
            fuel = new Fuel(15, 10.0, 5);
        }
    }
}

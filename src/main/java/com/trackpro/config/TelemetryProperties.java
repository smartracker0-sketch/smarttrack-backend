package com.trackpro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trackpro.telemetry")
public record TelemetryProperties(Tcp tcp, Mqtt mqtt) {

    public record Tcp(int port, boolean enabled) {}

    public record Mqtt(
            String brokerUrl,
            String clientId,
            String username,
            String password,
            String topicPrefix,
            boolean enabled
    ) {}
}

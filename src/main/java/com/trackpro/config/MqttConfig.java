package com.trackpro.config;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "trackpro.telemetry.mqtt", name = "enabled", havingValue = "true")
public class MqttConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttConfig.class);

    @Bean(destroyMethod = "disconnect")
    public IMqttClient mqttClient(TelemetryProperties props) throws MqttException {
        TelemetryProperties.Mqtt cfg = props.mqtt();
        MqttClient client = new MqttClient(cfg.brokerUrl(), cfg.clientId());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);
        if (cfg.username() != null && !cfg.username().isBlank()) {
            options.setUserName(cfg.username());
            options.setPassword(cfg.password().toCharArray());
        }

        try {
            client.connect(options);
            log.info("MQTT connected to {}", cfg.brokerUrl());
        } catch (MqttException e) {
            log.error("Failed to connect to MQTT broker {}: {}", cfg.brokerUrl(), e.getMessage());
            throw e;
        }
        return client;
    }
}

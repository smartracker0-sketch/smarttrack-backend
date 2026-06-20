package com.trackpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TrackProApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrackProApplication.class, args);
    }
}

package com.trackpro.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        TrackProSecurityProperties.class,
        BootstrapAdminProperties.class
})
public class AppConfig {
}

package com.trackpro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trackpro.bootstrap.admin")
public record BootstrapAdminProperties(
        boolean enabled,
        String email,
        String password,
        String displayName
) {
}

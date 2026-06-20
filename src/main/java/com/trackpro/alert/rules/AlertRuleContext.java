package com.trackpro.alert.rules;

import com.trackpro.alert.config.AlertThresholds;
import com.trackpro.alert.service.AlertRuleCache;
import com.trackpro.model.GeofenceEntity;
import com.trackpro.repository.GeofenceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Shared context passed to every alert rule. Wraps configuration lookup
 * and Redis access so rules don't repeat boilerplate.
 */
@Component
public class AlertRuleContext {

    private final AlertThresholds thresholds;
    private final AlertRuleCache cache;
    private final GeofenceRepository geofenceRepository;

    public AlertRuleContext(AlertThresholds thresholds,
                            AlertRuleCache cache,
                            GeofenceRepository geofenceRepository) {
        this.thresholds = thresholds;
        this.cache = cache;
        this.geofenceRepository = geofenceRepository;
    }

    public AlertThresholds thresholds() {
        return thresholds;
    }

    public AlertRuleCache cache() {
        return cache;
    }

    @Cacheable(value = "geofences", key = "#organisationId")
    public List<GeofenceEntity> activeGeofencesForOrg(UUID organisationId) {
        return geofenceRepository.findByOrganisationIdAndActiveTrue(organisationId);
    }
}

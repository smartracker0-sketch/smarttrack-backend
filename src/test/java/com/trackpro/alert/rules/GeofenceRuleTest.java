package com.trackpro.alert.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackpro.alert.AlertType;
import com.trackpro.alert.dto.AlertEvent;
import com.trackpro.alert.service.AlertRuleCache;
import com.trackpro.model.DeviceEntity;
import com.trackpro.model.GeofenceEntity;
import com.trackpro.model.OrganisationEntity;
import com.trackpro.telemetry.DeviceFrame;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GeofenceRuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeofenceRule rule = new GeofenceRule(objectMapper);

    @Test
    void firesEnterOnceWhenTransitioningOutsideToInside() {
        GeofenceEntity depot = circleGeofence("Depot", 6.5, 3.4, 500);
        OrganisationEntity org = new OrganisationEntity();
        org.setId(UUID.randomUUID());
        DeviceEntity device = device(org);
        AlertRuleContext ctx = mockContext(org, depot);
        when(ctx.cache().getGeofenceInside(depot.getId(), device.getId())).thenReturn(Optional.of(false));

        DeviceFrame current = frame(6.5, 3.4); // inside
        List<AlertEvent> events = rule.evaluate(current, null, device, ctx);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).alertType()).isEqualTo(AlertType.GEOFENCE_ENTER);
    }

    @Test
    void firesExitOnceWhenTransitioningInsideToOutside() {
        GeofenceEntity depot = circleGeofence("Depot", 6.5, 3.4, 500);
        OrganisationEntity org = new OrganisationEntity();
        org.setId(UUID.randomUUID());
        DeviceEntity device = device(org);
        AlertRuleContext ctx = mockContext(org, depot);
        when(ctx.cache().getGeofenceInside(depot.getId(), device.getId())).thenReturn(Optional.of(true));

        DeviceFrame current = frame(6.6, 3.5); // outside
        List<AlertEvent> events = rule.evaluate(current, null, device, ctx);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).alertType()).isEqualTo(AlertType.GEOFENCE_EXIT);
    }

    @Test
    void noEventWhenStateUnchanged() {
        GeofenceEntity depot = circleGeofence("Depot", 6.5, 3.4, 500);
        OrganisationEntity org = new OrganisationEntity();
        org.setId(UUID.randomUUID());
        DeviceEntity device = device(org);
        AlertRuleContext ctx = mockContext(org, depot);
        when(ctx.cache().getGeofenceInside(depot.getId(), device.getId())).thenReturn(Optional.of(true));

        DeviceFrame current = frame(6.5, 3.4); // still inside
        List<AlertEvent> events = rule.evaluate(current, null, device, ctx);
        assertThat(events).isEmpty();
    }

    private GeofenceEntity circleGeofence(String name, double lat, double lon, double radiusM) {
        GeofenceEntity g = new GeofenceEntity();
        g.setId(UUID.randomUUID());
        g.setName(name);
        g.setGeofenceType(GeofenceEntity.GeofenceType.CIRCLE);
        g.setCenterLat(lat);
        g.setCenterLng(lon);
        g.setRadiusM(radiusM);
        g.setSeverity(GeofenceEntity.GeofenceSeverity.LOW);
        return g;
    }

    private DeviceEntity device(OrganisationEntity org) {
        DeviceEntity d = new DeviceEntity();
        d.setId(UUID.randomUUID());
        d.setImei("123456789012345");
        d.setOrganisation(org);
        return d;
    }

    private DeviceFrame frame(double lat, double lon) {
        return DeviceFrame.builder().eventTime(Instant.now()).latitude(lat).longitude(lon).build();
    }

    private AlertRuleContext mockContext(OrganisationEntity org, GeofenceEntity geofence) {
        AlertRuleContext ctx = mock(AlertRuleContext.class);
        AlertRuleCache cache = mock(AlertRuleCache.class);
        when(ctx.activeGeofencesForOrg(org.getId())).thenReturn(List.of(geofence));
        when(ctx.cache()).thenReturn(cache);
        return ctx;
    }
}

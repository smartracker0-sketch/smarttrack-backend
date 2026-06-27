package com.trackpro.controller;

import com.trackpro.exception.NotFoundException;
import com.trackpro.model.GeofenceEntity;
import com.trackpro.model.GeofenceEntity.GeofenceSeverity;
import com.trackpro.model.GeofenceEntity.GeofenceType;
import com.trackpro.model.OrganisationEntity;
import com.trackpro.repository.GeofenceRepository;
import com.trackpro.repository.OrganisationRepository;
import com.trackpro.repository.UserRepository;
import com.trackpro.security.CurrentUser;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/geofences")
public class GeofenceController {

    private final GeofenceRepository geofenceRepo;
    private final UserRepository     userRepo;

    public GeofenceController(GeofenceRepository geofenceRepo, UserRepository userRepo) {
        this.geofenceRepo = geofenceRepo;
        this.userRepo = userRepo;
    }

    @GetMapping
    public List<GeofenceDto> list() {
        UUID orgId = currentOrgId();
        List<GeofenceEntity> entities = orgId != null
                ? geofenceRepo.findByOrganisationIdAndActiveTrue(orgId)
                : List.of();
        return entities.stream().map(GeofenceController::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public GeofenceDto create(@Valid @RequestBody GeofenceRequest req) {
        var user = userRepo.findById(CurrentUser.userId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        OrganisationEntity org = user.getOrganisation();
        if (org == null) throw new com.trackpro.exception.BadRequestException("User must belong to an organisation to create geofences");

        var g = new GeofenceEntity();
        g.setOrganisation(org);
        g.setName(req.name());
        g.setGeofenceType(GeofenceType.valueOf(req.geofenceType().toUpperCase()));
        g.setSeverity(req.severity() != null ? GeofenceSeverity.valueOf(req.severity().toUpperCase()) : GeofenceSeverity.LOW);
        g.setGeometryJson(req.geometryJson());
        g.setCenterLat(req.centerLat());
        g.setCenterLng(req.centerLng());
        g.setRadiusM(req.radiusM());
        return toDto(geofenceRepo.save(g));
    }

    @PatchMapping("/{id}/active")
    @Transactional
    public GeofenceDto setActive(@PathVariable UUID id, @RequestBody java.util.Map<String, Boolean> body) {
        var g = geofenceRepo.findById(id).orElseThrow(() -> new NotFoundException("Geofence not found"));
        g.setActive(body.getOrDefault("active", true));
        return toDto(geofenceRepo.save(g));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        geofenceRepo.deleteById(id);
    }

    private UUID currentOrgId() {
        return userRepo.findById(CurrentUser.userId())
                .map(u -> u.getOrganisation() != null ? u.getOrganisation().getId() : null)
                .orElse(null);
    }

    private static GeofenceDto toDto(GeofenceEntity g) {
        return new GeofenceDto(
                g.getId(), g.getName(),
                g.getGeofenceType().name(), g.getSeverity().name(),
                g.getGeometryJson(), g.getCenterLat(), g.getCenterLng(),
                g.getRadiusM(), g.isActive(), g.getCreatedAt());
    }

    public record GeofenceRequest(
            @NotBlank String name,
            @NotBlank String geofenceType,
            String severity,
            @NotBlank String geometryJson,
            Double centerLat, Double centerLng, Double radiusM) {}

    public record GeofenceDto(
            UUID id, String name, String geofenceType, String severity,
            String geometryJson, Double centerLat, Double centerLng,
            Double radiusM, boolean active, Instant createdAt) {}
}

package com.trackpro.controller;

import com.trackpro.dto.organisation.OrganisationCreateRequest;
import com.trackpro.dto.organisation.OrganisationDto;
import com.trackpro.dto.organisation.OrganisationUpdateRequest;
import com.trackpro.service.AdminOrganisationService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/organisations")
public class AdminOrganisationController {

    private final AdminOrganisationService service;

    public AdminOrganisationController(AdminOrganisationService service) {
        this.service = service;
    }

    @GetMapping
    public Page<OrganisationDto> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    public OrganisationDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganisationDto create(@Valid @RequestBody OrganisationCreateRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public OrganisationDto update(@PathVariable UUID id, @Valid @RequestBody OrganisationUpdateRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/status")
    public OrganisationDto setStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            throw new com.trackpro.exception.BadRequestException("status is required");
        }
        return service.setStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}

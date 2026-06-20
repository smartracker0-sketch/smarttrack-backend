package com.trackpro.service;

import com.trackpro.dto.organisation.OrganisationCreateRequest;
import com.trackpro.dto.organisation.OrganisationDto;
import com.trackpro.dto.organisation.OrganisationUpdateRequest;
import com.trackpro.exception.BadRequestException;
import com.trackpro.exception.NotFoundException;
import com.trackpro.model.OrganisationEntity;
import com.trackpro.repository.OrganisationRepository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AdminOrganisationService {

    private final OrganisationRepository orgRepository;

    public AdminOrganisationService(OrganisationRepository orgRepository) {
        this.orgRepository = orgRepository;
    }

    public Page<OrganisationDto> list(Pageable pageable) {
        return orgRepository.findAll(pageable).map(AdminOrganisationService::toDto);
    }

    public OrganisationDto get(UUID id) {
        return toDto(orgRepository.findById(id).orElseThrow(() -> new NotFoundException("Organisation not found")));
    }

    @Transactional
    public OrganisationDto create(OrganisationCreateRequest req) {
        if (orgRepository.existsBySlug(req.slug())) {
            throw new BadRequestException("Slug '" + req.slug() + "' is already taken");
        }
        OrganisationEntity org = new OrganisationEntity();
        org.setName(req.name());
        org.setSlug(req.slug());
        org.setAdminEmail(req.adminEmail());
        if (req.plan() != null && !req.plan().isBlank()) org.setPlan(req.plan());
        if (req.vehicleLimit() > 0) org.setVehicleLimit(req.vehicleLimit());
        return toDto(orgRepository.save(org));
    }

    @Transactional
    public OrganisationDto update(UUID id, OrganisationUpdateRequest req) {
        OrganisationEntity org = orgRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organisation not found"));
        if (req.name() != null && !req.name().isBlank()) org.setName(req.name());
        if (req.plan() != null && !req.plan().isBlank()) org.setPlan(req.plan());
        if (req.status() != null && !req.status().isBlank()) org.setStatus(req.status());
        if (req.vehicleLimit() > 0) org.setVehicleLimit(req.vehicleLimit());
        return toDto(orgRepository.save(org));
    }

    @Transactional
    public void delete(UUID id) {
        OrganisationEntity org = orgRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organisation not found"));
        orgRepository.delete(org);
    }

    @Transactional
    public OrganisationDto setStatus(UUID id, String status) {
        OrganisationEntity org = orgRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organisation not found"));
        org.setStatus(status);
        return toDto(orgRepository.save(org));
    }

    static OrganisationDto toDto(OrganisationEntity o) {
        return new OrganisationDto(
                o.getId(), o.getName(), o.getSlug(), o.getPlan(), o.getStatus(),
                o.getAdminEmail(), o.getVehicleLimit(), o.getCreatedAt(), o.getUpdatedAt()
        );
    }
}

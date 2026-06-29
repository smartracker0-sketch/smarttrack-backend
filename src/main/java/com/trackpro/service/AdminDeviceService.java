package com.trackpro.service;

import com.trackpro.dto.admin.AdminBulkDeviceRequest;
import com.trackpro.dto.admin.AdminDeviceDto;
import com.trackpro.exception.BadRequestException;
import com.trackpro.exception.NotFoundException;
import com.trackpro.model.DeviceEntity;
import com.trackpro.repository.DeviceRepository;
import com.trackpro.repository.OrganisationRepository;
import com.trackpro.repository.UserRepository;
import com.trackpro.sms.DeviceActivationService;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AdminDeviceService {

    private final DeviceRepository deviceRepository;
    private final OrganisationRepository orgRepository;
    private final UserRepository userRepository;
    private final DeviceActivationService activationService;

    public AdminDeviceService(DeviceRepository deviceRepository,
                              OrganisationRepository orgRepository,
                              UserRepository userRepository,
                              DeviceActivationService activationService) {
        this.deviceRepository = deviceRepository;
        this.orgRepository = orgRepository;
        this.userRepository = userRepository;
        this.activationService = activationService;
    }

    public Page<AdminDeviceDto> list(Pageable pageable) {
        return deviceRepository.findAll(pageable).map(AdminDeviceService::toDto);
    }

    public AdminDeviceDto get(UUID id) {
        return toDto(deviceRepository.findById(id).orElseThrow(() -> new NotFoundException("Device not found")));
    }

    public void checkActivation(String imei) {
        DeviceEntity d = deviceRepository.findByImei(imei)
                .orElseThrow(() -> new NotFoundException("Device not found: " + imei));
        activationService.initiateCheck(d);
    }

    @Transactional
    public List<AdminDeviceDto> bulkAdd(AdminBulkDeviceRequest req) {
        var org = req.organisationId() != null
                ? orgRepository.findById(req.organisationId()).orElseThrow(() -> new NotFoundException("Organisation not found"))
                : null;
        var user = req.userId() != null
                ? userRepository.findById(req.userId()).orElseThrow(() -> new NotFoundException("User not found"))
                : null;

        List<AdminDeviceDto> result = new ArrayList<>();
        for (String rawImei : req.imeis()) {
            String imei = rawImei.trim();
            if (!imei.matches("^[0-9]{10,20}$")) {
                throw new BadRequestException("Invalid IMEI: " + imei);
            }
            if (deviceRepository.existsByImei(imei)) {
                throw new BadRequestException("IMEI already registered: " + imei);
            }
            DeviceEntity d = new DeviceEntity();
            d.setImei(imei);
            d.setName(imei);
            if (req.deviceType() != null) d.setDeviceType(req.deviceType());
            if (req.firmware() != null) d.setFirmware(req.firmware());
            if (req.simCard() != null) d.setSimCard(req.simCard());
            if (req.serialNo() != null) d.setSerialNo(req.serialNo());
            if (req.vehiclePlate() != null) d.setVehiclePlate(req.vehiclePlate());
            if (req.notes() != null) d.setNotes(req.notes());
            if (req.simNumber() != null) d.setSimNumber(req.simNumber());
            if (req.simApn() != null) d.setSimApn(req.simApn());
            if (req.manufacturer() != null) d.setManufacturer(req.manufacturer());
            d.setOrganisation(org);
            d.setOwner(user);
            boolean assigned = org != null || user != null;
            d.setStatus(assigned ? "Assigned" : "Unassigned");
            DeviceEntity saved = deviceRepository.save(d);
            result.add(toDto(saved));
            if (assigned) activationService.initiateCheck(saved);
        }
        return result;
    }

    @Transactional
    public AdminDeviceDto assignOrganisation(UUID deviceId, UUID orgId) {
        DeviceEntity d = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found"));
        var org = orgRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organisation not found"));
        d.setOrganisation(org);
        d.setStatus("Assigned");
        DeviceEntity saved = deviceRepository.save(d);
        AdminDeviceDto dto = toDto(saved);
        activationService.initiateCheck(saved);
        return dto;
    }

    @Transactional
    public AdminDeviceDto assignUser(UUID deviceId, UUID userId) {
        DeviceEntity d = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found"));
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        d.setOwner(user);
        d.setStatus("Assigned");
        DeviceEntity saved = deviceRepository.save(d);
        AdminDeviceDto dto = toDto(saved);
        activationService.initiateCheck(saved);
        return dto;
    }

    @Transactional
    public AdminDeviceDto unassign(UUID deviceId) {
        DeviceEntity d = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found"));
        d.setOrganisation(null);
        d.setOwner(null);
        d.setVehiclePlate(null);
        d.setStatus("Unassigned");
        return toDto(deviceRepository.save(d));
    }

    @Transactional
    public void delete(UUID id) {
        DeviceEntity d = deviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Device not found"));
        deviceRepository.delete(d);
    }

    static AdminDeviceDto toDto(DeviceEntity d) {
        var org = d.getOrganisation();
        var owner = d.getOwner();
        return new AdminDeviceDto(
                d.getId(), d.getImei(), d.getName(),
                d.getDeviceType(), d.getFirmware(), d.getSimCard(),
                d.getSerialNo(), d.getVehiclePlate(), d.getStatus(), d.getNotes(),
                org != null ? org.getId() : null,
                org != null ? org.getName() : null,
                owner != null ? owner.getId() : null,
                owner != null ? owner.getDisplayName() : null,
                d.getSimNumber(),
                d.getManufacturer(),
                d.getActivationStatus(),
                d.getActivationAttempts(),
                d.getActivationAttemptedAt(),
                d.getActivationConfirmedAt(),
                d.getLastSmsReply(),
                d.isServerConfigured(),
                d.isApnConfigured(),
                d.getCreatedAt()
        );
    }
}

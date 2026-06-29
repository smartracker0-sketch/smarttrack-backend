package com.trackpro.service;

import com.trackpro.dto.device.DeviceCreateRequest;
import com.trackpro.dto.device.DeviceDto;
import com.trackpro.dto.device.DeviceUpdateRequest;
import com.trackpro.exception.BadRequestException;
import com.trackpro.exception.NotFoundException;
import com.trackpro.model.DeviceEntity;
import com.trackpro.repository.DeviceRepository;
import com.trackpro.repository.UserRepository;
import com.trackpro.security.CurrentUser;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    public DeviceService(DeviceRepository deviceRepository, UserRepository userRepository) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
    }

    public Page<DeviceDto> listMine(Pageable pageable) {
        var userId = CurrentUser.userId();
        if (userId == null) throw new com.trackpro.exception.UnauthorizedException("Authentication required");
        var user = userRepository.findById(userId).orElseThrow(() -> new com.trackpro.exception.NotFoundException("User not found"));
        var orgId = user.getOrganisation() != null ? user.getOrganisation().getId() : null;
        if (orgId != null) {
            return deviceRepository.findByOwnerIdOrOrganisationId(userId, orgId, pageable).map(DeviceService::toDto);
        }
        return deviceRepository.findByOwnerId(userId, pageable).map(DeviceService::toDto);
    }

    @Transactional
    public DeviceDto createMine(DeviceCreateRequest req) {
        if (deviceRepository.existsByImei(req.imei())) {
            throw new BadRequestException("IMEI already registered");
        }
        var userId = CurrentUser.userId();
        var owner = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        DeviceEntity device = new DeviceEntity();
        device.setOwner(owner);
        device.setImei(req.imei());
        device.setName(req.name());
        return toDto(deviceRepository.save(device));
    }

    public DeviceDto getMine(UUID id) {
        var userId = CurrentUser.userId();
        if (userId == null) throw new com.trackpro.exception.UnauthorizedException("Authentication required");
        var user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        var orgId = user.getOrganisation() != null ? user.getOrganisation().getId() : null;
        var device = orgId != null
                ? deviceRepository.findByIdAndOwnerIdOrIdAndOrganisationId(id, userId, id, orgId)
                        .orElseThrow(() -> new NotFoundException("Device not found"))
                : deviceRepository.findByIdAndOwnerId(id, userId)
                        .orElseThrow(() -> new NotFoundException("Device not found"));
        return toDto(device);
    }

    @Transactional
    public DeviceDto updateMine(UUID id, DeviceUpdateRequest req) {
        var userId = CurrentUser.userId();
        var device = deviceRepository.findByIdAndOwnerId(id, userId).orElseThrow(() -> new NotFoundException("Device not found"));
        device.setName(req.name());
        return toDto(deviceRepository.save(device));
    }

    @Transactional
    public void deleteMine(UUID id) {
        var userId = CurrentUser.userId();
        var device = deviceRepository.findByIdAndOwnerId(id, userId).orElseThrow(() -> new NotFoundException("Device not found"));
        deviceRepository.delete(device);
    }

    static DeviceDto toDto(DeviceEntity e) {
        var org = e.getOrganisation();
        var owner = e.getOwner();
        return new DeviceDto(
                e.getId(), e.getImei(), e.getName(),
                e.getDeviceType(), e.getFirmware(), e.getVehiclePlate(),
                e.getStatus(), e.getCreatedAt(),
                org != null ? org.getId() : null,
                org != null ? org.getName() : null,
                owner != null ? owner.getDisplayName() : null
        );
    }
}

package com.trackpro.service;

import com.trackpro.dto.location.LocationDto;
import com.trackpro.dto.location.LocationIngestRequest;
import com.trackpro.exception.NotFoundException;
import com.trackpro.model.LocationEntity;
import com.trackpro.repository.DeviceRepository;
import com.trackpro.repository.LocationRepository;
import com.trackpro.security.CurrentUser;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class LocationService {
    private final DeviceRepository deviceRepository;
    private final LocationRepository locationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public LocationService(
            DeviceRepository deviceRepository,
            LocationRepository locationRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.deviceRepository = deviceRepository;
        this.locationRepository = locationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public LocationDto ingestForMyDevice(UUID deviceId, LocationIngestRequest req) {
        var userId = CurrentUser.userId();
        var device = deviceRepository.findByIdAndOwnerId(deviceId, userId)
                .orElseThrow(() -> new NotFoundException("Device not found"));

        LocationEntity loc = new LocationEntity();
        loc.setDevice(device);
        loc.setLatitude(req.latitude());
        loc.setLongitude(req.longitude());
        loc.setSpeedKph(req.speedKph());
        loc.setHeadingDeg(req.headingDeg());
        loc.setAccuracyM(req.accuracyM());
        loc.setRecordedAt(req.recordedAt());
        LocationEntity saved = locationRepository.save(loc);

        LocationDto dto = toDto(saved);
        messagingTemplate.convertAndSend("/topic/locations", dto);
        messagingTemplate.convertAndSend("/topic/devices/" + deviceId + "/location", dto);
        return dto;
    }

    @Transactional
    public LocationDto latestForMyDevice(UUID deviceId) {
        var userId = CurrentUser.userId();
        deviceRepository.findByIdAndOwnerId(deviceId, userId).orElseThrow(() -> new NotFoundException("Device not found"));
        var latest = locationRepository.findTopByDeviceIdOrderByRecordedAtDesc(deviceId)
                .orElseThrow(() -> new NotFoundException("No locations"));
        return toDto(latest);
    }

    @Transactional
    public List<LocationDto> latestForMyDevices() {
        var userId = CurrentUser.userId();
        var devices = deviceRepository.findByOwnerId(userId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        var ids = devices.stream().map(d -> d.getId()).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return locationRepository.findLatestForDevices(ids).stream().map(LocationService::toDto).toList();
    }

    @Transactional
    public List<LocationDto> historyForMyDevice(UUID deviceId, Instant from, Instant to) {
        var userId = CurrentUser.userId();
        deviceRepository.findByIdAndOwnerId(deviceId, userId).orElseThrow(() -> new NotFoundException("Device not found"));
        return locationRepository.findHistory(deviceId, from, to).stream().map(LocationService::toDto).toList();
    }

    static LocationDto toDto(LocationEntity e) {
        return new LocationDto(
                e.getId(),
                e.getDevice().getId(),
                e.getLatitude(),
                e.getLongitude(),
                e.getSpeedKph(),
                e.getHeadingDeg(),
                e.getAccuracyM(),
                e.getRecordedAt(),
                e.getReceivedAt()
        );
    }
}

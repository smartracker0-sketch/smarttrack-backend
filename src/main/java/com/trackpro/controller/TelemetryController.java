package com.trackpro.controller;

import com.trackpro.dto.telemetry.DeviceAlertDto;
import com.trackpro.dto.telemetry.FuelReadingDto;
import com.trackpro.dto.telemetry.TelemetryEventDto;
import com.trackpro.exception.NotFoundException;
import com.trackpro.model.DeviceAlert;
import com.trackpro.model.FuelReading;
import com.trackpro.repository.DeviceAlertRepository;
import com.trackpro.repository.DeviceRepository;
import com.trackpro.repository.FuelReadingRepository;
import com.trackpro.repository.TelemetryEventRepository;
import com.trackpro.repository.UserRepository;
import com.trackpro.security.CurrentUser;
import com.trackpro.telemetry.TelemetryService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {

    private final TelemetryEventRepository telemetryRepository;
    private final FuelReadingRepository fuelRepository;
    private final DeviceAlertRepository alertRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    public TelemetryController(
            TelemetryEventRepository telemetryRepository,
            FuelReadingRepository fuelRepository,
            DeviceAlertRepository alertRepository,
            DeviceRepository deviceRepository,
            UserRepository userRepository
    ) {
        this.telemetryRepository = telemetryRepository;
        this.fuelRepository      = fuelRepository;
        this.alertRepository     = alertRepository;
        this.deviceRepository    = deviceRepository;
        this.userRepository      = userRepository;
    }

    @GetMapping("/latest")
    public TelemetryEventDto latest(@RequestParam UUID deviceId) {
        assertOwnsDevice(deviceId);
        return telemetryRepository.findTopByDeviceIdOrderByEventTimeDesc(deviceId)
                .map(TelemetryService::toDto)
                .orElseThrow(() -> new NotFoundException("No telemetry for device"));
    }

    @GetMapping("/history")
    public Page<TelemetryEventDto> history(
            @RequestParam UUID deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 200) Pageable pageable
    ) {
        assertOwnsDevice(deviceId);
        return telemetryRepository.findByDeviceIdOrderByEventTimeDesc(deviceId, pageable)
                .map(TelemetryService::toDto);
    }

    @GetMapping("/fuel/latest")
    public FuelReadingDto fuelLatest(@RequestParam UUID deviceId) {
        assertOwnsDevice(deviceId);
        return fuelRepository.findTopByDeviceIdOrderByEventTimeDesc(deviceId)
                .map(TelemetryController::toFuelDto)
                .orElseThrow(() -> new NotFoundException("No fuel data for device"));
    }

    @GetMapping("/fuel/history")
    public List<FuelReadingDto> fuelHistory(
            @RequestParam UUID deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        assertOwnsDevice(deviceId);
        return fuelRepository.findHistory(deviceId, from, to).stream()
                .map(TelemetryController::toFuelDto).toList();
    }

    @GetMapping("/alerts")
    public List<DeviceAlertDto> alerts(
            @RequestParam UUID deviceId,
            @RequestParam(defaultValue = "false") boolean unacknowledgedOnly
    ) {
        assertOwnsDevice(deviceId);
        if (unacknowledgedOnly) {
            return alertRepository.findByDeviceIdAndAcknowledgedFalseOrderByAlertTimeDesc(deviceId)
                    .stream().map(TelemetryService::toAlertDto).toList();
        }
        return alertRepository.findByDeviceIdOrderByAlertTimeDesc(deviceId, Pageable.unpaged())
                .stream().map(TelemetryService::toAlertDto).toList();
    }

    @PatchMapping("/alerts/{alertId}/acknowledge")
    @Transactional
    public Map<String, Object> acknowledgeAlert(@PathVariable UUID alertId) {
        UUID userId = CurrentUser.userId();
        DeviceAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NotFoundException("Alert not found"));
        assertOwnsDevice(alert.getDevice().getId());
        var user = userRepository.findById(userId).orElseThrow();
        alertRepository.acknowledge(alertId, user);
        return Map.of("acknowledged", true, "alertId", alertId);
    }

    private void assertOwnsDevice(UUID deviceId) {
        UUID userId = CurrentUser.userId();
        deviceRepository.findByIdAndOwnerId(deviceId, userId)
                .orElseThrow(() -> new NotFoundException("Device not found"));
    }

    private static FuelReadingDto toFuelDto(FuelReading f) {
        return new FuelReadingDto(f.getId(), f.getDevice().getId(),
                f.getEventTime(), f.getReceivedAt(),
                f.getFuelLevelPct(), f.getFuelLiters(), f.getTemperatureC(), f.getTankId());
    }

}

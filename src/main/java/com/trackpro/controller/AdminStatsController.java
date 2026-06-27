package com.trackpro.controller;

import com.trackpro.repository.DeviceAlertRepository;
import com.trackpro.repository.DeviceRepository;
import com.trackpro.repository.OrganisationRepository;
import com.trackpro.repository.UserRepository;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/stats")
public class AdminStatsController {

    private final OrganisationRepository orgRepo;
    private final UserRepository userRepo;
    private final DeviceRepository deviceRepo;
    private final DeviceAlertRepository alertRepo;

    public AdminStatsController(OrganisationRepository orgRepo, UserRepository userRepo,
            DeviceRepository deviceRepo, DeviceAlertRepository alertRepo) {
        this.orgRepo    = orgRepo;
        this.userRepo   = userRepo;
        this.deviceRepo = deviceRepo;
        this.alertRepo  = alertRepo;
    }

    @GetMapping("/alerts")
    public org.springframework.data.domain.Page<com.trackpro.dto.telemetry.DeviceAlertDto> alerts(
            @org.springframework.data.web.PageableDefault(size = 100) org.springframework.data.domain.Pageable pageable) {
        return alertRepo.findAllByOrderByAlertTimeDesc(pageable)
                .map(com.trackpro.telemetry.TelemetryService::toAlertDto);
    }

    @GetMapping
    public Map<String, Object> stats() {
        long totalOrgs    = orgRepo.count();
        long activeOrgs   = orgRepo.findByStatus("Active", org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        long totalUsers   = userRepo.count();
        long totalDevices = deviceRepo.count();
        long onlineDevices = deviceRepo.countByStatus("Online");
        long totalAlerts  = alertRepo.count();
        long unackedAlerts = alertRepo.countByAcknowledgedFalse();

        return Map.of(
            "totalOrgs",     totalOrgs,
            "activeOrgs",    activeOrgs,
            "totalUsers",    totalUsers,
            "totalDevices",  totalDevices,
            "onlineDevices", onlineDevices,
            "totalAlerts",   totalAlerts,
            "unackedAlerts", unackedAlerts
        );
    }
}

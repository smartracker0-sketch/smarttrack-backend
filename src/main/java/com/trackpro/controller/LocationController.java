package com.trackpro.controller;

import com.trackpro.dto.location.LocationDto;
import com.trackpro.dto.location.LocationIngestRequest;
import com.trackpro.service.LocationService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/latest")
    public Object latest(@RequestParam(required = false) UUID deviceId) {
        if (deviceId != null) {
            return locationService.latestForMyDevice(deviceId);
        }
        return locationService.latestForMyDevices();
    }

    @GetMapping("/history")
    public List<LocationDto> history(
            @RequestParam UUID deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return locationService.historyForMyDevice(deviceId, from, to);
    }

    @PostMapping("/ingest")
    public LocationDto ingest(
            @RequestParam UUID deviceId,
            @Valid @RequestBody LocationIngestRequest req
    ) {
        return locationService.ingestForMyDevice(deviceId, req);
    }
}

package com.trackpro.controller;

import com.trackpro.model.MaintenanceRecord;
import com.trackpro.service.FleetService;
import com.trackpro.service.FleetService.MaintenanceRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/maintenance")
public class MaintenanceController {

    private final FleetService fleetService;

    public MaintenanceController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping
    public Page<MaintenanceRecord> list(@PageableDefault(size = 50) Pageable pageable) {
        return fleetService.listMaintenance(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceRecord create(@Valid @RequestBody MaintenanceRequest req) {
        return fleetService.createMaintenance(req);
    }

    @PutMapping("/{id}")
    public MaintenanceRecord update(@PathVariable UUID id, @Valid @RequestBody MaintenanceRequest req) {
        return fleetService.updateMaintenance(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        fleetService.deleteMaintenance(id);
    }
}

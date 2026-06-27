package com.trackpro.controller;

import com.trackpro.model.ElockDevice;
import com.trackpro.service.FleetService;
import com.trackpro.service.FleetService.ElockRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/elock")
public class ElockController {

    private final FleetService fleetService;

    public ElockController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping
    public Page<ElockDevice> list(@PageableDefault(size = 50) Pageable pageable) {
        return fleetService.listElocks(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ElockDevice create(@Valid @RequestBody ElockRequest req) {
        return fleetService.createElock(req);
    }

    @PatchMapping("/{id}/status")
    public ElockDevice updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return fleetService.patchElockStatus(id, body.getOrDefault("status", "UNKNOWN"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        fleetService.deleteElock(id);
    }
}

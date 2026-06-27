package com.trackpro.controller;

import com.trackpro.model.Consigner;
import com.trackpro.service.FleetService;
import com.trackpro.service.FleetService.ConsignerRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/consigners")
public class ConsignerController {

    private final FleetService fleetService;

    public ConsignerController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping
    public Page<Consigner> list(@PageableDefault(size = 50) Pageable pageable) {
        return fleetService.listConsigners(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Consigner create(@Valid @RequestBody ConsignerRequest req) {
        return fleetService.createConsigner(req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        fleetService.deleteConsigner(id);
    }
}

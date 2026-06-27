package com.trackpro.controller;

import com.trackpro.model.Vendor;
import com.trackpro.service.FleetService;
import com.trackpro.service.FleetService.VendorRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vendors")
public class VendorController {

    private final FleetService fleetService;

    public VendorController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping
    public Page<Vendor> list(@PageableDefault(size = 50) Pageable pageable) {
        return fleetService.listVendors(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Vendor create(@Valid @RequestBody VendorRequest req) {
        return fleetService.createVendor(req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        fleetService.deleteVendor(id);
    }
}

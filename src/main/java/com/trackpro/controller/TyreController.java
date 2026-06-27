package com.trackpro.controller;

import com.trackpro.model.TyreRecord;
import com.trackpro.service.FleetService;
import com.trackpro.service.FleetService.TyreRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tyres")
public class TyreController {

    private final FleetService fleetService;

    public TyreController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping
    public Page<TyreRecord> list(@PageableDefault(size = 50) Pageable pageable) {
        return fleetService.listTyres(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TyreRecord create(@Valid @RequestBody TyreRequest req) {
        return fleetService.createTyre(req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        fleetService.deleteTyre(id);
    }
}

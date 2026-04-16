package com.trackpro.controller;

import com.trackpro.dto.device.DeviceCreateRequest;
import com.trackpro.dto.device.DeviceDto;
import com.trackpro.dto.device.DeviceUpdateRequest;
import com.trackpro.service.DeviceService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public Page<DeviceDto> list(@PageableDefault(size = 20) Pageable pageable) {
        return deviceService.listMine(pageable);
    }

    @PostMapping
    public DeviceDto create(@Valid @RequestBody DeviceCreateRequest req) {
        return deviceService.createMine(req);
    }

    @GetMapping("/{id}")
    public DeviceDto get(@PathVariable UUID id) {
        return deviceService.getMine(id);
    }

    @PutMapping("/{id}")
    public DeviceDto update(@PathVariable UUID id, @Valid @RequestBody DeviceUpdateRequest req) {
        return deviceService.updateMine(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        deviceService.deleteMine(id);
    }
}

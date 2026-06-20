package com.trackpro.controller;

import com.trackpro.dto.admin.AdminBulkDeviceRequest;
import com.trackpro.dto.admin.AdminDeviceDto;
import com.trackpro.service.AdminDeviceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/devices")
public class AdminDeviceController {

    private final AdminDeviceService service;

    public AdminDeviceController(AdminDeviceService service) {
        this.service = service;
    }

    @GetMapping
    public Page<AdminDeviceDto> list(@PageableDefault(size = 50) Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    public AdminDeviceDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AdminDeviceDto> bulkAdd(@Valid @RequestBody AdminBulkDeviceRequest req) {
        return service.bulkAdd(req);
    }

    @PatchMapping("/{id}/assign")
    public AdminDeviceDto assign(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String orgIdStr = body.get("organisationId");
        if (orgIdStr == null || orgIdStr.isBlank()) {
            throw new com.trackpro.exception.BadRequestException("organisationId is required");
        }
        return service.assignOrganisation(id, UUID.fromString(orgIdStr));
    }

    @PatchMapping("/{id}/unassign")
    public AdminDeviceDto unassign(@PathVariable UUID id) {
        return service.unassign(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}

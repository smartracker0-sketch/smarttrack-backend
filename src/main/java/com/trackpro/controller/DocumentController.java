package com.trackpro.controller;

import com.trackpro.model.Document;
import com.trackpro.service.FleetService;
import com.trackpro.service.FleetService.DocumentRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final FleetService fleetService;

    public DocumentController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping
    public Page<Document> list(@PageableDefault(size = 50) Pageable pageable) {
        return fleetService.listDocuments(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Document create(@Valid @RequestBody DocumentRequest req) {
        return fleetService.createDocument(req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        fleetService.deleteDocument(id);
    }
}

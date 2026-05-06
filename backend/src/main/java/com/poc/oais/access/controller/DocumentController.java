package com.poc.oais.access.controller;

import com.poc.oais.access.model.AipMetadata;
import com.poc.oais.access.model.DipManifest;
import com.poc.oais.access.service.ArchivalStorageService;
import com.poc.oais.access.service.DipGeneratorService;
import com.poc.oais.access.service.ModeResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final ArchivalStorageService storage;
    private final DipGeneratorService dipGenerator;
    private final ModeResolver modeResolver;

    public DocumentController(
            ArchivalStorageService storage,
            DipGeneratorService dipGenerator,
            ModeResolver modeResolver
    ) {
        this.storage = storage;
        this.dipGenerator = dipGenerator;
        this.modeResolver = modeResolver;
    }

    @GetMapping
    public List<DipManifest> list(HttpServletRequest req) {
        int mode = modeResolver.effectiveMode(req);
        return storage.listAll().stream()
                .map(aip -> dipGenerator.manifest(aip, mode))
                .toList();
    }

    @GetMapping("/{id}/manifest")
    public ResponseEntity<DipManifest> manifest(@PathVariable String id, HttpServletRequest req) {
        int mode = modeResolver.effectiveMode(req);
        return storage.findById(id)
                .map(aip -> dipGenerator.manifest(aip, mode))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AipMetadata> metadata(@PathVariable String id) {
        return storage.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

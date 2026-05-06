package com.poc.oais.access.controller;

import com.poc.oais.access.service.ArchivalStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final ArchivalStorageService storage;

    public AdminController(ArchivalStorageService storage) {
        this.storage = storage;
    }

    @PostMapping("/rescan")
    public Map<String, Object> rescan() {
        long start = System.currentTimeMillis();
        int count = storage.rescan();
        long elapsed = System.currentTimeMillis() - start;
        log.info("Manual rescan completed: {} AIP(s) in {}ms", count, elapsed);
        return Map.of(
                "indexedCount", count,
                "elapsedMs", elapsed,
                "scannedAt", Instant.now().toString()
        );
    }
}

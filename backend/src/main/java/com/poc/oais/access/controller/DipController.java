package com.poc.oais.access.controller;

import com.poc.oais.access.model.AipMetadata;
import com.poc.oais.access.model.DocumentKind;
import com.poc.oais.access.config.AccessProperties;
import com.poc.oais.access.service.AccessAuditService;
import com.poc.oais.access.service.ArchivalStorageService;
import com.poc.oais.access.service.DipGeneratorService;
import com.poc.oais.access.service.ModeResolver;
import com.poc.oais.access.service.TokenService;
import com.poc.oais.access.service.WatermarkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/dip")
public class DipController {

    private final ArchivalStorageService storage;
    private final DipGeneratorService dipGenerator;
    private final TokenService tokenService;
    private final WatermarkService watermarkService;
    private final AccessAuditService audit;
    private final ModeResolver modeResolver;
    private final AccessProperties props;

    public DipController(
            ArchivalStorageService storage,
            DipGeneratorService dipGenerator,
            TokenService tokenService,
            WatermarkService watermarkService,
            AccessAuditService audit,
            ModeResolver modeResolver,
            AccessProperties props
    ) {
        this.storage = storage;
        this.dipGenerator = dipGenerator;
        this.tokenService = tokenService;
        this.watermarkService = watermarkService;
        this.audit = audit;
        this.modeResolver = modeResolver;
        this.props = props;
    }

    @GetMapping("/{id}/page-token/{n}")
    public Map<String, Object> issuePageToken(
            @PathVariable String id,
            @PathVariable int n,
            @RequestHeader(name = "X-Viewer-Id", required = false) String viewerId
    ) {
        AipMetadata aip = storage.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AIP_NOT_FOUND"));
        String vid = viewerId != null ? viewerId : "anonymous";
        long ttl = props.antiDownload().pageTokenTtlSeconds();
        String token = tokenService.issue(aip.id(), n, vid, Duration.ofSeconds(ttl));
        return Map.of(
                "token", token,
                "expiresInSeconds", ttl
        );
    }

    @GetMapping("/{id}/page/{n}")
    public ResponseEntity<Resource> servePage(
            @PathVariable String id,
            @PathVariable int n,
            @RequestParam(name = "t", required = false) String token,
            @RequestHeader(name = "X-Viewer-Id", required = false) String viewerId,
            HttpServletRequest req
    ) throws IOException {
        AipMetadata aip = storage.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AIP_NOT_FOUND"));
        if (aip.kind() == DocumentKind.VIDEO) {
            return ResponseEntity.badRequest().build();
        }

        int mode = modeResolver.effectiveMode(req);
        if (mode == 3) {
            tokenService.requireValid(token, aip.id(), n, viewerId);
        }

        Path source = dipGenerator.servePage(aip, n);
        String vid = viewerId != null ? viewerId : "anonymous";
        Resource body;
        if (watermarkService.shouldWatermark(mode)) {
            byte[] watermarked = watermarkService.overlayPng(source, vid);
            body = new ByteArrayResource(watermarked) {
                @Override public String getFilename() { return ""; }
            };
        } else {
            body = new FileSystemResource(source);
        }

        audit.recordView(aip.id(), vid, "PAGE_DELIVERED",
                Map.of("page", n, "ip", req.getRemoteAddr(), "mode", mode));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"\"")
                .header("X-Content-Type-Options", "nosniff")
                .header("Referrer-Policy", "no-referrer")
                .header("Cross-Origin-Resource-Policy", "same-origin")
                .header("X-Anti-Download-Mode", String.valueOf(mode))
                .body(body);
    }

    @GetMapping("/{id}/hls/{file}")
    public ResponseEntity<Resource> serveHlsAsset(
            @PathVariable String id,
            @PathVariable String file,
            @RequestParam(name = "t", required = false) String token,
            @RequestHeader(name = "X-Viewer-Id", required = false) String viewerId,
            HttpServletRequest req
    ) {
        AipMetadata aip = storage.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AIP_NOT_FOUND"));
        if (aip.kind() != DocumentKind.VIDEO) {
            return ResponseEntity.badRequest().build();
        }
        if (!file.matches("^(master\\.m3u8|seg-\\d+\\.ts)$")) {
            return ResponseEntity.badRequest().build();
        }
        int mode = modeResolver.effectiveMode(req);
        if (mode == 3) {
            // Single video-session token (page=0) covers entire HLS stream — simpler than per-segment tokens.
            tokenService.requireValid(token, aip.id(), 0, viewerId);
        }

        Path asset = dipGenerator.serveHlsAsset(aip, file);
        if (!Files.exists(asset)) return ResponseEntity.notFound().build();

        MediaType type = file.endsWith(".m3u8")
                ? MediaType.parseMediaType("application/vnd.apple.mpegurl")
                : MediaType.parseMediaType("video/mp2t");

        String vid = viewerId != null ? viewerId : "anonymous";
        audit.recordView(aip.id(), vid, "HLS_SEGMENT_DELIVERED",
                Map.of("file", file, "ip", req.getRemoteAddr(), "mode", mode));

        return ResponseEntity.ok()
                .contentType(type)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"\"")
                .header("X-Content-Type-Options", "nosniff")
                .header("Cross-Origin-Resource-Policy", "same-origin")
                .header("X-Anti-Download-Mode", String.valueOf(mode))
                .body(new FileSystemResource(asset));
    }
}

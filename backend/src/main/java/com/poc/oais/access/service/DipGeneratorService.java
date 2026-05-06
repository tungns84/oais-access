package com.poc.oais.access.service;

import com.poc.oais.access.model.AipMetadata;
import com.poc.oais.access.model.DipManifest;
import com.poc.oais.access.model.DocumentKind;
import com.poc.oais.access.service.renderer.Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class DipGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(DipGeneratorService.class);

    private final Map<DocumentKind, Renderer> registry = new EnumMap<>(DocumentKind.class);

    public DipGeneratorService(List<Renderer> renderers) {
        for (Renderer r : renderers) {
            registry.put(r.supports(), r);
            log.info("Registered renderer for kind {}: {}", r.supports(), r.getClass().getSimpleName());
        }
    }

    public DipManifest manifest(AipMetadata aip, int mode) {
        Renderer renderer = registry.get(aip.kind());
        int pageCount = 1;
        String hlsUrl = null;
        if (renderer != null) {
            try {
                pageCount = renderer.prepare(aip);
            } catch (Exception e) {
                log.error("Renderer prepare failed for {}: {}", aip.id(), e.getMessage());
                pageCount = 0;
            }
            if (aip.kind() == DocumentKind.VIDEO) {
                hlsUrl = "/api/dip/" + aip.id() + "/hls/master.m3u8";
            }
        }
        return new DipManifest(
                aip.id(),
                aip.title(),
                aip.kind(),
                aip.mimeType(),
                pageCount,
                hlsUrl,
                mode,
                mode == 3
        );
    }

    public Path servePage(AipMetadata aip, int pageNumber) {
        return renderer(aip).resolvePage(aip, pageNumber);
    }

    public Path serveHlsAsset(AipMetadata aip, String fileName) {
        return renderer(aip).resolveHlsAsset(aip, fileName);
    }

    private Renderer renderer(AipMetadata aip) {
        Renderer r = registry.get(aip.kind());
        if (r == null) {
            throw new IllegalStateException("No renderer registered for kind " + aip.kind());
        }
        return r;
    }
}

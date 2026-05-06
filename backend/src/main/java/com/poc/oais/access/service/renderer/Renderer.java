package com.poc.oais.access.service.renderer;

import com.poc.oais.access.model.AipMetadata;
import com.poc.oais.access.model.DocumentKind;

import java.nio.file.Path;

public interface Renderer {
    DocumentKind supports();

    /**
     * Idempotent prepare — render derivative cache if not yet present.
     * Returns the page count for the AIP (1 for IMAGE/VIDEO).
     */
    int prepare(AipMetadata aip);

    /**
     * Resolve concrete page file (PNG) for PDF/OFFICE/IMAGE.
     * Throws UnsupportedOperationException for VIDEO.
     */
    default Path resolvePage(AipMetadata aip, int pageNumber) {
        throw new UnsupportedOperationException("Page resolution not supported for " + supports());
    }

    /**
     * Resolve concrete HLS asset (playlist or segment) for VIDEO.
     * Throws UnsupportedOperationException for non-VIDEO.
     */
    default Path resolveHlsAsset(AipMetadata aip, String fileName) {
        throw new UnsupportedOperationException("HLS asset not supported for " + supports());
    }
}

package com.poc.oais.access.model;

public record DipManifest(
        String id,
        String title,
        DocumentKind kind,
        String mimeType,
        int pageCount,
        String hlsUrl,
        int antiDownloadMode,
        boolean requiresPageToken
) {}

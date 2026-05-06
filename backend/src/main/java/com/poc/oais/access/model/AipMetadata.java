package com.poc.oais.access.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.nio.file.Path;
import java.time.Instant;

public record AipMetadata(
        String id,
        String title,
        DocumentKind kind,
        String mimeType,
        long sizeBytes,
        @JsonIgnore Path sourcePath,
        Instant ingestedAt
) {}

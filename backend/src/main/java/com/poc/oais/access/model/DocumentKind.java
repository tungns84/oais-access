package com.poc.oais.access.model;

import java.util.Locale;
import java.util.Optional;

public enum DocumentKind {
    PDF, OFFICE, IMAGE, VIDEO;

    public static Optional<DocumentKind> fromExtension(String ext) {
        if (ext == null) return Optional.empty();
        String e = ext.toLowerCase(Locale.ROOT);
        return switch (e) {
            case "pdf" -> Optional.of(PDF);
            case "docx", "xlsx", "pptx", "doc", "xls", "ppt" -> Optional.of(OFFICE);
            case "png", "jpg", "jpeg" -> Optional.of(IMAGE);
            case "mp4" -> Optional.of(VIDEO);
            default -> Optional.empty();
        };
    }
}

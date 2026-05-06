package com.poc.oais.access.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "oais")
public record AccessProperties(
        Storage storage,
        AntiDownload antiDownload,
        Watermark watermark,
        Rendering rendering,
        Audit audit,
        Cors cors
) {
    public record Storage(String archivalRoot, String derivedCache) {}

    public record AntiDownload(int mode, long pageTokenTtlSeconds) {}

    public record Watermark(String template, List<Integer> enabledForModes) {}

    public record Rendering(
            int pdfDpi,
            long officeConvertTimeoutSeconds,
            long ffmpegTimeoutSeconds,
            String libreofficeBinary,
            String ffmpegBinary
    ) {}

    public record Audit(String logPath) {}

    public record Cors(String allowedOrigins) {}
}

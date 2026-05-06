package com.poc.oais.access.service.renderer;

import com.poc.oais.access.config.AccessProperties;
import com.poc.oais.access.model.AipMetadata;
import com.poc.oais.access.model.DocumentKind;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfRendererTest {

    private PdfRenderer renderer;
    private AccessProperties props;

    @TempDir
    Path tmp;

    @BeforeEach
    void setUp() {
        props = new AccessProperties(
                new AccessProperties.Storage(
                        tmp.resolve("samples").toString(),
                        tmp.resolve("cache").toString()),
                new AccessProperties.AntiDownload(1, 30L),
                new AccessProperties.Watermark("{viewerId}", List.of(2, 3)),
                new AccessProperties.Rendering(72, 60L, 300L, "soffice", "ffmpeg"),
                new AccessProperties.Audit("audit.log"),
                new AccessProperties.Cors("*")
        );
        renderer = new PdfRenderer(props);
    }

    @Test
    void supportsPdf() {
        assertThat(renderer.supports()).isEqualTo(DocumentKind.PDF);
    }

    @Test
    void renderThreePagesProducesThreePngs() throws IOException {
        Path pdf = createBlankPdf(tmp.resolve("samples").resolve("three.pdf"), 3);
        AipMetadata aip = new AipMetadata(
                "abc123", "three.pdf", DocumentKind.PDF, "application/pdf",
                Files.size(pdf), pdf, Instant.now()
        );

        int pageCount = renderer.prepare(aip);

        assertThat(pageCount).isEqualTo(3);
        Path cacheDir = tmp.resolve("cache").resolve("abc123");
        assertThat(cacheDir.resolve(".ready")).exists();
        assertThat(cacheDir.resolve("manifest.properties")).exists();
        assertThat(cacheDir.resolve("page-1.png")).exists();
        assertThat(cacheDir.resolve("page-2.png")).exists();
        assertThat(cacheDir.resolve("page-3.png")).exists();
        assertThat(cacheDir.resolve("page-4.png")).doesNotExist();
    }

    @Test
    void prepareIsIdempotent() throws IOException {
        Path pdf = createBlankPdf(tmp.resolve("samples").resolve("idempotent.pdf"), 2);
        AipMetadata aip = new AipMetadata(
                "id-idempotent", "idempotent.pdf", DocumentKind.PDF, "application/pdf",
                Files.size(pdf), pdf, Instant.now()
        );

        int first = renderer.prepare(aip);
        long firstReadyMtime = Files.getLastModifiedTime(
                tmp.resolve("cache").resolve("id-idempotent").resolve(".ready")).toMillis();

        // Second call should be a no-op (cache hit)
        int second = renderer.prepare(aip);
        long secondReadyMtime = Files.getLastModifiedTime(
                tmp.resolve("cache").resolve("id-idempotent").resolve(".ready")).toMillis();

        assertThat(first).isEqualTo(second).isEqualTo(2);
        assertThat(secondReadyMtime).isEqualTo(firstReadyMtime);  // file not rewritten
    }

    @Test
    void resolvePageReturnsExistingPng() throws IOException {
        Path pdf = createBlankPdf(tmp.resolve("samples").resolve("resolve.pdf"), 2);
        AipMetadata aip = new AipMetadata(
                "id-resolve", "resolve.pdf", DocumentKind.PDF, "application/pdf",
                Files.size(pdf), pdf, Instant.now()
        );
        renderer.prepare(aip);

        Path page1 = renderer.resolvePage(aip, 1);
        assertThat(page1).exists().hasFileName("page-1.png");
    }

    @Test
    void resolvePageOutOfRangeThrows() throws IOException {
        Path pdf = createBlankPdf(tmp.resolve("samples").resolve("bounds.pdf"), 2);
        AipMetadata aip = new AipMetadata(
                "id-bounds", "bounds.pdf", DocumentKind.PDF, "application/pdf",
                Files.size(pdf), pdf, Instant.now()
        );
        renderer.prepare(aip);

        assertThatThrownBy(() -> renderer.resolvePage(aip, 99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page out of range");
    }

    @Test
    void resolveHlsThrowsUnsupported() throws IOException {
        Path pdf = createBlankPdf(tmp.resolve("samples").resolve("hls.pdf"), 1);
        AipMetadata aip = new AipMetadata(
                "id-hls", "hls.pdf", DocumentKind.PDF, "application/pdf",
                Files.size(pdf), pdf, Instant.now()
        );
        assertThatThrownBy(() -> renderer.resolveHlsAsset(aip, "master.m3u8"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static Path createBlankPdf(Path path, int pageCount) throws IOException {
        Files.createDirectories(path.getParent());
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                doc.addPage(new PDPage());
            }
            doc.save(path.toFile());
        }
        return path;
    }
}

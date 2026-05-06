package com.poc.oais.access.service.renderer;

import com.poc.oais.access.model.AipMetadata;
import com.poc.oais.access.model.DocumentKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageRendererTest {

    private final ImageRenderer renderer = new ImageRenderer();

    @Test
    void supportsImage() {
        assertThat(renderer.supports()).isEqualTo(DocumentKind.IMAGE);
    }

    @Test
    void prepareAlwaysReturnsOnePage(@TempDir Path tmp) {
        AipMetadata aip = new AipMetadata(
                "id-1", "test.png", DocumentKind.IMAGE, "image/png",
                100L, tmp.resolve("test.png"), Instant.now()
        );
        assertThat(renderer.prepare(aip)).isEqualTo(1);
    }

    @Test
    void resolvePageOneReturnsSourcePath(@TempDir Path tmp) {
        Path source = tmp.resolve("photo.png");
        AipMetadata aip = new AipMetadata(
                "id-1", "photo.png", DocumentKind.IMAGE, "image/png",
                100L, source, Instant.now()
        );
        assertThat(renderer.resolvePage(aip, 1)).isEqualTo(source);
    }

    @Test
    void resolvePageGreaterThanOneThrows(@TempDir Path tmp) {
        AipMetadata aip = new AipMetadata(
                "id-1", "photo.png", DocumentKind.IMAGE, "image/png",
                100L, tmp.resolve("photo.png"), Instant.now()
        );
        assertThatThrownBy(() -> renderer.resolvePage(aip, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only 1 page");
    }

    @Test
    void resolveHlsAssetThrowsUnsupported(@TempDir Path tmp) {
        AipMetadata aip = new AipMetadata(
                "id-1", "photo.png", DocumentKind.IMAGE, "image/png",
                100L, tmp.resolve("photo.png"), Instant.now()
        );
        assertThatThrownBy(() -> renderer.resolveHlsAsset(aip, "master.m3u8"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

package com.poc.oais.access.service;

import com.poc.oais.access.config.AccessProperties;
import com.poc.oais.access.model.DocumentKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ArchivalStorageServiceTest {

    @TempDir
    Path samplesDir;

    private ArchivalStorageService service;

    @BeforeEach
    void setUp() {
        AccessProperties props = new AccessProperties(
                new AccessProperties.Storage(samplesDir.toString(), samplesDir.resolve("cache").toString()),
                new AccessProperties.AntiDownload(1, 30L),
                new AccessProperties.Watermark("{viewerId}", List.of(2, 3)),
                new AccessProperties.Rendering(72, 60L, 300L, "soffice", "ffmpeg"),
                new AccessProperties.Audit("audit.log"),
                new AccessProperties.Cors("*")
        );
        service = new ArchivalStorageService(props);
    }

    @Test
    void scanIndexesSupportedFiles() throws IOException {
        Files.writeString(samplesDir.resolve("doc.pdf"), "fake pdf");
        Files.writeString(samplesDir.resolve("photo.png"), "fake png");
        Files.writeString(samplesDir.resolve("notes.txt"), "should be skipped");

        service.scan();

        List<com.poc.oais.access.model.AipMetadata> all = service.listAll();
        assertThat(all).hasSize(2);
        assertThat(all).extracting("title").containsExactlyInAnyOrder("doc.pdf", "photo.png");
        assertThat(all).extracting("kind")
                .containsExactlyInAnyOrder(DocumentKind.PDF, DocumentKind.IMAGE);
    }

    @Test
    void skipsDotFilesAndReadme() throws IOException {
        Files.writeString(samplesDir.resolve(".hidden.pdf"), "x");
        Files.writeString(samplesDir.resolve("README.txt"), "x");
        Files.writeString(samplesDir.resolve("real.pdf"), "x");

        service.scan();

        assertThat(service.listAll()).hasSize(1);
        assertThat(service.listAll().get(0).title()).isEqualTo("real.pdf");
    }

    @Test
    void findByIdReturnsRegisteredAip() throws IOException {
        Files.writeString(samplesDir.resolve("findme.pdf"), "x");
        service.scan();

        String id = service.listAll().get(0).id();
        Optional<com.poc.oais.access.model.AipMetadata> found = service.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().title()).isEqualTo("findme.pdf");
    }

    @Test
    void findByIdReturnsEmptyForUnknown() {
        service.scan();
        assertThat(service.findById("does-not-exist")).isEmpty();
    }

    @Test
    void rescanClearsRemovedFiles() throws IOException {
        Path file = samplesDir.resolve("ephemeral.png");
        Files.writeString(file, "x");
        service.scan();
        assertThat(service.listAll()).hasSize(1);

        Files.delete(file);
        int count = service.rescan();

        assertThat(count).isEqualTo(0);
        assertThat(service.listAll()).isEmpty();
    }

    @Test
    void rescanPicksUpNewFiles() throws IOException {
        service.scan();
        assertThat(service.listAll()).isEmpty();

        Files.writeString(samplesDir.resolve("new.pdf"), "x");
        int count = service.rescan();

        assertThat(count).isEqualTo(1);
        assertThat(service.listAll()).hasSize(1);
        assertThat(service.listAll().get(0).title()).isEqualTo("new.pdf");
    }

    @Test
    void scanHandlesMissingArchivalRoot() {
        AccessProperties bad = new AccessProperties(
                new AccessProperties.Storage("/non-existent-path-xyz", "cache"),
                new AccessProperties.AntiDownload(1, 30L),
                new AccessProperties.Watermark("{viewerId}", List.of(2, 3)),
                new AccessProperties.Rendering(72, 60L, 300L, "soffice", "ffmpeg"),
                new AccessProperties.Audit("audit.log"),
                new AccessProperties.Cors("*")
        );
        ArchivalStorageService svc = new ArchivalStorageService(bad);

        // Should not throw, just log warning
        svc.scan();
        assertThat(svc.listAll()).isEmpty();
    }

    @Test
    void differentFilenamesProduceDifferentIds() throws IOException {
        Files.writeString(samplesDir.resolve("a.pdf"), "x");
        Files.writeString(samplesDir.resolve("b.pdf"), "x");
        service.scan();

        List<String> ids = service.listAll().stream().map(m -> m.id()).toList();
        assertThat(ids).hasSize(2).doesNotHaveDuplicates();
    }
}

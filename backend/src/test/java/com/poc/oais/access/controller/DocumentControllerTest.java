package com.poc.oais.access.controller;

import com.poc.oais.access.config.AppConfig;
import com.poc.oais.access.config.WebConfig;
import com.poc.oais.access.model.AipMetadata;
import com.poc.oais.access.model.DipManifest;
import com.poc.oais.access.model.DocumentKind;
import com.poc.oais.access.service.ArchivalStorageService;
import com.poc.oais.access.service.DipGeneratorService;
import com.poc.oais.access.service.ModeResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DocumentController.class)
@Import({GlobalExceptionHandler.class, AppConfig.class, WebConfig.class})
class DocumentControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    ArchivalStorageService storage;

    @MockBean
    DipGeneratorService dipGenerator;

    @MockBean
    ModeResolver modeResolver;

    @Test
    void listReturnsManifests() throws Exception {
        AipMetadata aip = sampleAip("abc123", "doc.pdf");
        DipManifest manifest = new DipManifest(
                "abc123", "doc.pdf", DocumentKind.PDF, "application/pdf", 5, null, 1, false);

        when(modeResolver.effectiveMode(any())).thenReturn(1);
        when(storage.listAll()).thenReturn(List.of(aip));
        when(dipGenerator.manifest(aip, 1)).thenReturn(manifest);

        mvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("abc123"))
                .andExpect(jsonPath("$[0].title").value("doc.pdf"))
                .andExpect(jsonPath("$[0].kind").value("PDF"))
                .andExpect(jsonPath("$[0].pageCount").value(5))
                .andExpect(jsonPath("$[0].antiDownloadMode").value(1));
    }

    @Test
    void manifestReturnsForExistingAip() throws Exception {
        AipMetadata aip = sampleAip("xyz", "video.mp4");
        DipManifest m = new DipManifest(
                "xyz", "video.mp4", DocumentKind.VIDEO, "video/mp4", 1,
                "/api/dip/xyz/hls/master.m3u8", 3, true);

        when(modeResolver.effectiveMode(any())).thenReturn(3);
        when(storage.findById("xyz")).thenReturn(Optional.of(aip));
        when(dipGenerator.manifest(aip, 3)).thenReturn(m);

        mvc.perform(get("/api/documents/xyz/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("VIDEO"))
                .andExpect(jsonPath("$.hlsUrl").value("/api/dip/xyz/hls/master.m3u8"))
                .andExpect(jsonPath("$.requiresPageToken").value(true));
    }

    @Test
    void manifestReturns404ForUnknownAip() throws Exception {
        when(modeResolver.effectiveMode(any())).thenReturn(1);
        when(storage.findById(any())).thenReturn(Optional.empty());

        mvc.perform(get("/api/documents/unknown/manifest"))
                .andExpect(status().isNotFound());
    }

    @Test
    void metadataReturnsAip() throws Exception {
        AipMetadata aip = sampleAip("a1", "img.png");
        when(storage.findById("a1")).thenReturn(Optional.of(aip));

        mvc.perform(get("/api/documents/a1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("a1"))
                .andExpect(jsonPath("$.title").value("img.png"));
    }

    @Test
    void metadataReturns404ForUnknown() throws Exception {
        when(storage.findById(any())).thenReturn(Optional.empty());
        mvc.perform(get("/api/documents/missing"))
                .andExpect(status().isNotFound());
    }

    private static AipMetadata sampleAip(String id, String title) {
        DocumentKind kind = title.endsWith(".pdf") ? DocumentKind.PDF
                : title.endsWith(".mp4") ? DocumentKind.VIDEO
                : DocumentKind.IMAGE;
        return new AipMetadata(id, title, kind, "application/octet-stream",
                100L, Paths.get("/tmp/" + title), Instant.now());
    }
}

package com.poc.oais.access.controller;

import com.poc.oais.access.config.AppConfig;
import com.poc.oais.access.config.WebConfig;
import com.poc.oais.access.model.AipMetadata;
import com.poc.oais.access.model.DocumentKind;
import com.poc.oais.access.service.AccessAuditService;
import com.poc.oais.access.service.ArchivalStorageService;
import com.poc.oais.access.service.DipGeneratorService;
import com.poc.oais.access.service.ModeResolver;
import com.poc.oais.access.service.TokenService;
import com.poc.oais.access.service.WatermarkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DipController.class)
@Import({GlobalExceptionHandler.class, AppConfig.class, WebConfig.class})
class DipControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean ArchivalStorageService storage;
    @MockBean DipGeneratorService dipGenerator;
    @MockBean TokenService tokenService;
    @MockBean WatermarkService watermarkService;
    @MockBean AccessAuditService auditService;
    @MockBean ModeResolver modeResolver;

    @TempDir
    Path tmp;

    @Test
    void issuePageTokenReturnsToken() throws Exception {
        AipMetadata aip = pdfAip("a1");
        when(storage.findById("a1")).thenReturn(Optional.of(aip));
        when(tokenService.issue(eq("a1"), eq(3), anyString(), any())).thenReturn("test-token-xyz");

        mvc.perform(get("/api/dip/a1/page-token/3").header("X-Viewer-Id", "viewer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token-xyz"))
                .andExpect(jsonPath("$.expiresInSeconds").value(30));
    }

    @Test
    void issuePageTokenReturns404ForUnknownAip() throws Exception {
        when(storage.findById(any())).thenReturn(Optional.empty());

        mvc.perform(get("/api/dip/unknown/page-token/1").header("X-Viewer-Id", "viewer-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void servePageMode1ReturnsImageWithAntiDownloadHeaders() throws Exception {
        AipMetadata aip = pdfAip("a1");
        Path png = createTinyPng(tmp.resolve("page-1.png"));

        when(storage.findById("a1")).thenReturn(Optional.of(aip));
        when(modeResolver.effectiveMode(any())).thenReturn(1);
        when(dipGenerator.servePage(aip, 1)).thenReturn(png);
        when(watermarkService.shouldWatermark(1)).thenReturn(false);

        mvc.perform(get("/api/dip/a1/page/1").header("X-Viewer-Id", "viewer-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"\""))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Cross-Origin-Resource-Policy", "same-origin"))
                .andExpect(header().string("X-Anti-Download-Mode", "1"));
    }

    @Test
    void servePageMode3WithoutTokenReturns403() throws Exception {
        AipMetadata aip = pdfAip("a1");
        when(storage.findById("a1")).thenReturn(Optional.of(aip));
        when(modeResolver.effectiveMode(any())).thenReturn(3);
        doThrow(new TokenService.TokenException("TOKEN_MISSING"))
                .when(tokenService).requireValid(any(), eq("a1"), anyInt(), anyString());

        mvc.perform(get("/api/dip/a1/page/1").header("X-Viewer-Id", "viewer-1"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("WWW-Authenticate", "PageToken"))
                .andExpect(jsonPath("$.error").value("TOKEN_MISSING"));
    }

    @Test
    void servePageRejectsVideoAip() throws Exception {
        AipMetadata aip = videoAip("v1");
        when(storage.findById("v1")).thenReturn(Optional.of(aip));
        when(modeResolver.effectiveMode(any())).thenReturn(1);

        mvc.perform(get("/api/dip/v1/page/1").header("X-Viewer-Id", "viewer-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void serveHlsMasterReturnsPlaylist() throws Exception {
        AipMetadata aip = videoAip("v1");
        Path master = Files.writeString(tmp.resolve("master.m3u8"), "#EXTM3U\n");

        when(storage.findById("v1")).thenReturn(Optional.of(aip));
        when(modeResolver.effectiveMode(any())).thenReturn(1);
        when(dipGenerator.serveHlsAsset(aip, "master.m3u8")).thenReturn(master);

        mvc.perform(get("/api/dip/v1/hls/master.m3u8").header("X-Viewer-Id", "viewer-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.apple.mpegurl"));
    }

    @Test
    void serveHlsRejectsInvalidFilename() throws Exception {
        AipMetadata aip = videoAip("v1");
        when(storage.findById("v1")).thenReturn(Optional.of(aip));
        when(modeResolver.effectiveMode(any())).thenReturn(1);

        mvc.perform(get("/api/dip/v1/hls/..%2Fetc%2Fpasswd").header("X-Viewer-Id", "viewer-1"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/dip/v1/hls/random.txt").header("X-Viewer-Id", "viewer-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void serveHlsRejectsNonVideoAip() throws Exception {
        AipMetadata aip = pdfAip("p1");
        when(storage.findById("p1")).thenReturn(Optional.of(aip));
        when(modeResolver.effectiveMode(any())).thenReturn(1);

        mvc.perform(get("/api/dip/p1/hls/master.m3u8").header("X-Viewer-Id", "viewer-1"))
                .andExpect(status().isBadRequest());
    }

    private static AipMetadata pdfAip(String id) {
        return new AipMetadata(id, id + ".pdf", DocumentKind.PDF, "application/pdf",
                100L, java.nio.file.Paths.get("/tmp/" + id), Instant.now());
    }

    private static AipMetadata videoAip(String id) {
        return new AipMetadata(id, id + ".mp4", DocumentKind.VIDEO, "video/mp4",
                100L, java.nio.file.Paths.get("/tmp/" + id), Instant.now());
    }

    private static Path createTinyPng(Path target) throws IOException {
        // Minimal valid PNG — 1x1 transparent
        byte[] png = {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
                (byte) 0x89, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x44, 0x41,
                0x54, 0x78, (byte) 0x9C, 0x62, 0x00, 0x01, 0x00, 0x00,
                0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
                0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
                0x42, 0x60, (byte) 0x82
        };
        Files.write(target, png);
        return target;
    }

    // mockito eq() shortcut
    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}

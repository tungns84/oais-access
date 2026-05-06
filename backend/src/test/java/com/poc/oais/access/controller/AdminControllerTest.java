package com.poc.oais.access.controller;

import com.poc.oais.access.config.AppConfig;
import com.poc.oais.access.config.WebConfig;
import com.poc.oais.access.service.ArchivalStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
@Import({AppConfig.class, WebConfig.class})
class AdminControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    ArchivalStorageService storage;

    @Test
    void rescanReturnsCount() throws Exception {
        when(storage.rescan()).thenReturn(5);

        mvc.perform(post("/api/admin/rescan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexedCount").value(5))
                .andExpect(jsonPath("$.elapsedMs").exists())
                .andExpect(jsonPath("$.scannedAt").exists());

        verify(storage).rescan();
    }

    @Test
    void rescanReturnsZeroWhenEmpty() throws Exception {
        when(storage.rescan()).thenReturn(0);

        mvc.perform(post("/api/admin/rescan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexedCount").value(0));
    }
}

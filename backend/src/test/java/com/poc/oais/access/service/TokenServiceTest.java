package com.poc.oais.access.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private final TokenService svc = new TokenService();

    @Test
    void issueAndValidate() {
        String token = svc.issue("aip-1", 3, "viewer-x", Duration.ofMinutes(1));
        svc.requireValid(token, "aip-1", 3, "viewer-x");
    }

    @Test
    void rejectsAipMismatch() {
        String token = svc.issue("aip-1", 3, "viewer-x", Duration.ofMinutes(1));
        assertThatThrownBy(() -> svc.requireValid(token, "aip-2", 3, "viewer-x"))
                .hasMessageContaining("TOKEN_AIP_MISMATCH");
    }

    @Test
    void rejectsPageMismatch() {
        String token = svc.issue("aip-1", 3, "viewer-x", Duration.ofMinutes(1));
        assertThatThrownBy(() -> svc.requireValid(token, "aip-1", 4, "viewer-x"))
                .hasMessageContaining("TOKEN_PAGE_MISMATCH");
    }

    @Test
    void rejectsViewerMismatch() {
        String token = svc.issue("aip-1", 3, "viewer-x", Duration.ofMinutes(1));
        assertThatThrownBy(() -> svc.requireValid(token, "aip-1", 3, "viewer-y"))
                .hasMessageContaining("TOKEN_VIEWER_MISMATCH");
    }

    @Test
    void rejectsExpired() throws InterruptedException {
        String token = svc.issue("aip-1", 3, "viewer-x", Duration.ofSeconds(-1));
        assertThatThrownBy(() -> svc.requireValid(token, "aip-1", 3, "viewer-x"))
                .hasMessageContaining("TOKEN_EXPIRED");
    }

    @Test
    void rejectsTampered() {
        String token = svc.issue("aip-1", 3, "viewer-x", Duration.ofMinutes(1));
        // Flip a few characters
        String tampered = token.substring(0, token.length() - 4) + "AAAA";
        assertThatThrownBy(() -> svc.requireValid(tampered, "aip-1", 3, "viewer-x"))
                .isInstanceOf(TokenService.TokenException.class);
    }

    @Test
    void rejectsMissingToken() {
        assertThat(true).isTrue();
        assertThatThrownBy(() -> svc.requireValid(null, "aip-1", 3, "viewer-x"))
                .hasMessageContaining("TOKEN_MISSING");
    }
}

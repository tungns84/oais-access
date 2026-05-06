package com.poc.oais.access.service;

import com.poc.oais.access.config.AccessProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModeResolverTest {

    private ModeResolver resolver;

    @BeforeEach
    void setUp() {
        AccessProperties props = new AccessProperties(
                new AccessProperties.Storage("samples", "cache"),
                new AccessProperties.AntiDownload(1, 30L),
                new AccessProperties.Watermark("{viewerId}", List.of(2, 3)),
                new AccessProperties.Rendering(150, 60L, 300L, "soffice", "ffmpeg"),
                new AccessProperties.Audit("audit.log"),
                new AccessProperties.Cors("http://localhost:5173")
        );
        resolver = new ModeResolver(props);
    }

    @Test
    void returnsConfigDefaultWhenNoOverride() {
        HttpServletRequest req = new MockHttpServletRequest();
        assertThat(resolver.effectiveMode(req)).isEqualTo(1);
    }

    @Test
    void honorsHeaderOverride() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Anti-Download-Mode", "3");
        assertThat(resolver.effectiveMode(req)).isEqualTo(3);
    }

    @Test
    void honorsQueryParamOverride() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("m", "2");
        assertThat(resolver.effectiveMode(req)).isEqualTo(2);
    }

    @Test
    void queryParamWinsOverHeader() {
        // Asset URLs (img/video) cannot send custom headers — query param must win
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("m", "1");
        req.addHeader("X-Anti-Download-Mode", "3");
        assertThat(resolver.effectiveMode(req)).isEqualTo(1);
    }

    @Test
    void invalidValuesFallBackToConfig() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Anti-Download-Mode", "abc");
        req.setParameter("m", "99");
        assertThat(resolver.effectiveMode(req)).isEqualTo(1);
    }

    @Test
    void rangeOnlyOneToThree() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Anti-Download-Mode", "0");
        assertThat(resolver.effectiveMode(req)).isEqualTo(1);

        req = new MockHttpServletRequest();
        req.addHeader("X-Anti-Download-Mode", "4");
        assertThat(resolver.effectiveMode(req)).isEqualTo(1);
    }

    @Test
    void nullRequestReturnsConfigDefault() {
        assertThat(resolver.effectiveMode(null)).isEqualTo(1);
    }
}

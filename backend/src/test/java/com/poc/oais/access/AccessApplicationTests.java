package com.poc.oais.access;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccessApplicationTests {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void healthEndpointReturnsOk() {
        @SuppressWarnings("unchecked")
        var body = restTemplate.getForObject("http://localhost:" + port + "/api/health", java.util.Map.class);
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("ok");
    }

    @Test
    void documentsEndpointReturnsList() {
        var body = restTemplate.getForObject("http://localhost:" + port + "/api/documents", Object[].class);
        assertThat(body).isNotNull();
    }
}

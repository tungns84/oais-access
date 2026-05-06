package com.poc.oais.access.controller;

import com.poc.oais.access.config.AccessProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final AccessProperties props;

    public HealthController(AccessProperties props) {
        this.props = props;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "antiDownloadMode", props.antiDownload().mode(),
                "archivalRoot", props.storage().archivalRoot()
        );
    }
}

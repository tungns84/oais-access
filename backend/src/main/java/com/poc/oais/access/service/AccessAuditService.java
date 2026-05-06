package com.poc.oais.access.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.oais.access.config.AccessProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AccessAuditService {

    private static final Logger log = LoggerFactory.getLogger(AccessAuditService.class);

    private final AccessProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    public AccessAuditService(AccessProperties props) {
        this.props = props;
    }

    @Async
    public void recordView(String aipId, String viewerId, String event, Map<String, Object> details) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("ts", Instant.now().toString());
        entry.put("aipId", aipId);
        entry.put("viewerId", viewerId);
        entry.put("event", event);
        entry.put("details", details);

        try {
            String json = mapper.writeValueAsString(entry);
            Path logPath = Paths.get(props.audit().logPath()).toAbsolutePath();
            Files.createDirectories(logPath.getParent());
            Files.writeString(
                    logPath, json + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND
            );
        } catch (JsonProcessingException e) {
            log.warn("Audit serialization failed: {}", e.getMessage());
        } catch (IOException e) {
            log.warn("Audit write failed: {}", e.getMessage());
        }
    }
}

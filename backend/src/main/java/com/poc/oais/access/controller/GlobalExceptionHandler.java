package com.poc.oais.access.controller;

import com.poc.oais.access.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        String code = e.getMessage();
        HttpStatus status = "AIP_NOT_FOUND".equals(code) || "Page out of range".equals(code)
                ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Map.of("error", code != null ? code : "BAD_REQUEST"));
    }

    @ExceptionHandler(TokenService.TokenException.class)
    public ResponseEntity<Map<String, Object>> tokenError(TokenService.TokenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header("WWW-Authenticate", "PageToken")
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> generic(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "INTERNAL", "message", e.getMessage()));
    }
}

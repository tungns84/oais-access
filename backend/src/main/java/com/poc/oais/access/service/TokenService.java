package com.poc.oais.access.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final String ALG = "HmacSHA256";

    private final byte[] hmacKey;

    public TokenService() {
        this.hmacKey = new byte[32];
        new SecureRandom().nextBytes(this.hmacKey);
        log.warn("TokenService HMAC key generated at startup — restart will invalidate existing tokens (POC behavior)");
    }

    public String issue(String aipId, int page, String viewerId, Duration ttl) {
        long expires = Instant.now().plus(ttl).getEpochSecond();
        String payload = aipId + "|" + page + "|" + viewerId + "|" + expires;
        String mac = sign(payload);
        String body = payload + "|" + mac;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(body.getBytes(StandardCharsets.UTF_8));
    }

    public void requireValid(String token, String expectedAipId, int expectedPage, String expectedViewerId) {
        if (token == null || token.isBlank()) {
            throw new TokenException("TOKEN_MISSING");
        }
        String decoded;
        try {
            decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new TokenException("TOKEN_MALFORMED");
        }
        String[] parts = decoded.split("\\|");
        if (parts.length != 5) throw new TokenException("TOKEN_MALFORMED");
        String aipId = parts[0];
        String pageStr = parts[1];
        String viewerId = parts[2];
        String expiresStr = parts[3];
        String mac = parts[4];

        String payload = aipId + "|" + pageStr + "|" + viewerId + "|" + expiresStr;
        String expectedMac = sign(payload);
        if (!MessageDigest.isEqual(
                expectedMac.getBytes(StandardCharsets.UTF_8),
                mac.getBytes(StandardCharsets.UTF_8))) {
            throw new TokenException("TOKEN_SIGNATURE_INVALID");
        }
        if (!aipId.equals(expectedAipId)) throw new TokenException("TOKEN_AIP_MISMATCH");
        if (Integer.parseInt(pageStr) != expectedPage) throw new TokenException("TOKEN_PAGE_MISMATCH");
        if (expectedViewerId != null && !viewerId.equals(expectedViewerId)) {
            throw new TokenException("TOKEN_VIEWER_MISMATCH");
        }
        long expires = Long.parseLong(expiresStr);
        if (Instant.now().getEpochSecond() > expires) {
            throw new TokenException("TOKEN_EXPIRED");
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(ALG);
            mac.init(new SecretKeySpec(hmacKey, ALG));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC signing failed", e);
        }
    }

    public static class TokenException extends RuntimeException {
        public TokenException(String message) {
            super(message);
        }
    }
}

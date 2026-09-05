package com.gaokao.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaokao.config.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenService {
    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final String ISSUER = "gaokao-simulator";

    private final ObjectMapper objectMapper;
    private final byte[] secret;

    public JwtTokenService(ObjectMapper objectMapper, SecurityProperties properties) {
        this.objectMapper = objectMapper;
        String configuredSecret = properties.getJwtSecret();
        if (configuredSecret == null || configuredSecret.isBlank()) {
            this.secret = new byte[32];
            new SecureRandom().nextBytes(this.secret);
            log.warn("GAOKAO_JWT_SECRET未配置，当前使用一次性密钥，应用重启后已有会话将失效");
            return;
        }
        if (configuredSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("GAOKAO_JWT_SECRET必须至少32字节");
        }
        this.secret = configuredSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(UUID sessionId, String username, String role, Instant issuedAt, Instant expiresAt) {
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", ISSUER);
        claims.put("sub", username);
        claims.put("role", role);
        claims.put("sid", sessionId.toString());
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());
        String content = encode(header) + "." + encode(claims);
        return content + "." + ENCODER.encodeToString(sign(content));
    }

    public ParsedToken parseAndVerify(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) {
                throw invalidToken();
            }
            String content = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(content), DECODER.decode(parts[2]))) {
                throw invalidToken();
            }
            Map<String, Object> header = decode(parts[0]);
            Map<String, Object> claims = decode(parts[1]);
            if (!"HS256".equals(header.get("alg")) || !ISSUER.equals(claims.get("iss"))) {
                throw invalidToken();
            }
            Instant expiresAt = Instant.ofEpochSecond(number(claims, "exp"));
            if (!expiresAt.isAfter(Instant.now())) {
                throw invalidToken();
            }
            return new ParsedToken(
                    UUID.fromString(text(claims, "sid")),
                    text(claims, "sub"),
                    text(claims, "role"),
                    expiresAt);
        } catch (IllegalArgumentException | IOException e) {
            throw invalidToken();
        }
    }

    public String hash(String token) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256不可用", e);
        }
    }

    private String encode(Map<String, Object> value) {
        try {
            return ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JWT序列化失败", e);
        }
    }

    private Map<String, Object> decode(String value) throws IOException {
        return objectMapper.readValue(DECODER.decode(value), new TypeReference<>() { });
    }

    private byte[] sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(content.getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("JWT签名失败", e);
        }
    }

    private String text(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (!(value instanceof String text) || text.isBlank()) {
            throw invalidToken();
        }
        return text;
    }

    private long number(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (!(value instanceof Number number)) {
            throw invalidToken();
        }
        return number.longValue();
    }

    private SecurityException invalidToken() {
        return new SecurityException("登录凭证无效或已过期");
    }

    public record ParsedToken(UUID sessionId, String username, String role, Instant expiresAt) {
    }
}

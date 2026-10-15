package io.github.mahdiarharandi.articles.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mahdiarharandi.articles.domain.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final String issuer;
    private final Duration lifetime;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.lifetime-minutes}") long lifetimeMinutes
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) throw new IllegalStateException("JWT_SECRET must be at least 32 bytes.");
        this.issuer = issuer;
        this.lifetime = Duration.ofMinutes(lifetimeMinutes);
    }

    public IssuedToken issue(AppUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(lifetime);
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", issuer);
        claims.put("sub", user.getUsername());
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());
        claims.put("userId", user.getId());
        claims.put("ver", user.getTokenVersion());
        claims.put("typ", "access");
        try {
            String encodedHeader = encodeJson(header);
            String encodedClaims = encodeJson(claims);
            String signingInput = encodedHeader + "." + encodedClaims;
            String signature = URL_ENCODER.encodeToString(sign(signingInput.getBytes(StandardCharsets.US_ASCII)));
            return new IssuedToken(signingInput + "." + signature, expiresAt);
        } catch (JsonProcessingException | GeneralSecurityException exception) {
            throw new IllegalStateException("Could not create JWT.", exception);
        }
    }

    public VerifiedToken verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new InvalidJwtException();
            byte[] expected = sign((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            byte[] actual = URL_DECODER.decode(parts[2]);
            // Reject non-canonical Base64URL encodings so any textual token modification is detected.
            if (!parts[2].equals(URL_ENCODER.encodeToString(actual))
                    || !MessageDigest.isEqual(expected, actual)) {
                throw new InvalidJwtException();
            }

            JsonNode header = objectMapper.readTree(URL_DECODER.decode(parts[0]));
            JsonNode claims = objectMapper.readTree(URL_DECODER.decode(parts[1]));
            if (!"HS256".equals(header.path("alg").asText()) || !"JWT".equals(header.path("typ").asText())) {
                throw new InvalidJwtException();
            }
            if (!issuer.equals(claims.path("iss").asText()) || !"access".equals(claims.path("typ").asText())) {
                throw new InvalidJwtException();
            }
            long now = Instant.now().getEpochSecond();
            long exp = requiredLong(claims, "exp");
            long iat = requiredLong(claims, "iat");
            long userId = requiredLong(claims, "userId");
            long version = requiredLong(claims, "ver");
            String subject = claims.path("sub").asText("");
            if (exp <= now || iat > now + 60 || userId <= 0 || version < 0 || subject.isBlank()) {
                throw new InvalidJwtException();
            }
            return new VerifiedToken(userId, subject, version, Instant.ofEpochSecond(exp));
        } catch (IllegalArgumentException | java.io.IOException | GeneralSecurityException exception) {
            throw new InvalidJwtException();
        }
    }

    private String encodeJson(Object value) throws JsonProcessingException {
        return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private byte[] sign(byte[] value) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
        return mac.doFinal(value);
    }

    private long requiredLong(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.canConvertToLong()) throw new InvalidJwtException();
        return value.asLong();
    }

    public record IssuedToken(String value, Instant expiresAt) {}
    public record VerifiedToken(long userId, String subject, long version, Instant expiresAt) {}
    public static class InvalidJwtException extends RuntimeException {}
}

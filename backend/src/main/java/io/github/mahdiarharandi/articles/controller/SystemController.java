package io.github.mahdiarharandi.articles.controller;

import io.github.mahdiarharandi.articles.dto.HealthResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SystemController {
    @GetMapping(value = "/api/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public HealthResponse health() {
        return new HealthResponse("UP", "article-sharing-api", LocalDateTime.now(ZoneOffset.UTC));
    }

    @GetMapping(value = {"/", "/api"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> apiIndex() {
        Map<String, Object> docs = new LinkedHashMap<>();
        docs.put("name", "Article Sharing Platform API");
        docs.put("security", Map.of(
                "authentication", "JWT HS256",
                "passwordStorage", "PBKDF2-HMAC-SHA256 with per-password random salt",
                "authorization", "authenticated routes and article ownership"
        ));
        docs.put("endpoints", List.of(
                Map.of("method", "POST", "path", "/api/auth/register", "access", "public"),
                Map.of("method", "POST", "path", "/api/auth/login", "access", "public"),
                Map.of("method", "POST", "path", "/api/auth/logout", "access", "authenticated"),
                Map.of("method", "GET", "path", "/api/articles", "access", "public"),
                Map.of("method", "GET", "path", "/api/articles/{id}", "access", "authenticated"),
                Map.of("method", "POST", "path", "/api/articles", "access", "authenticated"),
                Map.of("method", "GET", "path", "/api/profile", "access", "authenticated"),
                Map.of("method", "PUT", "path", "/api/profile/contact", "access", "authenticated"),
                Map.of("method", "PUT", "path", "/api/profile/password", "access", "authenticated")
        ));
        return docs;
    }
}

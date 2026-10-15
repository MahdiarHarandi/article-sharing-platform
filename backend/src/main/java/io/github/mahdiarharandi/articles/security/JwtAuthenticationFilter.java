package io.github.mahdiarharandi.articles.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mahdiarharandi.articles.dto.ApiErrorResponse;
import io.github.mahdiarharandi.articles.repository.UserRepository;
import io.github.mahdiarharandi.articles.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String AUTHENTICATED_USER_ID = JwtAuthenticationFilter.class.getName() + ".userId";
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String token = bearerToken(request);
        if (token == null) {
            if (isPublic(request)) {
                chain.doFilter(request, response);
            } else {
                unauthorized(response, "A Bearer JWT is required to access this resource.");
            }
            return;
        }

        try {
            JwtService.VerifiedToken verified = jwtService.verify(token);
            boolean validUser = userRepository.findById(verified.userId())
                    .filter(user -> user.getTokenVersion() == verified.version())
                    .filter(user -> user.getUsername().equals(verified.subject()))
                    .isPresent();
            if (!validUser) {
                unauthorized(response, "The JWT has been invalidated or its user no longer exists.");
                return;
            }
            request.setAttribute(AUTHENTICATED_USER_ID, verified.userId());
            chain.doFilter(request, response);
        } catch (JwtService.InvalidJwtException exception) {
            unauthorized(response, "The Bearer JWT is invalid or expired.");
        }
    }

    private boolean isPublic(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("/api/health".equals(path) || "/api/health/".equals(path) || "/error".equals(path)) return true;
        if ("/api/auth/register".equals(path) || "/api/auth/register/".equals(path)
                || "/api/auth/login".equals(path) || "/api/auth/login/".equals(path)) return true;
        return HttpMethod.GET.matches(request.getMethod())
                && ("/api/articles".equals(path) || "/api/articles/".equals(path));
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) return null;
        if (!authorization.regionMatches(true, 0, "Bearer ", 0, 7)) return "";
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? "" : token;
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                "UNAUTHORIZED", message, 401, LocalDateTime.now(ZoneOffset.UTC)
        ));
    }
}

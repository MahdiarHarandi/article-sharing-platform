package io.github.mahdiarharandi.articles.service;

import io.github.mahdiarharandi.articles.domain.AppUser;
import io.github.mahdiarharandi.articles.exception.UserNotFoundException;
import io.github.mahdiarharandi.articles.repository.UserRepository;
import io.github.mahdiarharandi.articles.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {
    private final UserRepository userRepository;
    private final HttpServletRequest request;

    public CurrentUserService(UserRepository userRepository, HttpServletRequest request) {
        this.userRepository = userRepository;
        this.request = request;
    }

    public AppUser requireCurrentUser() {
        return currentUser().orElseThrow(UserNotFoundException::new);
    }

    public Optional<AppUser> currentUser() {
        Object value = request.getAttribute(JwtAuthenticationFilter.AUTHENTICATED_USER_ID);
        if (!(value instanceof Long userId)) return Optional.empty();
        return userRepository.findById(userId);
    }

    public Long currentUserIdOrNull() {
        Object value = request.getAttribute(JwtAuthenticationFilter.AUTHENTICATED_USER_ID);
        return value instanceof Long id ? id : null;
    }
}

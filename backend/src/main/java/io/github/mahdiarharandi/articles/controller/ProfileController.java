package io.github.mahdiarharandi.articles.controller;

import io.github.mahdiarharandi.articles.domain.AppUser;
import io.github.mahdiarharandi.articles.dto.AuthResponse;
import io.github.mahdiarharandi.articles.dto.ChangePasswordRequest;
import io.github.mahdiarharandi.articles.dto.ProfileResponse;
import io.github.mahdiarharandi.articles.dto.UpdateContactRequest;
import io.github.mahdiarharandi.articles.dto.UserResponse;
import io.github.mahdiarharandi.articles.service.ArticleService;
import io.github.mahdiarharandi.articles.service.AuthService;
import io.github.mahdiarharandi.articles.service.CurrentUserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final ArticleService articleService;

    public ProfileController(AuthService authService, CurrentUserService currentUserService, ArticleService articleService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
        this.articleService = articleService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ProfileResponse profile() {
        AppUser user = currentUserService.requireCurrentUser();
        var articles = articleService.listPublishedBy(user);
        return new ProfileResponse(authService.toUserResponse(user), articles, articles.size());
    }

    @PutMapping(value = "/contact", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserResponse updateContact(@RequestBody UpdateContactRequest request) {
        return authService.updateContact(request);
    }

    @PutMapping(value = "/password", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AuthResponse changePassword(@RequestBody ChangePasswordRequest request) {
        return authService.changePassword(request);
    }
}

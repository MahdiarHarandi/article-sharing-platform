package io.github.mahdiarharandi.articles.controller;

import io.github.mahdiarharandi.articles.domain.AppUser;
import io.github.mahdiarharandi.articles.dto.ArticleDetailResponse;
import io.github.mahdiarharandi.articles.dto.ArticleListResponse;
import io.github.mahdiarharandi.articles.dto.ArticleRelationListResponse;
import io.github.mahdiarharandi.articles.dto.ArticleRequest;
import io.github.mahdiarharandi.articles.service.ArticleService;
import io.github.mahdiarharandi.articles.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    private final ArticleService articleService;
    private final CurrentUserService currentUserService;

    public ArticleController(ArticleService articleService, CurrentUserService currentUserService) {
        this.articleService = articleService;
        this.currentUserService = currentUserService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ArticleListResponse listArticles(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "q", required = false) String alternativeSearch,
            @RequestParam(name = "sort", defaultValue = "newest") String sort
    ) {
        String effectiveSearch = search != null ? search : alternativeSearch;
        return articleService.listArticles(effectiveSearch, sort, currentUserService.currentUserIdOrNull());
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ArticleDetailResponse getArticle(@PathVariable long id) {
        return articleService.getArticle(id, currentUserService.requireCurrentUser());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArticleDetailResponse> createArticle(@RequestBody ArticleRequest request) {
        AppUser author = currentUserService.requireCurrentUser();
        ArticleDetailResponse created = articleService.createArticle(request, author);
        return ResponseEntity.created(URI.create("/api/articles/" + created.id())).body(created);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ArticleDetailResponse updateArticle(@PathVariable long id, @RequestBody ArticleRequest request) {
        return articleService.updateArticle(id, request, currentUserService.requireCurrentUser());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable long id) {
        articleService.deleteArticle(id, currentUserService.requireCurrentUser());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping(value = "/{id}/references", produces = MediaType.APPLICATION_JSON_VALUE)
    public ArticleRelationListResponse getReferences(@PathVariable long id) { return articleService.getReferences(id); }

    @GetMapping(value = "/{id}/referenced-by", produces = MediaType.APPLICATION_JSON_VALUE)
    public ArticleRelationListResponse getReferencedBy(@PathVariable long id) { return articleService.getReferencedBy(id); }
}

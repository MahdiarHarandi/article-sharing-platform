package io.github.mahdiarharandi.articles.service;

import io.github.mahdiarharandi.articles.domain.AppUser;
import io.github.mahdiarharandi.articles.domain.Article;
import io.github.mahdiarharandi.articles.dto.ArticleDetailResponse;
import io.github.mahdiarharandi.articles.dto.ArticleListItemResponse;
import io.github.mahdiarharandi.articles.dto.ArticleListResponse;
import io.github.mahdiarharandi.articles.dto.ArticleRelationListResponse;
import io.github.mahdiarharandi.articles.dto.ArticleRequest;
import io.github.mahdiarharandi.articles.dto.TinyArticleResponse;
import io.github.mahdiarharandi.articles.exception.ArticleNotFoundException;
import io.github.mahdiarharandi.articles.exception.DuplicateTitleException;
import io.github.mahdiarharandi.articles.exception.ForbiddenException;
import io.github.mahdiarharandi.articles.exception.ValidationException;
import io.github.mahdiarharandi.articles.repository.ArticleRepository;
import io.github.mahdiarharandi.articles.repository.IncomingReferenceCount;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ArticleService {
    private static final String SORT_NEWEST = "newest";
    private static final String SORT_REFERENCES = "references";
    private final ArticleRepository repository;

    public ArticleService(ArticleRepository repository) { this.repository = repository; }

    public ArticleListResponse listArticles(String search, String sort, Long currentUserId) {
        String cleanSearch = search == null ? "" : search.trim();
        String normalizedSort = normalizeSort(sort);
        List<Article> articles = cleanSearch.isEmpty()
                ? repository.findAllWithReferences()
                : repository.searchWithReferences(cleanSearch);
        Map<Long, Long> counts = loadIncomingReferenceCounts(articles.stream().map(Article::getId).toList());
        Comparator<Article> order = articleOrder(normalizedSort, counts);
        if (!cleanSearch.isEmpty()) {
            String searchLower = cleanSearch.toLowerCase(Locale.ROOT);
            order = Comparator.<Article, Boolean>comparing(a -> !a.getTitle().toLowerCase(Locale.ROOT).contains(searchLower))
                    .thenComparing(order);
        }
        List<ArticleListItemResponse> items = articles.stream()
                .sorted(order)
                .map(article -> toListItem(article, counts.getOrDefault(article.getId(), 0L), currentUserId))
                .toList();
        return new ArticleListResponse(items, items.size(), cleanSearch, normalizedSort, true);
    }

    public ArticleDetailResponse getArticle(long id, AppUser currentUser) {
        Article article = findArticleWithReferences(id);
        List<Article> incoming = new ArrayList<>(repository.findAllReferencing(id));
        incoming.sort(Comparator.comparing(Article::getCreatedAt).reversed());
        return toDetail(article, incoming, currentUser.getId());
    }

    @Transactional
    public ArticleDetailResponse createArticle(ArticleRequest request, AppUser author) {
        CleanArticleInput input = validateRequest(request);
        ensureUniqueTitle(input.normalizedTitle(), null, input.title());
        List<Article> references = resolveReferences(input.referenceIds(), null);
        Article article = new Article(input.title(), input.normalizedTitle(), input.abstractText(), input.body(), author);
        article.replaceReferences(references);
        try {
            Article saved = repository.saveAndFlush(article);
            return toDetail(saved, List.of(), author.getId());
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateTitleException(input.title());
        }
    }

    @Transactional
    public ArticleDetailResponse updateArticle(long id, ArticleRequest request, AppUser currentUser) {
        Article article = findArticleWithReferences(id);
        assertOwner(article, currentUser);
        CleanArticleInput input = validateRequest(request);
        ensureUniqueTitle(input.normalizedTitle(), id, input.title());
        List<Article> references = resolveReferences(input.referenceIds(), id);
        article.updateContent(input.title(), input.normalizedTitle(), input.abstractText(), input.body());
        try {
            article.clearReferences();
            repository.saveAndFlush(article);
            article.replaceReferences(references);
            Article saved = repository.saveAndFlush(article);
            List<Article> incoming = new ArrayList<>(repository.findAllReferencing(id));
            incoming.sort(Comparator.comparing(Article::getCreatedAt).reversed());
            return toDetail(saved, incoming, currentUser.getId());
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateTitleException(input.title());
        }
    }

    @Transactional
    public void deleteArticle(long id, AppUser currentUser) {
        Article article = findArticleWithReferences(id);
        assertOwner(article, currentUser);
        List<Article> incomingArticles = repository.findAllReferencing(id);
        for (Article source : incomingArticles) source.removeReferenceTo(article);
        repository.saveAll(incomingArticles);
        article.clearReferences();
        repository.saveAndFlush(article);
        repository.delete(article);
        repository.flush();
    }

    public ArticleRelationListResponse getReferences(long id) {
        Article article = findArticleWithReferences(id);
        List<TinyArticleResponse> items = article.getReferences().stream().map(this::toTinyArticle).toList();
        return new ArticleRelationListResponse(id, items, items.size());
    }

    public ArticleRelationListResponse getReferencedBy(long id) {
        if (!repository.existsById(id)) throw new ArticleNotFoundException(id);
        List<TinyArticleResponse> items = repository.findAllReferencing(id).stream()
                .sorted(Comparator.comparing(Article::getCreatedAt).reversed())
                .map(this::toTinyArticle).toList();
        return new ArticleRelationListResponse(id, items, items.size());
    }

    public List<ArticleListItemResponse> listPublishedBy(AppUser user) {
        List<Article> articles = repository.findAllByAuthorIdWithReferences(user.getId());
        Map<Long, Long> counts = loadIncomingReferenceCounts(articles.stream().map(Article::getId).toList());
        return articles.stream()
                .sorted(Comparator.comparing(Article::getCreatedAt).reversed())
                .map(article -> toListItem(article, counts.getOrDefault(article.getId(), 0L), user.getId()))
                .toList();
    }

    public long countArticles() { return repository.count(); }

    private void assertOwner(Article article, AppUser user) {
        if (!article.getAuthor().getId().equals(user.getId())) {
            throw new ForbiddenException("Only the article author can edit or delete this article.");
        }
    }

    private Article findArticleWithReferences(long id) {
        if (id <= 0) throw new ValidationException("Article id must be a positive number.");
        return repository.findByIdWithReferences(id).orElseThrow(() -> new ArticleNotFoundException(id));
    }

    private CleanArticleInput validateRequest(ArticleRequest request) {
        if (request == null) throw new ValidationException("Request body cannot be empty.");
        String title = cleanRequiredText(request.title(), "title");
        if (title.length() > 300) throw new ValidationException("Field 'title' cannot be longer than 300 characters.");
        String abstractText = cleanRequiredText(request.abstractText(), "abstractText");
        if (abstractText.length() > 5000) throw new ValidationException("Field 'abstractText' cannot be longer than 5000 characters.");
        String body = cleanRequiredText(request.body(), "body");
        return new CleanArticleInput(title, normalizeTitle(title), abstractText, body,
                normalizeReferenceIds(request.referenceIds()));
    }

    private void ensureUniqueTitle(String normalizedTitle, Long currentArticleId, String originalTitle) {
        repository.findByNormalizedTitle(normalizedTitle).ifPresent(existing -> {
            if (currentArticleId == null || !existing.getId().equals(currentArticleId)) {
                throw new DuplicateTitleException(originalTitle);
            }
        });
    }

    private List<Article> resolveReferences(List<Long> referenceIds, Long currentArticleId) {
        if (referenceIds.isEmpty()) return List.of();
        if (currentArticleId != null && referenceIds.contains(currentArticleId)) {
            throw new ValidationException("An article cannot reference itself.");
        }
        List<Article> found = repository.findAllById(referenceIds);
        Map<Long, Article> articlesById = found.stream().collect(Collectors.toMap(Article::getId, Function.identity()));
        for (Long referenceId : referenceIds) {
            if (!articlesById.containsKey(referenceId)) {
                throw new ValidationException("Referenced article with id " + referenceId + " does not exist.");
            }
        }
        return referenceIds.stream().map(articlesById::get).toList();
    }

    private List<Long> normalizeReferenceIds(List<Long> referenceIds) {
        if (referenceIds == null || referenceIds.isEmpty()) return List.of();
        Set<Long> uniqueIds = new LinkedHashSet<>();
        for (Long referenceId : referenceIds) {
            if (referenceId == null) continue;
            if (referenceId <= 0) throw new ValidationException("Reference ids must be positive numbers.");
            uniqueIds.add(referenceId);
        }
        return List.copyOf(uniqueIds);
    }

    private String cleanRequiredText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Field '" + fieldName + "' is required and cannot be empty.");
        }
        return value.trim();
    }

    private String normalizeTitle(String title) {
        return Normalizer.normalize(title, Normalizer.Form.NFKC).trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) return SORT_NEWEST;
        String normalized = sort.trim().toLowerCase(Locale.ROOT);
        if (SORT_REFERENCES.equals(normalized) || "referenced".equals(normalized) || "citation".equals(normalized)) {
            return SORT_REFERENCES;
        }
        return SORT_NEWEST;
    }

    private Comparator<Article> articleOrder(String sort, Map<Long, Long> counts) {
        Comparator<Article> newestFirst = Comparator.comparing(Article::getCreatedAt, Comparator.reverseOrder())
                .thenComparing(Article::getId, Comparator.reverseOrder());
        if (SORT_REFERENCES.equals(sort)) {
            return Comparator.comparingLong((Article a) -> counts.getOrDefault(a.getId(), 0L)).reversed()
                    .thenComparing(newestFirst);
        }
        return newestFirst;
    }

    private Map<Long, Long> loadIncomingReferenceCounts(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        Map<Long, Long> counts = new HashMap<>();
        for (IncomingReferenceCount row : repository.countIncomingReferences(ids)) {
            counts.put(row.getArticleId(), row.getReferenceCount());
        }
        for (Long id : ids) counts.putIfAbsent(id, 0L);
        return counts;
    }

    private ArticleListItemResponse toListItem(Article article, long incomingCount, Long currentUserId) {
        boolean canModify = currentUserId != null && article.getAuthor().getId().equals(currentUserId);
        return new ArticleListItemResponse(
                article.getId(), article.getTitle(), article.getAbstractText(), article.getCreatedAt(),
                article.getReferences().stream().map(Article::getId).toList(), incomingCount,
                article.getAuthor().getId(), article.getAuthor().getUsername(), canModify
        );
    }

    private ArticleDetailResponse toDetail(Article article, List<Article> incoming, Long currentUserId) {
        boolean canModify = currentUserId != null && article.getAuthor().getId().equals(currentUserId);
        return new ArticleDetailResponse(
                article.getId(), article.getTitle(), article.getAbstractText(), article.getCreatedAt(),
                article.getReferences().stream().map(Article::getId).toList(), incoming.size(), article.getBody(),
                article.getReferences().stream().map(this::toTinyArticle).toList(),
                incoming.stream().map(this::toTinyArticle).toList(),
                article.getAuthor().getId(), article.getAuthor().getUsername(), canModify
        );
    }

    private TinyArticleResponse toTinyArticle(Article article) {
        return new TinyArticleResponse(article.getId(), article.getTitle());
    }

    private record CleanArticleInput(String title, String normalizedTitle, String abstractText, String body,
                                     List<Long> referenceIds) {}
}

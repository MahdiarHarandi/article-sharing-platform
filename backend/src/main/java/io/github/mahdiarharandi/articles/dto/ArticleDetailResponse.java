package io.github.mahdiarharandi.articles.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleDetailResponse(
        long id,
        String title,
        String abstractText,
        LocalDateTime createdAt,
        List<Long> referenceIds,
        long referenceCount,
        String body,
        List<TinyArticleResponse> references,
        List<TinyArticleResponse> referencedBy,
        long authorId,
        String authorUsername,
        boolean canModify
) {}

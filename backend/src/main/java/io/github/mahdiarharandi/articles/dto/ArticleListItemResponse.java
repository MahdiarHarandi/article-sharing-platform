package io.github.mahdiarharandi.articles.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleListItemResponse(
        long id,
        String title,
        String abstractText,
        LocalDateTime createdAt,
        List<Long> referenceIds,
        long referenceCount,
        long authorId,
        String authorUsername,
        boolean canModify
) {}

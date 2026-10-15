package io.github.mahdiarharandi.articles.dto;

import java.util.List;

public record ArticleRelationListResponse(
        long articleId,
        List<TinyArticleResponse> items,
        int count
) {
}

package io.github.mahdiarharandi.articles.dto;

import java.util.List;

public record ArticleListResponse(
        List<ArticleListItemResponse> items,
        int count,
        String search,
        String sort,
        boolean titleMatchesRankedFirst
) {
}

package io.github.mahdiarharandi.articles.dto;

import java.util.List;

public record ArticleRequest(
        String title,
        String abstractText,
        String body,
        List<Long> referenceIds
) {
}

package io.github.mahdiarharandi.articles.exception;

import org.springframework.http.HttpStatus;

public class ArticleNotFoundException extends ApiException {
    public ArticleNotFoundException(long id) {
        super(HttpStatus.NOT_FOUND, "ARTICLE_NOT_FOUND", "Article with id " + id + " was not found.");
    }
}

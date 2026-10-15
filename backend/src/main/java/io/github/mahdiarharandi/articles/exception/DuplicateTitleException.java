package io.github.mahdiarharandi.articles.exception;

import org.springframework.http.HttpStatus;

public class DuplicateTitleException extends ApiException {
    public DuplicateTitleException(String title) {
        super(HttpStatus.CONFLICT, "DUPLICATE_TITLE", "An article with title '" + title + "' already exists.");
    }
}

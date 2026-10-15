package io.github.mahdiarharandi.articles.repository;

public interface IncomingReferenceCount {
    Long getArticleId();

    long getReferenceCount();
}

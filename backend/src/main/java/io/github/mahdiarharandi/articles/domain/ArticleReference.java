package io.github.mahdiarharandi.articles.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;

@Entity
@Table(
        name = "article_references",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_article_reference_pair",
                        columnNames = {"source_article_id", "referenced_article_id"}
                ),
                @UniqueConstraint(
                        name = "uk_article_reference_position",
                        columnNames = {"source_article_id", "reference_position"}
                )
        },
        indexes = @Index(
                name = "idx_article_references_target",
                columnList = "referenced_article_id"
        )
)
public class ArticleReference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "source_article_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_article_references_source")
    )
    private Article sourceArticle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "referenced_article_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_article_references_target")
    )
    private Article referencedArticle;

    @Column(name = "reference_position", nullable = false)
    private int position;

    protected ArticleReference() {
        // Required by JPA/Hibernate.
    }

    ArticleReference(Article sourceArticle, Article referencedArticle, int position) {
        this.sourceArticle = Objects.requireNonNull(sourceArticle, "sourceArticle");
        this.referencedArticle = Objects.requireNonNull(referencedArticle, "referencedArticle");
        if (position < 0) {
            throw new IllegalArgumentException("position cannot be negative");
        }
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public Article getSourceArticle() {
        return sourceArticle;
    }

    public Article getReferencedArticle() {
        return referencedArticle;
    }

    public int getPosition() {
        return position;
    }

    boolean pointsTo(Article article) {
        return referencedArticle.getId() != null
                && referencedArticle.getId().equals(article.getId());
    }
}

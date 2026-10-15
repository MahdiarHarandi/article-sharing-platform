package io.github.mahdiarharandi.articles.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "articles",
        indexes = @Index(name = "idx_articles_author", columnList = "author_id"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_articles_normalized_title",
                columnNames = "normalized_title"
        )
)
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "normalized_title", nullable = false, length = 300)
    private String normalizedTitle;

    @Column(name = "abstract_text", nullable = false, length = 5000)
    private String abstractText;

    @Lob
    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "author_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_articles_author")
    )
    private AppUser author;

    @OneToMany(
            mappedBy = "sourceArticle",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ArticleReference> outgoingReferences = new ArrayList<>();

    @OneToMany(mappedBy = "referencedArticle", fetch = FetchType.LAZY)
    private List<ArticleReference> incomingReferences = new ArrayList<>();

    protected Article() {
        // Required by JPA/Hibernate.
    }

    public Article(String title, String normalizedTitle, String abstractText, String body, AppUser author) {
        this.title = Objects.requireNonNull(title, "title");
        this.normalizedTitle = Objects.requireNonNull(normalizedTitle, "normalizedTitle");
        this.abstractText = Objects.requireNonNull(abstractText, "abstractText");
        this.body = Objects.requireNonNull(body, "body");
        this.author = Objects.requireNonNull(author, "author");
    }

    @PrePersist
    void assignCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getNormalizedTitle() { return normalizedTitle; }
    public String getAbstractText() { return abstractText; }
    public String getBody() { return body; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public AppUser getAuthor() { return author; }

    public List<Article> getReferences() {
        return outgoingReferences.stream()
                .sorted(Comparator.comparingInt(ArticleReference::getPosition))
                .map(ArticleReference::getReferencedArticle)
                .toList();
    }

    public List<ArticleReference> getOutgoingReferences() { return List.copyOf(outgoingReferences); }
    public List<ArticleReference> getIncomingReferences() { return List.copyOf(incomingReferences); }

    public void updateContent(String title, String normalizedTitle, String abstractText, String body) {
        this.title = Objects.requireNonNull(title, "title");
        this.normalizedTitle = Objects.requireNonNull(normalizedTitle, "normalizedTitle");
        this.abstractText = Objects.requireNonNull(abstractText, "abstractText");
        this.body = Objects.requireNonNull(body, "body");
    }

    public void replaceReferences(Collection<Article> newReferences) {
        outgoingReferences.clear();
        if (newReferences == null) {
            return;
        }
        int position = 0;
        for (Article referencedArticle : newReferences) {
            outgoingReferences.add(new ArticleReference(this, referencedArticle, position++));
        }
    }

    public void removeReferenceTo(Article article) {
        outgoingReferences.removeIf(reference -> reference.pointsTo(article));
    }

    public void clearReferences() { outgoingReferences.clear(); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Article article)) return false;
        return id != null && id.equals(article.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}

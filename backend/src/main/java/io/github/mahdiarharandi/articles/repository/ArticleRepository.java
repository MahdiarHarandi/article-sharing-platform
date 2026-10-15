package io.github.mahdiarharandi.articles.repository;

import io.github.mahdiarharandi.articles.domain.Article;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findByNormalizedTitle(String normalizedTitle);

    @EntityGraph(attributePaths = {"author", "outgoingReferences", "outgoingReferences.referencedArticle"})
    @Query("select distinct a from Article a")
    List<Article> findAllWithReferences();

    @EntityGraph(attributePaths = {"author", "outgoingReferences", "outgoingReferences.referencedArticle"})
    @Query("""
            select distinct a
            from Article a
            where lower(a.title) like lower(concat('%', :term, '%'))
               or lower(a.abstractText) like lower(concat('%', :term, '%'))
            """)
    List<Article> searchWithReferences(@Param("term") String term);

    @EntityGraph(attributePaths = {"author", "outgoingReferences", "outgoingReferences.referencedArticle"})
    @Query("select distinct a from Article a where a.id = :id")
    Optional<Article> findByIdWithReferences(@Param("id") Long id);

    @EntityGraph(attributePaths = {"author", "outgoingReferences", "outgoingReferences.referencedArticle"})
    @Query("""
            select distinct source
            from Article source
            join source.outgoingReferences reference
            where reference.referencedArticle.id = :referencedArticleId
            """)
    List<Article> findAllReferencing(@Param("referencedArticleId") Long referencedArticleId);

    @EntityGraph(attributePaths = {"author", "outgoingReferences", "outgoingReferences.referencedArticle"})
    @Query("select distinct a from Article a where a.author.id = :authorId")
    List<Article> findAllByAuthorIdWithReferences(@Param("authorId") Long authorId);

    @Query("""
            select a.id as articleId, count(reference.id) as referenceCount
            from Article a
            left join a.incomingReferences reference
            where a.id in :articleIds
            group by a.id
            """)
    List<IncomingReferenceCount> countIncomingReferences(@Param("articleIds") Collection<Long> articleIds);
}

package io.github.mahdiarharandi.articles.config;

import io.github.mahdiarharandi.articles.domain.AppUser;
import io.github.mahdiarharandi.articles.dto.ArticleDetailResponse;
import io.github.mahdiarharandi.articles.dto.ArticleRequest;
import io.github.mahdiarharandi.articles.service.ArticleService;
import io.github.mahdiarharandi.articles.service.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataInitializer implements CommandLineRunner {
    private final ArticleService articleService;
    private final AuthService authService;
    private final String demoPassword;

    public DemoDataInitializer(ArticleService articleService, AuthService authService,
                               @Value("${app.demo-password:}") String demoPassword) {
        this.articleService = articleService;
        this.authService = authService;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(String... args) {
        if (demoPassword.isBlank()) {
            throw new IllegalStateException("DEMO_PASSWORD is required when SEED_DEMO_DATA=true.");
        }
        AppUser demo = authService.ensureDemoUser(demoPassword);
        if (articleService.countArticles() > 0) return;

        ArticleDetailResponse first = articleService.createArticle(new ArticleRequest(
                "Understanding RESTful APIs",
                "A short introduction to resources, HTTP verbs, status codes, and JSON responses.",
                "RESTful APIs expose application data as resources. Clients use HTTP methods such as GET and POST to read and create those resources. A good API should use meaningful URLs, correct status codes, and consistent response bodies.",
                List.of()
        ), demo);

        ArticleDetailResponse second = articleService.createArticle(new ArticleRequest(
                "Article Search in Web Applications",
                "This article explains title-based and abstract-based search ranking for article platforms.",
                "Search is more useful when the most relevant fields have higher priority. In this project, title matches are ranked before abstract matches because a title match usually means the article is more directly related to the query.",
                List.of(first.id())
        ), demo);

        articleService.createArticle(new ArticleRequest(
                "From Server-Rendered Pages to RIA",
                "A comparison between server-rendered HTML applications and rich internet applications.",
                "In a rich internet application, the backend provides data through APIs and the frontend renders the user interface in the browser. This separation allows the same API to be reused by web, mobile, or other clients.",
                List.of(first.id(), second.id())
        ), demo);
    }
}

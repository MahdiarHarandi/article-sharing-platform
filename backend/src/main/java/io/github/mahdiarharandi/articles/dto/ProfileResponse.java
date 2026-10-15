package io.github.mahdiarharandi.articles.dto;
import java.util.List;
public record ProfileResponse(UserResponse user, List<ArticleListItemResponse> articles, int articleCount) {}

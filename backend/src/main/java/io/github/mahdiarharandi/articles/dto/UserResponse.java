package io.github.mahdiarharandi.articles.dto;
import java.time.LocalDateTime;
public record UserResponse(long id, String username, String email, String phone, LocalDateTime createdAt) {}

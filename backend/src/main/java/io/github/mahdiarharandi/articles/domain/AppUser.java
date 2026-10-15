package io.github.mahdiarharandi.articles.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Check(constraints = "email IS NOT NULL OR phone IS NOT NULL")
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_normalized_username", columnNames = "normalized_username"),
                @UniqueConstraint(name = "uk_users_normalized_email", columnNames = "normalized_email"),
                @UniqueConstraint(name = "uk_users_normalized_phone", columnNames = "normalized_phone")
        }
)
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "normalized_username", nullable = false, length = 50)
    private String normalizedUsername;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "normalized_email", length = 254)
    private String normalizedEmail;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "normalized_phone", length = 20)
    private String normalizedPhone;

    @Column(name = "token_version", nullable = false)
    private long tokenVersion = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "author")
    private List<Article> articles = new ArrayList<>();

    protected AppUser() {
        // Required by JPA/Hibernate.
    }

    public AppUser(
            String username,
            String normalizedUsername,
            String passwordHash,
            String email,
            String normalizedEmail,
            String phone,
            String normalizedPhone
    ) {
        this.username = Objects.requireNonNull(username, "username");
        this.normalizedUsername = Objects.requireNonNull(normalizedUsername, "normalizedUsername");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.email = email;
        this.normalizedEmail = normalizedEmail;
        this.phone = phone;
        this.normalizedPhone = normalizedPhone;
    }

    @PrePersist
    void assignCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getNormalizedUsername() { return normalizedUsername; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmail() { return email; }
    public String getNormalizedEmail() { return normalizedEmail; }
    public String getPhone() { return phone; }
    public String getNormalizedPhone() { return normalizedPhone; }
    public long getTokenVersion() { return tokenVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Article> getArticles() { return List.copyOf(articles); }

    public void updateContacts(String email, String normalizedEmail, String phone, String normalizedPhone) {
        this.email = email;
        this.normalizedEmail = normalizedEmail;
        this.phone = phone;
        this.normalizedPhone = normalizedPhone;
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        invalidateTokens();
    }

    public void invalidateTokens() {
        tokenVersion++;
    }
}

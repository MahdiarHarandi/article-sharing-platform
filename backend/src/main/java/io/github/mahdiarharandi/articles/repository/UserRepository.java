package io.github.mahdiarharandi.articles.repository;

import io.github.mahdiarharandi.articles.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByNormalizedUsername(String normalizedUsername);
    boolean existsByNormalizedUsername(String normalizedUsername);
    boolean existsByNormalizedEmail(String normalizedEmail);
    boolean existsByNormalizedPhone(String normalizedPhone);
    boolean existsByNormalizedEmailAndIdNot(String normalizedEmail, Long id);
    boolean existsByNormalizedPhoneAndIdNot(String normalizedPhone, Long id);
}

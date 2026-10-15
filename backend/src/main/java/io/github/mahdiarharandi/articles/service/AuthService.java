package io.github.mahdiarharandi.articles.service;

import io.github.mahdiarharandi.articles.domain.AppUser;
import io.github.mahdiarharandi.articles.dto.AuthResponse;
import io.github.mahdiarharandi.articles.dto.ChangePasswordRequest;
import io.github.mahdiarharandi.articles.dto.LoginRequest;
import io.github.mahdiarharandi.articles.dto.RegisterRequest;
import io.github.mahdiarharandi.articles.dto.UpdateContactRequest;
import io.github.mahdiarharandi.articles.dto.UserResponse;
import io.github.mahdiarharandi.articles.exception.AuthenticationFailedException;
import io.github.mahdiarharandi.articles.exception.ConflictException;
import io.github.mahdiarharandi.articles.exception.ValidationException;
import io.github.mahdiarharandi.articles.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import io.github.mahdiarharandi.articles.security.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher, JwtService jwtService,
                       CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request == null) throw new ValidationException("Request body cannot be empty.");
        String username = cleanUsername(request.username());
        String normalizedUsername = normalizeUsername(username);
        validatePassword(request.password(), "password");
        Contact contact = validateAndNormalizeContact(request.email(), request.phone());

        if (userRepository.existsByNormalizedUsername(normalizedUsername)) {
            throw new ConflictException("Username is already in use.");
        }
        ensureContactUnique(contact, null);

        AppUser user = new AppUser(
                username,
                normalizedUsername,
                passwordHasher.hash(request.password()),
                contact.email(),
                contact.normalizedEmail(),
                contact.phone(),
                contact.normalizedPhone()
        );
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Username, email, or phone number is already in use.");
        }
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            throw new AuthenticationFailedException();
        }
        AppUser user = userRepository.findByNormalizedUsername(normalizeUsername(request.username()))
                .orElseThrow(AuthenticationFailedException::new);
        if (!passwordHasher.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationFailedException();
        }
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUserResponse() {
        return toUserResponse(currentUserService.requireCurrentUser());
    }

    @Transactional
    public UserResponse updateContact(UpdateContactRequest request) {
        if (request == null) throw new ValidationException("Request body cannot be empty.");
        AppUser user = currentUserService.requireCurrentUser();
        Contact contact = validateAndNormalizeContact(request.email(), request.phone());
        ensureContactUnique(contact, user.getId());
        user.updateContacts(contact.email(), contact.normalizedEmail(), contact.phone(), contact.normalizedPhone());
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Email or phone number is already in use.");
        }
        return toUserResponse(user);
    }

    @Transactional
    public AuthResponse changePassword(ChangePasswordRequest request) {
        if (request == null) throw new ValidationException("Request body cannot be empty.");
        AppUser user = currentUserService.requireCurrentUser();
        if (request.currentPassword() == null || !passwordHasher.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AuthenticationFailedException();
        }
        validatePassword(request.newPassword(), "newPassword");
        if (passwordHasher.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ValidationException("New password must be different from the current password.");
        }
        user.changePasswordHash(passwordHasher.hash(request.newPassword()));
        userRepository.saveAndFlush(user);
        return authResponse(user);
    }

    @Transactional
    public void logout() {
        AppUser user = currentUserService.requireCurrentUser();
        user.invalidateTokens();
        userRepository.saveAndFlush(user);
    }

    @Transactional
    public AppUser ensureDemoUser(String demoPassword) {
        validatePassword(demoPassword, "demoPassword");
        String normalized = normalizeUsername("demo_user");
        return userRepository.findByNormalizedUsername(normalized).orElseGet(() -> userRepository.saveAndFlush(
                new AppUser(
                        "demo_user",
                        normalized,
                        passwordHasher.hash(demoPassword),
                        "demo@example.com",
                        "demo@example.com",
                        null,
                        null
                )
        ));
    }

    public UserResponse toUserResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getPhone(), user.getCreatedAt());
    }

    private AuthResponse authResponse(AppUser user) {
        JwtService.IssuedToken token = jwtService.issue(user);
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), toUserResponse(user));
    }

    private String cleanUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username is required.");
        }
        String cleaned = username.trim();
        if (!USERNAME_PATTERN.matcher(cleaned).matches()) {
            throw new ValidationException("Username must be 3-50 characters and contain only letters, numbers, dot, underscore, or hyphen.");
        }
        return cleaned;
    }

    private void validatePassword(String password, String fieldName) {
        if (password == null || password.length() < 6 || password.length() > 72
                || !LOWERCASE.matcher(password).find()
                || !UPPERCASE.matcher(password).find()
                || !DIGIT.matcher(password).find()) {
            throw new ValidationException("Field '" + fieldName + "' must be 6-72 characters and include a lowercase letter, uppercase letter, and number.");
        }
    }

    private Contact validateAndNormalizeContact(String rawEmail, String rawPhone) {
        String email = blankToNull(rawEmail);
        String phone = blankToNull(rawPhone);
        if (email == null && phone == null) {
            throw new ValidationException("At least one of email or phone is required.");
        }

        String normalizedEmail = null;
        if (email != null) {
            email = email.trim();
            normalizedEmail = email.toLowerCase(Locale.ROOT);
            if (email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
                throw new ValidationException("Email format is invalid.");
            }
        }

        String normalizedPhone = null;
        if (phone != null) {
            normalizedPhone = phone.replaceAll("[\\s()\\-]", "");
            if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
                throw new ValidationException("Phone format is invalid. Use 10-15 digits with an optional leading +.");
            }
            phone = normalizedPhone;
        }
        return new Contact(email, normalizedEmail, phone, normalizedPhone);
    }

    private void ensureContactUnique(Contact contact, Long currentUserId) {
        if (contact.normalizedEmail() != null) {
            boolean exists = currentUserId == null
                    ? userRepository.existsByNormalizedEmail(contact.normalizedEmail())
                    : userRepository.existsByNormalizedEmailAndIdNot(contact.normalizedEmail(), currentUserId);
            if (exists) throw new ConflictException("Email is already in use.");
        }
        if (contact.normalizedPhone() != null) {
            boolean exists = currentUserId == null
                    ? userRepository.existsByNormalizedPhone(contact.normalizedPhone())
                    : userRepository.existsByNormalizedPhoneAndIdNot(contact.normalizedPhone(), currentUserId);
            if (exists) throw new ConflictException("Phone number is already in use.");
        }
    }

    private String normalizeUsername(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC).trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }

    private record Contact(String email, String normalizedEmail, String phone, String normalizedPhone) {}
}

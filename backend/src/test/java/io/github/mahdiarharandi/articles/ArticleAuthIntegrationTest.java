package io.github.mahdiarharandi.articles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ArticleAuthIntegrationTest {
    private static final String PASSWORD = "Strong123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;

    @BeforeEach
    void clearDatabase() {
        jdbcTemplate.update("DELETE FROM article_references");
        jdbcTemplate.update("DELETE FROM articles");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void publicEndpointsAndAnonymousRestrictionsMatchRequirements() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("article-sharing-api")));

        mockMvc.perform(get("/api/articles"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(articlePayload("Anonymous", List.of()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/articles/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationAcceptsEmailOrPhoneAndStoresSaltedHash() throws Exception {
        Auth first = register("first_user", "First@Example.com", null, PASSWORD);
        Auth second = register("second_user", null, "+98 912 123 4567", PASSWORD);

        assertThat(first.token()).isNotBlank();
        assertThat(second.token()).isNotBlank();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT username, password_hash, email, phone FROM users ORDER BY username"
        );
        assertThat(rows).hasSize(2);
        String firstHash = String.valueOf(rows.get(0).get("password_hash"));
        String secondHash = String.valueOf(rows.get(1).get("password_hash"));
        assertThat(firstHash).startsWith("pbkdf2_sha256$").doesNotContain(PASSWORD);
        assertThat(secondHash).startsWith("pbkdf2_sha256$").doesNotContain(PASSWORD);
        assertThat(firstHash).isNotEqualTo(secondHash); // independent random salts

        assertThat(rows.stream().anyMatch(row -> "+989121234567".equals(row.get("phone")))).isTrue();
    }

    @Test
    void registrationValidatesContactPasswordAndFormats() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "missing_contact", "password", PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("At least one")));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "bad_email", "password", PASSWORD, "email", "wrong@format"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Email format")));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "bad_phone", "password", PASSWORD, "phone", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Phone format")));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "weak_pass", "password", "abcdef", "email", "weak@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("lowercase letter, uppercase letter, and number")));
    }

    @Test
    void usernameEmailAndPhoneAreUniqueCaseInsensitively() throws Exception {
        register("Unique_User", "Unique@Example.com", "+491234567890", PASSWORD);

        expectRegistrationConflict("unique_user", "other1@example.com", null, "Username");
        expectRegistrationConflict("other_user", "unique@example.com", null, "Email");
        expectRegistrationConflict("phone_user", null, "+49 123 456 7890", "Phone");
    }

    @Test
    void loginProducesSignedJwtWithExpectedClaims() throws Exception {
        Auth registered = register("jwt_user", "jwt@example.com", null, PASSWORD);
        String loginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "JWT_USER", "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.username", is("jwt_user")))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginBody).get("token").asText();
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
        assertThat(payload.get("iss").asText()).isEqualTo("article-sharing-test");
        assertThat(payload.get("sub").asText()).isEqualTo("jwt_user");
        assertThat(payload.get("userId").asLong()).isEqualTo(registered.userId());
        assertThat(payload.has("iat")).isTrue();
        assertThat(payload.has("exp")).isTrue();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "jwt_user", "password", "Wrong123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("AUTHENTICATION_FAILED")));
    }

    @Test
    void fullArticleCrudReferencesAndProfileFlowWorks() throws Exception {
        Auth author = register("author", "author@example.com", null, PASSWORD);
        long firstId = create(author.token(), "Persistent Article", List.of());
        long secondId = create(author.token(), "Hibernate Article", List.of(firstId));

        mockMvc.perform(get("/api/articles/{id}", firstId).header(HttpHeaders.AUTHORIZATION, bearer(author.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceCount", is(1)))
                .andExpect(jsonPath("$.referencedBy[0].id", is((int) secondId)))
                .andExpect(jsonPath("$.authorUsername", is("author")))
                .andExpect(jsonPath("$.canModify", is(true)));

        mockMvc.perform(get("/api/profile").header(HttpHeaders.AUTHORIZATION, bearer(author.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articleCount", is(2)))
                .andExpect(jsonPath("$.articles[0].authorUsername", is("author")));

        mockMvc.perform(put("/api/articles/{id}", secondId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(author.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Hibernate Article Updated",
                                "abstractText", "updated abstract",
                                "body", "updated body",
                                "referenceIds", List.of(firstId)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Hibernate Article Updated")));

        mockMvc.perform(delete("/api/articles/{id}", firstId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(author.token())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/articles/{id}", secondId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(author.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceIds.length()", is(0)));
    }

    @Test
    void onlyArticleAuthorCanEditOrDelete() throws Exception {
        Auth author = register("owner", "owner@example.com", null, PASSWORD);
        Auth other = register("other", "other@example.com", null, PASSWORD);
        long id = create(author.token(), "Owner Article", List.of());

        mockMvc.perform(put("/api/articles/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(other.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(articlePayload("Stolen Edit", List.of()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));

        mockMvc.perform(delete("/api/articles/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(other.token())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/articles/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(other.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canModify", is(false)));
    }

    @Test
    void contactCanBeEditedButMustRemainUniqueAndNonEmpty() throws Exception {
        Auth first = register("contact_one", "one@example.com", null, PASSWORD);
        register("contact_two", "two@example.com", "+491111111111", PASSWORD);

        mockMvc.perform(put("/api/profile/contact")
                        .header(HttpHeaders.AUTHORIZATION, bearer(first.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", "+49 222 222 2222"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phone", is("+492222222222")));

        mockMvc.perform(put("/api/profile/contact")
                        .header(HttpHeaders.AUTHORIZATION, bearer(first.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "two@example.com"))))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/profile/contact")
                        .header(HttpHeaders.AUTHORIZATION, bearer(first.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":null,\"phone\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordChangeInvalidatesOldTokensAndOldPassword() throws Exception {
        Auth user = register("password_user", "password@example.com", null, PASSWORD);
        String response = mockMvc.perform(put("/api/profile/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", PASSWORD, "newPassword", "NewStrong456"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newToken = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(get("/api/profile").header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/profile").header(HttpHeaders.AUTHORIZATION, bearer(newToken)))
                .andExpect(status().isOk());

        loginExpecting("password_user", PASSWORD, 401);
        loginExpecting("password_user", "NewStrong456", 200);
    }

    @Test
    void logoutInvalidatesJwtServerSide() throws Exception {
        Auth user = register("logout_user", "logout@example.com", null, PASSWORD);
        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/profile").header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void searchRankingReferenceSortAndTitleUniquenessRemainCompatible() throws Exception {
        Auth author = register("compat", "compat@example.com", null, PASSWORD);
        long target = create(author.token(), "Spring Persistence", List.of());
        create(author.token(), "Completely Different", List.of(target));
        create(author.token(), "Source Two", List.of(target));

        jdbcTemplate.update("UPDATE articles SET abstract_text='spring appears only in abstract' WHERE title='Completely Different'");

        mockMvc.perform(get("/api/articles").param("search", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title", is("Spring Persistence")))
                .andExpect(jsonPath("$.titleMatchesRankedFirst", is(true)));

        mockMvc.perform(get("/api/articles").param("sort", "references"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is((int) target)))
                .andExpect(jsonPath("$.items[0].referenceCount", is(2)));

        mockMvc.perform(post("/api/articles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(author.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(articlePayload(" spring persistence ", List.of()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("DUPLICATE_TITLE")));
    }

    @Test
    void validationAndJsonErrorsUseStableShape() throws Exception {
        Auth user = register("errors", "errors@example.com", null, PASSWORD);
        mockMvc.perform(post("/api/articles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error", is("UNSUPPORTED_MEDIA_TYPE")));

        mockMvc.perform(post("/api/articles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_JSON")));
    }

    @Test
    void invalidReferencesAndSelfReferencesRemainRejected() throws Exception {
        Auth user = register("reference_user", "reference@example.com", null, PASSWORD);

        mockMvc.perform(post("/api/articles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(articlePayload("Bad Reference", List.of(99999L)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not exist")));

        long id = create(user.token(), "Self Candidate", List.of());
        mockMvc.perform(put("/api/articles/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(articlePayload("Self Candidate", List.of(id)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot reference itself")));
    }

    @Test
    void tamperedJwtIsRejected() throws Exception {
        Auth user = register("tamper_user", "tamper@example.com", null, PASSWORD);
        String token = user.token();
        char replacement = token.charAt(token.length() - 1) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, token.length() - 1) + replacement;

        mockMvc.perform(get("/api/profile").header(HttpHeaders.AUTHORIZATION, bearer(tampered)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("invalid or expired")));
    }

    @Test
    void generatedSchemaContainsUserArticleAndReferenceKeys() throws Exception {
        register("schema_user", "schema@example.com", null, PASSWORD);
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            assertThat(primaryKeyColumns(metadata, "users")).containsExactly("id");
            assertThat(primaryKeyColumns(metadata, "articles")).containsExactly("id");
            assertThat(primaryKeyColumns(metadata, "article_references")).containsExactly("id");
            assertThat(importedKeyColumns(metadata, "articles")).contains("author_id");
            assertThat(importedKeyColumns(metadata, "article_references"))
                    .containsExactlyInAnyOrder("source_article_id", "referenced_article_id");
        }
    }

    @Test
    void databaseRejectsAUserWithoutEmailAndPhone() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (username, normalized_username, password_hash, token_version, created_at) "
                        + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                "no_contact", "no_contact", "pbkdf2_sha256$2000$fakeSalt$fakeHash", 0
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void createdArticleHasAuthorForeignKeyAndLocationHeader() throws Exception {
        Auth user = register("database_user", "database@example.com", null, PASSWORD);
        String response = mockMvc.perform(post("/api/articles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(articlePayload("Database Article", List.of()))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/articles/")))
                .andExpect(jsonPath("$.id", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.authorId", is((int) user.userId())))
                .andReturn().getResponse().getContentAsString();
        long articleId = objectMapper.readTree(response).get("id").asLong();
        Number authorId = jdbcTemplate.queryForObject("SELECT author_id FROM articles WHERE id=?", Number.class, articleId);
        assertThat(authorId.longValue()).isEqualTo(user.userId());
    }

    private Auth register(String username, String email, String phone, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "password", password,
                                "email", email == null ? "" : email,
                                "phone", phone == null ? "" : phone
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(response);
        return new Auth(root.get("token").asText(), root.path("user").path("id").asLong());
    }

    private void expectRegistrationConflict(String username, String email, String phone, String field) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "password", PASSWORD,
                                "email", email == null ? "" : email,
                                "phone", phone == null ? "" : phone
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString(field)));
    }

    private long create(String token, String title, List<Long> referenceIds) throws Exception {
        String response = mockMvc.perform(post("/api/articles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(articlePayload(title, referenceIds))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Map<String, Object> articlePayload(String title, List<Long> referenceIds) {
        return Map.of("title", title, "abstractText", "abstract for " + title,
                "body", "body for " + title, "referenceIds", referenceIds);
    }

    private void loginExpecting(String username, String password, int statusCode) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().is(statusCode));
    }

    private String bearer(String token) { return "Bearer " + token; }
    private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }

    private Set<String> primaryKeyColumns(DatabaseMetaData metadata, String tableName) throws Exception {
        Set<String> columns = new HashSet<>();
        try (ResultSet resultSet = metadata.getPrimaryKeys(null, null, tableName)) {
            while (resultSet.next()) columns.add(resultSet.getString("COLUMN_NAME").toLowerCase());
        }
        return columns;
    }

    private Set<String> importedKeyColumns(DatabaseMetaData metadata, String tableName) throws Exception {
        Set<String> columns = new HashSet<>();
        try (ResultSet resultSet = metadata.getImportedKeys(null, null, tableName)) {
            while (resultSet.next()) columns.add(resultSet.getString("FKCOLUMN_NAME").toLowerCase());
        }
        return columns;
    }

    private record Auth(String token, long userId) {}
}

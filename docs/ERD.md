# Entity-relationship design

```mermaid
erDiagram
    USERS ||--o{ ARTICLES : publishes
    ARTICLES ||--o{ ARTICLE_REFERENCES : "source article"
    ARTICLES ||--o{ ARTICLE_REFERENCES : "referenced article"

    USERS {
        BIGINT id PK
        VARCHAR_50 username
        VARCHAR_50 normalized_username UK
        VARCHAR_100 password_hash
        VARCHAR_254 email
        VARCHAR_254 normalized_email UK
        VARCHAR_20 phone
        VARCHAR_20 normalized_phone UK
        BIGINT token_version
        DATETIME_6 created_at
    }

    ARTICLES {
        BIGINT id PK
        VARCHAR_300 title
        VARCHAR_300 normalized_title UK
        VARCHAR_5000 abstract_text
        LONGTEXT body
        DATETIME_6 created_at
        BIGINT author_id FK
    }

    ARTICLE_REFERENCES {
        BIGINT id PK
        BIGINT source_article_id FK
        BIGINT referenced_article_id FK
        INT reference_position
    }
```

## Design decisions

- `users.id`, `articles.id`, and `article_references.id` are generated primary keys.
- `articles.author_id` is a mandatory foreign key, so every new article has a persistent author.
- Username, email, and phone are normalized before their uniqueness constraints are checked.
- Nullable unique email/phone columns allow different users to omit one contact method while preventing duplicate non-null values.
- `token_version` supports server-side invalidation of all existing JWTs after logout or password change.
- Password plaintext and password salt are never stored separately. The `password_hash` field contains the PBKDF2 algorithm marker, iteration count, random salt, and derived hash.
- The article-reference association remains an explicit entity because it needs a primary key and `reference_position`.

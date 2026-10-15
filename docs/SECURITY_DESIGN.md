# Security design

## JWT choice

The project uses compact JSON Web Tokens signed with **HS256 (HMAC-SHA256)**. A single backend both issues and verifies tokens, so a symmetric HMAC secret is appropriate and simpler than an asymmetric key pair for this course project.

JWT payload fields:

- `iss`: configured issuer
- `sub`: username
- `iat`: issue time
- `exp`: expiration time
- `userId`: relational user identifier
- `ver`: token version used for server-side invalidation
- `typ`: `access`

The backend validates the token structure, `alg`, `typ`, HMAC signature using constant-time comparison, issuer, timestamps, user existence, username, and token version. The default lifetime is 120 minutes.

The backend does not store complete JWT values. It stores only `token_version` in the user row. Logout and password change increment this value, immediately invalidating all tokens issued with the old version.

The frontend stores the current token and non-sensitive user summary in `localStorage` so login survives a browser refresh. Every API call automatically adds `Authorization: Bearer <token>`. A `401` response removes the local session and returns the user to the login flow. Passwords are never stored in frontend storage.

## Password storage choice

Passwords use **PBKDF2-HMAC-SHA256** with:

- a cryptographically random 16-byte salt generated separately for every password
- a configurable work factor, defaulting to 210,000 iterations
- a 256-bit derived key
- constant-time hash comparison

Stored format:

```text
pbkdf2_sha256$<iterations>$<base64url-salt>$<base64url-derived-key>
```

PBKDF2 is an adaptive one-way password-based key derivation function available in the Java standard cryptography APIs. The random per-password salt prevents equal passwords from producing equal stored values and protects against precomputed rainbow tables. The work factor increases the cost of offline guessing. Plaintext passwords are never persisted or logged.

## Authorization matrix

| Endpoint | Guest | Authenticated user | Owner requirement |
|---|---:|---:|---:|
| `POST /api/auth/register` | Yes | Yes | No |
| `POST /api/auth/login` | Yes | Yes | No |
| `POST /api/auth/logout` | No | Yes | No |
| `GET /api/articles` | Yes | Yes | No |
| `GET /api/articles/{id}` | No | Yes | No |
| `POST /api/articles` | No | Yes | Current user becomes author |
| `PUT /api/articles/{id}` | No | Yes | Yes |
| `DELETE /api/articles/{id}` | No | Yes | Yes |
| `GET /api/profile` | No | Yes | Own profile only |
| `PUT /api/profile/contact` | No | Yes | Own profile only |
| `PUT /api/profile/password` | No | Yes | Own profile only |

The frontend route guards improve the user experience, but backend checks are authoritative and cannot be bypassed by calling the API directly.

## Additional HTTP protections

Every backend response includes `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, a no-referrer policy, and a restrictive permissions policy. CORS accepts only configured frontend origins and only the methods/headers needed by the application. Because authentication is carried in an explicit Bearer header rather than an automatically attached authentication cookie, the API does not rely on cookie-based sessions.

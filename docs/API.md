# Article Sharing REST API

Base URL: `http://localhost:8080/api`

Protected endpoints require:

```http
Authorization: Bearer <jwt>
```

## Authentication

### Register

`POST /auth/register`

```json
{
  "username": "student_user",
  "password": "Strong123",
  "email": "student@example.com",
  "phone": null
}
```

Returns `201 Created` with a signed JWT and user summary. Email and phone are individually optional, but at least one must be supplied.

### Login

`POST /auth/login`

```json
{
  "username": "student_user",
  "password": "Strong123"
}
```

Returns `200 OK` with `token`, `tokenType`, `expiresAt`, and `user`.

### Logout

`POST /auth/logout` - protected. Returns `204 No Content` and invalidates all existing tokens for the current user.

## Profile

- `GET /profile` - current user details and published articles
- `PUT /profile/contact` - update email and phone
- `PUT /profile/password` - verify current password, change password, and issue a replacement JWT

Contact update example:

```json
{
  "email": "new@example.com",
  "phone": "+989121234567"
}
```

Password update example:

```json
{
  "currentPassword": "Strong123",
  "newPassword": "NewStrong456"
}
```

## Articles

- `GET /articles` - public main list; supports `search`/`q` and `sort`
- `GET /articles/{id}` - authenticated article detail
- `POST /articles` - authenticated creation; current user becomes author
- `PUT /articles/{id}` - author only
- `DELETE /articles/{id}` - author only
- `GET /articles/{id}/references` - authenticated
- `GET /articles/{id}/referenced-by` - authenticated

Article payload:

```json
{
  "title": "Article title",
  "abstractText": "Short abstract",
  "body": "Complete article body",
  "referenceIds": [1, 2]
}
```

## Status codes

- `200` successful read/update/login
- `201` registration or article creation
- `204` logout or delete
- `400` validation/JSON error
- `401` missing, invalid, expired, or revoked JWT
- `403` authenticated user is not the article owner
- `404` resource not found
- `409` duplicate username, email, phone, or article title
- `415` unsupported media type

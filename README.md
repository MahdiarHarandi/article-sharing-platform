# Full-Stack Article Sharing Platform

A web application for publishing, discovering, and managing articles with cross-article references. Developed as a university software engineering project, it demonstrates a complete React–Spring Boot–MySQL application with token-based authentication and a containerized deployment.

## Features

- **Accounts:** registration, login, logout, profile updates, and password changes.
- **Article publishing:** create, read, update, and delete articles, with author-only editing and deletion.
- **Discovery:** searchable public article list, sorted by recency or incoming reference count.
- **Article references:** link articles together and view both outgoing and incoming references.
- **Access control:** signed JWT access tokens, revocation by token version, and password hashing using PBKDF2.
- **Deployment:** Docker Compose with a React/Nginx frontend, Spring Boot API, and persistent MySQL database.

## Tech stack

| Layer | Technologies |
| --- | --- |
| Frontend | React 18, JavaScript, React Router, Vite |
| Backend | Java 17, Spring Boot 3.5, Spring Data JPA, REST APIs |
| Database | MySQL 8.4 (H2 for integration tests) |
| Deployment | Docker Compose, Nginx reverse proxy, multi-stage Docker builds |
| Testing | JUnit / Spring MockMvc integration tests, configuration validation, HTTP smoke tests |

## Quick start (Docker)

**Requirements:** Docker Engine or Docker Desktop with Docker Compose v2, and Python 3 if you want automatic local secret generation. No local Java, Node.js, or MySQL installation is needed when using Docker.

1. Generate local credentials (never commit the generated `.env` file):

   ```bash
   python scripts/generate_env.py
   ```

   Alternatively, copy `.env.example` to `.env` and set strong values for `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, and `JWT_SECRET` (32+ random bytes).

2. Build and launch the application:

   ```bash
   docker compose up --build -d
   ```

3. Open **http://localhost:8080** in your browser. Create a user through the registration form.

4. Check status or view logs:

   ```bash
   docker compose ps
   docker compose logs -f
   ```

5. Stop without deleting saved database data:

   ```bash
   docker compose down
   ```

   To also delete the database volume, use `docker compose down -v` (**destructive**).

The public web port can be changed with `APP_PORT` in `.env`. Only Nginx is published to the host; the API and MySQL services remain on Docker networks.

## Project structure

```text
.
├── backend/                 # Spring Boot REST API and integration tests
├── frontend/                # React single-page application and Nginx config
├── docs/                    # Architecture, API, data model, and security docs
├── scripts/                 # Environment setup, configuration checks, smoke test
├── .github/workflows/       # Frontend / backend continuous integration
├── .env.example             # Example runtime configuration, without real secrets
└── docker-compose.yml       # Frontend + API + database
```

## Development and verification

**Backend** (Java 17 and Maven required for local development):

```bash
cd backend
mvn test
mvn spring-boot:run
```

A MySQL connection and the environment variables described in `backend/.env.example` are required for the running backend; the integration tests use an in-memory H2 database.

**Frontend** (Node.js 20+ recommended):

```bash
cd frontend
npm ci
npm run dev
npm run build
```

For a locally running API, copy `frontend/.env.example` to `frontend/.env.local`. The Docker production build uses relative `/api` URLs through Nginx.

**Additional checks:**

```bash
python scripts/validate_project.py
# After the Docker stack is up:
bash scripts/smoke-test.sh
```

## API overview

| Endpoint | Description |
| --- | --- |
| `POST /api/auth/register`, `/api/auth/login` | Create account and obtain an access token |
| `POST /api/auth/logout` | Invalidate the current user's issued tokens |
| `GET /api/articles` | Public article search and sorting |
| `GET /api/articles/{id}` | Authenticated article details and references |
| `POST /api/articles` | Create article |
| `PUT /api/articles/{id}`, `DELETE /api/articles/{id}` | Edit or remove your own article |
| `GET /api/profile` | Authenticated profile |
| `PUT /api/profile/contact`, `/api/profile/password` | Update profile |
| `GET /api/health` | API health |

See [REST API documentation](docs/API.md), [architecture](docs/ARCHITECTURE.md), [data model](docs/ERD.md), [security notes](docs/SECURITY_DESIGN.md), and [Docker design](docs/DOCKER_DESIGN.md).

## Security and deployment notes

- Never commit `.env`, real credentials, or database exports. A local generator is provided for secrets.
- Optional example articles can be enabled with `SEED_DEMO_DATA=true` **only if** `DEMO_PASSWORD` is also set to a unique, sufficiently strong password. The default is disabled.
- This is a **portfolio/educational application**, not a production security audit. The current client stores its token in browser `localStorage`, and production deployment would require additional hardening, TLS, monitoring, backups, and secret management.

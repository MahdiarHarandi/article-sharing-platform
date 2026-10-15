# Docker Design Decisions

## 1. Frontend image

### Build stage: `node:20-alpine`

The build stage needs Node.js and npm to install the locked dependencies with `npm ci` and create the optimized Vite bundle. Node 20 is compatible with the project's declared engine requirement. The Alpine variant keeps the temporary builder smaller.

### Runtime stage: `nginx:1.27-alpine`

The browser needs only static HTML, CSS, and JavaScript after the React build. Shipping Node.js and all development dependencies in production would be unnecessary. Nginx is designed for efficient static-file delivery and reverse proxying. The runtime image receives only `dist/` and the Nginx configuration.

The Nginx process runs as the existing unprivileged `nginx` user on container port 8080. Writable temporary paths are placed under `/tmp`, allowing the root filesystem to be read-only in Compose.

## 2. Backend image

### Build stage: `maven:3.9.9-eclipse-temurin-17-alpine`

The builder contains Maven and a Java 17 JDK, which are needed to resolve dependencies and compile the Spring Boot application. Dependencies are downloaded in a separate layer before source code is copied, improving rebuild caching.

### Runtime stage: `eclipse-temurin:17-jre-alpine`

Only a Java 17 JRE and the executable JAR are required at runtime. Maven, source files, and the Java compiler are absent from the final image. Eclipse Temurin is a widely used OpenJDK distribution. The application runs as a dedicated non-root `app` user.

`curl` is included only for the health check. Java memory behavior is configured through `JAVA_TOOL_OPTIONS` so the JVM respects the container memory limit more effectively.

## 3. Database image

The database uses the official `mysql:8.4` image. A custom Dockerfile is unnecessary because configuration is provided with supported environment variables and server command options. MySQL files are stored in a named volume mounted at `/var/lib/mysql`.

## 4. Multi-stage build benefits

- build tools do not exist in runtime images
- smaller attack surface
- smaller deployment artifacts
- deterministic dependency installation through `package-lock.json` and Maven
- independent caching of dependencies and source changes
- one reproducible build process on different systems

## 5. Networking

Two networks are deliberately used:

- `application_network`: frontend and backend
- `database_network`: backend and database

The database network is marked `internal: true`. The frontend has no network path to MySQL. The database has no published host port. The backend also has no published host port; it is reached only by the Nginx service name `backend`.

Docker's internal DNS resolves service names automatically, so Nginx connects to `backend:8080`, and Spring Boot connects to `database:3306`.

## 6. Persistence

`mysql_data` is a Docker named volume. Removing or recreating the database container does not remove this volume. Data is deleted only when the user explicitly runs `docker compose down -v` or removes the volume.

A named volume was selected instead of a bind mount because Docker manages its location and permissions consistently across Linux, Windows, and macOS.

## 7. Health and startup ordering

- MySQL is healthy only after `mysqladmin ping` succeeds.
- The backend starts after MySQL becomes healthy and is healthy only when `/api/health` responds successfully.
- The frontend starts after the backend becomes healthy and exposes `/healthz` for its own health check.

This reduces failures caused by starting dependent services too early.

## 8. Security measures

- secrets are runtime environment variables, not Docker build arguments
- `.env` is excluded from Git and submission packages
- `.dockerignore` prevents accidental secret/build-artifact copying
- Compose requires non-empty database passwords and JWT secret
- no default password or JWT secret exists in `application.yml`
- frontend and backend run as non-root users
- frontend and backend root filesystems are read-only
- `no-new-privileges` is enabled
- only Nginx is published to the host
- production demo data is disabled by default

For a larger production system, Docker secrets or an external secret manager would be preferable. For this single-host educational project, an ignored runtime `.env` file provides the requested simple protection without embedding secrets into images.

## 9. Trade-offs

Alpine images are compact, but their minimal toolset makes interactive debugging less convenient. A reverse proxy adds one internal network hop and another configuration layer, but it provides a single public endpoint, hides backend topology, avoids browser CORS complexity, and centralizes static serving and HTTP policies.

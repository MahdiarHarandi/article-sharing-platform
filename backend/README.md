# Backend API

Spring Boot 3.5 / Java 17 REST API with Spring Data JPA, MySQL persistence, JWT authentication, and ownership checks.

## Local checks

```bash
mvn test
mvn package
```

Integration tests use H2 in MySQL compatibility mode and require no external database. For runtime configuration, see the project root [README](../README.md) and [API reference](../docs/API.md).

The Docker image uses a multi-stage Maven/JDK build and a Java 17 JRE runtime.

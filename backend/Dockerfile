# syntax=docker/dockerfile:1

# Stage 1: compile the Spring Boot application with a complete Maven/JDK image.
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

# Cache dependencies separately from application sources.
COPY pom.xml ./
RUN mvn -B -ntp -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests clean package

# Stage 2: run only the packaged JAR on a small Java 17 JRE image.
FROM eclipse-temurin:17-jre-alpine AS runtime

RUN apk add --no-cache curl \
    && addgroup -S app \
    && adduser -S app -G app

WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/article-sharing-api.jar /app/app.jar

USER app
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/urandom"

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=5 \
  CMD curl --fail --silent --show-error http://127.0.0.1:8080/api/health >/dev/null || exit 1

STOPSIGNAL SIGTERM
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

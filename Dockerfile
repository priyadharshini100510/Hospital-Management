# ---- Build stage ----
# Compiles the app with Maven + JDK 17, producing a runnable jar.
# Using the wrapper's pinned Maven version keeps builds reproducible.
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Leverage Docker layer caching: copy only what's needed to resolve
# dependencies first, so `mvn dependency:go-offline` is cached across builds
# that only change application source code.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src ./src
RUN ./mvnw -q clean package -DskipTests

# ---- Run stage ----
# Small JRE-only image; the build stage's JDK and Maven caches are discarded.
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Run as a non-root user rather than the image's default root.
RUN groupadd -r spring && useradd -r -g spring spring
USER spring

COPY --from=build /app/target/*.jar app.jar

# Default to the production-hardened Spring profile.
# Override at runtime: docker run -e SPRING_PROFILES_ACTIVE=tidb ...
ENV SPRING_PROFILES_ACTIVE=prod

# Actual listen port is controlled by the PORT env var via
# server.port=${PORT:8080} in application-prod.properties; most PaaS hosts
# (Render, Railway, Fly.io) inject PORT automatically.
EXPOSE 8080

# Health check — lets Docker / container orchestrators know the app is ready.
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# SPRING_PROFILES_ACTIVE is set above but can be overridden at `docker run` time.
ENTRYPOINT ["java", "-jar", "app.jar"]

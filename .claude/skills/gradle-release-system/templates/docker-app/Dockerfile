# ============================================
# Multi-stage Dockerfile for Gradle/JVM application
# Optimised for minimal image size and security
# ============================================
# TODO: adjust module paths, JDK version, labels, and EXPOSE port for your project.

# ---- Stage 1: Build ----
FROM gradle:8.11-jdk21-alpine AS builder

# Build arguments for private registry authentication (e.g. GitHub Packages)
ARG GITHUB_USER
ARG GITHUB_TOKEN

ENV GITHUB_USER=${GITHUB_USER}
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

WORKDIR /build

# Copy only dependency-resolution files first (better layer caching)
COPY gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
# TODO: list your subproject build files for dependency caching
# COPY module-a/build.gradle.kts ./module-a/
# COPY module-b/build.gradle.kts ./module-b/

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source code
# TODO: list your subproject source dirs
# COPY module-a/src ./module-a/src
# COPY module-b/src ./module-b/src
COPY app.version ./

# Build fat JAR (Shadow plugin recommended for merging META-INF/services)
# TODO: adjust the Gradle task to your actual application module
RUN gradle :app:shadowJar --no-daemon

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine

# OCI image labels
# TODO: update source URL, description, and license
LABEL org.opencontainers.image.source="https://github.com/OWNER/REPO"
LABEL org.opencontainers.image.description="Application description"
LABEL org.opencontainers.image.licenses="MIT"

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Copy fat JAR from builder
# TODO: adjust the path to match your Shadow plugin output
COPY --from=builder /build/app/build/libs/app-all.jar ./app.jar

RUN chown -R appuser:appgroup /app

USER appuser

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# JVM container-aware flags
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]


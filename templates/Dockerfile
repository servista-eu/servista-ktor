# =============================================================================
# Servista Service Dockerfile
# =============================================================================
#
# Three-stage multi-stage build:
#   1. build   - Compile the application fat JAR
#   2. otel    - Download the OpenTelemetry Java Agent
#   3. runtime - Minimal JRE image with OTel agent attached
#
# CUSTOMIZE for your service:
#   - OTEL_SERVICE_NAME is set per-service in Kubernetes Deployment, not here
#   - Build task: uses buildFatJar (Ktor plugin). Change if using a different
#     build setup (e.g., shadowJar)
#   - JAR path: adjust the COPY --from=build path if your build output differs
#   - OTEL_AGENT_VERSION: update to latest stable release as needed
#
# =============================================================================

# ---------------------------------------------------------------------------
# Stage 1: Build the application
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /project

# Copy Gradle wrapper and build files first for better layer caching
COPY gradle/ gradle/
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and build
COPY src/ src/
RUN ./gradlew buildFatJar --no-daemon

# ---------------------------------------------------------------------------
# Stage 2: Download OpenTelemetry Java Agent
# ---------------------------------------------------------------------------
FROM alpine:3.21 AS otel

ARG OTEL_AGENT_VERSION=2.20.0

RUN wget -q -O /opentelemetry-javaagent.jar \
    "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar"

# ---------------------------------------------------------------------------
# Stage 3: Runtime image
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Copy OTel Java Agent from download stage
COPY --from=otel /opentelemetry-javaagent.jar /opt/opentelemetry-javaagent.jar

# Copy application fat JAR from build stage
# NOTE: Adjust the source path if your build output location differs
COPY --from=build /project/build/libs/*-all.jar /app/app.jar

# Attach OTel Java Agent via JAVA_TOOL_OPTIONS
# Using JAVA_TOOL_OPTIONS instead of modifying ENTRYPOINT makes it easy to
# disable the agent for debugging: docker run -e JAVA_TOOL_OPTIONS="" my-service
ENV JAVA_TOOL_OPTIONS="-javaagent:/opt/opentelemetry-javaagent.jar"

# Default OTel configuration -- override per environment via Kubernetes
# OTEL_SERVICE_NAME is set per-service in Kubernetes Deployment
ENV OTEL_EXPORTER_OTLP_ENDPOINT="http://otel-collector:4317"
ENV OTEL_EXPORTER_OTLP_PROTOCOL="grpc"
ENV OTEL_TRACES_SAMPLER="parentbased_traceidratio"
ENV OTEL_TRACES_SAMPLER_ARG="0.1"

# Production logging format (JSON). Set to "console" for local development.
ENV SERVISTA_LOG_FORMAT="json"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Phase 1: Service Scaffold Template - Context

**Gathered:** 2026-03-04
**Status:** Ready for planning

<domain>
## Phase Boundary

Create a shared `servista-ktor` library that provides the Ktor-specific runtime wiring all backend services need: status-pages error handling (RFC 7807), health check routes, ServistaContext header extraction, content negotiation, Flyway integration, structured logging configuration, and OTel agent setup. Includes reference files (Dockerfile, Kubernetes manifests, Flyway migration directory structure) for bootstrapping new service repos. This phase bridges Phase 3's dependency-only convention plugins and Phase 4's framework-agnostic commons SDK with concrete Ktor application infrastructure.

</domain>

<decisions>
## Implementation Decisions

### Scaffold delivery
- **New shared library repo:** `servista-ktor`, published to Forgejo Maven as `eu.servista:servista-ktor`
- Follows the multi-repo pattern (like gradle-platform, servista-commons, servista-avro-schemas)
- Clear boundary: servista-commons = framework-agnostic abstractions, servista-ktor = Ktor-specific runtime wiring
- **Single dependency** for now — services add one `implementation("eu.servista:servista-ktor:x.y.z")` and get everything
- Modularization (splitting into servista-ktor-core, servista-ktor-health, etc.) deferred until needed — migration path is clean (original artifact becomes meta-dependency)
- **Includes `templates/` directory** with reference files: Dockerfile, Kubernetes deployment manifests, application.conf template, Flyway migration directory structure — available as copy-paste starting points for new services
- Applies `servista.api-service` + `servista.testing` + `servista.observability` convention plugins from gradle-platform

### Configuration loading
- **HOCON with environment variable substitution** — Ktor's built-in `application.conf` format
- **Single config file** per service — no profile-based files (application-dev.conf, application-prod.conf). All environment differences come from env vars injected by Kubernetes ConfigMaps/Secrets
- **Standard `servista {}` HOCON block** defined by servista-ktor with consistent keys across all services:
  - `servista.database.url`, `servista.database.user`, `servista.database.password`
  - `servista.kafka.brokers`, `servista.kafka.schema-registry-url`
  - `servista.observability.*` (OTel agent config, log format)
  - `servista.health.*` (probe config)
  - `servista.logging.requests.*` (request logging config)
- Services extend with their own HOCON sections for service-specific config
- Reference `application.conf` in `templates/` shows the full standard structure with documented env var substitution points
- **Testcontainers for local development** — reference config includes localhost defaults; developers use Testcontainers (already in `servista.testing` plugin) for PostgreSQL, Kafka, Valkey without needing a Kubernetes cluster

### OTel approach
- **Java Agent only** — OTel Java Agent as `-javaagent` JVM argument in the Dockerfile
- Auto-instruments Ktor HTTP, JDBC/jOOQ queries, Kafka producer/consumer, Logback MDC — zero instrumentation code needed
- Custom business spans available later via `GlobalOpenTelemetry.getTracer()` — the agent provides the SDK API at runtime, no SDK dependency needed in servista-ktor or service repos
- **OTLP export to OTel Collector** (gRPC) — decouples services from the tracing/metrics backend. Collector configuration is IaC territory
- **Head-based 10% sampling** in production — `OTEL_TRACES_SAMPLER=parentbased_traceidratio`, `OTEL_TRACES_SAMPLER_ARG=0.1`. Errors always sampled (configured in collector)
- **Metrics enabled** — OTel agent auto-exports JVM metrics (heap, GC, threads) and HTTP/Kafka metrics alongside traces. Same OTLP export to collector

### Logging structure
- **JSON in production, console in development** — switch via `SERVISTA_LOG_FORMAT=json|console` environment variable
- JSON format via logstash-logback-encoder (or equivalent) for production/staging
- Human-readable colored console output for local development — reading JSON in a terminal is painful
- **Standard JSON log fields:**
  - `timestamp`, `level`, `logger`, `message`, `thread`
  - `service_name`, `service_version` (from build info)
  - `trace_id`, `span_id` (auto-injected by OTel agent into Logback MDC)
  - `correlation_id`, `organization_id`, `account_id` (from ServistaContext MDC bridge, Phase 4)
  - Exception stack traces as nested JSON
- **Request logging enabled by default** via Ktor CallLogging plugin:
  - Logs: method, path, status code, duration_ms, organization_id, account_id
  - Excludes health check endpoints (`/health/*`) to avoid noise
  - **Configurable opt-out** via HOCON: `servista.logging.requests.enabled = false`
  - Configurable log level: `servista.logging.requests.level = INFO`
  - Configurable exclude paths: `servista.logging.requests.exclude-paths = ["/health/*"]`
- **Stdout output** — services log to stdout (Kubernetes standard). Log collection (Promtail, Fluent Bit, OTel Collector) scrapes container stdout and ships to backend. Generic JSON format, not coupled to any specific log backend

### Claude's Discretion
- Exact Ktor plugin installation order and module organization
- Flyway integration implementation (auto-migration on startup vs explicit migration command)
- logstash-logback-encoder vs alternative JSON encoder choice
- Reference Dockerfile structure (multi-stage build, base image, OTel agent download)
- Reference Kubernetes manifests structure (Deployment, Service, probes, ConfigMap references)
- Content negotiation configuration details
- How `installServista()` or equivalent setup function is structured
- Test strategy for servista-ktor (TestKit, integration tests with Testcontainers)
- Internal package structure within servista-ktor

</decisions>

<specifics>
## Specific Ideas

- User explicitly wants the ability to modularize servista-ktor later without breaking consumers — single JAR now, split into submodules when/if services need different subsets
- Request logging must remain configurable (opt-in/opt-out per service) even though it's enabled by default — configurability is important
- OTel custom spans should be available to services without adding SDK dependencies — the agent provides the API at runtime
- Reference files in `templates/` are copy-paste starting points, not auto-applied — keeps the library focused while giving services a head start
- Local dev experience matters — Testcontainers + console logging + localhost defaults so developers can code without a Kubernetes cluster

</specifics>

<code_context>
## Existing Code Insights

### Reusable Assets
- **servista-commons (Phase 4):** ServistaException hierarchy (sealed class with RFC 7807 fields), HealthCheck/HealthRegistry interfaces, ServistaContext (CoroutineContext element with MDC bridging), EventEnvelope serializer/deserializer, Kafka consumer wrapper with DLQ routing
- **gradle-platform convention plugins (Phase 3):** `servista.api-service` (Ktor + HTTP), `servista.observability` (OTel + Micrometer + kotlin-logging + Logback), `servista.testing` (JUnit 5 + Testcontainers + MockK + Kotest), `servista.jooq` (jOOQ + HikariCP + Flyway)
- **servista-avro-schemas (Phase 5):** Generated Avro classes for event types
- **Version catalog:** All dependency versions locked in gradle-platform libs.versions.toml

### Established Patterns
- Multi-repo with Forgejo Maven registry for artifact distribution
- Convention plugins provide dependencies; runtime behavior lives in consuming code (this phase fills that gap)
- Framework-agnostic commons + framework-specific scaffold (explicit separation decided in Phase 4)
- Single `application.conf` HOCON with env var substitution (Ktor standard)
- Composable plugin model: services pick convention plugins they need

### Integration Points
- **servista-commons:** servista-ktor depends on servista-commons for ServistaException, HealthCheck, ServistaContext, Kafka wrappers — wires them into Ktor
- **gradle-platform:** servista-ktor applies convention plugins; services apply both convention plugins and depend on servista-ktor
- **All service repos:** Every API service, event sink, and pipeline service will depend on servista-ktor for consistent runtime behavior
- **Phase 9 (CI Pipeline):** Tekton templates will reference the standard Dockerfile and OTel agent configuration from this phase
- **IaC governance:** OTel Collector deployment and log collection infrastructure are IaC territory, informed by the export formats defined here

</code_context>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 01-ktor-scaffold*
*Context gathered: 2026-03-04*

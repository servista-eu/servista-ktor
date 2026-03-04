# Phase 1: Service Scaffold Template - Research

**Researched:** 2026-03-04
**Domain:** Ktor runtime wiring library (servista-ktor), structured logging, health checks, OTel agent, Flyway, Kafka boilerplate
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **New shared library repo:** `servista-ktor`, published to Forgejo Maven as `eu.servista:servista-ktor`
- Follows the multi-repo pattern (like gradle-platform, servista-commons, servista-avro-schemas)
- Clear boundary: servista-commons = framework-agnostic abstractions, servista-ktor = Ktor-specific runtime wiring
- **Single dependency** for now -- services add one `implementation("eu.servista:servista-ktor:x.y.z")` and get everything
- Modularization deferred until needed -- migration path is clean (original artifact becomes meta-dependency)
- **Includes `templates/` directory** with reference files: Dockerfile, Kubernetes deployment manifests, application.conf template, Flyway migration directory structure
- Applies `servista.api-service` + `servista.testing` + `servista.observability` convention plugins from gradle-platform
- **HOCON with environment variable substitution** -- Ktor's built-in `application.conf` format
- **Single config file** per service -- no profile-based files. All environment differences from env vars via Kubernetes ConfigMaps/Secrets
- **Standard `servista {}` HOCON block** with consistent keys: database, kafka, observability, health, logging.requests
- **Testcontainers for local development** -- localhost defaults with Testcontainers for PostgreSQL, Kafka, Valkey
- **OTel Java Agent only** -- `-javaagent` JVM argument in the Dockerfile, zero instrumentation code
- Auto-instruments Ktor HTTP, JDBC/jOOQ, Kafka, Logback MDC
- Custom business spans via `GlobalOpenTelemetry.getTracer()` -- agent provides SDK API at runtime
- **OTLP export to OTel Collector** (gRPC), collector config is IaC territory
- **Head-based 10% sampling** in production -- `OTEL_TRACES_SAMPLER=parentbased_traceidratio`, `OTEL_TRACES_SAMPLER_ARG=0.1`
- **Metrics enabled** -- OTel agent auto-exports JVM + HTTP + Kafka metrics via OTLP
- **JSON in production, console in development** -- switch via `SERVISTA_LOG_FORMAT=json|console`
- JSON format via logstash-logback-encoder (or equivalent)
- Human-readable colored console output for local development
- **Standard JSON log fields:** timestamp, level, logger, message, thread, service_name, service_version, trace_id, span_id, correlation_id, organization_id, account_id
- **Request logging enabled by default** via Ktor CallLogging plugin with health endpoint exclusion
- **Configurable opt-out** via HOCON: `servista.logging.requests.enabled`, `.level`, `.exclude-paths`
- **Stdout output** -- services log to stdout (Kubernetes standard)

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

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| FOUND-06 | Service scaffold template -- database migrations (Flyway), health checks, structured logging, OpenTelemetry tracing, Kafka producer/consumer boilerplate | All components researched: Flyway 12.0.3 programmatic API, HealthRegistry wiring to Ktor routes, logstash-logback-encoder 8.1 for JSON logging, OTel Java Agent 2.20.0 auto-instrumentation, Kafka producer/consumer wiring via servista-commons abstractions |
</phase_requirements>

## Summary

Phase 1 creates a new shared library repo (`servista-ktor`) that bridges the gap between Phase 3's convention plugins (compile-time dependency management) and Phase 4's framework-agnostic commons SDK (domain abstractions) by providing Ktor-specific runtime wiring. This library gives every service a single dependency that installs StatusPages error handling (RFC 7807 via `ProblemDetail`), health check routes (wiring `HealthRegistry` to `/health/startup`, `/health/live`, `/health/ready`), structured JSON logging (logstash-logback-encoder 8.1 with OTel trace correlation), Flyway auto-migration on startup, Kafka producer/consumer setup, content negotiation, and request logging.

The upstream code from servista-commons is already well-designed for this integration: `ServistaException` carries `statusCode` and `toProblemDetail()` for direct StatusPages mapping, `HealthRegistry` aggregates named checks by probe type, `ServistaContext` bridges MDC across coroutine boundaries, and `RlsSessionManager` enforces tenant isolation at the database layer. The servista-ktor library wires these into Ktor's plugin system and provides a single `installServista()` extension function (or equivalent) that services call in their Application module.

**Primary recommendation:** Build `servista-ktor` as a new multi-repo library following the established pattern from servista-commons (same settings.gradle.kts structure, convention plugin application, Forgejo Maven publishing). Provide a single `Application.installServista(config)` function that installs all Ktor plugins in correct order, plus a `templates/` directory with copy-paste reference files for bootstrapping new service repos.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| io.ktor:ktor-server-core | 3.4.0 | Ktor server framework | Already in version catalog, locked by convention plugin |
| io.ktor:ktor-server-netty | 3.4.0 | HTTP server engine | Already in version catalog, Netty auto-instrumented by OTel |
| io.ktor:ktor-server-status-pages | 3.4.0 | Exception-to-HTTP-response mapping | Already in convention plugin, maps ServistaException to RFC 7807 |
| io.ktor:ktor-server-content-negotiation | 3.4.0 | JSON serialization for requests/responses | Already in convention plugin |
| io.ktor:ktor-serialization-kotlinx-json | 3.4.0 | kotlinx.serialization converter for ContentNegotiation | Already in convention plugin |
| io.ktor:ktor-server-call-logging | 3.4.0 | Request logging with MDC | Already in convention plugin |
| io.ktor:ktor-server-metrics-micrometer | 3.4.0 | Micrometer metrics + Prometheus scrape endpoint | Already in version catalog |
| io.insert-koin:koin-ktor | 4.1.1 | Dependency injection | Already in convention plugin |
| io.micrometer:micrometer-registry-prometheus | 1.16.3 | Prometheus metrics registry | Already in observability convention plugin |
| net.logstash.logback:logstash-logback-encoder | 8.1 | JSON structured logging for production | De facto standard for Logback JSON; 8.1 compatible with Jackson 2.x and Logback 1.5.x |
| ch.qos.logback:logback-classic | 1.5.32 | Logging runtime | Already in observability convention plugin |
| io.github.oshai:kotlin-logging-jvm | 7.0.3 | Kotlin logging facade | Already in observability convention plugin |
| org.flywaydb:flyway-core | 12.0.3 | Database migration | Already in jooq convention plugin |
| org.flywaydb:flyway-database-postgresql | 12.0.3 | PostgreSQL-specific Flyway support | Already in jooq convention plugin |
| com.zaxxer:HikariCP | 7.0.2 | Connection pooling | Already in jooq convention plugin |
| org.postgresql:postgresql | 42.7.10 | JDBC driver | Already in jooq convention plugin |
| eu.servista:servista-commons | 0.1.0 | Framework-agnostic commons (ServistaException, HealthRegistry, ServistaContext, Kafka wrappers) | Upstream dependency from Phase 4 |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| io.ktor:ktor-server-test-host | 3.4.0 | In-process test server | Testing servista-ktor plugin installation without real HTTP |
| io.insert-koin:koin-test | 4.1.1 | Koin DI testing | Verifying DI module configuration in tests |
| org.testcontainers:testcontainers | 2.0.3 | Test container orchestration | Integration tests for Flyway, health checks, Kafka |
| org.testcontainers:testcontainers-postgresql | 2.0.3 | PostgreSQL test container | Flyway migration and RLS integration tests |
| org.testcontainers:testcontainers-kafka | 2.0.3 | Kafka test container | Kafka producer/consumer integration tests |
| io.opentelemetry.javaagent:opentelemetry-javaagent | 2.20.0 | OTel auto-instrumentation agent | Referenced in Dockerfile only, NOT a compile dependency |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| logstash-logback-encoder 8.1 | Logback's built-in JsonLayout | logstash-logback-encoder offers far richer customization (custom fields, MDC providers, nested exceptions as JSON); built-in JsonLayout is limited |
| logstash-logback-encoder 8.1 | logstash-logback-encoder 9.0 | 9.0 requires Jackson 3.x and Java 17; while Java 21 is fine, Jackson 3.x may conflict with other dependencies. Use 8.1 for Jackson 2.x compatibility |
| Auto-migrate on startup | CLI-based migration | Auto-migrate is simpler for development and matches the "services own their schema" pattern; CLI migration adds operational complexity with no benefit at this stage |
| Single `installServista()` function | Individual plugin installs | Single function ensures correct plugin ordering and prevents services from accidentally missing required plugins; can still be decomposed later |

**Dependencies to add (beyond what convention plugins provide):**
```kotlin
// In servista-ktor build.gradle.kts
dependencies {
    implementation("eu.servista:servista-commons:0.1.0")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    implementation(libs.ktor.server.metrics.micrometer)
}
```

Note: Most dependencies are already provided by the `servista.api-service`, `servista.observability`, and `servista.jooq` convention plugins. The library only needs to add servista-commons (upstream), logstash-logback-encoder (new), and ktor-server-metrics-micrometer (in catalog but not in a convention plugin yet).

## Architecture Patterns

### Recommended Project Structure
```
servista-ktor/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties                  # version=0.1.0, group=eu.servista
├── config/
│   └── detekt/
│       └── detekt.yml                 # Local detekt config (same pattern as servista-commons)
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── eu/servista/ktor/
│   │   │       ├── ServistaApplication.kt       # installServista() extension + config DSL
│   │   │       ├── ServistaConfig.kt            # HOCON → typed config data classes
│   │   │       ├── error/
│   │   │       │   └── StatusPagesSetup.kt      # StatusPages plugin wiring
│   │   │       ├── health/
│   │   │       │   └── HealthRoutes.kt          # /health/* routes wiring HealthRegistry
│   │   │       ├── logging/
│   │   │       │   └── RequestLoggingSetup.kt   # CallLogging plugin configuration
│   │   │       ├── database/
│   │   │       │   └── DatabaseSetup.kt         # HikariCP + Flyway auto-migration
│   │   │       ├── metrics/
│   │   │       │   └── MetricsSetup.kt          # MicrometerMetrics + /metrics route
│   │   │       ├── context/
│   │   │       │   └── ContextInterceptor.kt    # ServistaContext from request headers
│   │   │       └── serialization/
│   │   │           └── ContentNegotiationSetup.kt  # JSON + problem+json content negotiation
│   │   └── resources/
│   │       ├── logback-json.xml                 # JSON logging config (production)
│   │       ├── logback-console.xml              # Console logging config (development)
│   │       └── logback.xml                      # Routing config (delegates based on SERVISTA_LOG_FORMAT)
│   └── test/
│       └── kotlin/
│           └── eu/servista/ktor/
│               ├── ServistaApplicationTest.kt   # Full integration test
│               ├── error/
│               │   └── StatusPagesTest.kt       # Exception → RFC 7807 response tests
│               ├── health/
│               │   └── HealthRoutesTest.kt      # Health probe endpoint tests
│               ├── database/
│               │   └── DatabaseSetupTest.kt     # Flyway migration integration test
│               └── logging/
│                   └── RequestLoggingTest.kt    # Request log format/filter tests
└── templates/
    ├── Dockerfile                               # Multi-stage build with OTel agent
    ├── k8s/
    │   ├── deployment.yaml                      # Deployment with probes + env vars
    │   ├── service.yaml                         # ClusterIP Service
    │   └── configmap.yaml                       # ConfigMap for application.conf overrides
    ├── application.conf                         # Reference HOCON with all servista.* keys
    └── db/
        └── migration/
            └── V1__initial_schema.sql           # Example migration with RLS policy
```

### Pattern 1: Single Entry Point (installServista)
**What:** A single `Application.installServista()` extension function that installs all Ktor plugins in the correct dependency order and registers Koin modules for infrastructure services.
**When to use:** Every Servista API service calls this in its Application module.
**Example:**
```kotlin
// Source: Architecture decision from CONTEXT.md
fun Application.installServista() {
    val config = ServistaConfig.load(environment.config)

    // 1. Content negotiation (must be before StatusPages for JSON error responses)
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    // 2. Status pages (maps ServistaException → RFC 7807 ProblemDetail)
    install(StatusPages) {
        exception<ServistaException> { call, cause ->
            call.response.header(
                HttpHeaders.ContentType,
                "application/problem+json"
            )
            call.respond(
                HttpStatusCode.fromValue(cause.statusCode),
                cause.toProblemDetail()
            )
        }
        exception<Throwable> { call, cause ->
            logger.error(cause) { "Unhandled exception" }
            call.respond(
                HttpStatusCode.InternalServerError,
                ProblemDetail(
                    type = "urn:servista:error:internal:unhandled",
                    title = "Internal Server Error",
                    status = 500,
                )
            )
        }
    }

    // 3. Request logging (with health endpoint exclusion)
    if (config.logging.requests.enabled) {
        install(CallLogging) {
            level = config.logging.requests.logLevel
            filter { call ->
                config.logging.requests.excludePaths.none { pattern ->
                    call.request.path().startsWith(pattern.removeSuffix("*"))
                }
            }
            mdc("correlation_id") { call ->
                call.request.header("X-Correlation-Id")
            }
            mdc("organization_id") { call ->
                call.request.header("X-Organization-Id")
            }
            mdc("account_id") { call ->
                call.request.header("X-Account-Id")
            }
        }
    }

    // 4. Metrics (Prometheus scrape endpoint)
    val promRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        registry = promRegistry
    }

    // 5. Context interceptor (extract headers → ServistaContext for coroutines)
    // 6. Health routes
    // 7. Metrics route (/metrics)
    // 8. Database + Flyway (if config.database is present)
}
```

### Pattern 2: Typed HOCON Configuration
**What:** Data classes that mirror the `servista {}` HOCON block, loaded via Ktor's `ApplicationConfig`.
**When to use:** Reading configuration from application.conf with environment variable substitution.
**Example:**
```kotlin
// Source: Ktor HOCON configuration docs (https://ktor.io/docs/server-configuration-file.html)
data class ServistaConfig(
    val database: DatabaseConfig?,
    val kafka: KafkaConfig?,
    val logging: LoggingConfig,
    val health: HealthConfig,
) {
    companion object {
        fun load(config: ApplicationConfig): ServistaConfig {
            val servista = config.config("servista")
            return ServistaConfig(
                database = servista.configOrNull("database")?.let { DatabaseConfig.from(it) },
                kafka = servista.configOrNull("kafka")?.let { KafkaConfig.from(it) },
                logging = LoggingConfig.from(servista.config("logging")),
                health = HealthConfig.from(servista.config("health")),
            )
        }
    }
}

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
) {
    companion object {
        fun from(config: ApplicationConfig) = DatabaseConfig(
            url = config.property("url").getString(),
            user = config.property("user").getString(),
            password = config.property("password").getString(),
        )
    }
}
```

### Pattern 3: Flyway Auto-Migration on Startup
**What:** Run Flyway migrations automatically when the application starts, before jOOQ connects.
**When to use:** Every service that has a database. Migration runs once on startup.
**Example:**
```kotlin
// Source: Flyway Java API docs (https://documentation.red-gate.com/fd/api-java-277579358.html)
fun migrateDatabase(config: DatabaseConfig): HikariDataSource {
    val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = config.url
        username = config.user
        password = config.password
        maximumPoolSize = 10
    })

    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .cleanDisabled(true)  // CRITICAL: prevent accidental clean in production
        .load()
        .migrate()

    return dataSource  // Return for jOOQ DSLContext creation
}
```

### Pattern 4: Health Route Wiring
**What:** Wire `HealthRegistry` from servista-commons into Ktor HTTP routes at `/health/startup`, `/health/live`, `/health/ready`.
**When to use:** Every service exposes these endpoints for Kubernetes probes.
**Example:**
```kotlin
// Source: Kubernetes probe docs + servista-commons HealthRegistry
fun Route.healthRoutes(healthRegistry: HealthRegistry) {
    route("/health") {
        get("/startup") {
            val response = healthRegistry.checkStartup()
            call.respond(response.status.toHttpStatus(), response)
        }
        get("/live") {
            val response = healthRegistry.checkLiveness()
            call.respond(response.status.toHttpStatus(), response)
        }
        get("/ready") {
            val response = healthRegistry.checkReadiness()
            call.respond(response.status.toHttpStatus(), response)
        }
    }
}

private fun HealthStatus.toHttpStatus(): HttpStatusCode = when (this) {
    HealthStatus.UP -> HttpStatusCode.OK
    HealthStatus.DEGRADED -> HttpStatusCode.OK  // Still serving, but warn
    HealthStatus.DOWN -> HttpStatusCode.ServiceUnavailable
}
```

### Pattern 5: Context Interceptor
**What:** Extract `X-Organization-Id`, `X-Account-Id`, `X-Correlation-Id` headers from incoming requests (injected by APISIX gateway) and create a `ServistaContext` for the coroutine scope.
**When to use:** Every authenticated API request. Headers are set by the gateway after token validation.
**Example:**
```kotlin
// Source: servista-commons ServistaContext + Ktor intercept pattern
fun Application.installContextInterceptor() {
    intercept(ApplicationCallPipeline.Plugins) {
        val correlationId = call.request.header("X-Correlation-Id")
            ?: java.util.UUID.randomUUID().toString()
        val organizationId = call.request.header("X-Organization-Id")?.toLongOrNull()
        val accountId = call.request.header("X-Account-Id")?.toLongOrNull()

        val ctx = ServistaContext(
            correlationId = correlationId,
            organizationId = organizationId,
            accountId = accountId,
        )
        // Add to coroutine context so downstream suspend functions inherit it
        withContext(ctx) {
            proceed()
        }
    }
}
```

### Pattern 6: RLS-Enforcing Database Access Pattern
**What:** The scaffold demonstrates the pattern where every database call is wrapped in `RlsSessionManager.withOrganization()` to enforce tenant isolation. This is not enforced by servista-ktor itself (it cannot intercept jOOQ calls), but the template migration includes RLS policies and the reference code shows the mandatory pattern.
**When to use:** Every database query in every service.
**Example reference migration:**
```sql
-- V1__initial_schema.sql (in templates/db/migration/)
-- Example: Create a tenant-scoped table with RLS

CREATE TABLE example_entity (
    id BIGINT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- RLS policy: rows only visible when app.current_organization_id matches
ALTER TABLE example_entity ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON example_entity
    USING (organization_id = current_setting('app.current_organization_id')::BIGINT);

-- Force RLS even for table owner (prevents bypassing via superuser)
ALTER TABLE example_entity FORCE ROW LEVEL SECURITY;
```

### Anti-Patterns to Avoid
- **Skipping RLS in migrations:** Every table with tenant data MUST have `ENABLE ROW LEVEL SECURITY` and a `FORCE ROW LEVEL SECURITY` policy. The template migration demonstrates this pattern. Without FORCE, the table owner bypasses RLS.
- **Installing StatusPages before ContentNegotiation:** The JSON serializer must be registered before StatusPages tries to `call.respond()` with a `ProblemDetail` object. ContentNegotiation first.
- **Adding OTel SDK as a compile dependency:** The OTel Java Agent provides the API at runtime. Adding `opentelemetry-api` as a compile dependency creates version conflicts and classpath issues. Only reference `GlobalOpenTelemetry.getTracer()` when needed -- it's available at runtime via the agent.
- **Profile-based config files:** Do not create `application-dev.conf`, `application-prod.conf`, etc. Use a single `application.conf` with `${?ENV_VAR}` substitution. All environment differences come from Kubernetes ConfigMaps/Secrets.
- **Calling `Flyway.clean()` without guards:** Always set `cleanDisabled(true)` in production. The template defaults to clean disabled.
- **Manual HikariCP pool creation per service:** The scaffold should create and manage the pool. Services should receive a `DSLContext` from Koin, not create their own DataSource.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JSON structured logging | Custom Logback JSON encoder | logstash-logback-encoder 8.1 | MDC integration, nested exception serialization, custom field providers, battle-tested |
| RFC 7807 error responses | Custom error response format | StatusPages + `ProblemDetail.toProblemDetail()` | Already built in servista-commons with full RFC 7807 field set |
| Database connection pooling | Manual connection management | HikariCP (via jooq convention plugin) | Pool sizing, leak detection, metrics, connection validation |
| Database migrations | Manual SQL scripts or custom runner | Flyway 12.0.3 programmatic API | Versioning, checksums, baseline, repair, team collaboration |
| Distributed tracing | Manual span creation | OTel Java Agent auto-instrumentation | Zero-code instrumentation of Ktor HTTP, JDBC, Kafka, Logback MDC |
| Prometheus metrics | Custom metrics endpoint | Ktor MicrometerMetrics + PrometheusMeterRegistry | Auto-collects HTTP request metrics, JVM metrics; scrape endpoint built-in |
| Health check aggregation | Custom health endpoint logic | HealthRegistry from servista-commons | Already built with startup/liveness/readiness separation and aggregation |
| Coroutine context propagation | Manual MDC management | ServistaContext (CopyableThreadContextElement) | Already built in servista-commons with proper MDC bridging across suspension points |

**Key insight:** Phase 4 (servista-commons) deliberately built framework-agnostic abstractions (`HealthCheck`, `ServistaException`, `ServistaContext`, `RlsSessionManager`) so that Phase 1 only needs to wire them into Ktor -- not rebuild them. The entire value of servista-ktor is the wiring layer, not new domain logic.

## Common Pitfalls

### Pitfall 1: Plugin Installation Order
**What goes wrong:** StatusPages attempts to serialize a response object but ContentNegotiation is not installed, resulting in `UnsupportedMediaTypeException` or empty responses.
**Why it happens:** Ktor plugins are installed in order. StatusPages needs ContentNegotiation's serialization support to convert `ProblemDetail` objects to JSON.
**How to avoid:** Install ContentNegotiation BEFORE StatusPages. The `installServista()` function enforces this order.
**Warning signs:** Error responses returning 500 or empty bodies instead of JSON `ProblemDetail`.

### Pitfall 2: logstash-logback-encoder Version vs Jackson
**What goes wrong:** Using logstash-logback-encoder 9.0 fails at runtime because it requires Jackson 3.x, which conflicts with libraries still on Jackson 2.x.
**Why it happens:** The 9.0 release dropped Jackson 2.x support entirely.
**How to avoid:** Use logstash-logback-encoder 8.1 (latest 8.x), which is compatible with Jackson 2.x and Logback 1.5.x. This version requires Java 11+ (we use JVM 21, so fine).
**Warning signs:** `NoClassDefFoundError` or `NoSuchMethodError` referencing Jackson classes at startup.

### Pitfall 3: HikariCP Connection Leak with RLS
**What goes wrong:** A connection is returned to the pool with `app.current_organization_id` still set, and the next request using that connection reads another tenant's data.
**Why it happens:** If `RlsSessionManager.withOrganization()` is not used (or its finally block is bypassed), the PostgreSQL session variable persists on the connection.
**How to avoid:** `RlsSessionManager` already has a `finally` block that resets the variable. The template migration includes `FORCE ROW LEVEL SECURITY` as a safety net -- even the table owner must satisfy the RLS policy.
**Warning signs:** Test data from one organization appearing in another's queries.

### Pitfall 4: OTel Agent Classpath Conflicts
**What goes wrong:** Adding `opentelemetry-api` or `opentelemetry-sdk` as compile dependencies causes `ClassCastException` or duplicate class issues when the agent also provides these classes at runtime.
**Why it happens:** The OTel Java Agent injects its own API and SDK classes via bytecode instrumentation. Having the same classes on the application classpath creates conflicts.
**How to avoid:** Never add OTel dependencies to the build. If custom spans are needed later, use `GlobalOpenTelemetry.getTracer()` at runtime -- the agent provides the API.
**Warning signs:** `ClassCastException` involving OTel types, or tracing not working despite agent attachment.

### Pitfall 5: Flyway Migration Location Mismatch
**What goes wrong:** Flyway finds no migrations and reports "nothing to migrate" even though SQL files exist.
**Why it happens:** Default Flyway location is `classpath:db/migration` but files are placed elsewhere, or the directory is not included in the JAR's classpath resources.
**How to avoid:** Always place migrations in `src/main/resources/db/migration/`. Verify with `Flyway.configure().locations("classpath:db/migration")` explicitly.
**Warning signs:** Flyway reports 0 pending migrations when migrations should exist.

### Pitfall 6: Testcontainers 2.x Module Renames
**What goes wrong:** Build fails with "module not found" for `org.testcontainers:postgresql` or `org.testcontainers:kafka`.
**Why it happens:** Testcontainers 2.x renamed modules: `postgresql` -> `testcontainers-postgresql`, `kafka` -> `testcontainers-kafka`. The convention plugin in gradle-platform still uses old names.
**How to avoid:** Apply the same workaround as servista-commons: exclude old module names and depend on new module names explicitly.
**Warning signs:** Gradle dependency resolution failure referencing missing Testcontainers modules.

## Code Examples

### Reference application.conf (for templates/ directory)
```hocon
# Servista Service Configuration
# All environment differences come from env vars (Kubernetes ConfigMaps/Secrets)
# Use ${?VAR} syntax for optional substitution (keeps default if VAR not set)

ktor {
    deployment {
        port = 8080
        port = ${?PORT}
    }
    application {
        modules = [ com.example.ApplicationKt.module ]
    }
}

servista {
    database {
        url = "jdbc:postgresql://localhost:5432/myservice"
        url = ${?SERVISTA_DATABASE_URL}
        user = "myservice"
        user = ${?SERVISTA_DATABASE_USER}
        password = "changeme"
        password = ${?SERVISTA_DATABASE_PASSWORD}
    }

    kafka {
        brokers = "localhost:9092"
        brokers = ${?SERVISTA_KAFKA_BROKERS}
        schema-registry-url = "http://localhost:8081"
        schema-registry-url = ${?SERVISTA_KAFKA_SCHEMA_REGISTRY_URL}
    }

    logging {
        requests {
            enabled = true
            enabled = ${?SERVISTA_LOG_REQUESTS_ENABLED}
            level = "INFO"
            level = ${?SERVISTA_LOG_REQUESTS_LEVEL}
            exclude-paths = ["/health"]
        }
    }

    health {
        # No special config needed -- health routes are always registered
    }
}
```

### Reference Dockerfile (for templates/ directory)
```dockerfile
# Multi-stage build: build app, download OTel agent, create runtime image
# Source: OTel Agent docs (https://opentelemetry.io/docs/zero-code/java/agent/)

# Stage 1: Build application
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew shadowJar --no-daemon

# Stage 2: Download OTel Java Agent
FROM alpine:3.21 AS otel
ARG OTEL_AGENT_VERSION=2.20.0
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar /opt/opentelemetry-javaagent.jar

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Copy OTel agent
COPY --from=otel /opt/opentelemetry-javaagent.jar /opt/opentelemetry-javaagent.jar

# Copy application JAR
COPY --from=build /app/build/libs/*-all.jar /app/app.jar

# OTel agent configuration via environment variables (set in Kubernetes)
ENV JAVA_TOOL_OPTIONS="-javaagent:/opt/opentelemetry-javaagent.jar"
# Default OTLP endpoint -- override in Kubernetes
ENV OTEL_EXPORTER_OTLP_ENDPOINT="http://otel-collector:4317"
ENV OTEL_EXPORTER_OTLP_PROTOCOL="grpc"
# Service name set per-service in Kubernetes Deployment
# ENV OTEL_SERVICE_NAME="my-service"
# Sampling: 10% in production, 100% in dev
ENV OTEL_TRACES_SAMPLER="parentbased_traceidratio"
ENV OTEL_TRACES_SAMPLER_ARG="0.1"
# Log format: json in production, console in dev
ENV SERVISTA_LOG_FORMAT="json"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Reference Kubernetes Deployment (for templates/k8s/ directory)
```yaml
# Source: Kubernetes probe configuration docs
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: my-service
  template:
    metadata:
      labels:
        app: my-service
    spec:
      containers:
        - name: my-service
          image: registry.example.com/my-service:latest
          ports:
            - containerPort: 8080
          env:
            - name: OTEL_SERVICE_NAME
              value: "my-service"
            - name: OTEL_EXPORTER_OTLP_ENDPOINT
              value: "http://otel-collector.observability:4317"
            - name: SERVISTA_DATABASE_URL
              valueFrom:
                secretKeyRef:
                  name: my-service-db
                  key: url
            - name: SERVISTA_DATABASE_USER
              valueFrom:
                secretKeyRef:
                  name: my-service-db
                  key: username
            - name: SERVISTA_DATABASE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: my-service-db
                  key: password
            - name: SERVISTA_KAFKA_BROKERS
              valueFrom:
                configMapKeyRef:
                  name: kafka-config
                  key: bootstrap-servers
          startupProbe:
            httpGet:
              path: /health/startup
              port: 8080
            initialDelaySeconds: 5
            periodSeconds: 5
            failureThreshold: 30
          livenessProbe:
            httpGet:
              path: /health/live
              port: 8080
            periodSeconds: 15
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /health/ready
              port: 8080
            periodSeconds: 10
            failureThreshold: 3
          resources:
            requests:
              cpu: 200m
              memory: 256Mi
            limits:
              cpu: "1"
              memory: 512Mi
```

### Logback Configuration (routing based on SERVISTA_LOG_FORMAT)
```xml
<!-- logback.xml: Routes to json or console config based on environment variable -->
<configuration>
    <if condition='property("SERVISTA_LOG_FORMAT").contains("json")'>
        <then>
            <include resource="logback-json.xml"/>
        </then>
        <else>
            <include resource="logback-console.xml"/>
        </else>
    </if>
</configuration>
```

```xml
<!-- logback-json.xml: Production JSON logging -->
<included>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>correlation_id</includeMdcKeyName>
            <includeMdcKeyName>organization_id</includeMdcKeyName>
            <includeMdcKeyName>account_id</includeMdcKeyName>
            <includeMdcKeyName>trace_id</includeMdcKeyName>
            <includeMdcKeyName>span_id</includeMdcKeyName>
            <customFields>
                {"service_name":"${OTEL_SERVICE_NAME:-unknown}",
                 "service_version":"${SERVICE_VERSION:-unknown}"}
            </customFields>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</included>
```

```xml
<!-- logback-console.xml: Development console logging -->
<included>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %highlight(%-5level) [%thread] %cyan(%logger{36}) - %msg [%mdc]%n</pattern>
        </encoder>
    </appender>
    <root level="DEBUG">
        <appender-ref ref="STDOUT"/>
    </root>
</included>
```

### Koin Module for Infrastructure Services
```kotlin
// Source: Koin docs (https://insert-koin.io/docs/reference/koin-ktor/ktor/)
fun servistaModule(config: ServistaConfig) = module {
    // Health registry
    single { HealthRegistry(BuildInfo.fromClasspath()) }

    // Database (if configured)
    config.database?.let { dbConfig ->
        single { migrateDatabase(dbConfig) }  // HikariDataSource
        single {
            DSL.using(get<HikariDataSource>(), SQLDialect.POSTGRES)
        }  // jOOQ DSLContext
        single { RlsSessionManager() }

        // Register DB health check
        single {
            get<HealthRegistry>().apply {
                registerReadiness("database") {
                    try {
                        get<DSLContext>().selectOne().execute()
                        HealthCheckResult("database", HealthStatus.UP)
                    } catch (e: Exception) {
                        HealthCheckResult("database", HealthStatus.DOWN, error = e.message)
                    }
                }
            }
        }
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Logback JSON via custom PatternLayout | logstash-logback-encoder with structured fields | Stable since 7.x (2023) | Rich JSON output with MDC, nested exceptions, custom fields |
| Manual OTel SDK instrumentation | OTel Java Agent auto-instrumentation | Agent matured in 2023-2024 | Zero-code tracing of Ktor, JDBC, Kafka, Logback MDC |
| Spring Boot auto-configuration | Ktor manual plugin installation | Ktor design philosophy | Explicit is better; `installServista()` makes it one call |
| Flyway CLI migrations | Programmatic `Flyway.configure().migrate()` | Available since Flyway 3.x | Simpler ops, no separate migration step in CI/CD |
| Testcontainers 1.x module names | Testcontainers 2.x renamed modules | TC 2.0.0 (2024) | `postgresql` -> `testcontainers-postgresql`, `kafka` -> `testcontainers-kafka` |
| logstash-logback-encoder 8.x (Jackson 2.x) | logstash-logback-encoder 9.0 (Jackson 3.x) | October 2024 | Must stay on 8.1 until all dependencies support Jackson 3.x |

**Deprecated/outdated:**
- logstash-logback-encoder 9.0: Requires Jackson 3.x which is incompatible with current dependency tree. Use 8.1.
- Testcontainers 1.x module names: Still referenced in gradle-platform convention plugins -- services must apply the exclusion workaround from servista-commons.

## Discretion Recommendations

### Flyway Integration: Auto-Migrate on Startup
**Recommendation:** Auto-migrate on startup via `Flyway.configure().dataSource(ds).load().migrate()` called during application initialization, before jOOQ DSLContext is created.
**Rationale:** Simpler operational model (no separate migration step), consistent across all services, appropriate for the "services own their schema" pattern. If needed later, a CLI migration mode can be added via a command-line flag.

### JSON Encoder: logstash-logback-encoder 8.1
**Recommendation:** Use logstash-logback-encoder 8.1 (latest 8.x line).
**Rationale:** De facto standard for Logback JSON logging. Supports Logback 1.5.x (our version). Compatible with Jackson 2.x. Version 9.0 requires Jackson 3.x which would create dependency conflicts. Rich feature set: MDC key inclusion, custom fields, nested exception serialization as JSON, provider-based extensibility.

### Dockerfile Structure: Three-Stage Build
**Recommendation:** Three-stage Dockerfile: (1) build app with Gradle, (2) download OTel agent from GitHub releases, (3) runtime with JRE Alpine + agent + app JAR. Use `JAVA_TOOL_OPTIONS` env var for agent attachment.
**Rationale:** Separating the OTel agent download from the build stage keeps the build cache efficient. Using `JAVA_TOOL_OPTIONS` instead of modifying the ENTRYPOINT makes it easy to disable the agent for debugging.

### Kubernetes Manifests: Deployment + Service + ConfigMap
**Recommendation:** Three YAML files: Deployment (with all three probe types, resource limits, env vars referencing Secrets/ConfigMaps), Service (ClusterIP), ConfigMap (optional overrides). Database credentials via Kubernetes Secrets, Kafka config via ConfigMap.
**Rationale:** Minimal but complete. Covers the common case. Services will customize from this starting point.

### Content Negotiation: Register application/problem+json
**Recommendation:** Register kotlinx.serialization JSON for both `application/json` and `application/problem+json` content types. In StatusPages, set the response Content-Type header to `application/problem+json` explicitly when responding with ProblemDetail.
**Rationale:** RFC 7807 specifies `application/problem+json` as the content type for problem details. Services should respond with this type for errors while using standard `application/json` for success responses.

### installServista() Function: Single Extension with Config DSL
**Recommendation:** A single `Application.installServista()` extension function (no parameters) that loads configuration from HOCON and installs all plugins in correct order. For testability, provide an overload `Application.installServista(config: ServistaConfig)` that accepts a pre-built config.
**Rationale:** Single entry point prevents ordering errors, ensures nothing is missed, and makes service Application modules minimal (3-5 lines). Config overload enables unit testing without HOCON files.

### Test Strategy: Ktor TestApplication + Testcontainers
**Recommendation:** Use Ktor `testApplication {}` for testing plugin installation, error handling, health routes, and request logging (fast, no real HTTP). Use Testcontainers for Flyway migration tests and Kafka integration tests (slower, but verifies real infrastructure).
**Rationale:** Two-tier testing mirrors the library's concerns: plugin wiring (fast tests) and infrastructure integration (slow tests). Convention plugin already provides both test frameworks.

### Package Structure: Feature-Based Under eu.servista.ktor
**Recommendation:** `eu.servista.ktor.{feature}` packages: `error`, `health`, `logging`, `database`, `metrics`, `context`, `serialization`. Top-level `ServistaApplication.kt` and `ServistaConfig.kt`.
**Rationale:** Feature-based organization makes it easy to find and modify specific concerns. Mirrors the plugin installation order in `installServista()`.

## Open Questions

1. **Logback Conditional Processing (`<if>` tag)**
   - What we know: Logback supports conditional processing via Janino library for `<if condition>` in XML
   - What's unclear: Whether this requires an additional Janino dependency or is built into Logback 1.5.x
   - Recommendation: Verify during implementation. If Janino is required, add it as a dependency. Alternative: use two separate logback XML files and select via `logback.configurationFile` system property set from `SERVISTA_LOG_FORMAT` env var.

2. **Shadow JAR Plugin**
   - What we know: The Dockerfile references a shadow/fat JAR for deployment
   - What's unclear: Whether a `shadowJar` task or Ktor's application plugin is the better approach for creating the deployable JAR
   - Recommendation: The Ktor Gradle plugin (`io.ktor.plugin`) provides `buildFatJar` task. Since `servista.api-service` convention plugin already includes Ktor, this may be the simpler path. Verify during implementation -- the Dockerfile template should reference whichever task the convention plugin provides.

3. **Kafka Producer Boilerplate**
   - What we know: servista-commons provides `EventEnvelopeSerializer`, `EventBuilder`, `ServistaKafkaConsumer`, and `DeadLetterRouter` for the consumer side. The producer side has the serializer and builder but no producer wrapper class.
   - What's unclear: Whether servista-ktor should provide a `ServistaKafkaProducer` wrapper similar to the consumer, or if the raw `KafkaProducer` with `EventEnvelopeSerializer` is sufficient.
   - Recommendation: Provide a thin `ServistaKafkaProducer` wrapper in servista-ktor that configures Avro SerDes, sets bootstrap servers from HOCON config, and provides a `suspend fun send(envelope: EventEnvelope)` method. Keep it thin -- the `EventBuilder` already handles context propagation.

## Sources

### Primary (HIGH confidence)
- servista-commons source code -- reviewed `ServistaException`, `HealthRegistry`, `HealthCheck`, `HealthCheckResult`, `HealthStatus`, `BuildInfo`, `HealthResponse`, `ProblemDetail`, `ServistaContext`, `ServistaContextAccessor`, `RlsSessionManager`, `ServistaKafkaConsumer`, `EventEnvelopeSerializer`, `EventBuilder`, `EventContextHandler`
- gradle-platform convention plugins -- reviewed `servista.api-service`, `servista.observability`, `servista.jooq`, `servista.testing`, `servista.kafka-producer`, `servista.kafka-consumer`
- gradle-platform version catalog (`catalog/libs.versions.toml`) -- all version numbers verified
- [Ktor StatusPages docs](https://ktor.io/docs/server-status-pages.html) -- exception handler DSL syntax
- [Ktor CallLogging docs](https://ktor.io/docs/server-call-logging.html) -- filter, MDC, format configuration
- [Ktor HOCON Configuration docs](https://ktor.io/docs/server-configuration-file.html) -- `${?ENV_VAR}` substitution syntax
- [Ktor MicrometerMetrics docs](https://ktor.io/docs/server-metrics-micrometer.html) -- PrometheusMeterRegistry setup, /metrics endpoint
- [Flyway Java API docs](https://documentation.red-gate.com/fd/api-java-277579358.html) -- programmatic migration pattern
- [OTel Java Agent Supported Libraries](https://opentelemetry.io/docs/zero-code/java/agent/supported-libraries/) -- Ktor 1.0+, JDBC, Kafka 0.11+, Logback 1.0+, HikariCP 3.0+, Netty 3.8+ confirmed
- [OTel Agent Environment Variables](https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/) -- OTEL_SERVICE_NAME, OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_TRACES_SAMPLER configuration
- [Koin for Ktor docs](https://insert-koin.io/docs/reference/koin-ktor/ktor/) -- install(Koin), module definition, inject pattern

### Secondary (MEDIUM confidence)
- [logstash-logback-encoder GitHub](https://github.com/logfellow/logstash-logback-encoder) -- version 8.1 compatibility: Java 11+, Logback 1.5.x, Jackson 2.x. Version 9.0 dropped Jackson 2.x support.
- [logstash-logback-encoder releases](https://github.com/logfellow/logstash-logback-encoder/releases) -- 8.1 released April 2025, 9.0 released October 2024 (Jackson 3.x only)
- [Kubernetes Probes docs](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) -- HTTP probe configuration for startup, liveness, readiness

### Tertiary (LOW confidence)
- Logback `<if>` conditional processing requiring Janino -- needs verification during implementation

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all versions verified from version catalog and official docs; logstash-logback-encoder 8.1 compatibility verified
- Architecture: HIGH -- upstream code (servista-commons) reviewed in full; wiring patterns follow Ktor official docs
- Pitfalls: HIGH -- based on actual code review (Testcontainers 2.x rename, RLS finally block, OTel agent classpath)

**Research date:** 2026-03-04
**Valid until:** 2026-04-04 (stable stack, all versions locked in version catalog)

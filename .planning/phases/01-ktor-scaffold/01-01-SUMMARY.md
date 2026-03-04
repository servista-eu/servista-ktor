---
phase: 01-ktor-scaffold
plan: 01
subsystem: infra
tags: [ktor, kotlin, hocon, logback, micrometer, prometheus, rfc7807, health-checks, mdc, context-propagation]

# Dependency graph
requires:
  - phase: servista-commons
    provides: ServistaException, HealthRegistry, ServistaContext, ProblemDetail
  - phase: gradle-platform
    provides: Convention plugins (api-service, observability, testing, jooq), version catalog
provides:
  - servista-ktor library with installServista() entry point
  - Typed HOCON config loading (ServistaConfig with nullable database/kafka)
  - RFC 7807 error handling via StatusPages
  - Health probe endpoints (/health/startup, /health/live, /health/ready)
  - Request logging with MDC correlation (correlation_id, organization_id, account_id)
  - Context propagation via ServistaContext coroutine element
  - Micrometer metrics with Prometheus registry and /metrics endpoint
  - Logback JSON/console switching via SERVISTA_LOG_FORMAT env var
  - ContentNegotiation for application/json and application/problem+json
affects: [01-02-PLAN, 01-03-PLAN, service-repos]

# Tech tracking
tech-stack:
  added: [logstash-logback-encoder 8.1, janino 3.1.12, ktor-server-metrics-micrometer]
  patterns: [single-entry-point-plugin-installation, typed-hocon-config, serializable-dto-bridge]

key-files:
  created:
    - settings.gradle.kts
    - build.gradle.kts
    - gradle.properties
    - config/detekt/detekt.yml
    - src/main/kotlin/eu/servista/ktor/ServistaConfig.kt
    - src/main/kotlin/eu/servista/ktor/ServistaApplication.kt
    - src/main/kotlin/eu/servista/ktor/error/StatusPagesSetup.kt
    - src/main/kotlin/eu/servista/ktor/serialization/ContentNegotiationSetup.kt
    - src/main/kotlin/eu/servista/ktor/health/HealthRoutes.kt
    - src/main/kotlin/eu/servista/ktor/health/HealthResponseDto.kt
    - src/main/kotlin/eu/servista/ktor/logging/RequestLoggingSetup.kt
    - src/main/kotlin/eu/servista/ktor/context/ContextInterceptor.kt
    - src/main/kotlin/eu/servista/ktor/metrics/MetricsSetup.kt
    - src/main/resources/logback.xml
    - src/main/resources/logback-json.xml
    - src/main/resources/logback-console.xml
  modified: []

key-decisions:
  - "Used respondText with manual JSON encoding for StatusPages and HealthRoutes to avoid ContentType negotiation issues with application/problem+json"
  - "Created HealthResponseDto bridge because upstream HealthResponse is not @Serializable"
  - "Added Janino 3.1.12 for Logback conditional processing in logback.xml"
  - "configOrNull extension handles missing HOCON sections gracefully via try-catch"

patterns-established:
  - "Single entry point: Application.installServista() installs all plugins in correct order"
  - "Typed HOCON config: ServistaConfig.load(config) with nullable optional sections"
  - "Serializable DTO bridge: HealthResponseDto wraps non-serializable upstream types for Ktor JSON"
  - "Extension function per concern: each package exposes an Application.install*() function"

requirements-completed: [FOUND-06]

# Metrics
duration: 6min
completed: 2026-03-04
---

# Phase 1 Plan 01: Repo Scaffold + Core HTTP Infrastructure Summary

**Ktor runtime wiring library with installServista() entry point, RFC 7807 error handling, health probes, request logging with MDC correlation, Micrometer metrics, and JSON/console Logback switching**

## Performance

- **Duration:** 6 min
- **Started:** 2026-03-04T06:53:08Z
- **Completed:** 2026-03-04T06:59:44Z
- **Tasks:** 3
- **Files modified:** 20

## Accomplishments
- servista-ktor project scaffolded with 4 convention plugins and servista-commons dependency
- All HTTP-layer Ktor plugins implemented and wired through installServista() in correct order
- Typed HOCON config with nullable database/kafka sections and KafkaConsumerConfig (groupId, autoOffsetReset)
- RFC 7807 error handling, health probes, request logging, metrics, context propagation all implemented
- Logback JSON/console switching via SERVISTA_LOG_FORMAT environment variable

## Task Commits

Each task was committed atomically:

1. **Task 1: Create servista-ktor repository scaffold and build configuration** - `1b74f29` (feat)
2. **Task 2a: Implement core wiring files** - `4c09be3` (feat)
3. **Task 2b: Implement supporting infrastructure files** - `9d79423` (feat)

## Files Created/Modified
- `settings.gradle.kts` - Project settings with gradle-platform convention plugins and version catalog
- `build.gradle.kts` - Build config applying 4 convention plugins with servista-commons dependency
- `gradle.properties` - group=eu.servista, version=0.1.0
- `config/detekt/detekt.yml` - Detekt style rules matching servista-commons
- `src/main/kotlin/eu/servista/ktor/ServistaConfig.kt` - Typed HOCON config data classes
- `src/main/kotlin/eu/servista/ktor/ServistaApplication.kt` - installServista() entry point with Koin DI
- `src/main/kotlin/eu/servista/ktor/error/StatusPagesSetup.kt` - ServistaException to RFC 7807 mapping
- `src/main/kotlin/eu/servista/ktor/serialization/ContentNegotiationSetup.kt` - JSON + problem+json content types
- `src/main/kotlin/eu/servista/ktor/health/HealthRoutes.kt` - /health/startup, /health/live, /health/ready
- `src/main/kotlin/eu/servista/ktor/health/HealthResponseDto.kt` - Serializable DTOs for health responses
- `src/main/kotlin/eu/servista/ktor/logging/RequestLoggingSetup.kt` - CallLogging with MDC correlation
- `src/main/kotlin/eu/servista/ktor/context/ContextInterceptor.kt` - Gateway headers to ServistaContext
- `src/main/kotlin/eu/servista/ktor/metrics/MetricsSetup.kt` - MicrometerMetrics with Prometheus registry
- `src/main/resources/logback.xml` - Routing config based on SERVISTA_LOG_FORMAT
- `src/main/resources/logback-json.xml` - Production JSON logging with LogstashEncoder
- `src/main/resources/logback-console.xml` - Development colored console logging

## Decisions Made
- Used `respondText` with manual JSON encoding for StatusPages and HealthRoutes instead of `call.respond` to ensure correct `application/problem+json` Content-Type header without relying on ContentNegotiation routing
- Created `HealthResponseDto` bridge DTOs because upstream `HealthResponse`, `HealthCheckResult`, and `BuildInfo` from servista-commons are not annotated with `@Serializable` (they are framework-agnostic)
- Added Janino 3.1.12 dependency for Logback's `<if>` conditional processing to switch between JSON and console configs
- Implemented `configOrNull` extension function using try-catch since Ktor's `ApplicationConfig.config()` throws on missing sections

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added HealthResponseDto serializable bridge**
- **Found during:** Task 2b (HealthRoutes implementation)
- **Issue:** Upstream `HealthResponse`, `HealthCheckResult`, `BuildInfo` from servista-commons are plain data classes without `@Serializable` annotation. ContentNegotiation cannot automatically serialize them.
- **Fix:** Created `HealthResponseDto.kt` with `@Serializable` DTO classes that map from upstream types, used `respondText` with manual JSON encoding.
- **Files modified:** `src/main/kotlin/eu/servista/ktor/health/HealthResponseDto.kt`, `src/main/kotlin/eu/servista/ktor/health/HealthRoutes.kt`
- **Verification:** compileKotlin succeeds
- **Committed in:** 9d79423 (Task 2b commit)

**2. [Rule 3 - Blocking] Added Janino dependency for Logback conditional processing**
- **Found during:** Task 2b (Logback configuration)
- **Issue:** Logback's `<if>` tag for conditional config inclusion requires Janino library on classpath
- **Fix:** Added `implementation("org.codehaus.janino:janino:3.1.12")` to build.gradle.kts
- **Files modified:** `build.gradle.kts`
- **Verification:** compileKotlin succeeds, dependency resolves
- **Committed in:** 9d79423 (Task 2b commit)

**3. [Rule 1 - Bug] Fixed Ktor 3.x API compatibility for pipeline intercept and route handlers**
- **Found during:** Task 2a/2b (initial compilation)
- **Issue:** Ktor 3.x changed the route handler receiver to `RoutingContext` and pipeline intercept receiver to `PipelineContext<Unit, PipelineCall>`. The `call` property requires explicit import or accessing via the correct receiver.
- **Fix:** Added `import io.ktor.server.application.call` for pipeline interceptors, used proper `Route` extension with imported `get` for route handlers.
- **Files modified:** `ServistaApplication.kt`, `ContextInterceptor.kt`
- **Verification:** compileKotlin succeeds
- **Committed in:** 4c09be3, 9d79423

---

**Total deviations:** 3 auto-fixed (1 missing critical, 1 blocking, 1 bug)
**Impact on plan:** All auto-fixes necessary for correctness. No scope creep.

## Issues Encountered
None beyond the auto-fixed deviations above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Project scaffold complete, all HTTP-layer plugins implemented and compiling
- Ready for Plan 02 (Database + Kafka integration, Koin DI module, test suite)
- Ready for Plan 03 (Reference templates: Dockerfile, Kubernetes manifests, application.conf)
- Database and Kafka wiring slots are prepared in ServistaConfig (nullable sections) and installServista()

## Self-Check: PASSED

All 16 created files verified present. All 3 task commits verified in git log. SUMMARY.md exists.

---
*Phase: 01-ktor-scaffold*
*Completed: 2026-03-04*

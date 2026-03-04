---
phase: 01-ktor-scaffold
plan: 02
subsystem: infra
tags: [ktor, kotlin, flyway, hikaricp, kafka, koin, jooq, testcontainers, kotest, tdd]

# Dependency graph
requires:
  - phase: 01-ktor-scaffold/01
    provides: installServista() entry point, ServistaConfig, StatusPages, HealthRoutes, RequestLogging, Metrics, ContextInterceptor
  - phase: servista-commons
    provides: ServistaException, HealthRegistry, ServistaContext, RlsSessionManager, ServistaKafkaConsumer, ServistaConsumerConfig
  - phase: gradle-platform
    provides: Convention plugins (api-service, testing, observability, jooq), version catalog
provides:
  - DatabaseSetup with HikariCP pool + Flyway auto-migration (cleanDisabled=true safety)
  - ServistaKafkaProducer with coroutine-friendly send via suspendCancellableCoroutine
  - KafkaConsumerProperties bundle for HOCON-driven consumer config (groupId, autoOffsetReset, brokers, schemaRegistryUrl)
  - ServistaModule Koin DI with conditional database/kafka/consumer singletons
  - Database readiness health check registered in HealthRegistry
  - Comprehensive test suite covering StatusPages, HealthRoutes, RequestLogging, DatabaseSetup, KafkaConsumer, and full integration
affects: [01-03-PLAN, service-repos]

# Tech tracking
tech-stack:
  added: [kafka-clients 4.1.1, testcontainers-postgresql 2.0.3]
  patterns: [conditional-koin-singletons, kafka-consumer-properties-bundle, suspendCancellableCoroutine-kafka-send, isolated-koin-test-contexts]

key-files:
  created:
    - src/main/kotlin/eu/servista/ktor/database/DatabaseSetup.kt
    - src/main/kotlin/eu/servista/ktor/kafka/KafkaProducerSetup.kt
    - src/main/kotlin/eu/servista/ktor/kafka/KafkaConsumerProperties.kt
    - src/main/kotlin/eu/servista/ktor/di/ServistaModule.kt
    - src/test/kotlin/eu/servista/ktor/error/StatusPagesTest.kt
    - src/test/kotlin/eu/servista/ktor/health/HealthRoutesTest.kt
    - src/test/kotlin/eu/servista/ktor/logging/RequestLoggingTest.kt
    - src/test/kotlin/eu/servista/ktor/database/DatabaseSetupTest.kt
    - src/test/kotlin/eu/servista/ktor/kafka/KafkaConsumerSetupTest.kt
    - src/test/kotlin/eu/servista/ktor/ServistaApplicationTest.kt
    - src/test/resources/db/migration/V1__test_schema.sql
    - src/test/resources/logback-test.xml
  modified:
    - build.gradle.kts
    - src/main/kotlin/eu/servista/ktor/ServistaApplication.kt

key-decisions:
  - "KafkaConsumerProperties data class (not factory function) because ServistaKafkaConsumer requires service-specific handler and DeadLetterRouter"
  - "suspendCancellableCoroutine for Kafka send because KafkaProducer.send returns java.util.concurrent.Future (not CompletableFuture)"
  - "koinApplication {} (isolated) in tests instead of startKoin to avoid global Koin context conflicts with Ktor testApplication"
  - "Podman rootless socket configured in build.gradle.kts via DOCKER_HOST and TESTCONTAINERS_RYUK_DISABLED env vars"

patterns-established:
  - "Conditional Koin singletons: if (config.database != null) { single { migrateDatabase(...) } }"
  - "KafkaConsumerProperties bundle: pre-configured consumer properties from HOCON for services to create ServistaKafkaConsumer"
  - "Database health check: registerDatabaseHealthCheck() uses SELECT 1 and maps result to HealthCheckResult UP/DOWN"
  - "Isolated Koin tests: koinApplication {} instead of startKoin to avoid global state conflicts"

requirements-completed: [FOUND-06]

# Metrics
duration: 12min
completed: 2026-03-04
---

# Phase 1 Plan 02: Database + Kafka Integration, Koin DI Module, and Comprehensive Test Suite Summary

**HikariCP + Flyway auto-migration, Kafka producer wrapper with coroutine send, KafkaConsumerProperties bundle, Koin DI module with conditional singletons, and 24-test suite covering all library features**

## Performance

- **Duration:** 12 min
- **Started:** 2026-03-04T07:03:44Z
- **Completed:** 2026-03-04T07:15:52Z
- **Tasks:** 2
- **Files modified:** 22

## Accomplishments
- Complete database integration with HikariCP connection pooling and Flyway auto-migration (cleanDisabled=true for production safety)
- Thin Kafka producer wrapper with coroutine-friendly send using suspendCancellableCoroutine callback pattern
- KafkaConsumerProperties data class that bundles pre-configured consumer settings from HOCON for services to create their own ServistaKafkaConsumer instances
- Koin DI module (servistaModule) with conditional database/kafka/consumer singletons and database readiness health check
- 24 tests across 6 test classes covering StatusPages, HealthRoutes, RequestLogging, DatabaseSetup, KafkaConsumer config, and full installServista() integration

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement database setup, Kafka producer/consumer, and Koin DI module** - `920d7e3` (feat)
2. **Task 2: Write comprehensive test suite for all library features** - `1df1f73` (test)

## Files Created/Modified
- `src/main/kotlin/eu/servista/ktor/database/DatabaseSetup.kt` - HikariCP pool creation + Flyway auto-migration
- `src/main/kotlin/eu/servista/ktor/kafka/KafkaProducerSetup.kt` - Thin Kafka producer wrapper with coroutine send
- `src/main/kotlin/eu/servista/ktor/kafka/KafkaConsumerProperties.kt` - Pre-configured consumer properties from HOCON
- `src/main/kotlin/eu/servista/ktor/di/ServistaModule.kt` - Koin DI module with conditional singletons
- `src/main/kotlin/eu/servista/ktor/ServistaApplication.kt` - Updated to wire Koin servistaModule and database health check
- `build.gradle.kts` - Added kafka-clients, testcontainers-postgresql, Podman socket config
- `src/test/kotlin/eu/servista/ktor/error/StatusPagesTest.kt` - RFC 7807 error response tests (3 tests)
- `src/test/kotlin/eu/servista/ktor/health/HealthRoutesTest.kt` - Health probe endpoint tests (5 tests)
- `src/test/kotlin/eu/servista/ktor/logging/RequestLoggingTest.kt` - Request logging and MDC tests (4 tests)
- `src/test/kotlin/eu/servista/ktor/database/DatabaseSetupTest.kt` - Flyway + HikariCP integration tests with Testcontainers (2 tests)
- `src/test/kotlin/eu/servista/ktor/kafka/KafkaConsumerSetupTest.kt` - Consumer config wiring and Koin injection tests (5 tests)
- `src/test/kotlin/eu/servista/ktor/ServistaApplicationTest.kt` - Full installServista() integration smoke test (5 tests)
- `src/test/resources/db/migration/V1__test_schema.sql` - Test migration for DatabaseSetupTest
- `src/test/resources/logback-test.xml` - Test Logback config suppressing verbose logging

## Decisions Made
- **KafkaConsumerProperties data class over factory function**: ServistaKafkaConsumer from servista-commons requires service-specific `handler: suspend (EventEnvelope) -> Unit` and `DeadLetterRouter`, so the scaffold cannot construct the consumer directly. Instead, it provides a properties bundle that services use when constructing their consumer.
- **suspendCancellableCoroutine for Kafka send**: KafkaProducer.send() returns `java.util.concurrent.Future` (not `CompletableFuture`), so `kotlinx.coroutines.future.await()` cannot be used. Used the callback variant of `send()` with `suspendCancellableCoroutine` instead.
- **Isolated Koin contexts in tests**: Used `koinApplication {}` (isolated, not global) instead of `startKoin` to prevent `KoinApplicationAlreadyStartedException` when Ktor testApplication tests also use Koin.
- **Podman rootless socket config in build.gradle.kts**: Environment uses Podman instead of Docker. Added `DOCKER_HOST` and `TESTCONTAINERS_RYUK_DISABLED` env vars to test task configuration.
- **Renamed KafkaConsumerSetup.kt to KafkaConsumerProperties.kt**: Detekt MatchingDeclarationName rule requires filename to match the single top-level declaration.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed Kafka producer send using suspendCancellableCoroutine instead of Future.await()**
- **Found during:** Task 1
- **Issue:** `KafkaProducer.send()` returns `java.util.concurrent.Future`, not `CompletableFuture`. The `kotlinx.coroutines.future.await()` extension only works with `CompletableFuture`.
- **Fix:** Used `suspendCancellableCoroutine` with the Kafka callback variant of `send(record, callback)` to create a proper coroutine-friendly API.
- **Files modified:** `src/main/kotlin/eu/servista/ktor/kafka/KafkaProducerSetup.kt`
- **Verification:** compileKotlin succeeds
- **Committed in:** 920d7e3 (Task 1 commit)

**2. [Rule 1 - Bug] Fixed detekt MagicNumber in DatabaseSetup pool size**
- **Found during:** Task 2 (build verification)
- **Issue:** `maximumPoolSize = 10` flagged as magic number by detekt
- **Fix:** Extracted to `private const val DEFAULT_POOL_SIZE = 10`
- **Files modified:** `src/main/kotlin/eu/servista/ktor/database/DatabaseSetup.kt`
- **Committed in:** 1df1f73 (Task 2 commit)

**3. [Rule 3 - Blocking] Fixed Koin global context conflicts in tests**
- **Found during:** Task 2 (test execution)
- **Issue:** `startKoin` in KafkaConsumerSetupTest conflicted with Ktor testApplication's Koin installation, causing `KoinApplicationAlreadyStartedException`
- **Fix:** Replaced `startKoin`/`stopKoin` with isolated `koinApplication {}` blocks
- **Files modified:** `src/test/kotlin/eu/servista/ktor/kafka/KafkaConsumerSetupTest.kt`
- **Committed in:** 1df1f73 (Task 2 commit)

**4. [Rule 3 - Blocking] Configured Podman rootless socket for Testcontainers**
- **Found during:** Task 2 (DatabaseSetupTest)
- **Issue:** Testcontainers could not find Docker socket at `/var/run/docker.sock`. Environment uses Podman rootless with socket at `/run/user/1000/podman/podman.sock`.
- **Fix:** Added `DOCKER_HOST` and `TESTCONTAINERS_RYUK_DISABLED` environment variables to test task in build.gradle.kts
- **Files modified:** `build.gradle.kts`
- **Committed in:** 1df1f73 (Task 2 commit)

**5. [Rule 1 - Bug] Fixed detekt MatchingDeclarationName for KafkaConsumerSetup.kt**
- **Found during:** Task 2 (detekt check)
- **Issue:** File named `KafkaConsumerSetup.kt` contains single top-level declaration `KafkaConsumerProperties`
- **Fix:** Renamed file to `KafkaConsumerProperties.kt`
- **Files modified:** `src/main/kotlin/eu/servista/ktor/kafka/KafkaConsumerProperties.kt`
- **Committed in:** 1df1f73 (Task 2 commit)

---

**Total deviations:** 5 auto-fixed (3 bugs, 2 blocking)
**Impact on plan:** All auto-fixes necessary for correctness and test execution. No scope creep.

## Issues Encountered
None beyond the auto-fixed deviations above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All servista-ktor library features implemented and tested (24 tests, all green)
- `./gradlew build` passes (compile + test + detekt + ktfmt)
- Ready for Plan 03 (Reference templates: Dockerfile, Kubernetes manifests, application.conf, example migration)
- Database integration, Kafka wiring, and Koin DI module are complete and verified

## Self-Check: PASSED

All 13 created files verified present. Both task commits (920d7e3, 1df1f73) verified in git log. SUMMARY.md exists.

---
*Phase: 01-ktor-scaffold*
*Completed: 2026-03-04*

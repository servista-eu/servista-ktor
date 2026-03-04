---
phase: 01-ktor-scaffold
plan: 03
subsystem: infra
tags: [docker, kubernetes, hocon, flyway, otel, rls, templates]

# Dependency graph
requires:
  - phase: 01-ktor-scaffold/01-01
    provides: ServistaConfig data classes, health endpoints, installServista() entry point
provides:
  - Reference Dockerfile with three-stage build and OTel Java Agent
  - Kubernetes Deployment with startup, liveness, and readiness probes
  - Kubernetes Service (ClusterIP) and ConfigMap templates
  - Complete HOCON application.conf with all servista.* keys and env var substitution
  - Example Flyway migration with RLS tenant isolation pattern
affects: [service-repos, ci-pipeline]

# Tech tracking
tech-stack:
  added: []
  patterns: [three-stage-docker-build, otel-agent-via-java-tool-options, hocon-env-var-substitution, rls-enable-force-pattern]

key-files:
  created:
    - templates/Dockerfile
    - templates/k8s/deployment.yaml
    - templates/k8s/service.yaml
    - templates/k8s/configmap.yaml
    - templates/application.conf
    - templates/db/migration/V1__initial_schema.sql
  modified: []

key-decisions:
  - "OTel agent attached via JAVA_TOOL_OPTIONS env var for easy disable/debug override"
  - "10% head-based trace sampling via OTEL_TRACES_SAMPLER_ARG=0.1 as production default"
  - "Startup probe allows 2.5 min startup (initialDelay 5s + failureThreshold 30 * period 5s)"

patterns-established:
  - "Three-stage Dockerfile: build, otel-agent-download, runtime"
  - "HOCON env var substitution: default value + ${?ENV_VAR} override on next line"
  - "RLS migration pattern: ENABLE + FORCE ROW LEVEL SECURITY with tenant_isolation policy"
  - "ConfigMap for endpoints, Secret for credentials separation"

requirements-completed: [FOUND-06]

# Metrics
duration: 2min
completed: 2026-03-04
---

# Phase 1 Plan 03: Reference Templates Summary

**Copy-paste reference templates for Dockerfile with OTel agent, Kubernetes manifests with health probes, HOCON config with all servista.* keys, and Flyway migration with RLS tenant isolation**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-04T07:03:06Z
- **Completed:** 2026-03-04T07:05:19Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- Three-stage Dockerfile with OTel Java Agent 2.20.0 auto-attached via JAVA_TOOL_OPTIONS
- Complete Kubernetes manifests (Deployment, Service, ConfigMap) with all three probe types pointing to /health/* endpoints
- Reference HOCON application.conf documenting every servista.* config key including kafka.consumer (group-id, auto-offset-reset) with localhost defaults and env var overrides
- Example Flyway migration demonstrating mandatory RLS pattern with ENABLE + FORCE ROW LEVEL SECURITY

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Dockerfile and Kubernetes manifest templates** - `38f55ef` (feat)
2. **Task 2: Create reference application.conf and example database migration** - `68832d7` (feat)

## Files Created/Modified
- `templates/Dockerfile` - Three-stage build: compile, OTel agent download, JRE runtime with agent attached
- `templates/k8s/deployment.yaml` - Kubernetes Deployment with startup/liveness/readiness probes
- `templates/k8s/service.yaml` - ClusterIP Service exposing port 8080
- `templates/k8s/configmap.yaml` - Non-secret configuration (OTel endpoint, Kafka brokers)
- `templates/application.conf` - Complete HOCON with all servista.* keys and env var substitution
- `templates/db/migration/V1__initial_schema.sql` - Example table with RLS tenant isolation policy

## Decisions Made
- OTel agent attached via JAVA_TOOL_OPTIONS env var instead of ENTRYPOINT modification -- enables easy disable for debugging (`docker run -e JAVA_TOOL_OPTIONS=""`)
- 10% head-based trace sampling (`OTEL_TRACES_SAMPLER_ARG=0.1`) set as production default in Dockerfile
- Startup probe allows 2.5 minutes for application startup (initialDelaySeconds=5, periodSeconds=5, failureThreshold=30)
- ConfigMap vs Secret separation pattern: endpoints and non-sensitive config in ConfigMap, credentials in Secret

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - these are reference template files for new service bootstrapping.

## Next Phase Readiness
- All three plans in Phase 1 are now complete
- servista-ktor library code (Plans 01-02) plus reference templates (Plan 03) provide everything needed to bootstrap new Servista services
- Services can copy templates/ files as starting points and depend on eu.servista:servista-ktor for runtime wiring

## Self-Check: PASSED

All 6 created files verified present. All 2 task commits verified in git log. SUMMARY.md exists.

---
*Phase: 01-ktor-scaffold*
*Completed: 2026-03-04*

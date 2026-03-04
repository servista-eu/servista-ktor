# Roadmap: Servista Ktor

## Overview

This roadmap delivers the Servista Ktor runtime wiring library (`servista-ktor`). The library bridges Phase 3's dependency-only convention plugins and Phase 4's framework-agnostic commons SDK with concrete Ktor application infrastructure. It provides a single `installServista()` entry point that installs StatusPages error handling (RFC 7807), health check routes, content negotiation, structured logging, request logging, metrics, context propagation, Flyway database migration, and Kafka producer boilerplate. Includes reference templates for Dockerfile, Kubernetes manifests, and HOCON configuration. Plans are governed by the servista/governance/mgmt-platform repo.

## Governance Mapping

| Servista-Ktor Phase | Governance Phase | Name |
|---------------------|-----------------|------|
| 1 | 7 | Service Scaffold Template |

## Phases

### Phase 1: Ktor Scaffold
**Goal**: A `servista-ktor` library exists with all Ktor-specific runtime wiring implemented, tested, and ready for publishing to Forgejo Maven registry, plus reference templates for bootstrapping new service repos
**Depends on**: servista-commons (governance Phase 4, complete), gradle-platform (governance Phase 3, complete)
**Requirements**: FOUND-06
**Success Criteria** (what must be TRUE):
  1. Service scaffold includes Flyway database migrations, health/readiness endpoints, structured JSON logging, and OpenTelemetry tracing
  2. Kafka producer boilerplate is available with HOCON-configured bootstrap servers
  3. A new service can use servista-ktor as a single dependency and get all runtime wiring via installServista()
  4. RLS enforcement pattern is demonstrated in template migration with ENABLE and FORCE ROW LEVEL SECURITY
  5. Reference templates exist for Dockerfile (with OTel agent), Kubernetes manifests, and HOCON configuration
Plans:
- [ ] 01-01-PLAN.md -- Repo scaffold + core HTTP infrastructure (StatusPages, HealthRoutes, RequestLogging, Metrics, ContextInterceptor, Logback configs)
- [ ] 01-02-PLAN.md -- Database + Kafka integration, Koin DI module, and comprehensive test suite
- [ ] 01-03-PLAN.md -- Reference templates (Dockerfile, Kubernetes manifests, application.conf, example migration)

## Progress

| Phase | Status | Plans | Progress |
|-------|--------|-------|----------|
| 1     | Pending | 0/3  | 0%       |

---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
current_phase: "01"
current_plan: "01-03"
status: in-progress
stopped_at: Completed 01-03-PLAN.md
last_updated: "2026-03-04T07:05:19Z"
progress:
  total_phases: 1
  completed_phases: 0
  total_plans: 3
  completed_plans: 2
---

# Project State: Servista Ktor

**Last Updated:** 2026-03-04

## Current Milestone

**Milestone 1: Ktor Scaffold** -- Shared Ktor-specific runtime wiring library for all Servista services

## Execution Position

**Current Phase:** 01
**Current Plan:** 01-03
**Status:** In progress (2/3 plans, 01-02 pending)

## Phase Status

| Phase | Name | Status |
|-------|------|--------|
| 1 | Ktor Scaffold | In Progress (2/3 plans) |

## Decisions

- **GOV-01**: Planning governed by servista/governance/mgmt-platform repo -- plans generated there, copied here for execution
- **ADR-017**: Kotlin 2.3/JVM 21, Ktor 3.4, jOOQ 3.20, multi-repo with shared gradle-platform
- servista-commons = framework-agnostic abstractions, servista-ktor = Ktor-specific runtime wiring
- Single dependency for services: `implementation("eu.servista:servista-ktor:x.y.z")`
- OTel Java Agent only (no SDK compile deps) -- agent provides API at runtime
- JSON logging in production (logstash-logback-encoder 8.1), console in development
- HOCON with env var substitution, single config file per service
- Used respondText with manual JSON encoding for StatusPages and HealthRoutes for correct Content-Type
- Created HealthResponseDto bridge for non-@Serializable upstream health types
- Added Janino 3.1.12 for Logback conditional processing
- configOrNull extension handles missing HOCON sections via try-catch
- OTel agent attached via JAVA_TOOL_OPTIONS env var for easy disable/debug override
- 10% head-based trace sampling as production default (OTEL_TRACES_SAMPLER_ARG=0.1)
- Startup probe allows 2.5 min startup (initialDelay 5s + failureThreshold 30 * period 5s)
- ConfigMap for endpoints/non-sensitive config, Secret for credentials

## Performance Metrics

| Phase | Plan | Duration | Tasks | Files |
|-------|------|----------|-------|-------|
| 01 | 01 | 6min | 3 | 20 |
| 01 | 03 | 2min | 2 | 6 |

## Notes

- Governance phase 7 maps to servista-ktor phase 1
- Depends on infrastructure/gradle-platform convention plugins (resolved via includeBuild)
- Depends on infrastructure/servista-commons (resolved via includeBuild or Maven)
- Published to Forgejo Maven registry as `eu.servista:servista-ktor`
- Follow same Testcontainers 2.x workarounds as servista-commons (module renames, TC 2.x package names)

## Last Session

**Stopped at:** Completed 01-03-PLAN.md
**Timestamp:** 2026-03-04T07:05:19Z

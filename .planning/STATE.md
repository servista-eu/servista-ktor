---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
current_phase: "01"
current_plan: Not started
status: pending
stopped_at: Plans created, not yet executed
last_updated: "2026-03-04T12:00:00.000Z"
progress:
  total_phases: 1
  completed_phases: 0
  total_plans: 3
  completed_plans: 0
---

# Project State: Servista Ktor

**Last Updated:** 2026-03-04

## Current Milestone

**Milestone 1: Ktor Scaffold** -- Shared Ktor-specific runtime wiring library for all Servista services

## Execution Position

**Current Phase:** 01
**Current Plan:** Not started
**Status:** Plans created in governance repo, ready for execution

## Phase Status

| Phase | Name | Status |
|-------|------|--------|
| 1 | Ktor Scaffold | Pending (0/3 plans) |

## Decisions

- **GOV-01**: Planning governed by servista/governance/mgmt-platform repo -- plans generated there, copied here for execution
- **ADR-017**: Kotlin 2.3/JVM 21, Ktor 3.4, jOOQ 3.20, multi-repo with shared gradle-platform
- servista-commons = framework-agnostic abstractions, servista-ktor = Ktor-specific runtime wiring
- Single dependency for services: `implementation("eu.servista:servista-ktor:x.y.z")`
- OTel Java Agent only (no SDK compile deps) -- agent provides API at runtime
- JSON logging in production (logstash-logback-encoder 8.1), console in development
- HOCON with env var substitution, single config file per service

## Notes

- Governance phase 7 maps to servista-ktor phase 1
- Depends on infrastructure/gradle-platform convention plugins (resolved via includeBuild)
- Depends on infrastructure/servista-commons (resolved via includeBuild or Maven)
- Published to Forgejo Maven registry as `eu.servista:servista-ktor`
- Follow same Testcontainers 2.x workarounds as servista-commons (module renames, TC 2.x package names)

## Last Session

**Stopped at:** Plans created, not yet executed
**Timestamp:** 2026-03-04T12:00:00Z

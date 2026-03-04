# servista-ktor

Ktor-specific runtime wiring library for Servista services. Provides a single `installServista()` entry point that installs all standard infrastructure plugins.

## What it provides

- **StatusPages** — RFC 7807 `application/problem+json` error responses mapped from `ServistaException`
- **Health routes** — `/health/startup`, `/health/live`, `/health/ready` for Kubernetes probes
- **Content negotiation** — kotlinx.serialization JSON
- **Structured logging** — MDC-based request logging with `correlation_id`, `organization_id`, `account_id`; JSON (Logstash) in production, console in development
- **Metrics** — Micrometer + Prometheus registry
- **Context propagation** — `ServistaContext` interceptor for per-request tenant/account context
- **Database** — HikariCP connection pool + Flyway migrations with `cleanDisabled=true`
- **Kafka** — Producer wrapper with coroutine-based send, consumer properties from HOCON
- **DI** — Koin module with conditional singletons for database, Kafka producer, and consumer

## Usage

```kotlin
fun Application.module() {
    installServista()
    // your routes here
}
```

Configuration via HOCON (`application.conf`):

```hocon
servista {
    database {
        url = "jdbc:postgresql://localhost:5432/mydb"
        username = "user"
        password = "pass"
    }
    kafka {
        bootstrap-servers = "localhost:9092"
    }
}
```

Database and Kafka sections are optional — omit them to disable those features.

## Templates

Reference files for bootstrapping new services live in `templates/`:

- `Dockerfile` — Three-stage build with OpenTelemetry Java Agent
- `k8s/` — Kubernetes deployment, service, and configmap manifests
- `application.conf` — Complete HOCON reference with all `servista.*` keys
- `db/migration/V1__initial_schema.sql` — Example Flyway migration with Row Level Security

## Dependencies

- [servista-commons](https://git.hestia-ng.eu/servista/servista-commons) — Framework-agnostic abstractions
- [gradle-platform](https://git.hestia-ng.eu/servista/gradle-platform) — Shared convention plugins and version catalog

## Build

```bash
./gradlew build
```

Requires JVM 21+, Kotlin 2.3+.

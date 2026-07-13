# ADR-0004: Observability Strategy

# Context

Mini Core Banking v2.2 needs local operational visibility without changing transfer business behavior.

The project should expose health and metrics for runtime verification while avoiding a broad platform rewrite.

# Decision

Use Spring Boot Actuator and Micrometer with the Prometheus registry.

Expose only the required web endpoints:

- `health`
- `info`
- `prometheus`

Enable liveness and readiness probes. Liveness represents whether the application process is alive. Readiness represents whether the application is ready to serve traffic and includes dependency health such as PostgreSQL.

Use Prometheus and Grafana in Docker Compose for local verification. Prometheus scrapes the application through the Compose service name `app` at `/actuator/prometheus`. Grafana provisions a Prometheus datasource and a compact operational dashboard.

Custom transfer metrics are recorded explicitly in the transfer application service. This keeps metric semantics close to the business outcome without introducing AOP.

# Metric Semantics

- `bank.transfer.attempts`: increments after request-level validation passes and the transfer application flow begins.
- `bank.transfer.success`: increments after the transfer processor returns a new successful transfer result.
- `bank.transfer.failed`: increments after the separate failure recorder commits a FAILED history row.
- `bank.transfer.idempotency.replay`: increments when an existing SUCCESS or FAILED result is returned for the same idempotency key and payload.
- `bank.transfer.idempotency.conflict`: increments when the same idempotency key is reused with different transfer details.
- `bank.transfer.duration`: records success, failed, replay, and conflict paths.

# Cardinality Policy

Allowed metric tags must be low-cardinality:

- outcome
- enum-based error code
- standard framework tags from Micrometer auto-configuration

The following values must not be used as metric tags:

- request id
- idempotency key
- account id
- raw exception message
- raw user input

# Consequences

The service gains local operational visibility with a small dependency footprint.

JVM, HTTP server, process, and HikariCP metrics are provided by auto-configuration.

This does not provide distributed tracing, alerting, log aggregation, or production deployment hardening.

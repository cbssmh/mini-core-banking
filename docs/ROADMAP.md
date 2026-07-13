# Roadmap

Mini Core Banking uses an incremental release strategy for the v2 line.

The goal was to complete a reliable transfer MVP in v2.0, improve failure traceability in v2.1, and finish the project with local operational observability in v2.2.

# Release Roadmap

| Version | Goal | Main Features | Why |
| --- | --- | --- | --- |
| v2.0 | Reliable Transfer MVP | PostgreSQL, Flyway, Account, Transfer, transaction boundary, pessimistic lock, lock ordering, idempotency, Testcontainers, Docker Compose, GitHub Actions, runtime verification | Establish the smallest production-oriented transfer service that can be tested and operated with confidence. |
| v2.1 | Reliability Upgrade | FAILED persistence, error code, audit metadata, request ID, reconciliation helper | Improve failure traceability, operational diagnosis, and support workflows after the MVP is stable. |
| v2.2 | Observability & Final Release | Actuator, Micrometer, Prometheus, Grafana, metrics, health, readiness, final documentation | Make the service measurable and finish the Mini Core Banking project. |

# v2.0 Reliable Transfer MVP

## Goal

Deliver the minimum reliable transfer service that can be verified through automated tests, containerized execution, and runtime startup checks.

## Main Features

| Area | Scope |
| --- | --- |
| Database | PostgreSQL |
| Migration | Flyway |
| Core Domain | Account, Transfer |
| Consistency | Transaction Boundary |
| Concurrency | Pessimistic Lock, Lock Ordering |
| Reliability | Idempotency |
| Testing | Testcontainers |
| Local Runtime | Docker Compose |
| CI | GitHub Actions |
| Verification | Runtime Verification |

## Why

v2.0 should prove that transfer can be executed safely with clear transaction ownership and concurrency control.

The release intentionally focuses on a small, testable scope rather than broad banking functionality.

## Completion Criteria

- PostgreSQL is the primary runtime database.
- Flyway owns schema migration.
- Hibernate does not create or update schema automatically.
- Account and Transfer MVP behavior is implemented.
- Transfer runs within a clear transaction boundary.
- Concurrent transfer access uses pessimistic locking.
- Account locks follow deterministic lock ordering.
- Idempotency is supported for transfer requests.
- Testcontainers tests pass.
- Docker Compose starts the application and PostgreSQL.
- GitHub Actions runs the required verification pipeline.
- Runtime verification confirms application startup and database connectivity.

# v2.1 Reliability Upgrade

## Goal

Improve reliability and auditability around failed transfer attempts and operational troubleshooting.

## Main Features

| Area | Scope |
| --- | --- |
| Failure Recording | FAILED persistence with rollback-separated recording |
| Error Handling | Structured error codes |
| Audit | Request ID, error code, failure reason, completion timestamp |
| Traceability | `X-Request-ID` propagation |
| Operations | Failed-transfer reconciliation helper |

## Why

The MVP should first prove successful reliable transfer. v2.1 extends the system so failures are also traceable, classified, and easier to reconcile.

Request ID traces a single HTTP request. Idempotency key protects a single business transfer request from duplicate execution. A failed transfer stored under an idempotency key is reused for an identical retry, while a different request with the same key is rejected as a conflict.

# v2.2 Observability

## Goal

Make the service observable through health checks, metrics, operational dashboards, and runtime verification.

## Main Features

| Area | Scope |
| --- | --- |
| Runtime Management | Actuator |
| Metrics | Micrometer, transfer counters, transfer duration timer |
| Metrics Backend | Prometheus |
| Dashboard | Grafana provisioned dashboard |
| Signals | Health, Liveness, Readiness, JVM, HTTP, HikariCP, Transfer metrics |
| Finalization | README, release plan, final checklist |

## Why

Reliable systems require runtime visibility. v2.2 adds operational signals without changing core transfer behavior.

v2.2 is the final planned version of Mini Core Banking.

## Future Work

The following items are not part of this project and remain separate project topics:

- Kubernetes
- Terraform
- OpenTelemetry
- distributed tracing
- production deployment hardening

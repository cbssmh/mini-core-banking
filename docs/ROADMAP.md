# Roadmap

Mini Core Banking will use an incremental release strategy for the v2 line.

The goal is to complete a reliable transfer MVP quickly in v2.0, then improve reliability, observability, and platform readiness in later minor releases.

# Release Roadmap

| Version | Goal | Main Features | Why |
| --- | --- | --- | --- |
| v2.0 | Reliable Transfer MVP | PostgreSQL, Flyway, Account, Transfer, transaction boundary, pessimistic lock, lock ordering, idempotency, Testcontainers, Docker Compose, GitHub Actions, runtime verification | Establish the smallest production-oriented transfer service that can be tested and operated with confidence. |
| v2.1 | Reliability Upgrade | FAILED persistence, error code, audit metadata, request ID, reconciliation helper | Improve failure traceability, operational diagnosis, and support workflows after the MVP is stable. |
| v2.2 | Observability | Actuator, Micrometer, Prometheus, Grafana, metrics, health, readiness | Make the service measurable and easier to operate under real runtime conditions. |
| v2.3 | Platform | Docker optimization, multi-stage build, non-root runtime, CI improvements, runtime optimization | Improve deployment quality, runtime efficiency, and delivery pipeline reliability. |

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

Make the service observable through health checks, metrics, and operational dashboards.

## Main Features

| Area | Scope |
| --- | --- |
| Runtime Management | Actuator |
| Metrics | Micrometer |
| Metrics Backend | Prometheus |
| Dashboard | Grafana |
| Signals | Metrics, Health, Readiness |

## Why

Reliable systems require runtime visibility. v2.2 adds operational signals without changing core transfer behavior.

# v2.3 Platform

## Goal

Improve platform quality and runtime efficiency for repeatable delivery.

## Main Features

| Area | Scope |
| --- | --- |
| Container | Docker optimization |
| Build | Multi-stage build |
| Security | Non-root runtime |
| CI | CI improvements |
| Runtime | Runtime optimization |

## Why

After reliability and observability are in place, the service should become easier to package, run, and evolve in a controlled delivery environment.

## Future Work

The following items are not part of v2.3 and remain future work:

- Kubernetes
- Terraform
- OpenTelemetry

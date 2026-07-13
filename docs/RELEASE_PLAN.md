# Release Plan

Mini Core Banking v2 uses incremental releases.

Each release must satisfy its Definition of Done before the next release begins.

# Release Definitions

| Release | Goal | Definition of Done |
| --- | --- | --- |
| v2.0 | Reliable Transfer MVP | PostgreSQL runtime is configured. Flyway migrations run successfully. Account and Transfer MVP behavior is implemented. Transaction boundary is explicit. Pessimistic locking and lock ordering are applied. Idempotency is implemented. All Testcontainers tests pass. Docker Compose starts the application. GitHub Actions passes. README is updated. Release is created. |
| v2.1 | Reliability Upgrade | FAILED transfer persistence is implemented. Error codes are defined and applied. Audit metadata is stored. Request ID is propagated or stored. Reconciliation helper is available. Failure scenarios are covered by tests. Documentation is updated. Release is created. |
| v2.2 | Observability & Final Release | Actuator is enabled. Micrometer metrics are exposed. Prometheus can scrape metrics. Grafana dashboard is available. Health, liveness, and readiness checks are defined. Observability behavior is verified in runtime. Documentation and final checklist are updated. Release is created. |

# v2.0 Done

- PostgreSQL is used as the application database.
- Flyway applies all migrations successfully.
- Hibernate automatic schema update is disabled.
- Account MVP is implemented.
- Transfer MVP is implemented.
- Transfer transaction boundary is clearly defined.
- Pessimistic locking is used for transfer account access.
- Lock ordering prevents avoidable deadlocks.
- Idempotency protects repeated transfer requests.
- All Testcontainers tests pass.
- Docker Compose execution is verified.
- GitHub Actions passes.
- Runtime verification confirms startup, database connection, and migration state.
- README is updated.
- Release is created.

# v2.1 Done

- FAILED transfer attempts are persisted.
- Error code policy is defined and applied to API errors.
- Audit metadata is captured on transfer history.
- Request ID is available in response headers, error bodies, and transfer history.
- Reconciliation helper supports operational review of failed transfers.
- Failure-path tests pass.
- Documentation is updated.
- Release is created.

# v2.2 Done

- Actuator endpoints are configured.
- Micrometer metrics are emitted.
- Prometheus scraping is verified.
- Grafana dashboard is available.
- Health endpoint is verified.
- Liveness endpoint is verified.
- Readiness endpoint is verified.
- Transfer custom metrics are verified.
- JVM, HTTP server, and HikariCP metrics are available.
- Runtime observability check passes.
- Documentation is updated.
- Release is created.

# Project Completion

Mini Core Banking is complete at v2.2.0. Platform topics such as Kubernetes, Terraform, cloud deployment, and distributed tracing are separate project topics.

# Mini Core Banking v2.2

## Overview

Mini Core Banking is a Java 21 and Spring Boot 4 backend project for studying reliable account transfers with operational observability.

v2.2 is the final version of this project. It combines reliable transfer behavior, failed-transfer traceability, and a local observability stack with Actuator, Micrometer, Prometheus, and Grafana.

This project is a learning system, not a production banking platform.

## Version Evolution

| Version | Focus |
| --- | --- |
| v1.0.0 | Learning Prototype |
| v2.0.0 | Reliable Transfer MVP |
| v2.1.0 | Reliability Upgrade |
| v2.2.0 | Observability & Final Release |

## Architecture

| Layer | Responsibility |
| --- | --- |
| Controller | Exposes account, transfer, history, and reconciliation APIs. |
| Application Service | Validates transfer requests, coordinates transaction processing, failure recording, idempotency outcomes, and metrics. |
| Transfer Processor | Owns the main transfer transaction, account locking, balance movement, and SUCCESS history. |
| Failure Recorder | Persists FAILED transfer history in a separate `REQUIRES_NEW` transaction. |
| Repository | Provides JPA persistence and PostgreSQL locking access. |
| Database | Stores accounts and transfer history through Flyway-managed schema. |
| Observability | Exposes health and metrics to Prometheus and Grafana. |

## Reliability Design

- PostgreSQL is the runtime database.
- Flyway owns schema migration V1 through V3.
- Hibernate schema auto-update is disabled.
- Transfers use an explicit transaction boundary.
- Account rows are locked pessimistically.
- Account ids are ordered before locking to reduce avoidable deadlocks.
- Idempotency prevents duplicate debit and credit execution.
- Failed business transfer attempts are persisted as `FAILED`.
- Request validation failures do not create transfer history rows.
- `X-Request-ID` traces a single HTTP request.
- `idempotencyKey` identifies one business transfer request.

## Observability Design

v2.2 adds:

- Spring Boot Actuator
- Micrometer Prometheus registry
- Health, liveness, and readiness probes
- Transfer business metrics
- JVM, process, HTTP server, and HikariCP metrics from auto-configuration
- Prometheus scraping
- Grafana datasource and dashboard provisioning

In Spring Boot 4, datasource pool metrics are exposed with names such as `jdbc_connections_active`, `jdbc_connections_idle`, and `jdbc_connections_pending`.

No OpenTelemetry, tracing backend, alerting stack, Kubernetes, or Terraform is included.

## Metrics

Custom Micrometer meter names:

| Micrometer Name | Prometheus Name | Meaning |
| --- | --- | --- |
| `bank.transfer.attempts` | `bank_transfer_attempts_total` | Valid transfer application requests. |
| `bank.transfer.success` | `bank_transfer_success_total` | New transfers that returned successfully after processing. |
| `bank.transfer.failed` | `bank_transfer_failed_total` | Business failures recorded as FAILED history. |
| `bank.transfer.idempotency.replay` | `bank_transfer_idempotency_replay_total` | Existing SUCCESS or FAILED results returned for the same key and payload. |
| `bank.transfer.idempotency.conflict` | `bank_transfer_idempotency_conflict_total` | Same idempotency key reused with different transfer details. |
| `bank.transfer.duration` | `bank_transfer_duration_seconds_*` | Transfer application service duration for success, failure, replay, and conflict paths. |

Low-cardinality timer tags:

- `outcome=success|failed|replay|conflict|error|unknown`
- `error_code=NONE|ACCOUNT_NOT_FOUND|INSUFFICIENT_BALANCE|IDEMPOTENCY_CONFLICT|...`

High-cardinality values such as request id, idempotency key, account id, raw messages, and user input are not used as metric tags.

## Health and Readiness

Actuator endpoints exposed:

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/prometheus`

Liveness indicates that the application process is alive. Readiness includes dependency health such as database connectivity and is used by Docker Compose for the app healthcheck.

## Prometheus

Prometheus is configured by `observability/prometheus/prometheus.yml`.

It scrapes:

```text
job_name: mini-core-banking
target: app:8080
metrics_path: /actuator/prometheus
```

Local URL:

```text
http://localhost:9090
```

## Grafana

Grafana is provisioned with:

- Prometheus datasource
- Mini Core Banking dashboard

Local URL:

```text
http://localhost:3000
```

Default local credentials are `admin` / `admin` unless overridden with `GRAFANA_ADMIN_USER` and `GRAFANA_ADMIN_PASSWORD`. These defaults are for local development only.

## Docker Compose

Services:

- `postgres`
- `app`
- `prometheus`
- `grafana`

Run:

```bash
./gradlew build
docker compose up -d
docker compose ps
```

Stop:

```bash
docker compose down
```

Volumes are not removed by default.

## Testing

Run:

```bash
./gradlew clean test
```

Coverage includes:

- transfer success and rollback behavior
- failed transfer persistence
- idempotency replay and conflict
- request id propagation
- reconciliation query
- custom transfer metrics
- Actuator health endpoint
- Prometheus endpoint text exposure

## CI

GitHub Actions:

- validates the Gradle wrapper
- starts PostgreSQL
- starts the application
- verifies `/actuator/health/readiness`
- verifies `/actuator/prometheus`
- runs Testcontainers tests
- builds the project

No deploy, tag, or release step is included.

## Runtime Verification

Recommended final local verification:

```bash
./gradlew clean test
./gradlew build
docker compose config
docker compose up -d
docker compose ps
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
curl http://localhost:8080/actuator/prometheus
curl http://localhost:9090/api/v1/targets
curl http://localhost:3000/api/health
docker compose down
```

## Known Limitations

- No authentication or authorization.
- No ledger model.
- No multi-currency support.
- No distributed tracing.
- No alert manager or alert policy.
- No cloud deployment.
- Not certified as production-ready.

## Project Completion

Mini Core Banking is complete at v2.2.0.

Kubernetes, Terraform, OpenTelemetry, distributed tracing, and production deployment are intentionally left as separate project topics.

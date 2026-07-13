# Mini Core Banking v2

## Overview

Mini Core Banking v2 is a Spring Boot backend project for verifying a reliable account transfer flow.

v2.0 focuses on PostgreSQL persistence, Flyway-managed schema, transaction boundary, pessimistic locking, deterministic lock ordering, idempotency, integration tests, Docker Compose runtime, and CI verification.

This project is not a real banking system.

## Architecture

The application uses a layered Spring Boot structure.

| Layer | Responsibility |
| --- | --- |
| Controller | Exposes account and transfer HTTP APIs. |
| Application Service | Owns transfer transaction boundary, idempotency check, lock ordering, and transfer orchestration. |
| Entity | Maps the Flyway-managed database schema. |
| Repository | Provides JPA persistence access and pessimistic lock query. |
| Database | Stores accounts and transfer history in PostgreSQL. |

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.0.4 |
| Persistence | Spring Data JPA |
| Database | PostgreSQL 17 |
| Migration | Flyway |
| Tests | JUnit, Spring Boot Test, Testcontainers PostgreSQL |
| Build | Gradle |
| Runtime | Docker Compose |
| CI | GitHub Actions |

## Key Features

- Account creation
- Account query
- Transfer
- Transfer history query
- PostgreSQL schema managed by Flyway
- Hibernate `ddl-auto=none`
- Transfer transaction boundary
- Pessimistic account locking
- Deterministic lock ordering by account id
- Transfer idempotency key
- PostgreSQL Testcontainers integration tests

## Transfer Flow

```text
HTTP request
  -> request validation
  -> idempotency key lock
  -> existing idempotency key lookup
  -> create PENDING transfer history
  -> lock accounts by ascending account id
  -> check source balance
  -> debit source account
  -> credit target account
  -> mark transfer history SUCCESS
  -> commit
```

If account lookup, balance check, validation, or database work fails, the transaction rolls back. v2.0 does not persist independent FAILED transfer rows.

## Idempotency

The transfer API requires `idempotencyKey` in the JSON body.

Same key and same request:

- returns the existing transfer result
- does not move money again

Same key and different request:

- returns a conflict-style business error
- does not move money

The database enforces a unique constraint on `transfer_history.idempotency_key`.

## Concurrency Strategy

Transfers lock both account rows with JPA `PESSIMISTIC_WRITE`.

Account ids are sorted in ascending order before lock acquisition. This reduces deadlock risk when opposite-direction transfers access the same account pair.

This strategy does not claim that all deadlocks are impossible.

## Testing

Repository and transfer integration tests use PostgreSQL Testcontainers.

Verified test coverage includes:

- Account save and lookup
- Unique account number constraint
- Transfer history save
- Foreign key enforcement
- Successful transfer
- Balance updates
- Total amount invariant
- Insufficient balance rollback
- Self-transfer rejection
- Missing account rollback
- Idempotency replay
- Idempotency conflict
- Lock ordering structure
- Concurrent same-key transfer request

Run:

```bash
./gradlew clean test
```

## Docker

Docker Compose includes:

- `postgres`
- `app`
- bridge network
- PostgreSQL volume
- PostgreSQL healthcheck
- app startup after PostgreSQL health

The app service runs the local Gradle-built jar. Build the jar before starting Docker Compose.

PostgreSQL is exposed on host port `5433` to avoid conflicting with a local database already using `5432`.

## CI

GitHub Actions runs:

```text
checkout
setup Java 21
start PostgreSQL service
start application for Flyway migration
run Testcontainers tests
build
success
```

Deploy is not included.

## How to Run

Run tests:

```bash
./gradlew clean test
```

Build:

```bash
./gradlew build
```

Run with Docker Compose:

```bash
docker compose up
```

Stop:

```bash
docker compose down
```

Remove the PostgreSQL volume when a clean local database is required:

```bash
docker compose down -v
```

## Verification

Create two accounts:

```bash
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"v2-001","ownerName":"Alice","balance":10000}'

curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"v2-002","ownerName":"Bob","balance":5000}'
```

Transfer:

```bash
curl -X POST http://localhost:8080/accounts/transfer \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId":1,"toAccountId":2,"amount":1000,"idempotencyKey":"demo-key-001"}'
```

Expected response shape:

```json
{
  "transferId": 1,
  "status": "SUCCESS",
  "idempotencyKey": "demo-key-001"
}
```

Replay the same request with the same `idempotencyKey`. The same transfer result should be returned and balances should not change again.

Check accounts:

```bash
curl http://localhost:8080/accounts
```

## Known Limitations

- FAILED transfer persistence is not implemented in v2.0.
- Audit metadata is not implemented.
- Error codes are not structured.
- Request ID propagation is not implemented.
- Reconciliation helper is not implemented.
- There is no authentication or authorization.
- There is no ledger model.
- There is no observability stack.

## Roadmap

v2.1 focuses on:

- FAILED persistence
- Audit metadata
- Error codes
- Request ID
- Reconciliation helper

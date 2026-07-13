# Mini Core Banking v1

## Overview

Mini Core Banking v1 is a small Java and Spring Boot backend prototype for learning and verifying account APIs, balance transfer flow, JPA persistence, database transactions, and pessimistic locking.

This project is not a real banking system and is not production-ready. It is a learning-focused backend prototype that demonstrates a minimal money-transfer workflow and documents its current technical limits.

## Project Scope

Included in v1:

- Account creation
- Account query
- Balance transfer
- Transfer history
- Transaction boundary
- Pessimistic locking
- Request validation
- Custom exception handling

Not included in v1:

- Authentication
- Authorization
- Idempotency
- Ledger
- Concurrency guarantee
- Deadlock handling
- Observability
- Database migration
- Production deployment

## Tech Stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.4 |
| Web | Spring Web |
| Persistence | Spring Data JPA |
| Database | MySQL 8.4.10 |
| Build | Gradle 9.4.0 |

## Architecture

The project uses a simple Spring Boot layered structure.

| Component | Responsibility |
|---|---|
| Controller | Exposes HTTP APIs and maps request bodies to DTOs |
| Service | Applies validation, transaction boundary, transfer orchestration, and business rules |
| Repository | Provides database access through Spring Data JPA |
| Entity | Maps account and transfer history tables |
| DTO | Defines request and response payloads |
| Exception Handler | Converts custom business exceptions into JSON error responses |

Main package structure:

```text
src/main/java/com/minibank/mini_core_banking
├── domain
│   └── account
│       ├── controller
│       ├── dto
│       ├── exception
│       ├── history
│       ├── repository
│       ├── service
│       └── Account.java
└── global
    └── GlobalExceptionHandler.java
```

## Transfer Flow

Current successful transfer flow:

```text
HTTP request
  -> request-level validation
  -> create PENDING history
  -> lock source account
  -> lock destination account
  -> validate balance
  -> debit source
  -> credit destination
  -> mark SUCCESS
  -> commit
```

All of the transfer processing above runs inside a single transaction boundary in `AccountService.transfer()`.

If an exception occurs after the transaction starts, the whole transaction is rolled back. This means failed transfer attempts currently do not leave a transfer history row. The `FAILED` enum value exists, but independent failed-history persistence is not implemented in v1.

## Implemented Features

The following features were manually verified:

- Account creation
- Account list
- Account detail
- Transfer
- Transfer history list
- Account-specific transfer history
- Self-transfer validation
- Non-positive amount validation
- Pessimistic locking
- Rollback on insufficient balance
- SUCCESS history persistence

## API Examples

### Create Account

```bash
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "111-222-333",
    "balance": 100000,
    "ownerName": "kim"
  }'
```

Example response:

```json
{
  "id": 1,
  "accountNumber": "111-222-333",
  "balance": 100000,
  "ownerName": "kim"
}
```

### Get Accounts

```bash
curl http://localhost:8080/accounts
```

Example response:

```json
[
  {
    "id": 1,
    "accountNumber": "111-222-333",
    "balance": 100000,
    "ownerName": "kim"
  }
]
```

### Get Account Detail

```bash
curl http://localhost:8080/accounts/1
```

Example response:

```json
{
  "id": 1,
  "accountNumber": "111-222-333",
  "balance": 100000,
  "ownerName": "kim"
}
```

### Transfer

```bash
curl -X POST http://localhost:8080/accounts/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": 1,
    "toAccountId": 2,
    "amount": 5000
  }'
```

Example success response:

```text
OK
```

Example business error response:

```json
{
  "message": "잔액 부족"
}
```

### Get Transfer History

```bash
curl http://localhost:8080/transfers
```

Example response:

```json
[
  {
    "id": 8,
    "fromAccountId": 3,
    "toAccountId": 4,
    "amount": 10000,
    "transferredAt": "2026-07-13T14:14:31.531312",
    "status": "SUCCESS"
  }
]
```

### Get Transfer History by Account

```bash
curl http://localhost:8080/transfers/account/3
```

Example response:

```json
[
  {
    "id": 8,
    "fromAccountId": 3,
    "toAccountId": 4,
    "amount": 10000,
    "transferredAt": "2026-07-13T14:14:31.531312",
    "status": "SUCCESS"
  }
]
```

## How to Run

Prerequisites:

- Java 21
- Docker
- MySQL container

MySQL command used for verification:

```bash
docker run --name mini-core-banking-mysql \
  -e MYSQL_DATABASE=minibank \
  -e MYSQL_ALLOW_EMPTY_PASSWORD=yes \
  -p 3306:3306 \
  -d mysql:8.4
```

The empty root password is only for local learning and manual verification. Do not use this setting in an operational environment.

Run the application:

```bash
./gradlew clean test
./gradlew build
./gradlew bootRun
```

## Verification Results

| Scenario | Result |
|---|---|
| Account creation | Passed |
| Successful transfer | Passed |
| Balance conservation | Passed |
| Insufficient balance rollback | Passed |
| Self-transfer rejection | Passed |
| Pessimistic lock query | Observed |
| Duplicate request protection | Not implemented |

Manual verification data:

| Item | Value |
|---|---|
| Account 3 initial balance | 100000 |
| Account 4 initial balance | 10000 |
| Successful transfer history | id `8`, amount `10000`, status `SUCCESS` |
| Repeated request history | id `10`, amount `5000`, status `SUCCESS` |
| Repeated request history | id `11`, amount `5000`, status `SUCCESS` |
| Account 3 final balance | 80000 |
| Account 4 final balance | 30000 |

Balance invariant:

```text
100000 + 10000 = 80000 + 30000 = 110000
```

The repeated request verification used the same `5000` transfer request twice. Because idempotency is not implemented, both requests were processed and two `SUCCESS` history rows were created.

History id `9` was not present after verification. This is consistent with a failed transaction consuming a MySQL auto-increment value while the row itself was rolled back.

## Known Limitations

- No idempotency
- No deterministic lock ordering
- No concurrency integration test
- No deadlock handling or retry
- No FAILED persistence
- No immutable ledger
- No authentication or authorization
- No migration tool
- External MySQL is required
- Only minimal automated test coverage
- `spring.jpa.open-in-view` warning is present
- Not production-ready

The `FAILED` enum exists in the code, but failed transfer attempts currently roll back the transaction, so a failed transfer row is not independently persisted in v1.

## Learning Outcomes

This project documents and verifies the following learning points:

- Spring proxy transaction boundary
- Same-bean self-invocation problem
- Pessimistic locks require an active transaction
- Relationship between rollback and history persistence
- MySQL auto-increment values may not be rolled back
- Why idempotency is needed for retry-safe transfer APIs
- Why deterministic lock ordering is needed for stronger concurrency handling

## Version History

### v1.0.0

- Account APIs
- Working transfer flow
- Transaction boundary fix
- Pessimistic lock verification
- Rollback verification
- Documented limitations

## v2 Roadmap

The following items are intentionally deferred to v2:

- Testcontainers
- Flyway
- PostgreSQL
- Idempotency key
- Deterministic lock ordering
- Concurrency tests
- FAILED persistence strategy
- Docker Compose
- GitHub Actions
- Actuator
- Micrometer
- Prometheus
- Grafana

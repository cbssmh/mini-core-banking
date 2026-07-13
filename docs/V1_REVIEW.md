# Mini Core Banking v1 Review

## 1. Purpose

Mini Core Banking v1 was built as a compact Spring Boot backend prototype for learning account APIs, transfer orchestration, JPA persistence, transaction boundaries, rollback behavior, and pessimistic locking.

The goal of v1 is not to model a complete banking platform. It is a focused learning project that demonstrates a minimal transfer flow and documents the technical limits that remain.

## 2. Original Implementation

The first transfer implementation had the following structure:

- `transfer()` was not the transaction boundary.
- Transactional methods were called from another method in the same service class.
- `PENDING` transfer history was created before validation and account locking.
- `processTransfer()` attempted to execute the pessimistic lock query.
- The pessimistic lock query ran without an active transaction.

The intended design was stateful transfer processing, but the actual runtime behavior did not match that intention because the transaction boundary was not applied where the lock query needed it.

## 3. Reproduced Runtime Failure

The original implementation failed even for a normal transfer request.

Observed runtime behavior:

- Successful transfer request returned HTTP 500.
- Spring raised `InvalidDataAccessApiUsageException`.
- Root cause was `TransactionRequiredException: No active transaction`.
- Account balances did not change.
- `PENDING` transfer history remained.
- The transfer did not transition to `SUCCESS`.
- The transfer did not transition to `FAILED`.

This showed that the transfer flow was not only incomplete; it failed during the normal execution path.

## 4. Root Cause

The root cause was Spring proxy-based transaction behavior combined with same-bean self-invocation.

In the original implementation, `transfer()` called other methods in the same `AccountService` instance. Even though those methods had `@Transactional`, the calls did not go through the Spring proxy that applies transaction interception.

As a result, `processTransfer()` executed without an active transaction. When the repository method using `PESSIMISTIC_WRITE` was called, Hibernate required an active transaction and raised `TransactionRequiredException`.

For this project, the practical lesson is simple: the method that orchestrates the transfer must be the transaction boundary if the lock query and balance mutation are part of the same unit of work.

## 5. Minimal Fix

The v1 fix was intentionally small.

Applied changes:

- Replaced Jakarta `@Transactional` usage with Spring `org.springframework.transaction.annotation.Transactional`.
- Set `AccountService.transfer()` as the transaction boundary.
- Moved request-level validation before transfer history creation.
- Created `PENDING` history only after basic request validation passed.
- Executed account locking, balance validation, debit, credit, and `SUCCESS` status update in the same transaction.
- Let failures roll back the whole transaction.
- Kept `markTransferFailed()` for v2 with a TODO note, but removed it from the current v1 flow.

No service split, schema change, idempotency implementation, lock ordering, or transaction propagation customization was added.

## 6. Verified Runtime Behavior

The fixed v1 behavior was manually verified against MySQL 8.4.10.

Verified behavior:

- Normal transfer succeeds.
- Hibernate emits `for update` for the pessimistic lock query.
- Insufficient balance returns a business error and rolls back the transaction.
- Self-transfer returns HTTP 400 before history creation.
- Successful transfer persists `SUCCESS` history.
- Failed transfer attempts do not leave history rows.
- Repeating the same transfer request processes the transfer again because idempotency is not implemented.

## 7. Data Invariant Verification

Manual verification used the following account data:

| Account | Initial Balance | Final Balance |
|---|---:|---:|
| Account 3 | 100000 | 80000 |
| Account 4 | 10000 | 30000 |

Successful transfers from account 3 to account 4:

| History ID | Amount | Status |
|---:|---:|---|
| 8 | 10000 | SUCCESS |
| 10 | 5000 | SUCCESS |
| 11 | 5000 | SUCCESS |

Total successful transfer amount:

```text
10000 + 5000 + 5000 = 20000
```

Balance invariant:

```text
Initial total: 100000 + 10000 = 110000
Final total:   80000 + 30000 = 110000
```

Expected balances:

```text
Account 3: 100000 - 20000 = 80000
Account 4: 10000 + 20000 = 30000
```

The final account balances and `SUCCESS` transfer history sum are consistent.

## 8. Auto-Increment Observation

Transfer history id `9` was not present after verification.

The missing id is consistent with MySQL auto-increment allocation not being rolled back. During the insufficient-balance scenario, an insert was attempted inside a transaction that later rolled back. The row did not remain, but the allocated auto-increment value appears to have been consumed.

This is an observation from the verification run, not a feature of the application.

## 9. Strengths

v1 demonstrates the following:

- Spring Boot REST API implementation
- JPA repositories and entities
- A working transaction boundary for transfer processing
- Pessimistic lock usage inside an active transaction
- Rollback behavior for insufficient balance
- Request validation before history creation
- Custom exception handling for business errors
- Runtime troubleshooting from failure reproduction to minimal fix

These are useful backend engineering learning outcomes, especially because the original failure was reproduced and then fixed with a smaller transaction-boundary change.

## 10. Known Limitations

v1 still has important limitations:

- No idempotency key or retry-safe transfer request handling
- No deterministic lock ordering
- No concurrency integration test
- No deadlock handling or retry strategy
- No independent `FAILED` history persistence
- No immutable ledger or double-entry accounting model
- No account status model such as frozen, closed, or debit-blocked
- No authentication or authorization
- No audit log or trace ID
- No database migration tool
- External MySQL is required
- Automated test coverage is minimal
- `spring.jpa.open-in-view` warning remains
- Not production-ready

The `FAILED` enum exists, but v1 does not persist failed transfer rows independently. Failed transfer attempts roll back the transaction, including any history row created within that transaction.

## 11. Lessons Learned

The main lessons from v1 are:

- Transaction annotations must be applied where Spring can actually intercept the call.
- Same-bean self-invocation can prevent transaction behavior from being applied.
- Pessimistic locking requires an active database transaction.
- Rollback behavior affects both balance updates and history persistence.
- Persisting failure history requires a deliberate transaction strategy.
- MySQL auto-increment values may be consumed even when a transaction rolls back.
- Idempotency is necessary for safe client retries.
- Lock ordering is necessary for stronger concurrency behavior.

## 12. Improvements Planned for v2

v2 should focus on reliability and operational realism rather than adding more CRUD endpoints.

Planned directions:

- Integration testing with Testcontainers
- Database migration with Flyway
- PostgreSQL evaluation
- Idempotency key support
- Deterministic lock ordering
- Concurrent transfer tests
- FAILED persistence strategy
- Docker Compose delivery
- GitHub Actions CI
- Actuator health checks
- Micrometer metrics
- Prometheus and Grafana observability

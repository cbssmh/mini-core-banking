# Transfer Integrity Hardening

## Why this work was done

The transfer API already used transactions, idempotency keys, and pessimistic
locking, but those mechanisms needed failure- and contention-specific evidence.
The hardening work therefore started from invariants and adversarial schedules:
retain the correct outcome for an idempotency key, never expose a partially
committed transfer, keep supported balances nonnegative, and reject monetary
values that cannot be represented safely.

Two transaction-handling defects were reproduced before they were corrected.
The final design and the regression evidence are described below; the claims are
limited to the implementation and schedules actually exercised.

## Transaction model

[`TransferApplicationService`](../src/main/java/com/minibank/mini_core_banking/domain/account/service/TransferApplicationService.java)
validates the request and calls the transactional
[`TransferProcessor`](../src/main/java/com/minibank/mini_core_banking/domain/account/service/TransferProcessor.java).
The processor:

1. takes a PostgreSQL transaction-scoped advisory lock derived from the
   idempotency key;
2. replays an identical retained result or rejects a different payload;
3. inserts `PENDING` history for a new request;
4. locks both account rows in ascending account-id order;
5. commits either balance changes plus `SUCCESS`, or an expected `FAILED`
   outcome without balance changes.

For a new expected rejection, the processor returns a typed `FAILED` result so
its transactional proxy can commit while still owning the advisory lock. The
application service raises the original business error only after that commit.
An identical retry returns the retained `FAILED` response; a different payload
using the key conflicts. Unexpected persistence exceptions still escape and roll
back the transaction, leaving no retained outcome.

The advisory-key lock and account-row locks protect different boundaries. The
former serializes ownership of an idempotency outcome; the latter serializes
changes to shared balances. The database unique constraint remains a persistent
backstop for the key.

## Integrity invariants

| Invariant | Enforcement and evidence |
| --- | --- |
| Supported balances are integers from zero through `Long.MAX_VALUE`. | Strict HTTP binding and validation, checked destination addition, PostgreSQL `BIGINT NOT NULL`, and the V4 nonnegative check. |
| A tested successful transfer preserves the mathematical total of the participating accounts. | Both rows are locked and updated in one transaction; tests calculate totals with `BigInteger`. |
| Balance changes and `SUCCESS` history commit or roll back together. | One processor transaction; an injected PostgreSQL failure exercises rollback after managed balances have changed. |
| One retained terminal result owns an idempotency key and payload. | Advisory serialization covers lookup through expected `SUCCESS` or `FAILED` commit; replay and conflict behavior are tested. |
| Expected rejection does not mutate balances. | Missing-account, insufficient-funds, and destination-overflow checks complete before balance setters run. |

These are transfer-path invariants, not a database-level proof of conservation of
money. Direct database access can bypass application workflows.

## Finding 1 — Failed-outcome idempotency

### Expected property

Once a valid request is resolved as an expected failure, that outcome should
retain ownership of its idempotency key. An identical concurrent request should
replay the failure, while a different payload should conflict.

### Reproduction

The historical implementation allowed the processor transaction to roll back
before a separate failure recorder inserted `FAILED`. A controlled interleaving
paused the original request after rollback and submitted a smaller transfer with
the same key. The competing payload completed successfully and claimed the key;
the original request then failed during late outcome recording.

### Root cause

`pg_advisory_xact_lock` is scoped to the processor transaction. Rolling that
transaction back released the key lock and removed `PENDING` before the separate
`REQUIRES_NEW` recorder ran. The key therefore had an ownership gap.

### Correction

Expected failures now become `FAILED` in the processor transaction that already
owns the advisory lock. No balances have been mutated on that path. The proxy
commits the outcome first, and only then does the application service surface the
initial business exception.

### Regression evidence

[`TransferFailureOutcomeIntegrationTest`](../src/test/java/com/minibank/mini_core_banking/domain/account/service/TransferFailureOutcomeIntegrationTest.java)
holds the first request at `beforeCommit`, observes the contender waiting on the
advisory lock, and then releases the commit. It verifies both outcomes:

- the same payload replays the single retained `FAILED` row without an
  `UnexpectedRollbackException`;
- a different payload receives `IDEMPOTENCY_CONFLICT`, while the original amount
  and unchanged balances remain authoritative.

The evidence covers expected application-level rejections. An infrastructure
rollback retains no outcome, so a later request may attempt the key again.

## Finding 2 — Failure recording and rollback-only transactions

### Expected property

Handling a duplicate outcome should not replace the intended business result
with a transaction-completion exception.

### Reproduction

The historical separate recorder caught a unique-key
`DataIntegrityViolationException` from `saveAndFlush` and returned `false`.
PostgreSQL-backed reproduction showed that transaction completion still raised
`UnexpectedRollbackException`.

### Root cause

The persistence error had already marked the transaction rollback-only. Catching
the Java exception inside that same transactional method did not restore a
committable transaction.

### Correction

The duplicate-insert failure-recording path and its separate recorder were
removed. Expected outcomes are serialized and retained once by the processor;
same-payload callers replay that row instead of attempting a second insert.

### Regression evidence

The same-payload contention regression above permits only the intended first
`CustomException` and the subsequent `FAILED` replay. Any transaction-completion
exception would escape through the worker future and fail the test. Unit coverage
in [`TransferApplicationServiceTest`](../src/test/java/com/minibank/mini_core_banking/domain/account/service/TransferApplicationServiceTest.java)
also verifies that the initial business error is raised only after the processor
returns its committed `FAILED` result.

## Concurrency verification

### Competing withdrawals

[`competingWithdrawalsCannotOverspend`](../src/test/java/com/minibank/mini_core_banking/domain/account/service/TransferIntegrityIntegrationTest.java)
starts two different-key withdrawals of 80 against a source balance of 100 under
forced lock contention. Exactly one succeeds, the other is rejected for
insufficient balance, the source finishes at 20, the tested total remains 100,
and history contains one `SUCCESS` and one `FAILED`.

### Opposite-direction transfers

[`oppositeDirectionsCompleteWithoutDeadlockUnderForcedContention`](../src/test/java/com/minibank/mini_core_banking/domain/account/service/TransferIntegrityIntegrationTest.java)
starts A-to-B 30 and B-to-A 20 while a test transaction holds the lower account
row. Both workers must be observed waiting before the gate is released. Both
transfers then complete with balances 90 and 110, total 200, and two `SUCCESS`
rows.

These bounded schedules exercise the deterministic ascending-id query in
[`AccountRepository`](../src/main/java/com/minibank/mini_core_banking/domain/account/repository/AccountRepository.java).
They do not prove that the system is deadlock-free under every future query,
isolation level, or workload.

## Monetary boundaries

- [`TransferProcessor`](../src/main/java/com/minibank/mini_core_banking/domain/account/service/TransferProcessor.java)
  uses `Math.addExact` before either balance mutation. Overflow produces an
  expected `FAILED` outcome, while an exact `Long.MAX_VALUE` result remains valid.
- [`MonetaryInputTest`](../src/test/java/com/minibank/mini_core_banking/MonetaryInputTest.java)
  and [`MonetaryJsonBindingTest`](../src/test/java/com/minibank/mini_core_banking/MonetaryJsonBindingTest.java)
  verify null, nonpositive, malformed, fractional, exponent-form, and out-of-range
  handling, as well as valid integer bounds. Floating-point JSON tokens are not
  silently truncated into integer monetary values.
- [`V4__enforce_nonnegative_account_balance.sql`](../src/main/resources/db/migration/V4__enforce_nonnegative_account_balance.sql)
  adds `CHECK (balance >= 0)`. The PostgreSQL regression performs a direct
  negative update, observes rejection, and verifies the original balance remains.

The model uses integer units in a signed 64-bit range. It has no currency model,
decimal scale, or double-entry ledger.

## Injected persistence failure

[`databaseFailureAtHistoryCompletionRollsBackBalancesAndPendingRow`](../src/test/java/com/minibank/mini_core_banking/domain/account/service/TransferIntegrityIntegrationTest.java)
installs a test-only PostgreSQL trigger that rejects the update from `PENDING` to
`SUCCESS`. In that exercised ordering, the transaction leaves both balances at
their original values and commits no history row. After the trigger is removed,
the same key can be attempted successfully because the infrastructure failure
retained no terminal outcome.

This establishes atomic rollback for that injected database error. It does not
establish arbitrary process-crash recovery, network-failure behavior, or
ambiguous-commit safety.

## Test-harness debugging incident

The first forced-contention run failed at its observation gate before reaching
the monetary assertions. The observer queried `pg_stat_activity` repeatedly from
the transaction that also held the gate row. PostgreSQL could reuse its activity
snapshot, so current worker query information was not guaranteed to appear merely
because the loop slept and queried again.

The harness now calls `pg_stat_clear_snapshot()` before each poll and reports
backend and worker diagnostics on timeout. The original deadline and all final
balance, total, outcome, and history assertions remain. Production transaction
code was not changed in response to an observation failure; the harness was fixed
first, and the application invariants were then evaluated.

## Final verification

The final run executed 54 tests across 10 suites:

- 54 passed;
- 0 failures;
- 0 errors;
- 0 skipped;
- 32 cases used PostgreSQL through Testcontainers;
- the other 22 were unit or MVC tests.

The PostgreSQL-backed cases include application-context, repository,
observability, transfer, monetary constraint, contention, failed-outcome, and
injected-failure coverage. The count does not describe all 54 tests as concurrency
or integrity tests.

## What this evidence does NOT prove

- universal deadlock freedom;
- exactly-once transfer execution under every failure;
- durable audit history for infrastructure failures or every request;
- process-crash recovery or ambiguous-commit safety;
- database-level conservation of money;
- bank-grade accounting, a double-entry ledger, or full reconciliation;
- behavior under arbitrary direct database writes, outer transactions, isolation
  changes, or future locking queries.

The evidence supports the final implementation and the specific PostgreSQL
schedules above. Those boundaries are part of the result, not qualifications to
be generalized away.

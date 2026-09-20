# ADR-0002: Transfer History

# Context

Reliable transfer requires auditability.

A successful transfer must be traceable. A failed transfer must also be traceable because failures may affect customer support, operational monitoring, reconciliation, and idempotency decisions.

If only successful transfers are stored, the system loses evidence of rejected or failed attempts.

# Decision

Manage transfer history with explicit `SUCCESS` and `FAILED` statuses.

`SUCCESS` represents a completed transfer where debit and credit were committed.

`FAILED` represents a transfer attempt that did not complete successfully.

The processor inserts PENDING, then commits either SUCCESS with balance changes or
FAILED for an expected business rejection before balance mutation. The same
transaction-scoped advisory lock covers lookup through outcome commit. The
application raises the first rejection as a business error only after commit.
Unexpected database errors roll back balances and PENDING and retain no outcome.

This supersedes the v2.1 separate REQUIRES_NEW recorder: supplied PostgreSQL probes
reproduced a different payload claiming the key after rollback, followed by an
UnexpectedRollbackException from the recorder's duplicate insert. Catching the
integrity exception did not clear rollback-only state. No unrelated transaction
mechanisms were changed. Final PostgreSQL regression execution passed; see the evidence report.

Request-level validation failures, such as missing account IDs, invalid amount, missing idempotency key, or self-transfer, are rejected before the business transfer attempt and do not create transfer history rows. Business failures after a valid transfer attempt begins, such as missing account records or insufficient balance, are persisted as `FAILED`.

`request_id` and `idempotency_key` have different responsibilities:

- `request_id` traces one HTTP request across response headers, error response bodies, and transfer history.
- `idempotency_key` identifies one business transfer request and prevents duplicate debit and credit execution.

When an idempotency key already has a stored result, the same request returns that existing result. This includes a stored `FAILED` result. Reusing the same key with different transfer details is an idempotency conflict.

# Consequences

Transfer history can support both customer-facing history and internal operations.

Failure records improve observability and reduce ambiguity during incident analysis.

The application service must carefully record failure outcomes without violating transaction consistency.

Infrastructure failure auditing remains outside the implemented guarantee.

The history table no longer enforces account foreign keys because failed attempts may reference an account ID that does not exist. Account existence remains a business validation responsibility in the transfer transaction.

# Integrity qualification

The original defect probes established the unsafe interleaving. Replacement
regressions in TransferFailureOutcomeIntegrationTest require same-payload FAILED
replay and different-payload conflict under an observed advisory-lock wait.
The first business failure returns an error; a stored FAILED replay returns a
normal response with FAILED status. The endpoint named reconciliation only lists
FAILED rows; it performs no accounting comparison. See
[the integrity case study](../integrity-hardening.md).

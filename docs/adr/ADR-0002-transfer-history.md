# ADR-0002: Transfer History

# Context

Reliable transfer requires auditability.

A successful transfer must be traceable. A failed transfer must also be traceable because failures may affect customer support, operational monitoring, reconciliation, and idempotency decisions.

If only successful transfers are stored, the system loses evidence of rejected or failed attempts.

# Decision

Manage transfer history with explicit `SUCCESS` and `FAILED` statuses.

`SUCCESS` represents a completed transfer where debit and credit were committed.

`FAILED` represents a transfer attempt that did not complete successfully.

In v2.1, failed transfer persistence is implemented with a separate failure-recording transaction.

The main transfer transaction creates a `PENDING` row while it attempts the business operation. If account locking, account existence validation, balance validation, debit, or credit fails, the main transaction rolls back. This rollback also removes the in-transaction `PENDING` row, which is correct because account state must not be partially committed.

After that rollback, the application records a `FAILED` row through a separate Spring bean using `REQUIRES_NEW`. This keeps failure audit evidence without committing any account balance changes from the failed transfer.

Request-level validation failures, such as missing account IDs, invalid amount, missing idempotency key, or self-transfer, are rejected before the business transfer attempt and do not create transfer history rows. Business failures after a valid transfer attempt begins, such as missing account records or insufficient balance, are persisted as `FAILED`.

`request_id` and `idempotency_key` have different responsibilities:

- `request_id` traces one HTTP request across response headers, error response bodies, and transfer history.
- `idempotency_key` identifies one business transfer request and prevents duplicate debit and credit execution.

When an idempotency key already has a stored result, the same request returns that existing result. This includes a stored `FAILED` result. Reusing the same key with different transfer details is an idempotency conflict.

# Consequences

Transfer history can support both customer-facing history and internal operations.

Failure records improve observability and reduce ambiguity during incident analysis.

The application service must carefully record failure outcomes without violating transaction consistency.

Failure persistence may require separate transaction handling when the main transfer transaction rolls back.

The history table no longer enforces account foreign keys because failed attempts may reference an account ID that does not exist. Account existence remains a business validation responsibility in the transfer transaction.

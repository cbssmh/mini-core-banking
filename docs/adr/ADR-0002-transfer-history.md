# ADR-0002: Transfer History

# Context

Reliable transfer requires auditability.

A successful transfer must be traceable. A failed transfer must also be traceable because failures may affect customer support, operational monitoring, reconciliation, and idempotency decisions.

If only successful transfers are stored, the system loses evidence of rejected or failed attempts.

# Decision

Manage transfer history with explicit `SUCCESS` and `FAILED` statuses.

`SUCCESS` represents a completed transfer where debit and credit were committed.

`FAILED` represents a transfer attempt that did not complete successfully.

In v2, failed transfer persistence is a target requirement. The system should retain failed transfer attempts with enough information to support audit and diagnosis.

# Consequences

Transfer history can support both customer-facing history and internal operations.

Failure records improve observability and reduce ambiguity during incident analysis.

The application service must carefully record failure outcomes without violating transaction consistency.

Failure persistence may require separate transaction handling when the main transfer transaction rolls back.

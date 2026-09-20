# ADR-0001: Transaction Boundary

# Context

Transfer is the core reliability requirement of Mini Core Banking v2.

A transfer changes at least two account balances: the source account is debited and the target account is credited. These changes must be committed together or rolled back together.

If debit succeeds and credit fails, the system creates an inconsistent financial state.

# Decision

Use `TransferProcessor.process()` as the transaction boundary. `TransferApplicationService.transfer()` is nontransactional.

The processor loads locked accounts, validates and computes balances, and writes SUCCESS history within one transaction. Expected business rejections before mutation commit FAILED in that same transaction. The nontransactional application raises the initial business exception only after the processor proxy returns. Unexpected persistence failures still roll back. No blanket noRollbackFor or persistence-exception catch is used.

This boundary represents the business operation that must be atomic.

# Consequences

The system can guarantee that account balance changes for a transfer are committed atomically.

Transaction consistency is easier to reason about because the use case boundary and database transaction boundary are aligned.

The processor owns transaction demarcation; the application service translates committed results and records metrics. PostgreSQL verified the earlier balance/locking behavior and demonstrated the separate-recorder defect. The new FAILED commit path is verified by the final PostgreSQL regressions; see the [integrity case study](../integrity-hardening.md).

Long-running external calls must not be placed inside the transfer transaction.

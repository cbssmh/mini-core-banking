# ADR-0001: Transaction Boundary

# Context

Transfer is the core reliability requirement of Mini Core Banking v2.

A transfer changes at least two account balances: the source account is debited and the target account is credited. These changes must be committed together or rolled back together.

If debit succeeds and credit fails, the system creates an inconsistent financial state.

# Decision

Use `transfer()` as a single transaction boundary.

The transfer application service method will load the required accounts, apply domain operations, persist account state, and record transfer history within one transactional unit.

This boundary represents the business operation that must be atomic.

# Consequences

The system can guarantee that account balance changes for a transfer are committed atomically.

Transaction consistency is easier to reason about because the use case boundary and database transaction boundary are aligned.

The transfer application service becomes responsible for transaction demarcation and orchestration.

Long-running external calls must not be placed inside the transfer transaction.

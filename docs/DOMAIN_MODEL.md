# Implemented domain model

- Account: mutable JPA entity containing identity, owner and integer balance.
  Opening balances may be supplied at creation. Balances are nonnegative signed
  64-bit integers; V4 adds a database CHECK. The entity has setters, not domain
  deposit/withdraw operations.
- TransferHistory: one retained result per non-null idempotency key. The processor
  inserts PENDING and commits SUCCESS on success or FAILED for expected business
  rejection before any balance mutation. Infrastructure errors roll back the row. Request IDs identify the original request,
  not every replay. Foreign keys were removed in V3 to represent missing accounts.
- TransferRequest: distinct source/destination, positive integer amount and nonblank
  key. HTTP monetary JSON rejects floating-point tokens. A Money value object and
  currencies are not implemented.

TransferProcessor coordinates two account rows and history in a transaction.
Application validation and checked arithmetic express API/domain behavior; the
balance CHECK protects persisted data against writes outside that path. Neither
is a substitute for a ledger or for auditing arbitrary database writes.

See [the integrity case study](integrity-hardening.md) for the invariants, reproduced
failure race, correction, final PostgreSQL regression evidence, and limitations.

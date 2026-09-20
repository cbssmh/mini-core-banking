# Current architecture

Controllers perform JSON binding and Bean Validation. TransferApplicationService
performs transfer validation, invokes the transactional processor, translates
committed FAILED results into the initial business error, and records metrics.

TransferProcessor.process owns the Spring transaction. It takes a PostgreSQL
transaction advisory lock for the hashed idempotency key, checks an existing
outcome, inserts PENDING, and obtains pessimistic account locks with ORDER BY id.
Account existence, insufficient funds and checked credit arithmetic are evaluated
before balance mutation. Balances and SUCCESS history commit together.

Account is a mutable JPA entity. It does not implement deposit/withdraw methods or
encapsulate monetary rules. Those rules live in the processor, request validation,
and the database balance constraint. No separate Money value object exists.

Expected rejections (missing account, insufficient balance, destination overflow)
return a FAILED result without mutating balances. The processor commits that row
under its existing advisory lock; the nontransactional caller then raises the
business error. Unexpected persistence errors still escape and roll back. The old
separate recorder was removed after PostgreSQL probes confirmed its race and
rollback-only exception. The final PostgreSQL regression run passed (54/54);
see [the integrity case study](integrity-hardening.md).

Rows use PostgreSQL BIGINT / Java Long integer units, without currency metadata.
No external calls occur inside the transfer transaction. No ledger, automatic
reconciliation or distributed transaction mechanism is implemented.

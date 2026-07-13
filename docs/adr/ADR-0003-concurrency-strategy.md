# ADR-0003: Concurrency Strategy

# Context

Transfers can access the same accounts concurrently.

Concurrent withdrawals or transfers may create lost updates, invalid balances, or inconsistent transfer results if account rows are read and updated without proper locking.

The system requires a clear concurrency strategy before implementation.

# Decision

Prefer `PESSIMISTIC_WRITE` locking for account rows involved in transfer.

This strategy ensures that concurrent transactions attempting to modify the same account are serialized at the database level.

Use lock ordering when two accounts are involved in a transfer. Accounts should be locked in a deterministic order, such as ascending account id, regardless of transfer direction.

Optimistic locking remains a comparison option, but it is not the primary v2 choice. It may be revisited if contention is low and retry handling becomes acceptable.

# Consequences

Pessimistic locking provides predictable correctness for balance updates under concurrent transfer load.

Lock ordering reduces the risk of deadlocks when two transfers touch the same pair of accounts in opposite directions.

The implementation must ensure that repository queries acquire locks consistently.

Pessimistic locking can reduce throughput under high contention, so transaction scope must remain small.

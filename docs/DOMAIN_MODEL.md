# Domain Model

Mini Core Banking v2 focuses on account balance management and reliable transfer recording.

# Aggregate

| Aggregate | Responsibility |
| --- | --- |
| Account | Owns account identity and balance. Protects balance invariants. Supports deposit and withdraw operations. Acts as the main consistency boundary for account state. |

# Entity

| Entity | Responsibility |
| --- | --- |
| TransferHistory | Records the result of a transfer attempt. Stores source account, target account, amount, status, failure reason when applicable, and timestamps. Supports auditability and operational diagnosis. |

# Value Object

| Value Object | Responsibility |
| --- | --- |
| Money | Represents monetary amount and currency-safe arithmetic. Prevents invalid values and centralizes amount-related rules. |

Money is proposed as a future extension point.

In v2, Money will not be implemented. Monetary values may continue to use the existing representation until the model requires multi-currency support, stricter arithmetic rules, or richer monetary validation.

# Model Notes

Account is the aggregate for balance consistency.

TransferHistory is modeled separately because it records the transfer attempt outcome rather than owning account balance behavior.

The transfer use case coordinates multiple Account instances and records TransferHistory within a reliable transaction strategy.

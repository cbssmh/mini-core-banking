# Architecture

Mini Core Banking v2 follows a layered architecture.

```text
Controller
    |
Application Service
    |
Domain
    |
Repository
    |
Database
```

# Layer Responsibilities

| Layer | Responsibility |
| --- | --- |
| Controller | Exposes HTTP APIs, validates request shape, converts requests into application commands, and returns responses. |
| Application Service | Coordinates use cases, defines transaction boundaries, loads aggregates, invokes domain behavior, persists results, and records transfer history. |
| Domain | Holds business rules for accounts and transfers, protects account balance invariants, and expresses domain state changes. |
| Repository | Provides persistence access for aggregates and transfer history without exposing database details to the domain. |
| Database | Stores account state and transfer history with transactional guarantees. |

# Application Service and Domain Separation

Application Services are responsible for orchestration. They handle transaction demarcation, repository access, idempotency checks, and the order of operations required by a use case.

The Domain is responsible for business correctness. It should express rules such as whether a withdrawal is allowed, how balances change, and which invariants must always hold.

This separation keeps transaction and infrastructure concerns outside core business logic. It also makes the domain easier to test and keeps application workflows explicit.

For transfer in v2, the Application Service owns the transfer use case boundary. The Account aggregate owns balance changes and invariant protection.

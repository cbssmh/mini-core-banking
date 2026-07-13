# v2.0 Release Checklist

| Item | Status |
| --- | --- |
| All tests pass | Verified |
| Flyway migration verified | Verified |
| Docker Compose verified | Verified |
| CI passing | Pending remote GitHub Actions run |
| Runtime verification completed | Verified |
| README updated | Completed |

# Verification Commands

```bash
./gradlew clean test
./gradlew build
docker compose up
```

# Runtime Verification Scope

- Application starts on port 8080.
- PostgreSQL healthcheck passes before app startup.
- Flyway applies migrations.
- Account creation works.
- Transfer works.
- Idempotency replay does not move money twice.
- Same idempotency key with different payload returns an error.

# v2.1 Remaining Work

- FAILED persistence
- Audit metadata
- Error codes
- Request ID
- Reconciliation helper

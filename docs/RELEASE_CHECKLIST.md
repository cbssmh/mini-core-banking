# v2.2 Final Release Checklist

| Item | Status |
| --- | --- |
| All tests pass | Verify before release |
| Build passes | Verify before release |
| Actuator verified | Verify before release |
| Health endpoint verified | Verify before release |
| Liveness endpoint verified | Verify before release |
| Readiness endpoint verified | Verify before release |
| Prometheus scrape verified | Verify before release |
| Grafana datasource verified | Verify before release |
| Grafana dashboard verified | Verify before release |
| Custom transfer metrics verified | Verify before release |
| JVM metrics verified | Verify before release |
| HTTP server metrics verified | Verify before release |
| HikariCP metrics verified | Verify before release |
| Docker Compose stack verified | Verify before release |
| CI configuration verified | Verify before release |
| README updated | Completed |
| Runtime verification completed | Verify before release |
| Remote GitHub Actions run | Pending after push |

# Verification Commands

```bash
./gradlew clean test
./gradlew build
docker compose config
docker compose up -d
docker compose ps
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
curl http://localhost:8080/actuator/prometheus
curl http://localhost:9090/api/v1/targets
curl http://localhost:3000/api/health
docker compose down
```

# Runtime Verification Scope

- PostgreSQL is healthy.
- Application is healthy through readiness.
- Flyway V1, V2, and V3 are applied.
- Prometheus scrapes the app through the Compose service name `app`.
- Grafana provisions the Prometheus datasource.
- Grafana loads the Mini Core Banking dashboard.
- Transfer success, failure, replay, and conflict metrics are emitted.
- JVM, HTTP server, and HikariCP datasource metrics such as `jdbc_connections_active` are exposed.
- Core transfer behavior remains unchanged.

# Final Scope Check

- No Kubernetes.
- No Terraform.
- No Helm.
- No OpenTelemetry.
- No tracing backend.
- No alert manager.
- No Spring Security.
- No ledger, event sourcing, CQRS, microservices, UI, or multi-currency implementation.
- No commit, push, tag, or release in the implementation step.

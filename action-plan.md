# GameHub API — Action Plan

## Completed

- [x] Spring Boot 3 (Java 21) base app
- [x] Local development on PostgreSQL (Docker) — H2 removed from dev
- [x] Automatic Postgres for dev (Spring Boot Docker Compose) and tests (Testcontainers)
- [x] Core public API endpoints
  - [x] Health checks
  - [x] Meta information
  - [x] Projects
  - [x] Featured games
- [x] Authentication & Tokens
  - [x] Firebase ID token exchange → backend JWT + refresh
  - [x] Username/password login + signup (demo/dev)
  - [x] Refresh/rotate refresh tokens
- [x] RBAC
  - [x] Admin endpoints via `@PreAuthorize("hasRole('ADMIN')")`
  - [x] Admin role seeding from `app.admin.emails`/`APP_ADMIN_EMAILS`
- [x] CORS configuration (env-driven)
- [x] Global error handling — Problem Details (RFC‑7807)
- [x] REST rate limiting (Resilience4j) with 429 + Retry-After
- [x] Security headers (CSP, Frame-Options, Referrer-Policy, HSTS in prod, Content-Type-Options)
- [x] Caching (Caffeine) for hot read paths (leaderboard, featured, etc.)
- [x] OpenAPI/Swagger UI available at `/swagger-ui.html`
- [x] GraphQL endpoint enabled at `/graphql` (basic schema present)
- [x] CI/CD to Cloud Run (Artifact Registry image build + deploy)
- [x] Cloud SQL integration (Private IP recommended path documented)

## In Progress

- [ ] Feature flags enhancements
  - [ ] Segmentation (by role/user/email suffix)
  - [ ] Gradual rollouts (percentage / cohorts)
- [ ] Logging improvements
  - [ ] Structured JSON logs (Logstash encoder) with correlation IDs
  - [x] Consistent error responses (Problem Details)

## Planned (Next up)

### API Enhancements

- [ ] Pagination, sorting, filtering for list endpoints (projects, scores)

### Database

- [ ] Introduce Flyway migrations (baseline current schema; forward-only)
- [ ] Index review (scores per game/user; email uniqueness already in place)

### Monitoring & Observability

- [x] Prometheus metrics via Micrometer (exposed by Actuator)
- [ ] Distributed tracing (OpenTelemetry) — export to Cloud Trace
- [ ] Alerts (error rate, latency, 429/5xx spikes) — GCP + Grafana

### Security

- [ ] Regular security audits (OWASP dep-check profile available)
- [x] Harden Swagger with auth in prod (require JWT to view docs)

### Performance

- [x] Query optimizations and indexes for auth/refresh tokens
- [ ] Redis-backed caches and rate limits in prod (Memorystore + VPC)
- [x] Compression and response size tuning (prod profile)

### Documentation & Runbooks

- [x] README updated for dev/test/prod, auth, rate limits, RBAC
- [ ] Operational runbooks (deploy, rollback, secrets rotation)
- [ ] Monitoring/alerting docs (dashboards, SLOs)

## Delivery Timeline (proposed)

1. Flyway migrations + pagination/filtering (short) — 1–2 days
2. Structured logging + trace IDs end-to-end — 1 day
3. Redis-backed rate limiting/caching (prod) — 1–2 days
4. OpenTelemetry tracing + alerting setup — 2–3 days
5. Feature flags: segmentation + gradual rollout — 2–3 days

## Acceptance Criteria (for this phase)

- Swagger documents Bearer auth and common error responses (401/403/429)
- Pagination and validation on read endpoints
- Flyway manages schema end-to-end for dev/test/prod
- JSON logs contain correlation IDs and minimal PII
- Rate limits configurable per environment; prod uses Redis

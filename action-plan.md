# GameHub API - Action Plan

## Completed

- [x] Set up Spring Boot 3 with Java 21
- [x] Configure PostgreSQL for development (Docker-backed Postgres; H2 removed from dev)
- [x] Automatic Postgres launch
  - [x] Dev: Spring Boot Docker Compose support auto-starts `postgres` from `docker-compose.yml` and stops it on app
    exit
  - [x] Tests: Postgres Testcontainer starts automatically via `BaseIntegrationTest`
- [x] Implement basic API endpoints
    - [x] Health check endpoints
    - [x] Meta information endpoint
    - [x] Projects listing
    - [x] Featured games endpoint
- [x] Set up JWT authentication
- [x] Implement basic CORS configuration
- [x] Set up local development environment
- [x] Update documentation for dev using Postgres (README, guidelines)
- [x] Configure Testcontainers for integration testing
- [x] Set up GitHub Actions for CI/CD
- [x] Deploy to Google Cloud Run
- [x] Configure Cloud SQL integration
- [x] Implement basic STOMP WebSocket support

## In Progress

- [ ] Enhance feature flags system
    - [ ] Add support for user segmentation
    - [ ] Implement gradual rollouts
- [ ] Improve error handling and logging
    - [ ] Add structured logging
    - [ ] Implement better error responses

## Planned

### Authentication & Authorization

- [x] Implement OAuth2 with social providers
  - Implemented via Firebase/NextAuth (frontend performs OAuth with Google/GitHub; backend validates Firebase ID tokens
    and issues first‑party JWT + refresh tokens)
  - Added endpoint: POST /api/auth/firebase/exchange (accepts Authorization: Bearer <Firebase ID token> or {"
    idToken": "..."})
  - Enabled conditional FirebaseTokenFilter (active when Firebase is configured)
- [ ] Add role-based access control (RBAC)
- [ ] Set up rate limiting for public endpoints

### API Enhancements

- [ ] Add pagination to list endpoints
- [ ] Implement filtering and sorting
- [ ] Add OpenAPI documentation
- [ ] Implement GraphQL endpoint

### Database

- [ ] Set up database migrations (Flyway/Liquibase)
- [ ] Add read replicas for scaling
- [ ] Implement database performance monitoring

### Monitoring & Observability

- [ ] Add Prometheus metrics
- [ ] Set up distributed tracing
- [ ] Configure alerting

### Security

- [ ] Implement request validation
- [ ] Add rate limiting
- [ ] Set up security headers
- [ ] Regular security audits

### Performance

- [ ] Implement caching layer
- [ ] Add CDN for static assets
- [ ] Optimize database queries

### Documentation

- [ ] Add API documentation
- [ ] Create deployment runbooks
- [ ] Document monitoring and alerting

### Future Considerations

- [ ] Multi-region deployment
- [ ] A/B testing framework
- [ ] Feature flag analytics
- [ ] Serverless functions for specific features

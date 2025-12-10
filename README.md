# GameHub API (Spring Boot + PostgreSQL)

GameHub API is the backend for my portfolio: it powers publicly playable web games and a page listing my other projects.
It exposes public endpoints for games and projects, and gates some functionalities behind sign-in/subscription and
feature flags.

- Backend: Spring Boot 3 (Java 21), PostgreSQL, optional Firebase auth integration
- Feature flags: in-app service with environment defaults and admin overrides
- Deploy: Google Cloud Run (manual or via GitHub Actions)
- Frontend: a separate React/Next.js app consumes this API; the backend remains backend-agnostic

Key defaults for local dev:

- Backend runs on port 8080
- Frontend runs on port 3000 (when used) and calls the backend at `/api`

—

Table of contents

1. Tech stack and design choices (pros/cons and alternatives)
2. Environment variables (local, CI/CD, GCP)
3. Running locally (dev profile, port 8080)
4. Running tests (Testcontainers)
5. Manual deployment to GCP (Cloud Run)
6. CI/CD with GitHub Actions
7. Public vs gated routes and feature flags
8. Troubleshooting

—

## 1) Tech stack and design choices

Spring Boot 3 + Java 21

- Pros: mature ecosystem, strong security, test support, production-ready on Cloud Run
- Cons: higher memory footprint vs Go/Node; slower cold start than Go
- Alternatives: Node (NestJS), Go (Fiber, Echo). Both are lighter but would duplicate existing JVM skills/libs.

PostgreSQL (Cloud SQL in prod, H2 for dev profile)

- Pros: reliability, ACID, broad driver support; works seamlessly with Hibernate/JPA
- Alternatives: MySQL; or serverless options (AlloyDB Omni, Firestore) depending on data model

Feature flags (in-app service for now)

- Pros: no external dependency, simple; can evolve to OpenFeature providers later (Unleash/Flagd/LauchDarkly)
- Alternatives: Unleash for richer rollout strategies; Flagd for lightweight local experiments

Cloud Run deployment

- Pros: serverless, request-based autoscaling, HTTPS, IAM; easy integration with Artifact Registry and Secret Manager
- Alternatives: GKE (more control, more ops), Compute Engine (VMs), App Engine (opinionated)

Security

- Public read endpoints for games/projects; gated/admin endpoints require roles
- JWT path available; optional Firebase token filter can be enabled by providing credentials

## 2) Environment variables

Notes:

- For multiple CORS origins, separate by comma:
  `CORS_ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000,http://localhost:3001,http://127.0.0.1:3001`
- In production (Cloud Run), prefer Secret Manager and service-level env vars instead of `.env` files.

Contact/social (displayed by frontend and available via `/api/meta`):

- `APP_GITHUB_URL` — GitHub profile or org URL
- `APP_LINKEDIN_URL` — LinkedIn profile URL
- `APP_CONTACT_EMAIL` — public email

Database (default profile; not needed for `dev` H2 profile):

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`

JWT (optional if you use the JWT auth path):

- `APP_JWT_SECRET`, `APP_JWT_EXPIRATION_MS` (e.g., 86400000)

Firebase (optional; only if you enable Firebase auth):

- Mapped in `application.yml` under `firebase.*` from envs like `NEXT_FIREBASE_CREDS_*`

Cloud Run/Cloud SQL (prod):

- `SPRING_DATASOURCE_URL`, `DB_USER`, `DB_PASS`, `GCP_INSTANCE_CONNECTION_NAME` (if using socket factory)
- `SPRING_PROFILES_ACTIVE=cloud`

## 3) Running locally

Prerequisites: Java 21, Maven, Docker (for tests only).

Dev profile (H2; easy mode):
```
mvn -Pdev spring-boot:run -Dspring-boot.run.profiles=dev
```

Backend listens on http://localhost:8080.

Default profile (Postgres):
```
docker run --rm \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=gamesdb \
  -p 5432:5432 postgres:15-alpine

set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=gamesdb
set DB_USER=postgres
set DB_PASSWORD=postgres
mvn spring-boot:run
```

Useful public endpoints:

- `GET /` — welcome JSON
- `GET /healthz` — lightweight health
- `GET /api/meta` — contact links from env
- `GET /api/features` — current feature flags
- `GET /api/projects` — placeholder projects list
- `GET /api/featured` — sample featured games

## 4) Running tests

Requires Docker for Testcontainers.
```
mvn -q -DskipITs=false test
```

Postgres Testcontainer is auto-wired via `@ServiceConnection`. Firebase-dependent beans are conditional; tests run
without Firebase credentials.

## 5) Manual deployment to GCP (Cloud Run)

Setup (run once):
```
gcloud auth login
gcloud config set project <PROJECT_ID>
gcloud auth configure-docker <REGION>-docker.pkg.dev
```

Build/push image:

```
REGION=<your-region>
PROJECT=<your-project>
SERVICE=gamehub-api
SHA=$(git rev-parse --short HEAD)
IMAGE=${REGION}-docker.pkg.dev/${PROJECT}/apps/${SERVICE}:${SHA}

mvn -DskipTests package
docker build -t ${IMAGE} .
docker push ${IMAGE}
```

Deploy to Cloud Run (backend on port 8080):
```
gcloud run deploy ${SERVICE} \
  --region=${REGION} \
  --image=${IMAGE} \
  --allow-unauthenticated \
  --port=8080 \
  --set-env-vars SPRING_PROFILES_ACTIVE=cloud \
  --set-env-vars APP_GITHUB_URL=...,APP_LINKEDIN_URL=...,APP_CONTACT_EMAIL=...
```

If using Cloud SQL, either provide `SPRING_DATASOURCE_URL` and credentials, or configure the socket factory and set
`GCP_INSTANCE_CONNECTION_NAME`.

## 6) CI/CD (GitHub Actions)

Workflow: `.github/workflows/backend-ci.yml` builds/tests, builds/pushes the image to Artifact Registry, then deploys to
Cloud Run.

Repository settings → Secrets and variables → Actions:

- Secrets: `GCP_PROJECT_ID`, `GCP_REGION`, `GCP_SA_KEY` (JSON)
- Variables (optional): `AR_REPO` (default `apps`), `BACKEND_SERVICE` (default `gamehub-api`), `APP_GITHUB_URL`,
  `APP_LINKEDIN_URL`, `APP_CONTACT_EMAIL`

On push to `main`, the job deploys the latest commit.

## 7) Public vs gated routes and feature flags

Public routes (no sign-in):

- `/`, `/healthz`, `/api/meta`, `/api/features`, `/api/featured`, `/api/projects/**`

Admin/gated routes:

- `/api/admin/features/**` requires `ROLE_ADMIN`
- Add premium/mutation endpoints under roles or subscriptions as needed

Feature flags:

- `GET /api/features` returns all flags (env defaults merged with runtime overrides)
- `POST /api/admin/features/{flag}/toggle?enable=true|false` toggles a known flag (ADMIN only)

## 8) Troubleshooting

- Port in use: backend runs on 8080; stop conflicting processes or set `PORT` env when running
- Testcontainers slow: pre-pull `postgres:15-alpine`
- Cloud Run deploy fails: check Artifact Registry permissions and `gcloud auth configure-docker`
- Database connection: confirm `SPRING_DATASOURCE_URL` or `DB_*` envs; in `dev` profile H2 is used instead

—

License: MIT

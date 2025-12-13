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

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_PROFILES_ACTIVE=prod`

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
- `GET /actuator/health` — actuator health
- `GET /api/meta` — contact links from env
- `GET /api/features` — current feature flags
- `GET /api/projects` — placeholder projects list
- `GET /api/featured` — sample featured games

Quick API examples:

```
curl -s http://localhost:8080/healthz
curl -s http://localhost:8080/actuator/health
curl -s "http://localhost:8080/api/scores?gameType=snake" | jq
curl -s http://localhost:8080/api/scores/leaderboard | jq
```

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

Build/push image (Artifact Registry):

```
REGION=northamerica-northeast1
PROJECT=games-portal-479600
AR_REPO=gamehub
SERVICE=gamehub-api
SHA=$(git rev-parse --short HEAD)
IMAGE=${REGION}-docker.pkg.dev/${PROJECT}/${AR_REPO}/${SERVICE}:${SHA}

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
  --set-env-vars SPRING_PROFILES_ACTIVE=prod,CORS_ALLOWED_ORIGINS=https://<frontend-domain> \
  --set-secrets "SPRING_DATASOURCE_URL=SPRING_DATASOURCE_URL:latest,SPRING_DATASOURCE_USERNAME=SPRING_DATASOURCE_USERNAME:latest,SPRING_DATASOURCE_PASSWORD=SPRING_DATASOURCE_PASSWORD:latest"
  
  
  
  

gcloud run deploy $SERVICE `
  --region=$REGION `
  --image=$IMAGE `
  --allow-unauthenticated `
  --port=8080 `
  --add-cloudsql-instances $INSTANCE_CONNECTION_NAME `
  --vpc-connector "gamehub-vpc-connector" `
  --set-env-vars SPRING_PROFILES_ACTIVE=prod,CORS_ALLOWED_ORIGINS=https://<frontend-domain> `
  --set-secrets "SPRING_DATASOURCE_URL=SPRING_DATASOURCE_URL:latest,SPRING_DATASOURCE_USERNAME=SPRING_DATASOURCE_USERNAME:latest,SPRING_DATASOURCE_PASSWORD=SPRING_DATASOURCE_PASSWORD:latest"
```

If using Cloud SQL with Private IP (recommended), deploy with the Cloud SQL connector and VPC:

1) Ensure your Cloud SQL instance has Private IP enabled and that you have a Serverless VPC Access connector in the same
   region.
2) Set your Secret Manager keys for:
    - `SPRING_DATASOURCE_URL` (e.g., `jdbc:postgresql://<PRIVATE_IP>:5432/gamesdb`)
    - `SPRING_DATASOURCE_USERNAME`
    - `SPRING_DATASOURCE_PASSWORD`
3) Deploy with `--add-cloudsql-instances` using your instance connection name:

```
INSTANCE_CONNECTION_NAME=${PROJECT}:${REGION}:games-postgresql-instance

gcloud run deploy ${SERVICE} \
  --region=${REGION} \
  --image=${IMAGE} \
  --allow-unauthenticated \
  --port=8080 \
  --add-cloudsql-instances ${INSTANCE_CONNECTION_NAME} \
  --set-env-vars SPRING_PROFILES_ACTIVE=prod,CORS_ALLOWED_ORIGINS=https://<frontend-domain> \
  --set-secrets "SPRING_DATASOURCE_URL=SPRING_DATASOURCE_URL:latest,SPRING_DATASOURCE_USERNAME=SPRING_DATASOURCE_USERNAME:latest,SPRING_DATASOURCE_PASSWORD=SPRING_DATASOURCE_PASSWORD:latest"
```

## 6) CI/CD (GitHub Actions)

Workflow: `.github/workflows/ci-cd.yml` builds/tests, builds/pushes the image to Artifact Registry, then deploys to
Cloud Run (port 8080). On push to `main`, it will run automatically.

Repository settings → Secrets and variables → Actions:

- Secrets: `GCP_PROJECT_ID`, `GCP_REGION`, `GCP_SA_KEY` (JSON). Optional: `SECRET_FLAGS` string for extra
  `--set-secrets` pairs.
- Variables: `AR_REPO` (default `gamehub`), `BACKEND_SERVICE` (default `gamehub-api`), optional `FRONTEND_URL` for CORS,
  optional `INSTANCE_CONNECTION_NAME` if using Cloud SQL connector.

On push to `main`, the job deploys the latest commit.

See also: `.junie/guidelines.md` for a newcomer-friendly end‑to‑end guide.

### 6.1) Optional: Push images to Docker Hub

If you also want to publish images to Docker Hub, add repository secrets:

- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`

The workflow will push tags `${DOCKERHUB_USERNAME}/${BACKEND_SERVICE}:<SHA>` and `:latest` when these are present.

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

## 9) Cloud SQL connectivity: Private IP vs Public IP with Proxy

You have two mainstream options to connect Cloud Run to Cloud SQL Postgres:

- Private IP (recommended for production):
    - Pros: traffic stays on Google private network; lowest attack surface; simpler runtime (no sidecar proxy needed);
      IAM-based access possible; predictable egress costs within VPC.
    - Cons: requires setting up a VPC connector and enabling private service access; a bit more initial setup; needs
      instance private IP.
    - Changes: provision a Serverless VPC Access connector; enable Private IP on the Cloud SQL instance; deploy with
      `--add-cloudsql-instances ${PROJECT}:${REGION}:${INSTANCE}` and use a JDBC URL pointing to the private IP. Keep
      secrets in Secret Manager and map them at deploy time.

- Public IP with Cloud SQL Auth Proxy (or direct SSL):
    - Pros: fastest to get running; no VPC connector required; strong auth using IAM DB Authentication with the proxy;
      easy to run locally and in CI.
    - Cons: runs an extra proxy process in your container (adds memory/CPU and a point of failure); egress goes over
      public endpoints (still encrypted/authenticated); potential cold-start impact.
    - Changes: keep the `cloud_sql_proxy` binary in the image (this repo provides it) and set
      `GCP_INSTANCE_CONNECTION_NAME`. The `startup.sh` starts the proxy and sets `SPRING_DATASOURCE_URL` to
      `jdbc:postgresql://localhost:5432/${DB_NAME}`. Provide DB user/password via Secret Manager. (Note: we recommend
      Private IP for prod.)

Recommendation: Use Private IP for production (security and cost), and allow Public IP + Proxy for local experiments or
as a transitional setup. The included CI workflow supports adding `--add-cloudsql-instances` when you define
`INSTANCE_CONNECTION_NAME` as a repo variable.

—

License: MIT

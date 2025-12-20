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
9. Cloud SQL connectivity (Private IP vs Proxy)
10. STOMP rate limiting (Realtime WebSocket)

—

## 1) Tech stack and design choices

Spring Boot 3 + Java 21

- Pros: mature ecosystem, strong security, test support, production-ready on Cloud Run
- Cons: higher memory footprint vs Go/Node; slower cold start than Go
- Alternatives: Node (NestJS), Go (Fiber, Echo). Both are lighter but would duplicate existing JVM skills/libs.

PostgreSQL (Cloud SQL in prod, Postgres Docker for dev)

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

Database (default profile; dev uses PostgreSQL via Docker — not H2):

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`

JWT (optional if you use the JWT auth path):

- `APP_JWT_SECRET`, `APP_JWT_EXPIRATION_MS` (e.g., 86400000)

Firebase (optional; only if you enable Firebase auth):

- Mapped in `application.yml` under `firebase.*` from envs like `NEXT_FIREBASE_CREDS_*`

Cloud Run/Cloud SQL (prod):

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_PROFILES_ACTIVE=prod`

## 3) Running locally

Prerequisites: Java 21, Maven, Docker (for Postgres and tests).

Automatic Postgres startup (dev):

- With Spring Boot 3.2’s Docker Compose support enabled, running the app in the `dev` profile will automatically bring
  up the `postgres` service from the root `docker-compose.yml`, and tear it down when the app stops. No manual
  `docker compose up` is required.

Start the backend in dev profile (auto‑starts Postgres):
```
mvn -Pdev spring-boot:run -Dspring-boot.run.profiles=dev
```

Notes:

- The `dev` profile uses PostgreSQL. Defaults point to `jdbc:postgresql://localhost:5432/gamesdb` with
  `postgres/postgres`.
- You can override via envs: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.
- Spring Boot’s compose integration is configured in `application-dev.yml` under `spring.docker.compose.*` and uses the
  top‑level `docker-compose.yml`. It performs `start-and-stop` lifecycle management, so containers stop when the app
  exits.

Fallback/manual options (if you prefer to manage Docker yourself):

- Docker Compose (recommended):
  ```
  docker compose up -d postgres
  ```
- Single container:
  ```
  docker run --name gamehub_postgres --rm -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=gamesdb -p 5432:5432 postgres:15-alpine
  ```
- Backend listens on http://localhost:8080.

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

Automatic Postgres for tests:

- Integration tests start an isolated PostgreSQL Testcontainer automatically (see `BaseIntegrationTest`). The test
  environment does not depend on `docker-compose.yml` and does not reuse the dev database, ensuring isolation and
  reproducibility.
- Firebase-dependent beans are conditional; tests run without Firebase credentials.

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
3) Deploy with `--add-cloudsql-instances` using your instance connection name and specify your Serverless VPC Access
   connector (recommended when using Private IP). Ensure your Secret Manager value for `SPRING_DATASOURCE_URL` is the
   plain Private IP JDBC URL (no socket factory params), for example: `jdbc:postgresql://10.80.0.3:5432/gamesdb`.

```
INSTANCE_CONNECTION_NAME=${PROJECT}:${REGION}:games-postgresql-instance

gcloud run deploy ${SERVICE} \
  --region=${REGION} \
  --image=${IMAGE} \
  --allow-unauthenticated \
  --port=8080 \
  --add-cloudsql-instances ${INSTANCE_CONNECTION_NAME} \
  --vpc-connector gamehub-vpc-connector \
  --set-env-vars SPRING_PROFILES_ACTIVE=prod,CORS_ALLOWED_ORIGINS=https://<frontend-domain> \
  --set-secrets "SPRING_DATASOURCE_URL=SPRING_DATASOURCE_URL:latest,SPRING_DATASOURCE_USERNAME=SPRING_DATASOURCE_USERNAME:latest,SPRING_DATASOURCE_PASSWORD=SPRING_DATASOURCE_PASSWORD:latest"
```

## 6) CI/CD (GitHub Actions)

This repo uses exactly two workflows (consolidated):

- `.github/workflows/backend-tests.yml`
    - Trigger: pull_request (to `main`) and manual `workflow_dispatch`
    - Action: unit tests only (`mvn -B -Dtest="*Test" test`) for fast PR feedback

- `.github/workflows/backend-ci.yml`
    - Trigger: push to `main` and `workflow_dispatch`
    - Jobs (in order):
        1) `test-unit` — unit tests only
        2) `test-e2e` — full suite (integration/E2E); optional/allow-fail by design
        3) `build-local-and-tag-images` — build Docker image once and save as artifact
        4) `push-and-deploy-to-cloudrun` — authenticate to GCP, retag and push to Artifact Registry, deploy to Cloud Run
            - Deploy is gated by repo var `DEPLOY_ENABLED` and only runs on branch `main`
        5) `push-to-dockerhub` — optional fallback if AR/GCP is unavailable and Docker Hub creds exist

Required repository configuration (Settings → Secrets and variables → Actions):

- Secrets:
    - `GCP_PROJECT_ID` — your GCP project ID
    - `GCP_REGION` — e.g., `northamerica-northeast1`
    - One authentication method (pick one):
        - Workload Identity Federation (recommended):
            - `GCP_WORKLOAD_IDENTITY_PROVIDER` — full resource name of the provider
            - `GCP_SERVICE_ACCOUNT` — deployer service account email (e.g.,
              `gha-deploy@<project>.iam.gserviceaccount.com`)
        - Or Service Account key (fallback):
            - `GCP_SA_KEY` — JSON key for the deployer SA
    - Optional: `DOCKERHUB_TOKEN`, `SECRET_FLAGS` (string passed to `gcloud run deploy --set-secrets "..."`)

- Variables:
    - `AR_REPO` (default `gamehub`)
    - `BACKEND_SERVICE` (default `gamehub-api`)
    - `FRONTEND_URL` for CORS (e.g., `https://<frontend-domain>` or `http://localhost:3000`)
    - Optional: `INSTANCE_CONNECTION_NAME` (adds `--add-cloudsql-instances`)
    - Optional: `VPC_CONNECTOR` (adds `--vpc-connector`)
    - `DEPLOY_ENABLED` — set to `true` to allow deploy to run on `main` (defaults to `false`)

### 6.1) Enable deploys safely (permissions + toggle)

1) Toggle: set repo variable `DEPLOY_ENABLED=true`.

2) Grant IAM so the GitHub identity can deploy and can act-as the runtime service account used by Cloud Run
   (e.g., `gamehub-api-sa@<project>.iam.gserviceaccount.com`). Grant the following to your deployer principal
   (WIF or SA):

- On the runtime service account (Cloud Run service account):
    - Role: `roles/iam.serviceAccountUser` (provides `iam.serviceaccounts.actAs`)

- At project (or narrower scope):
    - Role: `roles/run.admin` (or a narrower deploy role)
    - Role: `roles/artifactregistry.writer` (to push images)

Sample commands (replace placeholders):

```
PROJECT_ID=<your-project>
REGION=northamerica-northeast1
RUNTIME_SA=gamehub-api-sa@${PROJECT_ID}.iam.gserviceaccount.com
# If using Workload Identity Federation
DEPLOY_SA=gha-deploy@${PROJECT_ID}.iam.gserviceaccount.com

# Allow deploy principal to actAs the runtime SA used by Cloud Run
gcloud iam service-accounts add-iam-policy-binding "$RUNTIME_SA" \
  --member="serviceAccount:${DEPLOY_SA}" \
  --role="roles/iam.serviceAccountUser"

# Grant deploy permissions (project-level shown here)
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${DEPLOY_SA}" \
  --role="roles/run.admin"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${DEPLOY_SA}" \
  --role="roles/artifactregistry.writer"

# If you use WIF directly without a GCP SA, bind your workload identity pool principal instead:
# PRINCIPAL="principalSet://iam.googleapis.com/projects/<NUMERIC_PROJECT_ID>/locations/global/workloadIdentityPools/<POOL_ID>/attribute.repository/<owner>/<repo>"
# gcloud iam service-accounts add-iam-policy-binding "$RUNTIME_SA" --member="$PRINCIPAL" --role="roles/iam.serviceAccountUser"
```

After a successful deploy, the workflow runs smoke checks:

```
SERVICE=${{ vars.BACKEND_SERVICE }}
REGION=${{ secrets.GCP_REGION }}
URL=$(gcloud run services describe "$SERVICE" --region "$REGION" --format='value(status.url)')
curl -fsS "$URL/healthz"; echo
curl -fsS "$URL/actuator/health"; echo
```

### 6.2) Optional: Push images to Docker Hub

If you also want to publish images to Docker Hub, set repository variables/secrets:

- Vars: `DOCKERHUB_USERNAME` (and optionally `DOCKERHUB_REPO`)
- Secret: `DOCKERHUB_TOKEN`

The pipeline will push `${DOCKERHUB_USERNAME}/${BACKEND_SERVICE}:<SHA>` and `:latest` when present.

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
- Database connection: confirm Postgres is running (`docker compose ps`), and the `dev` profile points to it (defaults
  provided). Override with `SPRING_DATASOURCE_*` if needed.

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

## 10) STOMP rate limiting (Realtime WebSocket)

This backend includes a Redis-backed STOMP rate limiter to protect realtime channels (/ws) from abuse and to keep
Cloud Run instances stable under load.

- What it does: caps SEND frames per minute per identity; identity is resolved in this order
  (userId → sessionId → x-forwarded-for IP → anon).
- Limits (defaults):
    - Authenticated users: 300 messages/min
    - Guests: 120 messages/min
    - Messages over the limit are dropped (not forwarded to handlers); clients should back off automatically.

Configuration (application.yml / env):

```
stomp.ratelimit.user.perMinute: 300   # env: STOMP_USER_MSGS_PER_MIN
stomp.ratelimit.guest.perMinute: 120  # env: STOMP_GUEST_MSGS_PER_MIN
```

Why Redis-backed (recommended for Cloud Run):

- Pros:
    - Consistent limits across multiple instances (Cloud Run scales out across pods).
    - Resilient to instance restarts; simple INCR/EXPIRE windowing.
    - Stateless app instances; no sticky sessions needed.
- Cons:
    - Requires Redis (Memorystore) connectivity and a VPC connector in prod.
    - Slightly more latency per SEND compared to in-memory counters.

Alternative: in-memory/Caffeine counters

- Pros: zero external dependency; ultra-low latency; works offline for dev.
- Cons: per-instance only (each instance maintains its own counters). Under Cloud Run autoscaling, a user could bypass
  limits by spreading messages across instances via load balancing.

Decision for this project: Redis-backed is the default in production to ensure consistent throttling as Cloud Run
scales horizontally. The code fails open if Redis is unavailable (no throttling) to prioritize app availability during
transient outages — monitor logs/metrics and set alerts accordingly.

Tuning & ops tips:

- Use stricter guest limits; authenticated users can be more generous.
- Set explicit `X-Forwarded-For` on your WS proxy if you need IP-based identities for guests.
- Observe message rate counters in logs/metrics and adjust thresholds to balance UX vs protection.

Cloud Run YAML reference

- A declarative manifest is provided at `infra/cloudrun/backend.yaml`. You can deploy it with:

```
REGION=northamerica-northeast1
PROJECT=games-portal-479600
SHA=$(git rev-parse --short HEAD)
gcloud run services replace infra/cloudrun/backend.yaml \
  --region=$REGION --project=$PROJECT
```

GitHub Actions (CI/CD)

- The workflow `.github/workflows/ci-cd.yml` runs tests, builds a container, pushes to Artifact Registry, and deploys to
  Cloud Run by image when GCP secrets are present.

Swagger/OpenAPI

- No new endpoints were introduced for throttling; behavior is applied at the STOMP channel layer. REST endpoints are
  unchanged. Swagger still documents the public REST routes.

—

License: MIT

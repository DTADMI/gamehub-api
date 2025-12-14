# GameHub API — Project Guidelines (Junie‑consumable)

Audience: newcomers and automation (Junie). This single document explains how to present, install, run, test, and deploy
the backend from scratch locally and on Google Cloud Run, and how CI/CD is configured.

---

## 1) What this service is

GameHub API is a Spring Boot 3 backend (Java 21) that powers a portfolio of web games and a projects page.

- Public endpoints for games data, scores, leaderboards, and projects.
- Optional auth/subscriptions for gated features.
- Deployed on Google Cloud Run. The paired frontend (Next.js) runs separately and calls this API via `/api`.

Default ports:

- Backend: 8080
- Frontend: 3000 (consuming backend at `/api`)

---

## 2) Technology choices (pros/cons)

- Spring Boot 3 (Java 21)
    - Pros: mature ecosystem, strong security, actuator, testing, first‑class Cloud Run support.
    - Cons: larger memory/cold start than Go/Node.
- PostgreSQL (Cloud SQL in prod, H2 for dev profile)
    - Pros: reliability, ACID, great JPA support.
    - Cons: requires managed instance or container for prod.
- Feature flags (in‑app now; can evolve to Unleash/Flagd)
    - Pros: simple, no extra infra.
    - Cons: fewer strategies until external provider is added.
- Cloud Run
    - Pros: serverless, HTTPS, auto‑scaling, IAM, Artifact Registry.
    - Cons: per‑request billing; cold starts possible.

---

## 3) Prerequisites

- Java 21 + Maven 3.9+
- Docker (only required for Testcontainers and local Postgres)
- gcloud CLI (for deploys): authenticated to your GCP project
- A GCP Artifact Registry Docker repo (e.g., `games`) in your project

Optional (prod features):

- Secret Manager entries for DB credentials and optional Firebase/Stripe

---

## 4) Environment variables

Local (dev):

- `CORS_ALLOWED_ORIGINS` — comma‑separated origins (e.g., `http://localhost:3000,http://127.0.0.1:3000`). Defaults to
  `*`.

Prod (Cloud Run):

- Database (if not using H2): `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- Profile: `SPRING_PROFILES_ACTIVE=prod` (or `cloud` depending on your config)
- CORS: `CORS_ALLOWED_ORIGINS=https://<frontend-domain>`
- Optional Firebase Admin creds (one secret per field, see README): `NEXT_FIREBASE_CREDS_*`

Where to get them:

- DB creds: create in Cloud SQL Postgres; store values in Secret Manager
- Firebase creds: create a Firebase service account JSON; split into Secret Manager keys per field
- CORS: use your frontend URL (Cloud Run or custom domain)

---

## 5) Run locally

Dev profile (H2 + seed data):

```
mvn -Pdev spring-boot:run -Dspring-boot.run.profiles=dev
```

The app listens on http://localhost:8080. The `DevDataSeeder` seeds demo users and scores if none exist.

Local Postgres (default profile):

```
docker run --rm -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=gamesdb -p 5432:5432 postgres:15-alpine

set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=gamesdb
set SPRING_DATASOURCE_URL=jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_NAME%
set SPRING_DATASOURCE_USERNAME=postgres
set SPRING_DATASOURCE_PASSWORD=postgres
mvn spring-boot:run
```

Health and sample endpoints:

- `GET /healthz` → 200
- `GET /actuator/health` → 200
- `GET /api/scores?gameType=snake` → recent scores
- `GET /api/scores/leaderboard` → top lists per game

Example cURLs:

```
curl -s http://localhost:8080/healthz
curl -s http://localhost:8080/api/scores?gameType=snake | jq
curl -s http://localhost:8080/api/scores/leaderboard | jq
```

Submitting a score (requires Authentication; tests use a stub principal):

- In real usage, include Authorization header (JWT/Firebase) once configured.

---

## 6) Tests

- Unit tests (fast): `mvn -Dtest="*Test" test`
    - Includes `ScoreValidationServiceTest` and standalone MockMvc tests for `ScoreController`.
- Full suite (may include ITs requiring local services): `mvn test`

Notes: Some legacy integration tests may require extra config (Auth/Redis). CI focuses on unit tests for reliability.

---

## 7) Manual deploy to Cloud Run (backend on port 8080)

Authenticate and set project/region once:

```
gcloud auth login
gcloud config set project <PROJECT_ID>
gcloud auth configure-docker <REGION>-docker.pkg.dev
```

Build and push image (run at repo root):

```
set REGION=northamerica-northeast1
set PROJECT=games-portal-479600
set AR_REPO=gamehub
set BACKEND_SERVICE=gamehub-api
set SHA=%GITHUB_SHA%  
:: or for local shells: set SHA=local

docker build -t %REGION%-docker.pkg.dev/%PROJECT%/%AR_REPO%/%BACKEND_SERVICE%:%SHA% -f Dockerfile .
docker push %REGION%-docker.pkg.dev/%PROJECT%/%AR_REPO%/%BACKEND_SERVICE%:%SHA%
```

Deploy (port 8080, prod profile):

```
gcloud run deploy %BACKEND_SERVICE% ^
  --region=%REGION% ^
  --image=%REGION%-docker.pkg.dev/%PROJECT%/%AR_REPO%/%BACKEND_SERVICE%:%SHA% ^
  --allow-unauthenticated ^
  --port=8080 ^
  --set-env-vars SPRING_PROFILES_ACTIVE=prod,CORS_ALLOWED_ORIGINS=http://localhost:3000 ^
  --set-secrets "SPRING_DATASOURCE_URL=SPRING_DATASOURCE_URL:latest,SPRING_DATASOURCE_USERNAME=SPRING_DATASOURCE_USERNAME:latest,SPRING_DATASOURCE_PASSWORD=SPRING_DATASOURCE_PASSWORD:latest"
```

Smoke test after deploy:

```
SERVICE_URL=https://<cloud-run-backend-url>
curl -sS %SERVICE_URL%/healthz
curl -sS %SERVICE_URL%/actuator/health
```

---

### 7.1) Private IP to Cloud SQL (recommended for production)

Ensure:

- Cloud SQL instance has Private IP enabled
- A Serverless VPC Access connector exists in your region (e.g., `gamehub-vpc-connector`)
- Secret Manager entries exist:
    - `SPRING_DATASOURCE_URL` set to a plain Private IP JDBC URL like `jdbc:postgresql://10.80.0.3:5432/gamesdb`
    - `SPRING_DATASOURCE_USERNAME` (e.g., `postgres`)
    - `SPRING_DATASOURCE_PASSWORD`

Bash/macOS/Linux:

```
REGION=northamerica-northeast1
PROJECT=games-portal-479600
AR_REPO=gamehub
SERVICE=gamehub-api
SHA=$(git rev-parse --short HEAD)
IMAGE=${REGION}-docker.pkg.dev/${PROJECT}/${AR_REPO}/${SERVICE}:${SHA}
INSTANCE_CONNECTION_NAME=${PROJECT}:${REGION}:games-postgresql-instance

mvn -DskipTests package
docker build -t "$IMAGE" -f Dockerfile .
docker push "$IMAGE"

gcloud run deploy "$SERVICE" \
  --region="$REGION" \
  --image="$IMAGE" \
  --allow-unauthenticated \
  --port=8080 \
  --add-cloudsql-instances "$INSTANCE_CONNECTION_NAME" \
  --vpc-connector "gamehub-vpc-connector" \
  --vpc-egress all \
  --set-env-vars SPRING_PROFILES_ACTIVE=prod,CORS_ALLOWED_ORIGINS=http://localhost:3000 \
  --set-secrets "SPRING_DATASOURCE_URL=SPRING_DATASOURCE_URL:latest,SPRING_DATASOURCE_USERNAME=SPRING_DATASOURCE_USERNAME:latest,SPRING_DATASOURCE_PASSWORD=SPRING_DATASOURCE_PASSWORD:latest"
```

PowerShell (Windows):

```
$REGION = "northamerica-northeast1"
$PROJECT = "games-portal-479600"
$AR_REPO = "gamehub"
$SERVICE = "gamehub-api"
$SHA = $(git rev-parse --short HEAD)
$IMAGE = "$REGION-docker.pkg.dev/$PROJECT/$AR_REPO/$SERVICE:$SHA"
$INSTANCE_CONNECTION_NAME = "$PROJECT:$REGION:games-postgresql-instance"
$VPC = "gamehub-vpc-connector"

mvn -DskipTests package
docker build -t $IMAGE -f Dockerfile .
docker push $IMAGE

gcloud run deploy $SERVICE `
  --region=$REGION `
  --image=$IMAGE `
  --allow-unauthenticated `
  --port=8080 `
  --add-cloudsql-instances $INSTANCE_CONNECTION_NAME `
  --vpc-connector $VPC `
  --vpc-egress all `
  --set-env-vars SPRING_PROFILES_ACTIVE=prod,CORS_ALLOWED_ORIGINS=http://localhost:3000 `
  --set-secrets "SPRING_DATASOURCE_URL=SPRING_DATASOURCE_URL:latest,SPRING_DATASOURCE_USERNAME=SPRING_DATASOURCE_USERNAME:latest,SPRING_DATASOURCE_PASSWORD=SPRING_DATASOURCE_PASSWORD:latest"
```

Notes:

- Use a plain Private IP JDBC URL in `SPRING_DATASOURCE_URL`. Do not include socket factory parameters when using
  Private IP.
- Do not set `GCP_INSTANCE_CONNECTION_NAME` in Cloud Run when using Private IP; the startup script will not alter your
  datasource URL.
- `startup.sh` exists for the Public IP + Proxy path; for Private IP it does nothing unless the proxy env is set.

---

## 8) CI/CD with GitHub Actions

Add repo secrets (Settings → Secrets and variables → Actions):

- `GCP_PROJECT_ID`, `GCP_REGION`, `GCP_SA_KEY` (JSON)

Add repo variables (Settings → Variables → Actions):

- `AR_REPO=gamehub`, `BACKEND_SERVICE=gamehub-api`
- Optional: `INSTANCE_CONNECTION_NAME=games-portal-479600:northamerica-northeast1:games-postgresql-instance` (adds
  `--add-cloudsql-instances`)
- Optional: `VPC_CONNECTOR=gamehub-vpc-connector` (adds `--vpc-connector`)
- Optional: `FRONTEND_URL` for CORS (e.g., `https://<frontend-domain>` or `http://localhost:3000`)

Optional secrets (if using real DB/Firebase):

- `SECRET_FLAGS` string for `gcloud run deploy --set-secrets "..."` content

Workflow does:

1) Check out; set up JDK 21; cache Maven
2) Run unit tests only (`*Test`) to avoid flaky legacy ITs
3) Build JAR
4) Authenticate to GCP and configure Docker
5) Build and push image to Artifact Registry
6) Deploy to Cloud Run on port 8080
    - If `INSTANCE_CONNECTION_NAME` is set, the workflow includes `--add-cloudsql-instances`
    - If `VPC_CONNECTOR` is set, the workflow includes `--vpc-connector`

### 8.1) Optional: Push to Docker Hub

If you set `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` as repository secrets, the CI will additionally push
`${DOCKERHUB_USERNAME}/${BACKEND_SERVICE}:<SHA>` and `:latest`.

---

## 9) Troubleshooting

- Container not listening on expected port: ensure `--port=8080` at deploy time; logs show binding.
- CORS blocked: set `CORS_ALLOWED_ORIGINS` to your frontend origin (no trailing slash; multiple origins
  comma‑separated).
- Frontend cannot reach backend: confirm frontend uses `NEXT_PUBLIC_API_URL=<backend-url>/api` and backend URL is
  publicly reachable.
- Tests fail on ITs: run unit tests only during development: `mvn -Dtest="*Test" test`.
- Startup fails with DB connection errors over Private IP:
    - Verify `SPRING_DATASOURCE_URL` secret is a plain Private IP JDBC URL (e.g.,
      `jdbc:postgresql://10.80.0.3:5432/gamesdb`). Do not include socket factory params.
    - Ensure `GCP_INSTANCE_CONNECTION_NAME` is NOT set on the service.
    - Keep `--add-cloudsql-instances` and `--vpc-connector` in the deploy command.

---

## 11) Cloud SQL connectivity: choose Private IP or Public IP + Proxy

Two supported patterns to connect Cloud Run to Cloud SQL Postgres:

- Private IP (recommended for production):
    - Pros: private networking, minimal attack surface, no proxy process, predictable egress within VPC.
    - Cons: requires a Serverless VPC Access connector and enabling Private IP on the instance.
    - How: create a VPC connector; enable Private IP on Cloud SQL; deploy with
      `--add-cloudsql-instances %PROJECT%:%REGION%:%INSTANCE%` and set datasource via Secret Manager (JDBC URL can be
      the private IP or use the Socket Factory).

- Public IP with Cloud SQL Auth Proxy (or direct SSL):
    - Pros: fastest to start, no VPC connector, strong auth via IAM DB Auth with proxy; easy locally and in CI.
    - Cons: extra proxy process (memory/CPU), public endpoint (still TLS/IAM), potential cold‑start impact.
    - How: keep `cloud_sql_proxy` in the image and set `GCP_INSTANCE_CONNECTION_NAME`. The included `startup.sh` will
      run the proxy and export `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/%DB_NAME%` at runtime. Provide DB
      creds via Secret Manager.

Recommendation: Use Private IP for production and optionally keep Public IP + Proxy for local or transitional
deployments.

---

## 12) Quick vars (safe to copy/paste)

Set these for this project; do not paste secrets into the repo or docs. Use Secret Manager and GitHub Actions secrets
for sensitive values.

```
REGION=northamerica-northeast1
PROJECT=games-portal-479600
AR_REPO=gamehub
BACKEND_SERVICE=gamehub-api
FRONTEND_SERVICE=gamehub-app
INSTANCE_CONNECTION_NAME=${PROJECT}:${REGION}:games-postgresql-instance
```

---

## 10) Presenting the project (talk track)

- Purpose: backend for portfolio games and projects with leaderboards and basic anti‑abuse validation.
- Stack: Spring Boot 3 + Postgres on Cloud Run; optional Firebase auth; feature flags ready to evolve.
- Operability: Swagger/OpenAPI at `/swagger-ui.html`, health at `/healthz` and `/actuator/health`.
- Delivery: manual deploy and CI/CD to Cloud Run. Backend 8080; frontend 3000.

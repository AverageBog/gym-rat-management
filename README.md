# Gym Rat Management

A gym CRM packaged as a single Docker image: a Spring Boot REST API backend bundled with a React (Vite) SPA frontend, both served from one container on port 8080.

This README is a quickstart for the Docker workflow. For architecture, design decisions, and non-Docker dev (Gradle / Vite hot reload), see [`CLAUDE.md`](./CLAUDE.md).

---

## Prerequisites

- Docker (Engine 20.10+ / Desktop with the WSL integration enabled if on Windows)
- `openssl` and `curl` on your `$PATH` (used by `scripts/test-docker-image.sh` to generate secrets and probe the container)
- `bash` to execute the test script

No JDK or Node install is required to build the image — both toolchains are vendored into the multi-stage build.

---

## Build the image

From the repo root:

```bash
docker build -t gym-rat-management:latest .
```

Three stages run in order: `node:20-alpine` builds the Vite frontend → `eclipse-temurin:17-jdk-jammy` packages the Spring Boot fat JAR with the frontend baked into `static/` → `eclipse-temurin:17-jre-jammy` is the runtime layer (non-root user `spring`, uid `10001`).

Verify the image is present and inspect its layers:

```bash
docker images gym-rat-management
docker history gym-rat-management:latest
```

The runtime layer should not contain source files, `node_modules/`, or Gradle caches — those are excluded by `.dockerignore` and dropped between stages.

---

## Run the container

The container requires three runtime values when run under the `prod` profile (the default for the image). It will fail fast on startup if any are missing.

| Env var               | Purpose                              | How to generate                  |
|-----------------------|--------------------------------------|----------------------------------|
| `JWT_SECRET`          | HMAC signing key for JWTs            | `openssl rand -base64 48`        |
| `CARD_ENCRYPTION_KEY` | AES-256 key (base64-encoded 32 bytes) for stored card numbers | `openssl rand -base64 32` |
| `ADMIN_SEED_PASSWORD` | Password for the seeded `admin@gym.com` account (re-seeded on every cold start since H2 is in-memory) | pick a strong password you'll remember |

```bash
docker run --rm -p 8080:8080 \
  -e JWT_SECRET="$(openssl rand -base64 48)" \
  -e CARD_ENCRYPTION_KEY="$(openssl rand -base64 32)" \
  -e ADMIN_SEED_PASSWORD='your-strong-admin-password' \
  gym-rat-management:latest
```

Then open <http://localhost:8080> and sign in with the seeded admin credentials documented in [`CLAUDE.md`](./CLAUDE.md#seed-credentials).

The image runs under the `prod` Spring profile by default (`SPRING_PROFILES_ACTIVE=prod`), which disables the H2 console and SQL logging. The H2 database is in-memory — restarting the container wipes all data. This is intentional for the current MVP scope.

---

## Run the pre-deployment test script

`scripts/test-docker-image.sh` is the gate before any image gets shipped. It builds the image, exercises the runtime contract end-to-end, and exits non-zero on the first failure so it can run inside CI or a deploy pipeline.

```bash
./scripts/test-docker-image.sh
```

What it verifies, in order:

1. **Image build** completes and the tag is present.
2. **Fail-fast on missing secrets** — `docker run` without `JWT_SECRET` / `CARD_ENCRYPTION_KEY` exits non-zero.
3. **Container startup** with random secrets becomes responsive within 90 seconds (logs are dumped if it doesn't).
4. **`prod` profile is active** in the startup log line.
5. **Runs as non-root** (`uid 10001`).
6. **Endpoint contract**:
   - `GET /` → 200 HTML (SPA shell)
   - `GET /members` → 200 HTML (SPA client-side route)
   - `GET /h2-console` → 404 (disabled under `prod`)
   - `GET /api/members` → 403 (auth required)
   - A real `/assets/*.js` file → 200 with `Content-Type: text/javascript` (regression guard against the SPA fallback hijacking real asset requests)
   - A missing asset → 404, *not* the SPA shell
7. **Login flow** — `POST /api/auth/login` returns a JWT, and authenticated `GET /api/members` returns the seeded members.
8. **Cleanup** — the container is force-removed via a shell trap, even if the script aborts mid-run.

Override the host port if `8080` or the default `18080` is taken:

```bash
HOST_PORT=29090 ./scripts/test-docker-image.sh
```

A successful run prints `All Docker image checks passed.` in green at the bottom. Any failure prints `[FAIL]` with the offending check, the container's last 50 log lines (when relevant), and exits non-zero.

---

## When to re-run

Run `./scripts/test-docker-image.sh` whenever you change any of:

- `Dockerfile` or `.dockerignore`
- `backend/src/main/resources/application-prod.properties` (or anything that affects the `prod` Spring profile)
- `backend/src/main/java/com/gymrat/config/SecurityConfig.java` (the URL matcher rules are runtime-critical)
- `backend/src/main/java/com/gymrat/controller/SpaFallbackAdvice.java` (SPA fallback behavior)
- Anything that adds a new required runtime env var or exposed port

The standard `./gradlew test` suite covers Java unit/integration tests but does not exercise the Docker runtime — that's what this script is for.

---

## Troubleshooting

**`Cannot connect to the Docker daemon`** — Docker isn't running. On Windows/WSL, start Docker Desktop and enable WSL integration for your distro under *Settings → Resources → WSL integration*.

**Container exits immediately with `Could not resolve placeholder 'JWT_SECRET'`** — you forgot to pass `-e JWT_SECRET=...`. The same applies to `CARD_ENCRYPTION_KEY`. This is the intended fail-fast behavior.

**`port is already allocated`** — something else is using `8080` (or `18080` if running the test script). Pick another port: `-p 9090:8080` for `docker run`, or `HOST_PORT=29090` for the script.

**Test script fails at "Container did not become responsive"** — the script dumps the last 50 log lines automatically. Most often this is a Spring startup failure caused by a malformed env var; check the logs for a `Caused by:` line.

**Need to inspect the running container** — `docker exec -it <container_name> sh` (the runtime image is Ubuntu Jammy — `bash` is also available).

---

## Deploy to Google Cloud Run

The same image deploys to Google Cloud Run as a public, scale-to-zero service. The Spring Boot app honors Cloud Run's `$PORT` env var (`server.port=${PORT:8080}` in `application.properties`), so no image changes are needed.

> **MVP caveat (Cloud Run amplifies):** the H2 database is in-memory, and Cloud Run scales to zero by default. Every cold start is a fresh database, and concurrent instances each hold their own copy. Acceptable for a demo; not a real persistence story.

### Prerequisites

- Everything from the Docker section above.
- `gcloud` CLI installed and authenticated (`gcloud auth login`).
- A GCP project with billing enabled and the following APIs on: `run.googleapis.com`, `artifactregistry.googleapis.com`, `secretmanager.googleapis.com`.
- Your project ID (`gcloud config get-value project` or check the GCP console).

### One-time GCP bootstrap

Run these once per GCP project. All commands assume `us-central1`; change the region if you prefer another and pass `GCP_REGION` to the deploy script later.

```bash
# 1. Point gcloud at the right project, then verify before proceeding.
#    Every command below acts on whatever project is active — running them
#    against the wrong project is the #1 source of "service account does
#    not exist" errors later, because resources land in a project you
#    aren't deploying to. Confirm the output matches <PROJECT_ID> exactly.
gcloud config set project <PROJECT_ID>
gcloud config get-value project   # must print <PROJECT_ID>

# 2. Create the Artifact Registry repo (Docker format).
gcloud artifacts repositories create gym-rat-management \
  --repository-format=docker \
  --location=us-central1 \
  --description="gym-rat-management container images"

# 3. Create the three runtime secrets in Secret Manager.
printf '%s' "$(openssl rand -base64 48)" | \
  gcloud secrets create jwt-secret --data-file=-

printf '%s' "$(openssl rand -base64 32)" | \
  gcloud secrets create card-encryption-key --data-file=-

# Pick a strong admin password — this is what you'll log in as admin@gym.com with.
# It is re-seeded on every cold start, so changing it requires a `gcloud secrets versions add`
# (see "Rotating secrets" below) plus a redeploy or revision restart.
printf '%s' '<your-strong-admin-password>' | \
  gcloud secrets create admin-seed-password --data-file=-

# 4. Create a dedicated runtime service account for the Cloud Run service.
#    (Google has deprecated auto-creation of the default Compute Engine SA;
#     using a dedicated SA is least-privilege and the now-recommended pattern.)
gcloud iam service-accounts create gym-rat-runtime \
  --display-name="gym-rat-management Cloud Run runtime"

# 5. Grant that SA access to all three secrets.
SA="gym-rat-runtime@<PROJECT_ID>.iam.gserviceaccount.com"

for SECRET in jwt-secret card-encryption-key admin-seed-password; do
  gcloud secrets add-iam-policy-binding "$SECRET" \
    --member="serviceAccount:${SA}" \
    --role=roles/secretmanager.secretAccessor
done
```

If step 5 is skipped, the deploy will succeed but the Cloud Run container will fail to start with a `permission denied to access secret` error in the logs. This is the most common bootstrap mistake.

The deploy script wires this SA into the Cloud Run service via `--service-account`. Override the SA email via the `RUNTIME_SA` env var if you need a different one.

### Configure the deploy script

The deploy script reads its config from env vars:

| Env var        | Required | Default              | Purpose                                |
|----------------|----------|----------------------|----------------------------------------|
| `GCP_PROJECT`  | yes      | —                    | GCP project ID                         |
| `GCP_REGION`   | no       | `us-central1`        | Cloud Run + Artifact Registry region   |
| `AR_REPO`      | no       | `gym-rat-management` | Artifact Registry repository name      |
| `SERVICE_NAME` | no       | `gym-rat-management` | Cloud Run service name                 |
| `RUNTIME_SA`   | no       | `gym-rat-runtime@${GCP_PROJECT}.iam.gserviceaccount.com` | Service account the Cloud Run service runs as (must have `secretAccessor` on both secrets) |
| `DEPLOY_SKIP_LOCAL_TESTS` | no | unset           | Set to `1` to skip the local pre-flight (emergency redeploys only) |

```bash
export GCP_PROJECT=<your-project-id>
```

### Deploy

```bash
./scripts/deploy-cloud-run.sh
```

What it does, in order:

1. Verifies prerequisites (`gcloud` auth, `docker` daemon).
2. Runs `scripts/test-docker-image.sh` as the local pre-flight gate.
3. Builds the image and tags it twice for Artifact Registry: `:<git-short-sha>` and `:latest`.
4. Pushes both tags.
5. Deploys to Cloud Run with `--allow-unauthenticated`, `min-instances=0`, `--cpu-boost`, the `prod` Spring profile, and the two secrets mounted via `--set-secrets`.
6. Resolves the live service URL.
7. Runs `scripts/test-cloud-run-deployment.sh` against that URL for the live smoke.
8. Prints the URL on success.

The whole thing exits non-zero on the first failure, so you can chain it into a deployment pipeline.

### Verify an existing deployment

To re-smoke a deployment without redeploying (e.g., after rotating a secret or restarting a revision):

```bash
./scripts/test-cloud-run-deployment.sh https://your-service-xyz-uc.a.run.app
```

This runs the same endpoint-contract checks the deploy script runs at the end. Useful for verifying that an out-of-band change didn't break the live URL.

### Rotating secrets

Cloud Run resolves the `:latest` alias on each revision start, so rotating a secret is a two-step:

```bash
# 1. Publish a new version of the secret.
printf '%s' "$(openssl rand -base64 48)" | \
  gcloud secrets versions add jwt-secret --data-file=-

# 2. Trigger a new revision so Cloud Run re-reads the secret.
./scripts/deploy-cloud-run.sh
```

Existing instances keep using the old value until they recycle.

### Cloud Run troubleshooting

**Deploy succeeds but the service is unhealthy / 503s** — the most common cause is the missing IAM binding from bootstrap step 4. Check the Cloud Run logs (`gcloud run services logs read gym-rat-management --region=us-central1 --limit=50`) for `permission denied to access secret`.

**`docker push` fails with `denied: Permission`** — `gcloud auth configure-docker us-central1-docker.pkg.dev` wasn't run, or your account doesn't have `roles/artifactregistry.writer` on the project. The deploy script runs `configure-docker` for you; the role grant is a one-time IAM step.

**Cold-start request times out** — Spring Boot needs ~5–10s with `--cpu-boost`. The smoke script tolerates up to 60s on the first hit. If a real request times out, raise `--timeout` in `deploy-cloud-run.sh` (default Cloud Run timeout is 300s).

**Stale image deployed** — the script tags every push with the git short SHA and deploys that specific tag, so this shouldn't happen. If you see it, check `gcloud run revisions list --service=gym-rat-management --region=us-central1` and confirm the active revision is using the SHA tag, not `:latest`.

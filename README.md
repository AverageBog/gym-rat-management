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

The container requires two secrets at runtime. It will fail fast on startup if either is missing.

| Env var               | Purpose                              | How to generate                  |
|-----------------------|--------------------------------------|----------------------------------|
| `JWT_SECRET`          | HMAC signing key for JWTs            | `openssl rand -base64 48`        |
| `CARD_ENCRYPTION_KEY` | AES-256 key (base64-encoded 32 bytes) for stored card numbers | `openssl rand -base64 32` |

```bash
docker run --rm -p 8080:8080 \
  -e JWT_SECRET="$(openssl rand -base64 48)" \
  -e CARD_ENCRYPTION_KEY="$(openssl rand -base64 32)" \
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

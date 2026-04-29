# Implementation Plan — Single Docker Image

Package the Spring Boot backend and the React/Vite frontend into one Docker image that runs as a single container exposing port 8080. The Spring Boot JAR will serve the compiled React assets as static resources, so there is no separate web server and no `docker compose` orchestration required. The image is intended for prod deployment.

## Decisions (locked in)

- **Single container** for the MVP. No `docker compose`, no separate frontend container.
- **Image name/tag**: `gym-rat-management:latest`.
- **Spring profile at runtime**: `prod`, backed by a new `application-prod.properties`. Disables H2 console and SQL logging.
- **Database**: H2 in-memory (MVP scope). Data wipes on container restart — accepted limitation.
- **Runtime base image**: `eclipse-temurin:17-jre-jammy` (glibc, maximum library compatibility; ~80 MB larger than Alpine but no musl surprise risk).

## Goals & Non-Goals

**Goals**
- One image, one container, one exposed port (`8080`).
- Multi-stage build so the final image contains only a JRE plus the fat JAR (no Node, no Gradle, no source).
- App is runnable with `docker run -p 8080:8080 -e JWT_SECRET=... -e CARD_ENCRYPTION_KEY=... gym-rat-management:latest`.
- Container runs as a non-root user.
- H2 console disabled in the prod profile.

**Non-goals (for this iteration)**
- Persistent database (H2 stays in-memory; data resets on container restart).
- HTTPS / TLS termination (handled by an upstream proxy in any real deployment).
- Production secret management (env vars are sufficient; no Vault/SOPS integration).
- Kubernetes manifests, registry pushes, CI integration.
- A `compose.yaml` (revisit when we add a real DB).

## Architecture: Single-process, Spring-served Frontend

The cleanest "single image" pattern for this stack is to bake the Vite build output into the Spring Boot JAR and let Spring serve it from the classpath. This avoids a second process (no nginx, no supervisord) and removes the dev-only Vite proxy from the runtime path.

Concretely:
1. `npm run build` produces `frontend/dist/`.
2. The Docker build copies `frontend/dist/` into `backend/src/main/resources/static/` before `./gradlew bootJar`.
3. Spring Boot auto-serves files under `static/` at `/`, so `GET /` returns `index.html` and `GET /api/...` still hits the controllers.
4. Client-side React Router routes (e.g. `/members/3`) need a forwarding rule so a deep-link refresh doesn't 404. This is handled by a small `WebMvcConfigurer` (or controller) that forwards non-`/api`, non-static requests to `index.html`.

## Implementation Steps

### 1. Backend: serve the SPA
- Add a `WebMvcConfigurer` (e.g. `backend/src/main/java/com/gymrat/config/SpaForwardingConfig.java`) that forwards any unmatched, non-`/api`, non-`/h2-console`, non-static request to `forward:/index.html`.
- Verify `SecurityConfig` permits `GET /`, `GET /index.html`, `GET /assets/**`, and any other static files Vite emits (e.g. `/vite.svg`, `/*.ico`) anonymously. Currently only `/api/auth/login` and `/h2-console/**` are public, so static asset paths must be added or `permitAll()` must be granted to `GET` on the SPA paths.

### 2. Backend: `prod` Spring profile
- Add `backend/src/main/resources/application-prod.properties`:
  - `spring.h2.console.enabled=false`
  - `spring.jpa.show-sql=false`
  - `spring.jpa.properties.hibernate.format_sql=false`
- The `prod` profile inherits everything else from `application.properties` (datasource URL, JPA settings, JWT secret resolution from env).
- Confirm `application.properties` already reads `JWT_SECRET` and `CARD_ENCRYPTION_KEY` from env without local-file fallback when `prod` is active. (Today both are `${JWT_SECRET}` / `${CARD_ENCRYPTION_KEY}` — fine; the dev convenience values live only in `application-local.properties`, which is gitignored and not packaged into the image.)

### 3. Frontend: production-friendly API base
- Confirm the frontend's Axios `client.js` already uses a relative `/api` base (it does in the current code via the Vite dev proxy). Same-origin in the container means no change is required and no `VITE_API_BASE_URL` is needed.
- The Vite proxy block in `vite.config.js` is dev-only and is irrelevant to the image.

### 4. Dockerfile (multi-stage)
Create `Dockerfile` at the repo root with three stages:

- **Stage `frontend-build`** — `node:20-alpine`
  - `WORKDIR /frontend`, copy `frontend/package*.json`, run `npm ci`
  - Copy the rest of `frontend/` and run `npm run build`
  - Output: `/frontend/dist`
- **Stage `backend-build`** — `eclipse-temurin:17-jdk-jammy` (use the wrapper) or `gradle:8.5-jdk17`
  - `WORKDIR /backend`, copy `backend/` (including `gradlew`, `gradle/`, `build.gradle`, `settings.gradle`, `src/`)
  - Copy the frontend artifacts: `COPY --from=frontend-build /frontend/dist /backend/src/main/resources/static`
  - Run `./gradlew --no-daemon clean bootJar` (produces `build/libs/*.jar`)
- **Stage `runtime`** — `eclipse-temurin:17-jre-jammy`
  - Create non-root user: `useradd --system --uid 10001 --no-create-home spring` (Ubuntu/Debian syntax — note this differs from the Alpine `adduser` form).
  - `COPY --from=backend-build /backend/build/libs/*.jar /app/app.jar`
  - `ENV SPRING_PROFILES_ACTIVE=prod`
  - `EXPOSE 8080`
  - `USER spring`
  - `ENTRYPOINT ["java","-jar","/app/app.jar"]`

Notes:
- Add a `.dockerignore` at the repo root to keep `node_modules/`, `frontend/dist/`, `backend/build/`, `backend/.gradle/`, `.git/`, and `*.log` out of the build context. This is essential — without it the build will be slow and may bake stale artifacts.
- Health check (optional, recommended): `HEALTHCHECK CMD wget -qO- http://localhost:8080/api/health || exit 1` once a `/api/health` endpoint exists. If we don't add a health endpoint, omit the HEALTHCHECK rather than relying on a noisy 401.

### 5. Runtime configuration
The image must NOT bake secrets. Required env vars at `docker run`:
- `JWT_SECRET` — required, no default.
- `CARD_ENCRYPTION_KEY` — required, base64-encoded 32 bytes.
- `SPRING_PROFILES_ACTIVE` — defaulted to `prod` in the Dockerfile; can be overridden at `docker run` time if needed.

The container will fail fast at startup if `JWT_SECRET` or `CARD_ENCRYPTION_KEY` are missing — that's the desired behavior.

### 6. CLAUDE.md
Updated in the same change as this plan:
- `### Docker` section now documents `docker build` + `docker run` (no compose).
- `## Docker Image Creation` section now pins the image name, the `prod` profile, the Jammy runtime, the env-var contract, the non-root requirement, and the `.dockerignore` requirement.

## Test Plan

Tests are layered cheap-to-expensive. Each later step assumes the previous passed.

### A. Pre-image sanity checks (no Docker)
1. `cd backend && ./gradlew test` — existing test suite still green.
2. `cd frontend && npm run build` — Vite build succeeds and emits `dist/`.
3. Manually copy `frontend/dist/*` into `backend/src/main/resources/static/`, run `./gradlew bootRun -Dspring-boot.run.profiles=prod` (with `JWT_SECRET` and `CARD_ENCRYPTION_KEY` set in the shell env), and verify in a browser:
   - `http://localhost:8080/` loads the React app.
   - `http://localhost:8080/members` (or any client route) reloads to the same app, not a 404.
   - `http://localhost:8080/h2-console` returns 404 / disabled (proves the `prod` profile is active).
   - `POST /api/auth/login` with seed creds returns a JWT.
   - Authenticated `GET /api/members` works.

   Then revert the `static/` copy so the source tree is clean.

### B. Image build
4. `docker build -t gym-rat-management:latest .` from repo root — completes without warnings.
5. `docker images gym-rat-management` — image present; size sanity check (target < 350 MB on Jammy; flag if > 600 MB).
6. `docker history gym-rat-management:latest` — confirm no layer contains source, `node_modules/`, or Gradle caches.

### C. Container runtime
7. Run with required env vars:
   ```
   docker run --rm -p 8080:8080 \
     -e JWT_SECRET=$(openssl rand -base64 48) \
     -e CARD_ENCRYPTION_KEY=$(openssl rand -base64 32) \
     gym-rat-management:latest
   ```
   Container starts; logs show `Started GymRatApplication` and `The following 1 profile is active: "prod"`.
8. `curl -i http://localhost:8080/` returns `200` with `text/html`.
9. `curl -i http://localhost:8080/members` returns `200` with `text/html` (SPA forwarding works).
10. `curl -i http://localhost:8080/h2-console` returns 404 (H2 console disabled under `prod`).
11. `curl -i http://localhost:8080/api/members` returns `401` (auth required, route is wired).
12. Login flow:
    ```
    TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
      -H 'Content-Type: application/json' \
      -d '{"email":"admin@gym.com","password":"Admin123!"}' | jq -r .token)
    curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/members | jq length
    ```
    Returns the seeded member count.
13. Browser smoke: open `http://localhost:8080`, log in as `admin@gym.com`, navigate members/attendance/merch pages — all work end-to-end.
14. `docker exec <id> id` — process is running as the non-root `spring` user (uid 10001).
15. Failure mode: `docker run --rm gym-rat-management:latest` (no env vars) exits non-zero with a clear "JWT_SECRET" / placeholder-resolution error.

### D. Cleanup
16. `docker rmi gym-rat-management:latest` and confirm no dangling intermediate images remain (`docker image prune -f` if needed).

## Risks

1. **No persistence** — every container restart wipes the H2 database. Acceptable for MVP, but flag clearly if a stakeholder expects data to survive.
2. **No HTTPS** — the container speaks plain HTTP on 8080. Any real deployment must terminate TLS upstream (load balancer, reverse proxy).
3. **Asset paths in `SecurityConfig`** — need to enumerate which static paths must be `permitAll()`. Will be finalized during step 1 once we see the exact files Vite emits (typically `/`, `/index.html`, `/assets/**`, `/*.svg`, `/*.ico`).
4. **Disabling H2 console under `prod`** — needs a verification test (step C.10) because it's easy to leave `permitAll()` on `/h2-console/**` in `SecurityConfig` even after the console itself is disabled. Disabling the auto-config is what actually closes the route; the `permitAll` becomes a no-op. Confirm both paths.
5. **Build context size** — without a correct `.dockerignore`, the build will copy `frontend/node_modules` (hundreds of MB) into the build context every time. Verify the `.dockerignore` works by checking `docker build` output for "Sending build context to Docker daemon" size.

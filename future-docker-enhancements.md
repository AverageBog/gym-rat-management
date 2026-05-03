# I=Future Enhancment Plan — Single Docker Image

## Decisions (locked in)

- **Single container** for the MVP. No `docker compose`, no separate frontend container.
- **Image name/tag**: `gym-rat-management:latest`.
- **Spring profile at runtime**: `prod`, backed by a new `application-prod.properties`. Disables H2 console and SQL logging.
- **Database**: H2 in-memory (MVP scope). Data wipes on container restart — accepted limitation.
- **Runtime base image**: `eclipse-temurin:17-jre-jammy` (glibc, maximum library compatibility; ~80 MB larger than Alpine but no musl surprise risk).

## Goals

**Goals**
- Persistent database (H2 stays in-memory; data resets on container restart).
- HTTPS / TLS termination (handled by an upstream proxy in any real deployment).
- Production secret management (env vars are sufficient; no Vault/SOPS integration).
- Kubernetes manifests, registry pushes, CI integration.
- A `compose.yaml` (revisit when we add a real DB).

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

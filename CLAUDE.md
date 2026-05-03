# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Backend (from `backend/`)
```bash
./gradlew bootRun          # start the Spring Boot server on :8080
./gradlew test             # run all tests
./gradlew test --tests "com.gymrat.controller.MemberControllerTest" # run a single test class
./gradlew build            # compile and package
```

### Frontend (from `frontend/`)
```bash
npm install                # install dependencies
npm run dev                # start Vite dev server on :5173
npm run build              # production build
npm run lint               # ESLint
```

### Docker (single-image, prod deployment)
```bash
docker build -t gym-rat-management:latest .                    # from repo root
docker run --rm -p 8080:8080 \
  -e JWT_SECRET=<secret> \
  -e CARD_ENCRYPTION_KEY=<base64-32-bytes> \
  gym-rat-management:latest
```
The image bundles the Vite-built frontend into the Spring Boot JAR and serves both the SPA (`/`) and the API (`/api/*`) from port 8080. `JWT_SECRET` and `CARD_ENCRYPTION_KEY` are required at runtime; the container fails fast if they are missing.

## Architecture

This is a gym CRM with a Spring Boot REST API backend and a React SPA frontend.

**Backend** — `backend/src/main/java/com/gymrat/`
- `entity/` — JPA entities: `AppUser`, `Member`, `MembershipPlan`, `Attendance`, `MemberNote`, `MerchandiseItem`, `MemberStatus` (enum), `UserRole` (enum)
- `repository/` — Spring Data JPA repositories (one per entity)
- `service/` — business logic layer called by controllers
- `controller/` — REST controllers under `/api/*`
- `dto/` — request/response shapes; admins and members get different DTOs for the same resource
- `security/` — JWT filter (`JwtAuthFilter`), token util (`JwtUtil`), and `AuthenticatedUser` record (the security principal)
- `config/` — `SecurityConfig` (Spring Security), `DataInitializer` (seeds data on startup)

**Frontend** — `frontend/src/`
- `api/` — one file per domain (`membersApi.js`, `authApi.js`, etc.) all using a shared Axios `client.js` instance
- `context/AuthContext.jsx` — JWT + session TTL (3 h) stored in `localStorage`
- `pages/` — one file per route
- `components/` — `auth/ProtectedRoute`, `layout/Navbar`, and small `common/` utilities

## Key Design Decisions

**Dual user model**: `AppUser` (authentication credentials + role) is a separate entity from `Member` (gym profile data), linked by an optional FK. Admins have an `AppUser` with no linked `Member`; gym members have both.

**JWT principal**: `JwtAuthFilter` parses the token and puts an `AuthenticatedUser(email, role, memberId)` record into the `SecurityContext`. Controllers cast `auth.getPrincipal()` to `AuthenticatedUser` to read `role` and `memberId`.

**Role-based access**: Most endpoints use `@PreAuthorize("hasRole('ADMIN')")`. The `/api/members/{id}` endpoint handles both roles inline — admins get the full `AdminMemberDto`, members can only access their own ID and get the narrower `MemberProfileDto`.

**Database**: H2 in-memory (`gymratdb`), schema recreated on every restart (`ddl-auto=create-drop`). Seed data comes from two sources: `data.sql` (membership plans, members, attendance, notes, merchandise) and `DataInitializer.java` (AppUser accounts). H2 console available at `http://localhost:8080/h2-console`.

**API proxy**: Vite proxies `/api` → `http://localhost:8080`, so the frontend always calls `/api/...` regardless of environment.

## Seed Credentials
The admin password is env-driven so the prod password isn't in git. The dev/local default below is checked in; prod requires `ADMIN_SEED_PASSWORD` (mounted from Secret Manager).

| Role   | Email                  | Password (dev/local)   |
|--------|------------------------|------------------------|
| Admin  | admin@gym.com          | `LocalDevAdmin#2026!` (overridable via `ADMIN_SEED_PASSWORD`) |
| Member | alex@example.com       | `Member123!`           |
| Member | jordan@example.com     | `Member123!`           |
| Member | sam@example.com        | `Member123!`           |

Member passwords are demo accounts with limited (own-profile) access and remain hardcoded.

## Docker Image Creation
The project ships as a single image, `gym-rat-management:latest`, used for prod deployment. There is intentionally no `docker compose` setup at this stage — H2 stays in-memory and there are no other services.

- **Image name**: always `gym-rat-management` (lowercase). Tag `:latest` for local builds.
- **Single image**: Spring Boot serves the React SPA from its classpath (`backend/src/main/resources/static/`), so one container exposes one port (`8080`). Do not split into separate frontend/backend containers.
- **Multi-stage Dockerfile** (at repo root) with three stages:
  1. `node:20-alpine` — runs `npm ci` and `npm run build` in `frontend/`.
  2. `eclipse-temurin:17-jdk-jammy` (or `gradle:8.5-jdk17`) — copies the Vite output into `backend/src/main/resources/static/` and runs `./gradlew --no-daemon clean bootJar`.
  3. `eclipse-temurin:17-jre-jammy` — runtime stage. Only the fat JAR is copied here; no JDK, no Node, no source. The "slim" requirement applies to this stage only; build stages can use full JDK/Node.
- **Spring profile**: the runtime defaults to `SPRING_PROFILES_ACTIVE=prod`, backed by `backend/src/main/resources/application-prod.properties`. The `prod` profile disables the H2 console and SQL logging. Do not enable `/h2-console` in the image.
- **Required runtime env vars** (no defaults, must be passed at `docker run`):
  - `JWT_SECRET` — HMAC signing secret.
  - `CARD_ENCRYPTION_KEY` — base64-encoded 32 bytes (AES-256).
  - `ADMIN_SEED_PASSWORD` — password used to seed the `admin@gym.com` AppUser on cold start. Required only when running with `SPRING_PROFILES_ACTIVE=prod`; the default profile has a checked-in dev value.
- **Non-root**: the runtime stage creates a `spring` user (uid 10001) and the Dockerfile ends with `USER spring` before `ENTRYPOINT`. Never run as root in the runtime stage.
- **`.dockerignore`**: keep `node_modules/`, `frontend/dist/`, `backend/build/`, `backend/.gradle/`, `.git/`, and IDE files out of the build context.
- **Verification**: after building, run `docker images gym-rat-management` to confirm the image exists, and `docker history gym-rat-management:latest` to confirm no source / `node_modules` / Gradle caches were carried into the runtime layer.

## Cloud Run Deployment
The same image deploys to Google Cloud Run as a public, scale-to-zero service. Bootstrap walk-through and full troubleshooting live in `README.md` ("Deploy to Google Cloud Run"); this section pins the contract.

- **Service name**: `gym-rat-management` (Cloud Run service); also the image name within Artifact Registry.
- **Region**: `us-central1` by default; overridable via `GCP_REGION` env var on the deploy script.
- **Artifact Registry repo**: `gym-rat-management` (Docker format) at `${GCP_REGION}-docker.pkg.dev/${GCP_PROJECT}/gym-rat-management`.
- **Image tagging**: every push gets two tags — `:<git-short-sha>` (deployed) and `:latest` (for human inspection). Cloud Run is always told to deploy the SHA tag, never `:latest`, to avoid stale-cache surprises.
- **Spring profile**: `SPRING_PROFILES_ACTIVE=prod` is set via `--set-env-vars` on the deploy command (same profile used locally in the image).
- **Secrets**: `JWT_SECRET`, `CARD_ENCRYPTION_KEY`, and `ADMIN_SEED_PASSWORD` are stored in Google Secret Manager as `jwt-secret`, `card-encryption-key`, and `admin-seed-password`. The Cloud Run service mounts them via `--set-secrets ...:latest`. Never pass these as `--set-env-vars`.
- **Runtime service account**: a dedicated SA `gym-rat-runtime@${GCP_PROJECT}.iam.gserviceaccount.com` runs the Cloud Run service. The deploy script passes it via `--service-account` (overridable with `RUNTIME_SA`). Do not fall back to the default compute SA — Google has deprecated its auto-creation, and least-privilege is the established pattern here.
- **Required IAM**: `gym-rat-runtime@${GCP_PROJECT}.iam.gserviceaccount.com` must have `roles/secretmanager.secretAccessor` on `jwt-secret`, `card-encryption-key`, and `admin-seed-password`. Missing this binding is the most common deploy-time failure (deploy succeeds, container fails to start with "permission denied to access secret").
- **Auth model**: deployed with `--allow-unauthenticated`. The application enforces auth via JWT — Cloud Run is not the auth boundary. Do not toggle this unless wrapping the service behind something else.
- **Scaling**: `--min-instances=0`, `--max-instances=3`, `--cpu-boost` for faster cold starts. Cold starts are ~5–10s; the in-memory H2 database resets on every cold start (known MVP limitation, amplified by Cloud Run).
- **Port contract**: the application reads `server.port=${PORT:8080}` so it honors Cloud Run's `$PORT` env var. Do not hardcode `8080` in future config — it would break Cloud Run if Google ever changes the default.
- **Deploy command**: `GCP_PROJECT=<id> ./scripts/deploy-cloud-run.sh`. Does build → push → deploy → live smoke. Exits non-zero on first failure.
- **Smoke an existing deployment**: `./scripts/test-cloud-run-deployment.sh <URL>` — works against any live URL, no Docker daemon needed.

## Testing Pattern
- **Always run tests:** Before submitting any PR or completing a task, run relevant test cases
- **Coverage Goal:** Maintain close to 80% branch coverage only within places that it makes sense. All test cases should be purposeful
- **Test Structure:** Every new feature requires a dedicated test file with edge-case tests.
- **Workflow:** Implement -> Run Tests -> Fix -> Confirm.
- Any specific testing instructions can be found in reference
@.claude/rules/testing.md
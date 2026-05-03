# Testing Rules

## Backend (Spring Boot)

### Test Slices
- Use `@WebMvcTest` for controller tests — do not spin up the full application context.
- Always `@MockBean JwtUtil` in any `@WebMvcTest` that loads `JwtAuthFilter`; omitting it will cause context load failures.
- Use `@DataJpaTest` for repository tests; they run against an in-memory H2 database automatically.
- Use plain `@ExtendWith(MockitoExtension.class)` for service-layer unit tests with no Spring context.

### Authentication in Tests
- Inject a pre-built `UsernamePasswordAuthenticationToken` via `SecurityMockMvcRequestPostProcessors.authentication(...)` — never perform a real HTTP login or parse a real JWT in tests.
- Build the principal as `new AuthenticatedUser(email, role, memberId)` to match what `JwtAuthFilter` puts into the `SecurityContext`.
- For admin-only endpoints, use a token with `UserRole.ADMIN`; for member endpoints, use `UserRole.MEMBER` with the correct `memberId`.

### Assertions & Structure
- Follow Arrange / Act / Assert; one logical assertion group per test method.
- Assert HTTP status, response body fields, and (where relevant) that the correct service method was called with the expected arguments.
- Do not assert on fields the endpoint does not return — test the contract, not internal state.
- Use `MockMvc.perform(...)` with `jsonPath` for response validation.
- Name tests `methodName_stateUnderTest_expectedBehavior` (e.g., `getMember_asAdmin_returnsFullDto`).

### Coverage Targets
- Every controller method needs at least: happy-path (200/201), unauthorized role (403), and not-found (404) cases.
- Service methods with branching logic (role checks, null guards) need a test per branch.
- Do not write tests for getters/setters or trivial DTOs.

### Cross-cutting Configuration
- `@ControllerAdvice` exception handlers need a unit test per branch of their handle logic. Direct invocation with `MockHttpServletRequest` and a stubbed exception is preferred — fast and focused. The `SpaFallbackAdvice` test is the reference: one test per `if` branch (`/api/`, `/h2-console`, has-extension, fall-through forward).
- Changes to `SecurityConfig` request-matcher rules require an integration test in the `@WebMvcTest` slice that explicitly `@Import`s both `SecurityConfig` and `JwtAuthFilter`. Without those imports, `@WebMvcTest` falls back to Spring Boot's default chain (auth required for everything), which silently masks matcher bugs. The test must cover both newly-protected and newly-permitted paths.
- For SPA forwarding behavior, assert `forwardedUrl("/index.html")` rather than HTTP status — MockMvc records the forward but does not re-dispatch, so status will be `200` regardless of whether the forward target exists. `forwardedUrl` proves both that security let the request through and that the advice forwarded.

### New `application-{profile}.properties`
- Any new Spring profile that changes runtime behavior (e.g. disables H2 console, changes logging) must have either: an integration test that boots with that profile active and asserts the changed behavior, OR an assertion in `scripts/test-docker-image.sh` if the profile is only meaningful in the deployed image. Prefer the script when the profile exists solely for deployment.

---

## Frontend (React + Vite)

### Test Framework
- Use Vitest as the test runner and `@testing-library/react` for component tests.
- Use `msw` (Mock Service Worker) to intercept Axios calls — do not mock the `axios` module directly.

### Component Tests
- Test behavior, not implementation: interact via roles and labels (`getByRole`, `getByLabelText`), not class names or internal state.
- Each component test should cover: initial render, user interaction (if any), and error/loading states.
- Wrap components that consume `AuthContext` with a test `AuthProvider` fixture that injects a known user.

### API Layer (`api/`)
- Unit-test each `api/` function against an `msw` handler that returns fixture data.
- Assert the correct URL, method, and request body are sent.
- Assert the returned value matches the expected shape — these tests guard against silent contract drift with the backend.

### Auth & Routing
- Test `ProtectedRoute` with both an authenticated user and an unauthenticated user; assert redirect behavior.
- Do not test the JWT parsing logic in components — that belongs in a dedicated `authUtils` unit test.

### General
- Co-locate test files next to the source file (`Component.test.jsx` beside `Component.jsx`).
- Keep fixtures in a `__fixtures__/` directory at the same level as the tests that use them.
- Do not snapshot-test components that change frequently; prefer explicit `getBy*` assertions.

---

## Docker Image (pre-deployment)

- Any change to `Dockerfile`, `.dockerignore`, the runtime Spring profile (`application-prod.properties`), or anything else that affects how the image is built or starts must be smoke-tested via `scripts/test-docker-image.sh` before merging.
- The script must build the image, run the container with required env vars, verify the runtime contract (non-root user, `prod` profile active, SPA + API on one port, login flow), and exit non-zero on any failure. It cleans up its container via a trap.
- Do not move these checks into the JUnit / Vitest suites — they require a live Docker daemon and would slow every test run. They belong in the standalone script so they can be invoked on demand or from a deployment pipeline.
- Add new assertions to `scripts/test-docker-image.sh` for any new runtime contract (new required env var, new exposed port, new endpoint that must respond from inside the container). Keep each assertion as one `check_status` / `check_content_type` line — the script is meant to read top-to-bottom as a deployment checklist.
- When the script fails, dump the container logs (last ~50 lines) before exiting so the failure is diagnosable in CI without re-running.
- Any change to `scripts/deploy-cloud-run.sh`, the Cloud Run deployment configuration (region, secrets, IAM, scaling flags), or the live runtime contract must be smoke-tested via `scripts/test-cloud-run-deployment.sh <URL>` against a non-production revision. The deploy script invokes this automatically; run it standalone if you're verifying an out-of-band deployment.

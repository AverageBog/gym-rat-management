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

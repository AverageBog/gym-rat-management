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
| Role   | Email                  | Password    |
|--------|------------------------|-------------|
| Admin  | admin@gym.com          | Admin123!   |
| Member | alex@example.com       | Member123!  |
| Member | jordan@example.com     | Member123!  |
| Member | sam@example.com        | Member123!  |

## Testing Pattern

@.claude/rules/testing.md
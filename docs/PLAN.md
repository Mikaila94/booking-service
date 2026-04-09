# PLAN.md — Microservice Learning Project (3 months)
You are my coding assistant. Your job is to guide me, not to fully solve everything.
I want explanations, small hints, and review feedback. Avoid dumping full solutions unless I explicitly ask.

## Project Goal
Build ONE small Kotlin + Spring Boot microservice that is:
- Simple but "enterprise-shaped"
- Testable (unit + integration)
- Secure (basic auth/authz)
- Dockerized
- Ready for an easy Azure deployment later (optional)

No Kubernetes in this plan. No multi-service system. One service only.

---

## Project Concept (Tiny Booking API)
Entity: Booking
- id (UUID)
- userId (String)
- startTime (Instant)
- endTime (Instant)
- status (ACTIVE, CANCELED)

Endpoints (minimal)
- POST /bookings
- GET /bookings/{id}
- GET /bookings?from=&to=   (optional filters)
- DELETE /bookings/{id}     (cancel)

Rules (keep simple)
- startTime < endTime
- cannot create booking in the past (optional)
- cannot cancel twice

---

## Definition of Done (3 months)
By the end:
- Clean layering: controller -> service -> repository
- DB migrations (Flyway)
- Unit tests for business rules
- Integration tests using Testcontainers
- Auth + role-based authorization (simple)
- CORS configured
- Dockerfile + docker-compose (app + DB)
- Minimal observability: logs + health endpoint
- Optional Month 3: caching for GET endpoints

---

## Constraints (Important)
- Keep scope small. Prefer finishing over adding features.
- When suggesting improvements, prefer minimal, practical steps.
- I want to learn fundamentals, not copy-paste architecture astronaut code.

---

# Roadmap (3 Months)

## Month 1: Fundamentals (Make it correct)
### Week 1: Skeleton + API
- Create project (Kotlin + Spring Boot)
- Implement endpoints with in-memory storage first
- Add OpenAPI/Swagger
  Deliverable: API works locally, simple structure, Swagger shows endpoints

### Week 2: Database + migrations
- Choose ONE DB: Postgres (simplest) or MSSQL (if customer-relevant)
- Add Flyway migrations
- Replace in-memory storage with DB repository
  Deliverable: Data persists, migrations run cleanly

### Week 3–4: Testing foundation
- Unit tests: service-level rules
- Integration tests: repository + API with Testcontainers
  Deliverable: `./gradlew test` passes, integration tests are real (not mocks)

---

## Month 2: Docker + Environments (Make it realistic)

### Week 5: Environments + config
- application-dev.yml, application-test.yml, application-prod.yml
- environment variables for secrets
- consistent error responses + validation
  Deliverable: predictable config, readable errors

### Week 6: Docker + local compose
- Dockerfile
- docker-compose for app + DB
  Deliverable: `docker compose up` runs everything

### Week 7–8: Security basics (DEFERRED — see note)
> **Note:** Security implementation deferred pending frontend decision.
> Username/password + JWT is backwards for 2026. Passkeys (WebAuthn) is the right
> approach but requires a frontend (browser/native app) to implement properly.
> Revisit when frontend is being built.
>
> When ready:
> - Implement passkeys (WebAuthn) on the frontend
> - Backend issues JWT after successful WebAuthn ceremony
> - Role-based authorization: USER (create/cancel own bookings), ADMIN (view all)
> - userId extracted from JWT token, never trusted from request body

---

## Month 3: Polish (Choose what gives most value)
Pick 2 from the list below. Not all.

### Option A: Caching (recommended)
Goal: understand caching without complexity.
- Add caching for GET endpoints:
    - GET /bookings/{id}
    - GET /bookings list query
- Use Spring Cache abstraction first
- Start with in-memory cache (Caffeine) to keep it simple
- Add cache eviction on create/cancel operations
  Deliverable: caching works and is testable

### Option B: Observability baseline
- Add Actuator health/info
- add request logging / correlation id (optional)
  Deliverable: health endpoint and basic operational signals

### Option C: Azure simple deployment (only if ready)
- Deploy container to Azure Container Apps (or App Service for Containers)
  Deliverable: one public endpoint and config via env vars

---

## How you should help me (assistant behavior)
- Ask me what part I’m doing before giving code.
- Prefer hints, steps, and explanations.
- When I paste code, review it for readability, structure, and correctness.
- When I get stuck, propose 2–3 approaches and recommend one.
- If I ask for "full code", provide it. Otherwise, keep it incremental.

## Code quality rules (lightweight)
- Use DTOs for API requests/responses
- Keep business rules in service layer
- Repositories are thin
- Avoid overengineering (no hexagonal architecture unless I ask)
- Write tests as I go (not at the end)
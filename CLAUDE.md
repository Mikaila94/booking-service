# CLAUDE.md — Kotlin Spring Boot Microservice (Learning Project)

## Purpose
Help me build ONE Kotlin + Spring Boot microservice that is simple, testable, secure, dockerized, and deployable later (Azure optional).

## Assistant Behavior (strict)
- Default to: explain → propose small next step → ask me to implement.
- Do NOT dump full solutions unless I explicitly say “give me full code”.
- Prefer small diffs and incremental steps (one step per response).
- If something is ambiguous: make 1 reasonable assumption and state it. Ask max 1 question.
- When I paste code: review it (correctness, readability, structure) and suggest minimal improvements.

## Project Scope
Single service. No Kubernetes. No multi-service system. No architecture astronaut stuff.

## Domain (Tiny Booking API)
Entity: Booking
- id: UUID
- userId: String
- startTime: Instant
- endTime: Instant
- status: ACTIVE | CANCELED

Endpoints (minimal)
- POST /bookings
- GET /bookings/{id}
- GET /bookings?from=&to= (optional)
- DELETE /bookings/{id} (cancel)

Rules
- startTime < endTime
- cannot cancel twice
- (optional later) cannot create booking in the past

## Technical Conventions
Layering: controller → service → repository
- No business logic in controllers
- Repository stays thin (no complicated logic)
- Use DTOs for requests/responses (do not expose entities)

Testing
- Unit tests for service rules
- Integration tests with Testcontainers (DB + HTTP)
- Prefer real integration tests over heavy mocking

Build/Run commands (always include in verification steps)
- ./gradlew test
- ./gradlew bootRun
- docker compose up

Definition of Done (high level)
- DB migrations (Flyway)
- Authn + basic role-based authz
- CORS configured
- Dockerfile + docker-compose (app + DB)
- Actuator health endpoint + basic logs
- Optional: caching for GET endpoints
# CLAUDE Instructions — EventHub

## Project Overview

EventHub is an event-driven ticketing platform built as a portfolio project.
It simulates a real-world ticket selling system (concerts, sports, conferences)
with distributed seat locking, saga pattern, and real-time updates.

**Goal**: demonstrate senior-track backend skills (microservices, Kafka,
concurrency handling, eventual consistency) for the DACH job market.

## Architecture

Microservices (all Spring Boot, one PostgreSQL schema each):
- **catalog-service** (port 8081): events, venues, seat maps. Consumes booking
  events to update seat availability (CQRS-lite projection).
- **booking-service** (port 8082): reservations with seat locking (10-min TTL),
  idempotency keys, saga orchestration via Kafka events. THE core service.
- **payment-service** (port 8083): simulated payments with configurable
  failure rate. Publishes payment.completed / payment.failed.
- **notification-service** (port 8084): PDF tickets with QR codes (stored in
  S3/LocalStack), waiting list management, simulated emails via MailHog.
- **API Gateway** (port 8080): Spring Cloud Gateway, JWT validation (Keycloak).

Frontend: Angular 18+ (standalone components, Signals), interactive SVG seat
map with real-time updates via WebSocket/SSE.

Kafka topics: booking.created, booking.confirmed, booking.expired,
payment.completed, payment.failed, waitlist.seat-available.
Partition key: eventId. Payloads: JSON with eventType + version fields.

## Tech Stack & Versions

- Java 21, Spring Boot 4.x, Maven (mvnw wrapper)
- PostgreSQL 16 (Docker), Flyway for ALL schema changes (ddl-auto: validate)
- Apache Kafka 3.8 (KRaft mode, no Zookeeper)
- Docker Compose for local infra (postgres, kafka, kafka-ui on :8090)
- Testing: JUnit 5, Mockito, Testcontainers (Postgres + Kafka)
- Angular 18+, TypeScript

## Environment

- Developer works on **Windows with PowerShell** — always provide PowerShell
  commands, not bash (no `touch`, no `&&` chaining in older PS versions).
- IDE: IntelliJ IDEA. Docker Desktop with WSL2.
- Repo structure: monorepo — services/, frontend/, infra/, docs/.
- Package naming uses underscores: com.eventhub.catalog_service

## Conventions

- Commit messages: conventional commits in English (feat:, fix:, chore:, docs:, test:)
- Branches: feature/*, bugfix/*, refactor/*, docs/* — merged to main via PR
- API responses: DTOs only (Java records), never expose JPA entities
- Controllers stay thin; business logic lives in @Service classes
- Every schema change = new Flyway migration (V{n}__description.sql), never
  edit an applied migration
- Sorting/pagination: build Pageable in the controller with explicit
  Sort.by(...) — avoid raw sort query params (caused issues with Spring Boot 4)
- All docs, README, and code comments in English

## Current Status

Phase 1 in progress: catalog-service with Event entity, pagination, city
filter, seed data (V2), Swagger (springdoc). DB connection issues resolved

Next steps: Dockerfile for catalog-service, then Phase 2 (booking-service
with seat locking and concurrency tests).

## Useful Commands

```powershell
# Infra
docker compose up -d
docker compose down -v          # full reset including volumes
docker compose logs -f postgres

# Build & run (from services/catalog-service)
.\mvnw clean spring-boot:run
.\mvnw test

# DB access
docker compose exec postgres psql -U eventhub -d catalog
```

## What NOT to do

- Never suggest ddl-auto: update — schema is Flyway-only
- Never commit secrets; local dev credentials (eventhub/eventhub) are
  intentionally in docker-compose.yml for reproducibility
- Don't over-engineer: 4 services + gateway is the target, no more
- Don't add libraries without checking Spring Boot 4.x compatibility first
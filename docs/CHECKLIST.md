# EventHub — Project Roadmap & Checklist

---

## Phase 0 — Foundations 

- [x] Public GitHub repository `eventHub`
- [x] Repository description and topics (spring-boot, microservices, kafka, angular, docker...)
- [x] MIT license
- [x] `.gitignore` (Java + target/, node_modules/, .env, .idea/)
- [x] Folder structure: `services/`, `frontend/`, `infra/`, `docs/`
- [x] Initial README with Mermaid architecture diagram
- [x] `docker-compose.yml` with PostgreSQL 16 + Kafka 3.8 (KRaft) + Kafka UI on :8090
- [x] Docker Desktop installed and running
- [x] `CLAUDE.md` with project context, stack and conventions

---

## Phase 1 — catalog-service

### Setup and first entity
- [x] Spring Boot 4.1.0 project generated (Web, JPA, Flyway, PostgreSQL, Validation, Lombok, DevTools)
- [x] `application.yml` with datasource, `ddl-auto: validate`, `open-in-view: false`, port 8081
- [x] Migration `V1__create_events_table.sql`
- [x] `Event` entity + `EventStatus` enum
- [x] `EventRepository`, first `GET /events` endpoint
- [x] Working database connection (resolved: stale Docker volume + zombie Java process)

### Complete API
- [x] `EventResponse` DTO (record) — never expose JPA entities
- [x] `EventService` service layer
- [x] Pagination + city filter
- [x] `Pageable` built in the controller with explicit `Sort.by(...)`
- [x] Migration `V2__seed_events.sql` (6 realistic DACH-themed events)
- [x] Swagger / springdoc working on `/swagger-ui.html`
- [x] `GET /events/{id}` + `EventNotFoundException`
- [x] `POST /events` with validated `CreateEventRequest`
- [x] `GlobalExceptionHandler` using `ProblemDetail` (RFC 7807): 404 and 400
- [x] `MethodArgumentTypeMismatchException` handler (malformed UUID returns a clean 400)

### Venue and seat map
- [x] Migration `V3__create_venues_and_seats.sql` (venues, seats, event_seats + FK on events)
- [x] Migration `V4__seed_seats.sql` (50 seats per venue, PREMIUM/STANDARD)
- [x] `Venue`, `Seat`, `EventSeat` entities + `SeatCategory`, `SeatStatus` enums
- [x] `@Version` on `EventSeat` (ready for Phase 2 optimistic locking)
- [x] `@EntityGraph` / `join fetch` to avoid the N+1 problem
- [x] `GET /events/{id}/seats` and `GET /events/{id}/availability`
- [x] `docs/api.http` with all requests

### Containerization
- [x] catalog-service `Dockerfile`
- [x] catalog-service added to `docker-compose.yml` (DB URL points to `postgres:5432`)
- [x] Final check: `docker compose up -d --build` starts everything from scratch and the API responds

### Tests and CI
- [x] Unit tests for `EventService` with Mockito: `getById` throws 404, `search` picks the right repository method, `create` resolves the venue
- [x] Integration test with **Testcontainers** (PostgreSQL): migrations applied, `GET /events` returns 6 events, `/seats` returns 50 seats
- [x] `GET /venues` (minimal endpoint, needed to fetch venueIds for the POST)
- [x] GitHub Actions: build + test workflow on every push
- [x] Build badge in the README
- [ ] Final commit closing the phase

---

## Phase 2 — booking-service

- [ ] Branch `feature/booking-service`
- [ ] New Spring Boot service, port 8082, `booking` database (add to compose)
- [ ] Migrations: `bookings`, `booking_seats`, `seat_locks`, `processed_events` tables
- [ ] `Booking` entity with PENDING / CONFIRMED / EXPIRED / CANCELLED states
- [ ] `POST /bookings`: creates a PENDING reservation and locks seats with a 10-minute TTL
- [ ] `GET /bookings/{id}` and `GET /bookings?userId=...`
- [ ] `DELETE /bookings/{id}` (cancellation releases the seats)
- [ ] **Concurrency handling**: pessimistic locking (`SELECT ... FOR UPDATE`) or optimistic (`@Version`) — pick one and document the trade-off in the README **(key)**
- [ ] **Concurrency test**: two threads book the same seat, only one may win **(key, highest value)**
- [ ] Idempotency: `Idempotency-Key` header + deduplication table **(key)**
- [ ] Scheduler (`@Scheduled`) releasing expired locks
- [ ] Kafka producer: `booking.created`, `booking.confirmed`, `booking.expired`, `booking.cancelled`
- [ ] Partition key = `eventId` (guarantees ordering per event)
- [ ] Consumer in catalog-service: updates `event_seats` status on booking events
- [ ] Idempotent consumers (`processed_events` table)
- [ ] Testcontainers with Kafka
- [ ] PR and merge into `main`

---

## Phase 3 — payment-service and full saga

- [ ] Branch `feature/payment-service`
- [ ] Service on port 8083, `payment` database
- [ ] `POST /payments`: simulated outcome (90% success) with random latency
- [ ] Producer: `payment.completed`, `payment.failed`
- [ ] booking-service consumes payment events, then CONFIRMED or seat rollback
- [ ] **Full choreographed saga** working end to end **(key)**
- [ ] Retry topic (`@RetryableTopic`) + Dead Letter Topic on one consumer **(key)**
- [ ] Saga sequence diagram in `docs/`
- [ ] Integration test covering the whole booking to payment to confirmation flow
- [ ] Update `docs/api.http` with payment requests
- [ ] PR and merge

---

## Phase 4 — API Gateway and authentication

- [ ] Branch `feature/gateway-auth`
- [ ] Spring Cloud Gateway on port 8080
- [ ] Routing to catalog (8081), booking (8082), payment (8083)
- [ ] Keycloak in `docker-compose.yml`, realm `eventhub`, USER / ADMIN roles
- [ ] Realm exported as JSON in `infra/keycloak/` (auto-imported on startup)
- [ ] JWT validation at the gateway
- [ ] User identity propagated to downstream services
- [ ] `POST /events` restricted to ADMIN
- [ ] Rate limiting (optional, with Redis)
- [ ] Keycloak credentials via `.env` + committed `.env.example`
- [ ] PR and merge

---

## Phase 5 — Angular frontend

- [ ] Branch `feature/angular-frontend`
- [ ] Angular 18+ setup in `frontend/`: standalone components, Signals, routing
- [ ] Styling choice: Angular Material or Tailwind
- [ ] Keycloak auth (`angular-auth-oidc-client`), JWT interceptor, refresh token
- [ ] Route guards by role, lazy-loaded modules
- [ ] **Home**: event search with filters and pagination
- [ ] **Event detail**: interactive SVG seat map with clickable seats and status colours **(key)**
- [ ] **Checkout**: 10-minute lock countdown (RxJS timer), simulated payment
- [ ] **Real time**: WebSocket/SSE — seats locked by other users change colour live **(key)**
- [ ] Kafka to WebSocket bridge in the backend (`booking.*` topics filtered by eventId)
- [ ] **My tickets**: booking list
- [ ] **Admin**: event creation (protected route)
- [ ] Centralized error handling (toast/snackbar)
- [ ] Tests on the key components
- [ ] Frontend Dockerfile (multi-stage build + nginx) added to compose
- [ ] **Demo GIF** for the README **(key)**
- [ ] PR and merge

---

## Phase 6 — notification-service

- [ ] Branch `feature/notification-service`
- [ ] Service on port 8084
- [ ] LocalStack in `docker-compose.yml` (S3)
- [ ] `booking.confirmed` consumer generates a PDF ticket with QR code (`zxing` + `openpdf`)
- [ ] PDF upload to S3 (LocalStack)
- [ ] `GET /tickets/{bookingId}/download` with presigned URL
- [ ] Ticket download from the frontend
- [ ] `booking.expired` consumer handles the waiting list, publishing `waitlist.seat-available`
- [ ] MailHog in compose for simulated emails (web UI, very effective in demos)
- [ ] PR and merge

---

## Phase 7 — Observability and polish

- [ ] Branch `feature/observability`
- [ ] Spring Boot Actuator on every service
- [ ] Prometheus + Grafana in compose with a base dashboard (requests/sec, latency, Kafka consumer lag)
- [ ] Structured JSON logging (Logback)
- [ ] Correlation ID propagated across services (Micrometer Tracing) **(key)**
- [ ] JaCoCo + coverage badge
- [ ] GitHub Actions: build Docker images on tags
- [ ] **Final README in English** **(key)**:
  - [ ] Architecture diagram
  - [ ] GIF/screenshots of the live seat map
  - [ ] `docker compose up` instructions (must work first try)
  - [ ] "Architectural decisions & trade-offs" section (why a choreographed saga, why that locking strategy, why event-driven)
  - [ ] "What I'd do differently in production" section
- [ ] Clean-machine test: `git clone` + `docker compose up` and everything works
- [ ] No secrets in the repo (`.env` gitignored, `.env.example` committed)
- [ ] PR and merge

---

## Phase 8 — AWS serverless on the real Free Tier

- [ ] **CloudWatch billing alarm at EUR 1** — the very first action, before any deployment
- [ ] Separate repository `eventhub-analytics`
- [ ] SAM template (`template.yaml`) — consistent with the DVA-C02 certification
- [ ] `POST /stats/booking`: HTTP API Gateway to Lambda to DynamoDB (PK `eventId`, SK `timestamp`)
- [ ] `GET /stats/events/{id}`: GSI query
- [ ] Scheduled Lambda via EventBridge (daily) writing a JSON report to S3
- [ ] Real deployment in `eu-central-1` (Frankfurt)
- [ ] Working URL linked from both repositories' READMEs **(key)**
- [ ] Cost section in the README: "runs entirely within AWS Free Tier"
- [ ] (Optional) notification-service sends statistics to the deployed API
- [ ] Verify: no NAT Gateway, RDS, EC2 or ALB. Run `sam delete` when not in use

---

## Phase 9 — Future work

- [ ] Kubernetes manifests in `infra/k8s/` (Deployment, Service, ConfigMap, Ingress) tested on kind or Docker Desktop — a bridge towards the CKAD
- [ ] Outbox pattern with a poller (consistency between DB and Kafka)
- [ ] Spring AI service: conversational assistant over events and bookings (RAG on catalog data)
- [ ] Load testing with k6 and documented results
- [ ] Avro + Schema Registry replacing JSON on the topics

---

## Final "strong impression" checklist

- [ ] `docker compose up` works first try on a clean machine
- [ ] README in English with diagrams and a demo GIF
- [ ] Commit messages in English, conventional commits
- [ ] Concurrency test on seat locking (the best story to tell in an interview) **(key)**
- [ ] Testcontainers + GitHub Actions + green badges
- [ ] "Architectural decisions & trade-offs" section
- [ ] Working real AWS URL
- [ ] Zero secrets in the repository

---

# Architecture & Conventions

Canonical reference for developers and AI agents. Domain language lives in [docs/domain-specification.md](docs/domain-specification.md). Agent entry point: [AGENTS.md](AGENTS.md).

## 1. Core principles

- **Modular monolith** — one Maven module, feature packages under `dev.vepo.issues.*`.
- **REST JSON API** — JAX-RS endpoints at `/api`; Angular SPA consumes JSON.
- **Full-stack bundle** — Quarkus Quinoa builds and serves the Angular app from the same JAR.
- **PostgreSQL + Flyway** — schema in `src/main/resources/db/migration/`.
- **JWT auth** — SmallRye JWT (RS256); roles on endpoints via `@RolesAllowed`.
- **Real-time** — Server-Sent Events (SSE) for in-app notifications.

## 2. Request lifecycle

1. Browser or Angular client calls `/api/...` with optional `Authorization: Bearer …`.
2. JAX-RS `*Endpoint` (`@ApplicationScoped`, `@Path`) validates input and authorization.
3. `*Repository` (and `*Service` when logic spans entities) reads/writes via `EntityManager`.
4. Response records (`*Response`) serialize to JSON; errors map via `IssuesException` + mappers in `infra`.
5. SSE clients register at `/api/notifications/register`; CDI events push `NotificationEvent`.

## 3. Domain overview

**Issues** is a change/ticket management system. A **Project** scopes **Tickets** that follow a **Workflow** of **Statuses** via **Transitions**. Tickets have **Categories**, **Assignees**, **Comments**, **History**, and **Subscribers** who receive **Notifications**.

```mermaid
erDiagram
    User ||--o{ Ticket : authors
    User ||--o{ Ticket : assigned_to
    User }o--o{ Ticket : subscribes
    Project ||--o{ Ticket : contains
    Project }||--|| Workflow : uses
    Workflow ||--o{ WorkflowStatus : includes
    Workflow ||--o{ WorkflowTransition : defines
    Ticket }o--|| WorkflowStatus : current_status
    Ticket }o--o| Category : classified_by
    Ticket ||--o{ Comment : has
    Ticket ||--o{ TicketHistory : audited_by
    User ||--o{ Notification : receives
    Notification }o--|| Ticket : refers_to
```

## 4. Tech stack

| Layer | Technology |
|-------|------------|
| Backend | Java 21, Quarkus 3.30, Jakarta REST, CDI, Hibernate ORM |
| Frontend | Angular 20, Angular Material, Chart.js |
| Integration | Quarkus Quinoa (SPA dev server + production bundle) |
| Database | PostgreSQL, Flyway |
| Auth | SmallRye JWT |
| Email | Quarkus Mailer + Qute HTML templates |
| Real-time | SSE + CDI async events |
| API docs | SmallRye OpenAPI → `/openapi` |
| Build | Maven (backend), npm/Angular CLI (frontend) |
| Tests | JUnit 5, REST Assured, AssertJ, ArchUnit; Karma/Jasmine (frontend) |
| Quality | SonarCloud, JaCoCo, formatter-maven-plugin |

## 5. Package layout

Feature-oriented packages (not a strict global layered tree). Each domain owns entities, repositories, endpoints, and Request/Response records.

```
dev.vepo.issues/
├── IssuesApplication.java     # JAX-RS @ApplicationPath("/api")
├── auth/                      # Login, JWT, password recovery
├── user/                      # User, Role, password reset tokens
├── project/                   # Project CRUD, project workflow/status views
├── workflow/                  # Workflow, WorkflowStatus, WorkflowTransition
├── categories/                # Ticket categories (name + color)
├── ticket/                    # Ticket CRUD, move, assign, subscribe
│   ├── comments/              # Ticket comments
│   ├── history/               # TicketHistory entity + repository
│   └── business/              # TicketHistoryService (audit logging)
├── notifications/             # Notification persistence + SSE endpoint
├── dashboards/                # Project analytics widgets
├── mailer/                    # Transactional email (Qute templates)
└── infra/                     # Exception mappers, SPA routing, dev DB setup
```

Frontend: `src/main/webui/src/app/` — `components/`, `services/`, `resolvers/`, `directives/`.

Bounded contexts and dependency rules: [docs/domain-specification.md](docs/domain-specification.md) §Bounded contexts.

## 6. Design patterns

### Repository

- One per entity; `@ApplicationScoped`; `EntityManager` directly (no base class).
- `Optional` for single results; `@Transactional` on mutating methods.

### Service layer

Use when logic spans entities or is reused across endpoints:

- `TicketHistoryService` — audit trail on ticket actions.
- `MailerService` — email on ticket changes and password reset.

Most endpoints call repositories directly for CRUD; add `*Service` when rules grow beyond a single entity.

### Endpoint layer

- Suffix `*Endpoint` (not `Controller` or `Resource`).
- HTTP DTOs: Java `record`, suffix `Request` / `Response`.
- Mapping: static factory `load()` on Response records (e.g. `TicketResponse.load(ticket)`).
- Nested response records allowed inside endpoints when local (e.g. `CategoryEndpoint.CategoryResponse`).

### CDI events

| Event | Consumer |
|-------|----------|
| `NotificationEvent` | SSE channel registration in `NotificationsEndpoint` |
| `UserNotificationEvent` | Per-user notification delivery |

### Testing

- **Backend:** `@QuarkusTest` + REST Assured; `Given` builder for seed data.
- **Architecture:** `ArchitectureTest` (ArchUnit) enforces Request/Response naming and records.
- **Frontend:** Karma/Jasmine `*.spec.ts` alongside components and services.

## 7. API surface

Base path: `/api`. OpenAPI at `/openapi.yaml`; Swagger UI at `/openapi`.

| Area | Endpoint class | Path prefix |
|------|----------------|-------------|
| Auth | `AuthenticationEndpoint` | `/auth` |
| Users | `UserEndpoint` | `/users` |
| Projects | `ProjectEndpoint` | `/projects` |
| Project tickets | `ProjectTicketEndpoint` | `/projects/{projectId}/tickets` |
| Tickets | `TicketEndpoint` | `/tickets` |
| Workflows | `WorkflowEndpoint` | `/workflows` |
| Statuses | `StatusEndpoint` | `/status` |
| Categories | `CategoryEndpoint` | `/categories` |
| Dashboards | `LoadDashboardDataEndpoint` | `/projects/{projectId}/dashboard` |
| Notifications | `NotificationsEndpoint` | `/notifications` |

## 8. Frontend routes

| Route | Component | Purpose |
|-------|-----------|---------|
| `/login` | Login | Authentication |
| `/login/reset-password` | PasswordResetRequest | Request reset |
| `/login/reset-password/:token` | PasswordReset | Complete reset |
| `/` | Home | Landing (auth required) |
| `/project/:projectId/kanban` | Kanban | Board view |
| `/project/:projectId/dashboard` | Dashboard | Analytics |
| `/search` | SearchTickets | Global ticket search |
| `/ticket/:ticketIdentifier` | TicketView | Ticket detail |
| `/users`, `/users/new`, `/users/:userId` | Users CRUD | User admin |
| `/projects`, `/projects/new`, `/projects/:projectId` | Projects CRUD | Project admin |

Full index: [docs/feature-catalog.md](docs/feature-catalog.md).

## 9. Security model

- JWT issuer: `https://issues.vepo.dev` (see `application.properties`).
- Most endpoints: class-level `@DenyAll` + method-level `@RolesAllowed`.
- Roles: `user`, `admin`, `project-manager` (combinable on a user).
- **Gap:** Some endpoints (`categories`, `status`, project ticket list) lack security annotations — treat as tech debt when hardening.

## 10. Database

Tables use prefix `tb_`. Initial migration: `V1.0.0__Database_Creation.sql`. Dev seed: `dev-import.sql` (loaded via `DatabaseDevSetup`).

## 11. Naming conventions

| Kind | Pattern | Example |
|------|---------|---------|
| Root package | `dev.vepo.issues` | — |
| Endpoint | `XxxEndpoint` | `TicketEndpoint` |
| Repository | `XxxRepository` | `TicketRepository` |
| Service | `XxxService` | `TicketHistoryService` |
| Entity | singular PascalCase | `Ticket`, `Project` |
| HTTP request | `XxxRequest` record | `CreateTicketRequest` |
| HTTP response | `XxxResponse` record | `TicketResponse` |
| Exception | `IssuesException` | domain errors in `infra` |
| Angular component | `*.component.ts` | `kanban.component.ts` |
| Angular service | `*.service.ts` | `ticket.service.ts` |
| Angular resolver | `*.resolver.ts` | `ticket.resolver.ts` |
| Docs in `docs/` | kebab-case | `domain-specification.md` |

**Avoid:** `VO`, `DTO` suffixes on HTTP types (use `Request` / `Response`).

## 12. Feature workflow (agents)

1. Read [docs/domain-specification.md](docs/domain-specification.md) — update if vocabulary changes.
2. Place code in the correct package (see §5 and bounded contexts).
3. **Create tests first** (TDD) — see [`.cursor/rules/issues-model.mdc`](.cursor/rules/issues-model.mdc) § TDD.
4. Run tiered tests — [`.cursor/rules/issues-testing.mdc`](.cursor/rules/issues-testing.mdc).
5. Run `mvn verify` once before done — [`.cursor/rules/static-analysis.mdc`](.cursor/rules/static-analysis.mdc).
6. Update [docs/feature-catalog.md](docs/feature-catalog.md) when routes or UI flows change.

## 13. Known gaps / WIP

| Item | Status |
|------|--------|
| Inconsistent endpoint security annotations | Partial — some public endpoints |
| Frontend API URL hardcoded to `localhost:8080` | Dev-only assumption |
| No dedicated `*Service` layer for all ticket operations | Acceptable for current size |
| SonarCloud project key migration | Renamed to `vepo_issues` |

## 14. Useful commands

```bash
# Dev mode (Quarkus + Angular via Quinoa)
mvn quarkus:dev

# Backend tests only
mvn test

# Full gate (backend + Sonar prep; run Angular tests separately in CI)
mvn verify

# Frontend tests
cd src/main/webui && npm test

# Format Java
mvn formatter:format
```

## 15. Related docs

| Document | Purpose |
|----------|---------|
| [AGENTS.md](AGENTS.md) | Agent index, rules, subagents, commands |
| [docs/domain-specification.md](docs/domain-specification.md) | Ubiquitous language, bounded contexts, invariants |
| [docs/feature-catalog.md](docs/feature-catalog.md) | UI routes and feature index |
| [docs/conventions-checklist.md](docs/conventions-checklist.md) | Doc debt and agent setup status |

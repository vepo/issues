# Architecture & Conventions

Canonical reference for developers and AI agents. Domain language lives in [docs/domain-specification.md](docs/domain-specification.md). Agent entry point: [AGENTS.md](AGENTS.md).

## 1. Core principles

- **Modular monolith** — one Maven module, feature packages under `dev.vepo.issues.*`.
- **REST JSON API** — JAX-RS endpoints at `/api`; Angular SPA consumes JSON.
- **Full-stack bundle** — Quarkus Quinoa builds and serves the Angular app from the same JAR.
- **PostgreSQL + Flyway** — schema in `src/main/resources/db/migration/`.
- **JWT auth** — SmallRye JWT (RS256); roles on endpoints via `@RolesAllowed`. Credential check is pluggable (**LOCAL** / **LDAP** / **ENDPOINT**) via `AUTH_PROVIDER`; session remains Issues-issued JWT.
- **Real-time** — Server-Sent Events (SSE) for in-app notifications.

## 2. Request lifecycle

1. Browser or Angular client calls `/api/...` with optional `Authorization: Bearer …`.
2. JAX-RS `*Endpoint` (`@ApplicationScoped`, `@Path`) validates input and authorization.
3. `*Repository` (and `*Service` when logic spans entities) reads/writes via `EntityManager`.
4. Response records (`*Response`) serialize to JSON; errors map via `IssuesException` + mappers in `infra`.
5. SSE clients register at `/api/notifications/register`; CDI events push `NotificationEvent`.

## 3. Domain overview

**Issues** is a change/ticket management system. A **Project** scopes **Tickets** that follow a **Workflow** of **Statuses** via **Transitions**. Tickets have **Categories**, **Assignees**, **Comments**, **History**, **Custom fields**, and **Subscribers** who receive **Notifications**.

```mermaid
erDiagram
    User ||--o{ Ticket : authors
    User ||--o{ Ticket : assigned_to
    User }o--o{ Ticket : subscribes
    Project ||--o{ Ticket : contains
    Project }||--|| Workflow : uses
    Project ||--o{ CustomField : defines
    Workflow ||--o{ WorkflowStatus : includes
    Workflow ||--o{ WorkflowTransition : defines
    Workflow ||--o{ CustomField : defines
    Ticket }o--|| WorkflowStatus : current_status
    Ticket }o--o| Category : classified_by
    Ticket ||--o{ Comment : has
    Ticket ||--o{ TicketHistory : audited_by
    Ticket ||--o{ CustomFieldValue : has
    CustomField ||--o{ CustomFieldValue : valued_as
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

Feature-oriented packages (not a strict global layered tree). Each bounded context owns entities, repositories, services, Request/Response records, and **one HTTP operation per endpoint class** under `{context}.{action}` subpackages.

```
dev.vepo.issues/
├── IssuesApplication.java     # JAX-RS @ApplicationPath("/api")
├── auth/                      # Login, JWT, password recovery, register; CDI CredentialAuthenticator; API tokens
│   ├── login/ capabilities/ me/ recovery/ register/ refresh/ changepassword/ updateprofile/
│   └── apitoken/              # Personal API tokens (create/list/revoke) + Bearer auth helpers
├── user/                      # User, Role; UserService; *Request/*Response
│   ├── create/ update/ find/ search/ delete/
├── project/                   # ProjectService, ProjectPaths
│   ├── list/ create/ update/ find/ workflow/ status/
│   ├── customfield/           # Nested project custom-field CRUD + in-scope list
│   ├── serviceaccount/        # Project service accounts + tokens
│   └── tickets/list/          # ListProjectTicketsEndpoint
├── workflow/                  # WorkflowService
│   ├── list/ create/
│   ├── customfield/           # Nested workflow custom-field CRUD (status-required)
│   └── status/list/           # ListStatusesEndpoint
├── customfield/               # CustomFieldService — defs, values, template defaults, validation
├── phase/                     # PhaseService, VersionService, PhasePaths
│   ├── list/ create/ update/ find/ activate/ complete/
│   └── version/               # Version CRUD + changelog endpoints
├── categories/                # CategoryService
│   ├── list/ create/ update/ delete/
├── ticket/                    # TicketService, TicketPaths
│   ├── list/ search/ find/ create/ update/ assign/ delete/ move/
│   ├── search/query/          # ANTLR query language (SearchTicketsByQueryEndpoint; cf.<key>)
│   ├── search/saved/          # SavedQuery CRUD + clone
│   ├── comments/list/ comments/add/
│   ├── history/               # TicketHistoryService + GetTicketHistoryEndpoint
│   ├── context/               # Composite ticket context for agents
│   └── subscribe/             # Subscribe / Unsubscribe endpoints
├── agent/                     # Agent setup-config (public URL snippet)
│   └── setup/
├── home/                      # HomeService — current/assigned tickets, activity, saved-query sections
│   ├── tickets/current/ tickets/assigned/
│   ├── activity/
│   └── savedqueries/
├── notifications/             # NotificationService, SSE register + read status
│   ├── register/
│   └── read/
├── dashboards/                # DashboardService, layout persistence, aggregations
│   ├── pie/ table/ kpi/ layout/
├── mailer/                    # Transactional email (Qute templates)
└── infra/                     # Exception mappers, SPA routing, dev DB setup
```

Sibling (not in reactor yet): `issues-mcp/` — Quarkus MCP HTTP server calling Issues `/api` with forwarded Bearer.
Frontend: `src/main/webui/src/app/` — `components/`, `services/` (thin facades), `generated/` (OpenAPI codegen, gitignored), `resolvers/`.

Bounded contexts and dependency rules: [docs/domain-specification.md](docs/domain-specification.md) §Bounded contexts.

## 6. Design patterns

### Repository

- One per entity; `@ApplicationScoped`; `EntityManager` directly (no base class).
- `Optional` for single results; `@Transactional` on mutating methods.

### Service layer

Domain services orchestrate repositories, enforce invariants, and fire CDI events. Endpoints delegate to services — no business logic in endpoint classes.

| Service | Responsibility |
|---------|----------------|
| `TicketService` | Ticket CRUD, move, assign, subscribe; notifications |
| `TicketHistoryService` | Audit trail on ticket actions |
| `UserService` | User create/update/delete with role validation and assignee guard |
| `ProjectService` | Project create/update validation |
| `AuthenticationService` | Login, recovery, register, me |
| `WorkflowService` | Workflow create/list; phase start and finish status config |
| `PhaseService` | Phase lifecycle, activation, deliverable copy from template |
| `VersionService` | Version CRUD (SemVer), changelog aggregation |
| `DashboardService` | Dashboard aggregations |
| `NotificationService` | Mark-as-read persistence |
| `CategoryService` | Category CRUD with delete guard |
| `MailerService` | Email on ticket changes and password reset |
| `ApiTokenService` | Personal API token create/list/revoke (hash-only storage) |
| `ServiceAccountService` | Project service accounts and tokens |
| `TicketContextService` | Composite ticket context for agents |

### Endpoint layer

- **One HTTP method per class** — e.g. `user.create.CreateUserEndpoint` (POST only).
- Class-level `@DenyAll` + method-level `@RolesAllowed` on every endpoint.
- `@Operation(operationId = "...")` and `@Tag(name = "...")` for OpenAPI codegen.
- Shared `@Path` prefix via `{Context}Paths` constants at context root.
- HTTP DTOs: Java `record`, suffix `Request` / `Response` at context root.
- Mapping: static factory `load()` on Response records (e.g. `TicketResponse.load(ticket)`).

### CDI events

| Event | Consumer |
|-------|----------|
| `NotificationEvent` | SSE via `RegisterNotificationsEndpoint` |
| `UserNotificationEvent` | Per-user notification delivery |

### Testing

- **Backend:** `@QuarkusTest` + REST Assured; `Given` builder for seed data.
- **Architecture:** `ArchitectureTest` (ArchUnit) — Request/Response records, `@DenyAll` on endpoints, one HTTP method per endpoint class, EntityManager only in repositories.
- **Frontend:** Karma/Jasmine `*.spec.ts`; API types from OpenAPI codegen (`src/app/generated/`, gitignored).

## 7. API surface

Base path: `/api`. OpenAPI at `/openapi.yaml`; Swagger UI at `/openapi`.

Base path: `/api`. OpenAPI at `/openapi.yaml`; Swagger UI at `/openapi`. Test profile exports spec to `target/openapi/openapi.yaml` for TypeScript codegen.

Each row is one endpoint class. Path prefixes come from `{Context}Paths`.

| Area | Example class | Path |
|------|---------------|------|
| Auth | `auth.login.LoginEndpoint` | `POST /auth/login` |
| Auth | `auth.register.RegisterUserEndpoint` | `POST /auth/register` |
| Auth | `auth.capabilities.GetAuthCapabilitiesEndpoint` | `GET /auth/capabilities` |
| Auth | `auth.me.MeEndpoint` | `GET /auth/me` |
| Auth | `auth.recovery.ResetPasswordEndpoint` | `POST /auth/recovery` |
| Auth | `auth.recovery.ConfirmPasswordResetEndpoint` | `POST /auth/recovery/confirm` |
| Personal API tokens | `auth.apitoken.*` | `/account/api-tokens` |
| Agent setup | `agent.setup.GetAgentSetupConfigEndpoint` | `GET /agent/setup-config` |
| Users | `user.create.CreateUserEndpoint` | `POST /users` |
| Users | `user.update.UpdateUserEndpoint` | `POST /users/{id}` |
| Users | `user.delete.DeleteUserEndpoint` | `DELETE /users/{id}` |
| Users | `user.find.FindUserByIdEndpoint` | `GET /users/{id}` |
| Users | `user.search.SearchUsersEndpoint` | `GET /users/search` |
| Projects | `project.*` | `/projects` (+ workflow, status subpaths) |
| Project custom fields | `project.customfield.*` | `/projects/{id}/custom-fields` (+ `/in-scope`) |
| Service accounts | `project.serviceaccount.*` | `/projects/{id}/service-accounts` (+ `…/tokens`) |
| Project tickets | `project.tickets.list.ListProjectTicketsEndpoint` | `GET /projects/{id}/tickets` |
| Tickets | `ticket.*` | `/tickets` (+ comments, history, subscribe; `customFields` on create/update/detail) |
| Ticket context | `ticket.context.GetTicketContextEndpoint` | `GET /tickets/{id}/context` |
| Ticket search | `ticket.search.SearchTicketsEndpoint` | `GET /tickets/search` |
| Query language | `ticket.search.query.SearchTicketsByQueryEndpoint` | `POST /tickets/search/query` (`cf.<key>`) |
| Saved queries | `ticket.search.saved.*` | `/saved-queries` (CRUD, by-slug, clone) |
| Home | `home.tickets.*`, `home.activity.*`, `home.savedqueries.*` | `/home/tickets/*`, `/home/activity`, `/home/saved-queries` |
| Workflows | `workflow.list.ListWorkflowsEndpoint` | `GET /workflows` |
| Workflows | `workflow.create.CreateWorkflowEndpoint` | `POST /workflows` |
| Workflow custom fields | `workflow.customfield.*` | `/workflows/{id}/custom-fields` |
| Phases | `phase.*` | `/projects/{id}/phases` |
| Versions | `phase.version.*` | `/projects/{id}/versions` |
| Statuses | `workflow.status.list.ListStatusesEndpoint` | `GET /status` |
| Categories | `categories.list.ListCategoriesEndpoint` | `GET /categories` |
| Categories | `categories.create.CreateCategoryEndpoint` | `POST /categories` |
| Categories | `categories.update.UpdateCategoryEndpoint` | `PUT /categories/{id}` |
| Categories | `categories.delete.DeleteCategoryEndpoint` | `DELETE /categories/{id}` |
| Dashboards | `dashboards.pie/table/kpi.Load*DashboardEndpoint` | `/projects/{id}/dashboard/{pie\|table\|kpi}/{type}` |
| Dashboards | `dashboards.layout.GetDashboardLayoutEndpoint` | `GET /projects/{id}/dashboard/layout` |
| Dashboards | `dashboards.layout.SaveDashboardLayoutEndpoint` | `PUT /projects/{id}/dashboard/layout` |
| Notifications | `notifications.list.ListNotificationsEndpoint` | `GET /notifications?page=&size=` |
| Notifications | `notifications.unread.UnreadNotificationCountEndpoint` | `GET /notifications/unread-count` |
| Notifications | `notifications.readall.MarkAllNotificationsReadEndpoint` | `POST /notifications/read-all` |
| Notifications | `notifications.register.RegisterNotificationsEndpoint` | `GET /notifications/register` (SSE, live only) |
| Notifications | `notifications.read.UpdateNotificationReadEndpoint` | `POST /notifications/{id}/read` |

## 8. Frontend routes

| Route | Component | Purpose |
|-------|-----------|---------|
| `/login` | Login | Authentication |
| `/login/register` | Register | Public self-registration |
| `/login/reset-password` | PasswordResetRequest | Request reset |
| `/login/reset-password/:token` | PasswordReset | Complete reset |
| `/` | Home | Landing (auth required) |
| `/project/:projectId/kanban` | Kanban | Board view |
| `/project/:projectId/dashboard` | Dashboard | Analytics |
| `/search` | SearchTickets | Simple term search |
| `/search/advanced` | AdvancedSearch | Query language editor |
| `/search/queries` | SavedQueryList | Saved queries list |
| `/search/queries/new`, `/search/queries/:id/edit` | SavedQueryEdit | Create/edit saved query |
| `/search/q/:slug` | SavedQueryView | Shared saved query + results |
| `/ticket/:ticketIdentifier` | TicketView | Ticket detail (incl. **Agente em nome de …** attribution) |
| `/account/settings` | AccountSettings | Profile, **Conectar agente**, **Tokens de API** |
| `/projects/:projectId/service-accounts` | ServiceAccounts | Project service accounts (PM/admin) |
| `/users`, `/users/new`, `/users/:userId` | Users CRUD | User admin |
| `/projects`, `/projects/new`, `/projects/:projectId` | Projects CRUD | Project admin |

Full index: [docs/feature-catalog.md](docs/feature-catalog.md).

## 9. Security model

- JWT issuer: `https://issues.vepo.dev` (see `application.properties`).
- All endpoints: class-level `@DenyAll` + method-level `@RolesAllowed`.
- Roles: `user`, `admin`, `project-manager` (combinable on a user).
- **Bearer auth:** `Authorization: Bearer` accepts session JWT **or** personal API token (`iss_pat_…`) **or** service-account token (`iss_sat_…`). API-token callers act with the principal’s powers; mutations set `via_agent` for UI attribution.
## 10. Database

Tables use prefix `tb_`. Initial migration: `V1.0.0__Database_Creation.sql`. Dev seed: `dev-import.sql` (loaded via `DatabaseDevSetup`).

## 11. Naming conventions

| Kind | Pattern | Example |
|------|---------|---------|
| Root package | `dev.vepo.issues` | — |
| Endpoint | `XxxEndpoint` in `{context}.{action}` | `user.create.CreateUserEndpoint` |
| Repository | `XxxRepository` | `TicketRepository` |
| Service | `XxxService` | `TicketHistoryService` |
| Entity | singular PascalCase | `Ticket`, `Project` |
| HTTP request | `XxxRequest` record | `CreateTicketRequest` |
| HTTP response | `XxxResponse` record | `TicketResponse` |
| Exception | `IssuesException` | domain errors in `infra` |
| Angular component | `*.component.ts` | `kanban.component.ts` |
| Angular service | `*.service.ts` facade over generated `*Api` | `ticket.service.ts` |
| Angular resolver | `*.resolver.ts` | `ticket.resolver.ts` |
| Docs in `docs/` | kebab-case | `domain-specification.md` |

**Avoid:** `VO`, `DTO` suffixes on HTTP types (use `Request` / `Response`).

## 12. Feature workflow (agents)

Mandatory: [`.cursor/rules/development-process.mdc`](.cursor/rules/development-process.mdc).

1. **Feature analysis** — create or extend `feature/<feature-slug>.md`; update [docs/domain-specification.md](docs/domain-specification.md) if vocabulary changes.
2. **Task break** — numbered tasks and test coverage plan in the changelog entry.
3. **Development approved** — user selects tasks; no code until approved.
4. **Development (TDD)** — Red → Green → Refactor per approved task; tests must cover the change.
5. Place code in the correct package (see §5 and bounded contexts).
6. Run tiered tests — [`.cursor/rules/issues-testing.mdc`](.cursor/rules/issues-testing.mdc).
7. Run `mvn verify` once before done — [`.cursor/rules/static-analysis.mdc`](.cursor/rules/static-analysis.mdc).
8. Update [docs/feature-catalog.md](docs/feature-catalog.md) when routes or UI flows change.

## 13. Known gaps / WIP

| Item | Status |
|------|--------|
| Workflow update API/UI | Done — [workflow-configuration.md](feature/workflow-configuration.md) v2; editable statuses + `statusReplacements` remap; history without notify |
| Kanban swimlanes + WIP limits | Done — [kanban-board.md](feature/kanban-board.md) v3; `tb_workflow_wip_limits`; hard enforce on `moveTicket`; **Agrupar por** toolbar |
| Header **Projetos** menu (all users → Kanban) | Done — [project-navigation.md](feature/project-navigation.md) v1; `GET /projects` viewable scope |
| Immutable project prefix | Done — [project-administration.md](feature/project-administration.md) v2; `prefixLocked` on `ProjectResponse`; reject prefix change when tickets exist |
| Pluggable auth (LOCAL / LDAP / ENDPOINT) | Done — [authentication.md](feature/authentication.md) v3; `AUTH_PROVIDER` + CDI `CredentialAuthenticator`; `GET /auth/capabilities` |
| Notifications SSE reconnect + infinite scroll | Done — [notifications.md](feature/notifications.md) v2; `GET /notifications` page API; SSE live-only; client reconnect + token refresh |
| Notifications mark-all + unread badge | Done — [notifications.md](feature/notifications.md) v3; `GET /notifications/unread-count`; `POST /notifications/read-all`; badge `99+` |
| Shared rich-text editor | Done — [rich-text-editor.md](feature/rich-text-editor.md) v1; Description, Text CF, project/template description; `PlainTextLength` / `@PlainTextSize` |
| Custom fields (project/workflow defs, ticket values) | Done — [custom-fields.md](feature/custom-fields.md) v1; `customfield` package; nested `/projects|workflows/{id}/custom-fields`; ticket values; import by key; query `cf.<key>` |
| Agentic Development integration (PAT + SA + separate Quarkus MCP) | Near-complete — [agentic-integration.md](feature/agentic-integration.md) v1 (T1–T14 docs; T15 seed/`verify` remaining). Issues: PAT/SA Bearer, `GET /tickets/{id}/context`, `GET /agent/setup-config`, `via_agent` attribution. Standalone [`issues-mcp/`](issues-mcp/) Quarkus MCP HTTP (`/mcp` :8082) forwards tools to Issues `/api`; **not in reactor** (multi-module later). Backup skill: [`.cursor/skills/issues-agent/`](.cursor/skills/issues-agent/) |
| Ticket links, epics & subtasks | Done — [ticket-links.md](feature/ticket-links.md) v1; `TicketType`; `tb_ticket_links`; cross-project; Epic hierarchy; Angular Vínculos/Subtarefas |
| Project ticket backlog (ranked list + reorder) | Done — [ticket-backlog.md](feature/ticket-backlog.md) v1; `backlog_rank`; `GET/POST …/backlog`; Angular infinite scroll + drag |
| Burndown (story points, Kanban-peer page) | Done — [burndown.md](feature/burndown.md) v1; `story_points`/`canceled_at`; `GET …/burndown`; Angular `/project/:id/burndown` |
| Project visibility (Private / Internal / Public) | Done — [project-visibility.md](feature/project-visibility.md) v1; `securityLevel` + `ProjectAccessService.canRead`; `@PermitAll` Public reads; SEC1 |
| Project dashboard (Painel) hardening | Tasks-ready — [project-dashboard.md](feature/project-dashboard.md) v3; dual `DashboardType` parse; `requireView`; UX FQ5–8; await approval T1–T9 |
| Git integration (repo association + linked commits) | Tasks-ready — [git-integration.md](feature/git-integration.md) v1; `git` package; webhook + inbound API; await task approval T1–T10 |
| CSV import chunked upload | Tasks-ready — [ticket-import.md](feature/ticket-import.md) v2; init/part/complete; 5 MB / 1 MB / 500 rows; await approval T1–T7 |

## 14. OpenAPI → TypeScript codegen

Backend tests export OpenAPI to `target/openapi/openapi.yaml` (`%test.quarkus.smallrye-openapi.store-schema-directory`).

After API changes:

```bash
mvn test                              # export spec
cd src/main/webui && npm run generate:api   # → src/app/generated/ (gitignored)
```

Angular facades in `services/` wrap generated `*Api` classes. OpenAPI paths include `/api`, so `BASE_PATH` is `''`. SSE (`sse.client.ts`) stays hand-written.

CI order: `mvn test` → `npm run generate:api` → `ng test` → `mvn verify`.

## 15. Useful commands

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

## 16. Related docs

| Document | Purpose |
|----------|---------|
| [AGENTS.md](AGENTS.md) | Agent index, rules, subagents, commands |
| [docs/domain-specification.md](docs/domain-specification.md) | Ubiquitous language, bounded contexts, invariants |
| [docs/feature-catalog.md](docs/feature-catalog.md) | UI routes and feature index |
| [docs/conventions-checklist.md](docs/conventions-checklist.md) | Doc debt and agent setup status |

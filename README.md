# Issues — Change & Ticket Management

Track work items across **projects** with configurable **workflows**, a **Kanban** board, **dashboards**, comments, audit history, and real-time **notifications**.

Quarkus REST API + Angular SPA, bundled in one deployable via Quinoa.

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=vepo_issues&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=vepo_issues) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=vepo_issues&metric=coverage)](https://sonarcloud.io/summary/new_code?id=vepo_issues)

## Tech stack

| Layer | Technology |
|-------|------------|
| Backend | Quarkus 3, Java 21, JAX-RS, CDI, Hibernate ORM |
| Frontend | Angular 20, Angular Material, Chart.js |
| Integration | Quarkus Quinoa (SPA dev server + production bundle) |
| Database | PostgreSQL, Flyway |
| Auth | SmallRye JWT (RS256) |
| Real-time | Server-Sent Events (SSE) + CDI events |
| Email | Quarkus Mailer + Qute templates |
| Tests | JUnit 5, REST Assured, ArchUnit; Karma/Jasmine (frontend) |
| Build | Maven |

## Quick start

```bash
mvn quarkus:dev
```

- API: [http://localhost:8080/api](http://localhost:8080/api)
- Angular dev server (via Quinoa): [http://localhost:4200](http://localhost:4200)
- OpenAPI / Swagger UI: [http://localhost:8080/openapi](http://localhost:8080/openapi)

Dev mode runs Flyway clean+migrate (`%dev.quarkus.flyway.clean-at-start=true`) and loads sample data from `dev-import.sql`.

### After backend API changes

```bash
mvn test
cd src/main/webui && npm run generate:api
```

Generated TypeScript clients land in `src/app/generated/` (gitignored). Angular facades in `services/` wrap the generated `*Api` classes.

| Email | Role | Password |
|-------|------|----------|
| `admin@issues.vepo.dev` | admin | see `application.properties` (`password.default`) |
| `pm@issues.vepo.dev` | project-manager | same |
| `user@issues.vepo.dev` | user | same |

## Features

### Tickets & workflow

- **Tickets** — create, edit, assign, and soft-delete work items scoped to a project
- **Identifiers** — human-readable keys (`ISS-001`) from project prefix + sequence
- **Workflows** — configurable status graphs with allowed transitions per project
- **Move ticket** — status changes validated against workflow rules
- **Categories** — classify tickets with name and color
- **Comments** — discussion thread on each ticket
- **Ticket history** — immutable audit log of create, update, assign, move, comment, and delete actions
- **Subscribers** — watch tickets and receive alerts on changes

### Projects & administration

- **Projects** — name, prefix, description, and assigned workflow
- **Workflow builder** — create workflows with statuses and transitions (API)
- **User management** — CRUD for users and role assignment (admin)
- **Roles** — `user`, `admin`, `project-manager` (combinable)

### Views & analytics

- **Kanban board** — columns by workflow status; move tickets between stages
- **Project dashboard** — charts (tickets by day, status, priority), recent tickets table, performance KPIs
- **Global search** — full-text ticket search across projects
- **Ticket detail** — expanded view with history, comments, assignee, and status actions

### Notifications & email

- **In-app notifications** — persisted alerts with mark-as-read
- **Real-time delivery** — SSE channel registers on login; pushes on ticket changes
- **Email** — password reset and ticket change notifications (Mailer + Qute)

### Authentication

- **Login** — JWT-based session via Bearer token
- **Password recovery** — email link with single-use reset token
- **Current user profile** — `/auth/me` for authenticated context

## Documentation

| Doc | Contents |
|-----|----------|
| [AGENTS.md](AGENTS.md) | Index for AI / Cursor agents |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Stack, packages, API map, patterns, naming |
| [docs/domain-specification.md](docs/domain-specification.md) | Ubiquitous language and business rules |
| [docs/feature-catalog.md](docs/feature-catalog.md) | UI routes, roles, and click paths |
| [docs/conventions-checklist.md](docs/conventions-checklist.md) | Doc debt and agent setup status |
| [.cursor/rules/](.cursor/rules/) | Cursor project rules |

## Vídeos

* Playlist Vibe Coding https://www.youtube.com/playlist?list=PLnenY3wke2pcUmFM7iUPbgeWt6J32BoTk
* Vibe Coding I: Implementando um sistema de Gestão de Mudanças https://youtu.be/ReVWhBpTblU
* Vibe Coding II: Implementando um sistema de Gestão de Mudanças https://youtu.be/iPo7-PRQr9c
* Vibe Coding III: Implementando um sistema de Gestão de Mudanças https://youtu.be/FRQtnk4tmYw
* Vibe Coding IV: Implementando um sistema de Gestão de Mudanças https://youtu.be/QTRejWsEx3o
* Vibe Coding V: Implementando um sistema de Gestão de Mudanças https://youtu.be/W-zLN-5dBRE
* Vibe Coding VI: Implementando um sistema de Gestão de Mudanças https://youtu.be/otjcaUk2L7A
* Vibe Coding VII: Implementando um sistema de Gestão de Mudanças https://youtu.be/ih9Of_GnGrA
* Vibe Coding VIII: Implementando um sistema de Gestão de Mudanças https://youtu.be/PN7CFqyPG_c
* Vibe Coding IX: Implementando um sistema de Gestão de Mudanças https://youtu.be/fu9SM0xXwLs
* Vibe Coding X: Implementando um sistema de Gestão de Mudanças https://youtu.be/XHgOGLK3ZXg
* Deploy de Sexta: Criando página de autenticação no Quarkus + Angular https://youtube.com/live/1wWcOmFH3GI
* Vibe Coding XI: Implementando um sistema de Gestão de Mudanças https://youtu.be/d69aThqDj1Q
* Vibe Coding XII: Implementando um sistema de Gestão de Mudanças https://youtu.be/OAW5slzxRGI
* Abandonei o Vibe Code I: Evoluindo uma API Rest pra Gestão de Tickets https://youtu.be/BAZyxPWlf4E
* Abandonei o Vibe Code II: Explorando Jakarta EE, JAX-RS e JPA na Prática https://youtu.be/IS0biu6x9CU
* Abandonei o Vibe Code III: Configurando logs https://youtu.be/qtAxt6WaqNI
* Vibe Coding Live II: Java, Quarkus e IA na Prática https://youtube.com/live/miKerswJBb8
* Vibe Coding Live III: Componentização, Java no Back e Desafios Reais https://youtube.com/live/4QiPH6wMu5M
* Vibe Coding Live IV: Angular, Java e os Desafios do Frontend! https://youtube.com/live/SwAHEWr-61s
* Vibe Coding Live V: Implementando Server-Sent Events com CDI e Quarkus + Configuração de E-mail https://youtube.com/live/CQIngZ9OKv8
* Vibe Coding Live VI: Implementando subscrição ao ticket https://youtube.com/live/yNErFoPJWhw

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=vepo/issues&type=date&legend=top-left)](https://www.star-history.com/#vepo/issues&type=date&legend=top-left)

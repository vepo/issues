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
| `cto@issues.ui` | admin, project-manager, user | see `application.properties` (`password.default`) |
| `project_lead@issues.ui` | project-manager | same |
| `junior_dev@issues.ui` | user | same |
| `tech_lead@issues.ui` | admin, user (no PM) | same — useful for admin-without-PM checks |

Full persona table: [docs/feature-catalog.md](docs/feature-catalog.md) § Dev personas.

## Features

### Tickets & workflow

- **Tickets** — create via `/tickets/new` or project-scoped route; edit, assign, optional **due date**, set **ticket type** (Épico / História / Tarefa), set priority, and soft-delete work items scoped to a project
- **Clone ticket** — start from an active ticket, choose any writable target project, review compatible copied fields and omission warnings, then create a fresh ticket through the standard create flow
- **Identifiers** — human-readable keys (`ISS-001`) from project prefix + sequence
- **Workflows** — configurable status graphs with allowed transitions per project
- **Priority** — `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` on create and edit
- **Ticket links** — typed relationships (`BLOCKS`, `RELATES_TO`, `DUPLICATES`, `DERIVED_FROM`, `REMAINING_WORK_OF`, `CHILD_OF`); **Vínculos** and Epic **Subtarefas** with progress on ticket detail; cross-project allowed
- **Move ticket** — status changes validated against workflow rules
- **Finish date** — set when moving to a workflow **done** finish status; cleared when leaving done
- **Versions** — SemVer labels per project; ticket **planned** and **shipped** version fields; grouped **version changelog** (excludes canceled tickets)
- **Phases** — time-boxed planning periods with objective and deliverables; **activate** / **complete** lifecycle; optional ticket phase assignment; **project phase template** copied into new phases
- **Categories** — classify tickets with name and color
- **Comments** — discussion thread on each ticket (rich-text editor)
- **Attachments** — upload, list, download, and delete files on a ticket (max 10 MB each; images, PDF, Office, zip, etc.)
- **Rich text** — shared editor for ticket Description, Text custom fields, project description, and ticket template description
- **Ticket history** — structured audit log (create, field changes, assign, move, subscribe, delete, link add/remove, attachment add/remove) merged with comments in the **Atividade** feed
- **Subscribers** — watch tickets and receive alerts on changes
- **CSV import** — bulk-create tickets from a CSV file; project-scoped (from Kanban) or global (header Importar, project per row from CSV column); **chunked upload** (max 5 MB / 1 MB parts / 500 rows); map built-in columns and **custom field keys**; server parses with OpenCSV, stores rows in database, then column-mapping wizard creates tickets
- **Custom fields** — typed attributes (String, Text, Integer, Boolean, Enum) defined on projects and workflows; values on create/detail; required and status-required; template defaults; query with `cf.<key>`

### Projects & administration

- **Projects** — name, prefix (immutable once tickets exist), description, assigned workflow, **project owner** (PM role), **project members**, optional **ticket template** (built-in defaults plus in-scope **custom field** defaults), and **project custom field** definitions
- **Service accounts** — project-scoped machine identities and tokens for agents/CI (`/projects/:id/service-accounts`); member-aligned powers on that project
- **Project allocation** — dedicated page to add/remove members; removal blocked while member has open assigned tickets
- **Project hub** — member landing page with links to Kanban, Burndown, Backlog, and dashboard (replaces project grid on home)
- **Header Projetos** — labeled menu for all authenticated users listing readable projects (membership plus Internal/Public; Private only for members/admin); each item opens that project’s Kanban; PM/admin also **Gerenciar projetos**
- **Project security level** — Private / Internal / Public read visibility on create/edit (**Nível de segurança**); default Internal; Public allows anonymous read of tickets, Kanban, and related project surfaces
- **Workflow builder** — create and edit workflows with statuses (add/rename/remove on edit, with ticket remap), transitions, optional per-status **WIP limits**, and **workflow custom fields** (incl. status-required) (`/workflows` UI + API)
- **Categories admin** — list, create, edit, and delete ticket categories (`/categories`, admin); delete blocked while tickets or project templates reference the category
- **User management** — admin CRUD and soft-delete (blocked while assignee on open tickets); public self-registration (`/login/register`) with strong password policy
- **Roles** — `user`, `admin`, `project-manager` (combinable)

### Views & analytics

- **Home hub** — personal work view: open tickets in your projects, tickets assigned to you, and recent activity (comments + status changes)
- **Kanban board** — columns by workflow status; optional swimlanes (assignee / priority); WIP `n/limit` with hard drop/move enforcement; move tickets between stages
- **Burndown** — phase story-points remaining vs ideal line; peer of Kanban; chart requires phase start/end dates; warns when tickets lack points
- **Dashboards** — project analytics widgets (pie, KPI, table) with per-user layout
- **Project backlog** — ranked planning list (separate from Priority); infinite scroll; PM/admin drag-and-drop reorder; excludes done and deleted tickets
- **Project dashboard** — charts (tickets by day as bar, status, priority), recent tickets (top 20), **Tickets por status** KPI; editable layout with autosave + **Concluir**; per-user layout on the server
- **Git integration** — one repository per project; forge push webhook (HMAC) and inbound commit API (PAT/SA); commits mentioning ticket ids appear on ticket history
- **Global search** — simple term search and **query language** (ANTLR, plain text) across ticket fields, comments, and custom fields (`cf.<key>`)
- **Saved queries** — name, share by link, optional home sections, clone for non-owners
- **Ticket detail** — expanded view with unified **Atividade** feed (comments + history), assignee, and status actions; agent mutations show **Agente em nome de …**

### Notifications & email

- **In-app notifications** — persisted alerts, accurate unread badge, mark-as-read, and mark all as read
- **Real-time delivery** — SSE channel registers on login for live pushes; dropdown loads history via paginated API with infinite scroll; SSE auto-reconnects after network drop
- **Email** — password reset and ticket change notifications (Mailer + Qute)

### Authentication

- **Login** — short-lived JWT access token plus refresh token (`POST /auth/refresh`); credentials verified by the active provider (`AUTH_PROVIDER`: `local`, `ldap`, or `endpoint`)
- **Password recovery** — email link with token; LOCAL provider only (hidden when capabilities say otherwise)
- **Account** — edit name, email, and **UI language** (`pt` / `en`) at `/account/settings`; change password only when LOCAL
- **UI languages** — Portuguese (source) and English via path prefixes `/pt/` and `/en/`; preference stored on the user profile
- **Personal API tokens** — create/list/revoke at account settings (**Tokens de API**); Bearer `iss_pat_…` for scripts and agents; secret shown once
- **Agent setup** — **Conectar agente** generates a token and copy-ready MCP config (`issues.public-base-url` / `issues.mcp-public-base-url`)
- **Issues MCP** — separate Quarkus MCP HTTP app ([`issues-mcp/`](issues-mcp/)) forwarding tools to Issues `/api` (not in the main reactor yet)
| Env / property | Purpose |
|----------------|---------|
| `AUTH_PROVIDER` / `auth.provider` | `local` (default), `ldap`, or `endpoint` |
| `AUTH_LDAP_*` / `auth.ldap.*` | LDAP URL, base DN, bind, filters, group→role map |
| `AUTH_ENDPOINT_URL` / `auth.endpoint.url` | External credential POST URL |

Production JWT signing key rotation: configure `mp.jwt.verify.publickey.location` to a JWKS resource listing current and previous public keys during rollover (see `application.properties` comments).

**Development-only JWT PEMs:** bundled `privateKey.pem` / `publicKey.pem` must not be used outside `%dev`/`%test`. Shared/prod hosts must set `SMALLRYE_JWT_SIGN_KEY_LOCATION` and `MP_JWT_VERIFY_PUBLICKEY_LOCATION` (see [SECURITY.md](SECURITY.md)).

## Documentation

| Doc | Contents |
|-----|----------|
| [AGENTS.md](AGENTS.md) | Index for AI / Cursor agents |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Stack, packages, API map, patterns, naming |
| [docs/domain-specification.md](docs/domain-specification.md) | Ubiquitous language and business rules |
| [docs/feature-catalog.md](docs/feature-catalog.md) | UI routes, roles, and click paths |
| [docs/backlog.md](docs/backlog.md) | Ordered product backlog |
| [docs/ui-elements-gallery.md](docs/ui-elements-gallery.md) | UI element catalog |
| [docs/conventions-checklist.md](docs/conventions-checklist.md) | Doc debt and agent setup status |
| [.cursor/rules/](.cursor/rules/) | Cursor project rules |
| [.cursor/skills/issues-agent/](.cursor/skills/issues-agent/) | Use Issues via MCP / API tokens |

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

# Issues — Domain Specification

Canonical domain language for **Issues**, a change/ticket management system. Developers, reviewers, and AI agents must align code, tests, and UI copy with this document.

**Related references:** [ARCHITECTURE.md](../ARCHITECTURE.md) (technical patterns), [feature-catalog.md](feature-catalog.md) (routes and UI flows).

**Maintenance:** When a change introduces or alters domain concepts, UI labels, or business rules, update this file **before** merging (see [.cursor/rules/domain-model.mdc](../.cursor/rules/domain-model.mdc)).

---

## Context

Issues lets teams track **tickets** (change/work items) within **projects**, each governed by a configurable **workflow**. Projects organize work into **phases** and **versions** for release planning. Users authenticate with JWT, view tickets on a **Kanban** board or **dashboard**, collaborate via **comments**, and receive **notifications** when subscribed tickets change.

```mermaid
erDiagram
    User ||--o{ Ticket : authors
    User ||--o{ Ticket : assigned_to
    User }o--o{ Ticket : subscribes
    Project ||--o{ Ticket : contains
    Project ||--o{ Phase : schedules
    Project ||--o{ Version : releases
    Project }||--|| Workflow : uses
    Workflow ||--o{ WorkflowStatus : includes
    Workflow ||--o{ WorkflowTransition : defines
    Workflow }o--o| WorkflowStatus : phase_start
    Workflow }o--o{ WorkflowStatus : finish_statuses
    Phase }o--o| Version : deliverable_version
    Phase ||--o{ PhaseDeliverable : lists
    Ticket }o--o| Phase : assigned_to
    Ticket }o--o| Version : observed_version
    Ticket }o--o| Version : target_version
    Ticket }o--|| WorkflowStatus : current_status
    Ticket }o--o| Category : classified_by
    Ticket ||--o{ Comment : has
    Ticket ||--o{ TicketHistory : audited_by
    Ticket ||--o{ CustomFieldValue : has
    Project ||--o{ CustomField : defines
    Workflow ||--o{ CustomField : defines
    CustomField ||--o{ EnumOption : offers
    CustomField ||--o{ CustomFieldValue : valued_as
    User ||--o{ Notification : receives
```

---

## Bounded contexts

Issues is a **modular monolith**: one deployable, feature packages under `dev.vepo.issues.*`. Contexts communicate via CDI events, shared entities referenced by ID, and service calls at documented boundaries — not by reaching into another context's repositories from unrelated endpoints.

| Context | Packages | May depend on |
|---------|----------|---------------|
| **Platform** | `infra` | JDK/Jakarta only |
| **Identity & access** | `auth`, `auth.apitoken`, `user` | platform |
| **Project administration** | `project`, `project.serviceaccount` | platform, identity, workflow, customfield |
| **Workflow configuration** | `workflow` | platform, customfield, identity, ticket |
| **Custom fields** | `customfield` | platform |
| **Phase & version planning** | `phase` | platform, project, workflow |
| **Ticket management** | `ticket`, `ticket.comments`, `ticket.history`, `ticket.business` | platform, identity, project, workflow, categories, phase, customfield |
| **Git / SCM** | `git` | platform, identity, project, ticket |
| **Classification** | `categories` | platform |
| **Notifications** | `notifications` | platform, identity, ticket |
| **Analytics** | `dashboards` | platform, project, ticket, workflow |
| **Email** | `mailer` | platform, identity, ticket |

**Rules:**

- Feature packages must not depend on unrelated contexts (e.g. `categories` must not depend on `dashboards`).
- Cross-context reactions (notify subscribers on ticket move) use CDI events or dedicated services in the owning context.
- `infra` holds exception mappers, SPA routing, and dev setup — no domain logic.
- **Issues MCP** is not a bounded context of the monolith — it is an external HTTP client of `/api` (see Ubiquitous Language).

---

## Ubiquitous Language

Terms below are the **only** approved names for aggregates, entities, states, actions, and user-visible labels unless this document is updated first.

### Platform & people

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Issues** | The product (change/ticket management). | UI title, email templates |
| **User** | Registered account with name, email, optional local password, roles, **auth provider**, and optional **locale preference**. | `User`, `tb_users` |
| **UI locale** | Language for product chrome, **system labels**, and formatting: `pt` or `en`. Source templates are Portuguese (`pt`). | Angular i18n; [feature/i18n.md](../feature/i18n.md) |
| **Locale preference** | User’s stored **UI locale** on the server; edited on account settings. Unauthenticated visits use browser `Accept-Language`. | `User` column (planned); `GET /auth/me`, `POST /auth/profile` |
| **System label** | Product-owned display string for fixed enums and UI copy (e.g. Priority, Phase status). Distinct from admin-authored names (workflow statuses, custom fields). | Shared `$localize` catalog (planned) |
| **Auth provider** | Deployment-wide credential source: **LOCAL**, **LDAP**, or **ENDPOINT** (selected via `AUTH_PROVIDER`). | `AuthProvider`, `auth.provider` |
| **Role** | Platform capability assigned to a user (multi-role). | `Role` enum |
| **User** (role) | Default role; create and work on tickets. | `Role.USER` — label: "User" |
| **Administrator** | Full user management. | `Role.ADMIN` — label: "Admin" |
| **Project manager** | Create projects and workflows; soft-delete tickets. | `Role.PROJECT_MANAGER` |
| **Session** | Authenticated state via JWT Bearer token; **refresh token** renews access without re-login. Issued after any successful provider verification. | `AuthenticationService`, Angular `auth.service` |
| **Password recovery** | Self-service flow to reset password via email link; **LOCAL** provider only. | `AuthenticationService` `/auth/recovery` |
| **Password reset token** | Single-use secret sent by email. | `PasswordResetToken` |
| **Auth capabilities** | Public flags telling the UI whether password recovery and change-password are available. | `GET /auth/capabilities` |
| **Personal API token** | Long-lived secret a **user** creates to authenticate `/api` as themselves (scripts, MCP, CI) without password login. Secret shown once at create; stored hashed; revocable. Prefix `iss_pat_`. | `auth.apitoken`; UI: account **Tokens de API**; [agentic-integration.md](../feature/agentic-integration.md) |
| **Service account** | Project-scoped machine identity for agents/CI; has its own tokens; managed at **`/projects/:projectId/service-accounts`** by project manager/admin. Display name used in **Agente em nome de &lt;nome&gt;**. Token prefix `iss_sat_`. Permissions: **project member–aligned** on that project. | `project.serviceaccount`; [agentic-integration.md](../feature/agentic-integration.md) |
| **Agent setup** | Guided account-settings flow (**Conectar agente**) that creates a token and offers **copy-ready** MCP/IDE configuration using **`issues.public-base-url`** and **`issues.mcp-public-base-url`**. | `GET /agent/setup-config`; UI **Conectar agente** |
| **Public base URL** | Configurable absolute base URL(s) of Issues API and MCP for generated agent config (`application.properties` in v1; no admin UI). | `issues.public-base-url`, `issues.mcp-public-base-url` |
| **Issues MCP** | Separate Quarkus **Model Context Protocol** client app (Java) — **external to Issues core**. Calls Issues `/api` with the client’s Bearer token; never uses EntityManager. May later join Issues as a **multi-module** reactor module. No Python MCP. | Sibling [`issues-mcp/`](../issues-mcp/); [agentic-integration.md](../feature/agentic-integration.md) **AQ7** |
| **Agent channel** (`via_agent`) | Request authenticated with a **personal API token** or **service account** token; persisted as `via_agent` on history/comments; UI shows **Agente em nome de &lt;nome&gt;** (PAT: user name; SA: service account display name). | `via_agent` on `tb_ticket_history` / `tb_comments` |
| **Ticket context** | Composite read for agents: ticket detail + allowed transitions + in-scope custom fields in one response. | `GET /tickets/{id}/context` |
| **Agentic integration** | Capability for coding agents to read and update tickets while implementing features. Distinct from repo `.cursor/agents` / skills used to *build* Issues. | [agentic-integration.md](../feature/agentic-integration.md); backup skill [`.cursor/skills/issues-agent/`](../.cursor/skills/issues-agent/) |

### Projects & workflows

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Project** | Bounded scope for tickets: name, prefix, required description, assigned workflow, and **project security level**. | `Project`, `tb_projects` |
| **Project security level** | Per-project **read** visibility for tickets and related surfaces (tickets, Kanban, versions, hub, phases, backlog, burndown, dashboard, filtered search). Levels: **Private**, **Internal**, **Public**. **Writes** always require membership (or manage); never anonymous. Default: **Internal**. UI: **Nível de segurança**. API/field: `securityLevel`. Persist: VARCHAR + Java `SecurityLevel`. | Shipped — [feature/project-visibility.md](../feature/project-visibility.md); remediates ticket IDOR (SEC1) |
| **Private** (security level) | Only **project members** and **admin** may read gated surfaces. | `PRIVATE` / UI **Privado** |
| **Internal** (security level) | Any **authenticated** user may read; non-members cannot write. | `INTERNAL` / UI **Interno** — default |
| **Public** (security level) | **Anonymous** and authenticated users may read gated surfaces (no anonymous global search/home in v1). | `PUBLIC` / UI **Público** |
| **Project owner** | Single user accountable for a project; must have **project-manager** role; set at creation; may be **transferred** by admin or current owner on edit. | `Project.owner` / `tb_projects.owner_id` |
| **Project member** | User assigned to a project; required to be eligible as ticket **assignee** on that project. | `tb_project_members`; M:N project ↔ user |
| **Project allocation** | UI for project owner or admin to add or remove **project members**. | `/projects/:projectId/allocation`; UI **Alocação** |
| **Project hub** | Project landing: Kanban and dashboard entry; **readable** per **project security level**. | `/projects/:projectId` |
| **Project navigation menu** | Global header **Projetos** control listing **viewable** projects; each item opens that project’s **Kanban**. Empty: disabled control with tooltip. | Shell `ProjectMenuComponent`; `GET /projects` viewable scope |
| **Assigned project** | Project where the current user is a **project member**. | Scopes home and hub for `user` role |
| **Project prefix** | Short uppercase code used in ticket identifiers (e.g. `ISS`). | `Project.prefix` |
| **Workflow** | Named state machine: start status, allowed statuses, transitions. | `Workflow`, `tb_workflows` |
| **Status** | Named step in a workflow (e.g. TODO, IN_PROGRESS, DONE). | `WorkflowStatus`, `tb_workflow_status` |
| **WIP limit** (workflow status) | Optional positive integer cap on tickets in that status within a workflow; absent = unlimited. | `tb_workflow_wip_limits` |
| **Transition** | Allowed move from one status to another within a workflow. | `WorkflowTransition` |
| **Status remap** (workflow edit) | When a **status** is removed from a workflow while tickets still use it (including soft-deleted), the editor requires a **replacement status**; tickets are moved in the same transaction with **STATUS_CHANGED** history and without subscriber notify. | `UpdateWorkflowRequest.statusReplacements`; [feature/workflow-configuration.md](../feature/workflow-configuration.md) v2 |
| **Start status** | Required initial status for every new ticket in a workflow. | `Workflow.start` / `start_id` |
| **Phase start status** | Optional status on a workflow; when a **phase** is **activated**, each assigned ticket moves here if a valid transition exists. | `Workflow.phaseStart`; UI **Status inicial da fase** |
| **Finish status** | Workflow status marked as terminal with outcome **done** or **canceled**. | `WorkflowFinishStatus`, `tb_workflow_finish_statuses` |
| **Finish outcome** | Classification of a finish status: `DONE` or `CANCELED`. | `FinishOutcome` enum |
| **Git repository association** | One remote git repository linked to a **project** (URL, optional provider hint, optional default branch, webhook secret) for commit ingest and deep links. | Planned — `tb_project_git_repositories`; [feature/git-integration.md](../feature/git-integration.md) |
| **Ticket template** | Optional default field values for new tickets in a project: built-in fields (title, description, category, priority) and, when configured, **custom field** defaults for in-scope project/workflow fields. | Embedded / related on `Project`; `customFieldDefaults` — [feature/custom-fields.md](../feature/custom-fields.md) |
| **Template enabled** | Project manager opted in; when true, at least one template field must be configured; only configured fields pre-fill the create form. | `Project.ticketTemplateEnabled`; UI checkbox **Usar template de ticket** |
| **Phase template objective** | Default plain-text **objective** copied into each new phase for the project. | `Project.phaseTemplateObjective` |
| **Phase template deliverable** | Default **deliverable** row copied into each new phase for the project. | `tb_project_phase_deliverable_templates` |
| **Custom field** | Named, typed attribute that extends ticket data beyond **built-in ticket fields**. Defined on a **project** or a **workflow**. | `customfield.CustomField`; [feature/custom-fields.md](../feature/custom-fields.md) |
| **Custom field type** | Allowed type: `STRING`, `TEXT`, `INTEGER`, `BOOLEAN`, `ENUM`. | `CustomFieldType` |
| **Custom field key** | Stable machine identifier for a custom field; immutable after create; unique within owner and across a project’s in-scope union. | `CustomField.key` |
| **String (custom field)** | Short plain text; per-field max length ≤ platform cap **255**. | Distinct from **Title** |
| **Text (custom field)** | Long text using the same storage/editor model as **Description** (plain-text max **1200**; may store rich-text HTML). | Distinct from **Description** |
| **Integer (custom field)** | Whole number with optional min and max bounds. | |
| **Boolean (custom field)** | True/false; optional fields may be unset (null). | UI: checkbox |
| **Enum (custom field)** | Single choice from a fixed set of **enum options**. | Single-select in v1 |
| **Enum option** | One allowed value/label for an enum custom field; ordered; cannot be removed while in use. | |
| **Project custom field** | Custom field defined on a **project**. | |
| **Workflow custom field** | Custom field defined on a **workflow**. | |
| **Status-required custom field** | A **workflow custom field** that must have a valid value when creating at or moving into configured **statuses**. Globally required workflow fields imply all statuses. | `tb_custom_field_status_required` |
| **Built-in ticket field** | Fixed ticket attributes (title, description, category, priority, assignee, phase, versions, due date, …). | Contrast with **custom field** |
| **In-scope custom fields** | Enabled **project custom fields** ∪ enabled **workflow custom fields** for the ticket’s project (union by **key**). | |

### Phases & versions

Methodology-neutral planning terms. UI chrome uses **UI locale** (`pt` / `en`); Portuguese remains the Angular source locale.

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Phase** | Time-boxed planning period within a project. | `Phase`, `tb_phases`; UI **Fase** |
| **Phase status** | Lifecycle of a phase: `PLANNED`, `ACTIVE`, `COMPLETED`. | `PhaseStatus` enum |
| **Active phase** | The single phase in `ACTIVE` status for a project. | At most one per project |
| **Activate phase** | Transition `PLANNED` → `ACTIVE`; completes any other active phase in the same project. | `POST .../phases/{id}/activate` |
| **Complete phase** | Transition `ACTIVE` → `COMPLETED`. | `POST .../phases/{id}/complete` |
| **Objective** | Single plain-text statement of the phase's main achievement. | `Phase.objective`; UI **Objetivo** |
| **Deliverable** | Outcome statement attached to a phase (ordered list). | `PhaseDeliverable`, `tb_phase_deliverables`; UI **Entregável** |
| **Deliverable version** | Version associated with a phase as its delivery target. | `Phase.deliverableVersion` |
| **Version** | SemVer release label scoped to a project (e.g. `1.2.0`). | `Version`, `tb_versions`; UI **Versão** |
| **Version changelog** | Derived release view listing tickets associated with a version, in grouped sections, sorted by finish date. | `VersionChangelogResponse`; UI **Registro de alterações da versão** |
| **Changelog association** | Why a ticket appears on a version changelog: `TARGET`, `OBSERVED`, or `PHASE_DELIVERABLE`. | `ChangelogAssociation` enum |
| **Assignable phase** | Phase eligible for ticket assignment: `PLANNED` or `ACTIVE` only. | Validation on create/update ticket |
| **Unplanned ticket** | Ticket with no phase assignment. | `Ticket.phase` null |

### Tickets

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Ticket** | A change/work item within a project. | `Ticket`, `tb_tickets` |
| **Ticket type** | Role of the ticket in planning: `EPIC`, `STORY`, or `TASK` (default). | `TicketType`; `tb_tickets.ticket_type`; UI Épico / História / Tarefa |
| **Epic** | Feature-level ticket (`type=EPIC`) that groups child tickets; delivery work is on children; may span many **phases**. Distinct from **Phase** and **Category**. | Hierarchy via `CHILD_OF` links |
| **Identifier** | Human-readable ticket key: `{project.prefix}-{seq}` (e.g. `ISS-003`). | `Ticket.identifier`, URL `/ticket/:ticketIdentifier` |
| **Title** | Short summary of the ticket. | `Ticket.title` |
| **Description** | Longer explanation of the work; may contain rich-text HTML from the shared editor. Plain-text length max **1200**. | `Ticket.description` |
| **Category** | Classification label with display color (e.g. Feature, Bug). Tickets reference category **by id** (`category_id`). | `Category`, `tb_categories` |
| **Assignee** | User responsible for the ticket (optional). | `Ticket.assignee` |
| **Author** | User who created the ticket. | `Ticket.author` |
| **Current status** | Ticket's position in the project workflow. | `Ticket.status` → `WorkflowStatus` |
| **Priority** | Ticket urgency level. | `TicketPriority` enum: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`; `Ticket.priority` |
| **Backlog** | Project-scoped ordered list of non-deleted, non-done tickets for planning “what’s next.” | UI `/project/:projectId/backlog`; [feature/ticket-backlog.md](../feature/ticket-backlog.md) |
| **Backlog rank** | Relative position of a ticket in the project backlog (lower = higher in the list). Distinct from **Priority**. | `Ticket.backlogRank`; `tb_tickets.backlog_rank` |
| **Reorder (backlog)** | Change a ticket’s backlog rank relative to peers; project-manager and admin only. | `POST /projects/{id}/backlog/reorder` |
| **Soft delete** | Ticket marked deleted without physical removal; may be **restored** by admin/PM. | `Ticket.deleted`; excluded from search until restored |
| **Move (ticket)** | Change ticket status following workflow transition rules. | `POST /tickets/{id}/move`, `MoveTicketRequest` |
| **Phase (on ticket)** | Optional assignment of a ticket to a project phase. | `Ticket.phase`; UI **Fase** |
| **Observed version** | Version where the change was observed or shipped. | `Ticket.observedVersion`; UI **Versão observada** |
| **Target version** | Version where the change is intended to land. | `Ticket.targetVersion`; UI **Versão alvo** |
| **Finish date** | Timestamp set when ticket reaches a **done** finish status; cleared when leaving done. | `Ticket.finishedAt`; UI **Data de conclusão** |
| **Story points** | Optional non-negative integer estimate of ticket size/effort; null means unset (burndown warns and treats as 0 until set). | `Ticket.storyPoints` |
| **Due date** | Optional user-planned deadline for the ticket; distinct from finish date. | `Ticket.dueDate`; UI **Data de vencimento** |
| **Comment** | Text note attached to a ticket; may be marked **agent channel** (`via_agent`) when created via API token. | `Comment`, `tb_comments` |
| **Ticket history** | Immutable structured audit log of non-comment actions on a ticket (`action`, `field`, `oldValue`, `newValue`); may be marked **agent channel** (`via_agent`) when mutated via API token. | `TicketHistory`, `TicketHistoryService` |
| **Ticket history action** | Typed event: `CREATED`, `FIELD_CHANGED`, `STATUS_CHANGED`, `ASSIGNEE_CHANGED`, `SUBSCRIBED`, `UNSUBSCRIBED`, `DELETED`, `RESTORED`, `LINK_ADDED`, `LINK_REMOVED`. | `TicketHistoryAction` |
| **Linked commit** | Immutable record that a git commit (SHA) mentioned a ticket identifier; shown on the **activity feed**. Author may match an Issues **user** by email or remain unmatched (name/email from SCM). | Planned — `tb_ticket_commits`; [feature/git-integration.md](../feature/git-integration.md) |
| **Commit ingest** | Delivery of commits into Issues via forge **push webhook** (HMAC) and/or authenticated **inbound API** (PAT or project service account). | Planned — `POST …/git/webhook`, `POST …/git/commits` |
| **Activity feed** | Unified chronological UI on ticket detail merging comments, history events, and **linked commits**. | Ticket detail **Atividade** / Histórico section |
| **Subscriber** | User watching a ticket; receives notifications on changes. | `Ticket.subscribers`, M:N `tb_tickets_subscribers` |
| **Subscribe** | Add a user to ticket subscribers. | `PUT /tickets/{id}/subscribe` |
| **Unsubscribe** | Remove a subscriber from a ticket. | `DELETE /tickets/{id}/subscribe/{subscriberId}` |
| **Ticket link** | Directed association between two tickets with a **link type**; may be **cross-project**. | `TicketLink`, `tb_ticket_links`; UI **Vínculos** |
| **Link type** | Fixed kind: `BLOCKS`, `RELATES_TO`, `DUPLICATES`, `DERIVED_FROM`, `REMAINING_WORK_OF`, `CHILD_OF`. | `TicketLinkType` |
| **Child ticket** / **Subtask** | Ticket linked to an **Epic** via `CHILD_OF` (source=child, target=Epic). | UI **Subtarefas** |
| **CSV import** | Bulk creation of tickets from a CSV file. May be **project-scoped** (fixed project) or **global** (project resolved per row from a mapped column). | `POST /projects/{projectId}/tickets/import/upload` or `POST /tickets/import/upload`; UI at `/project/:projectId/tickets/import` or `/tickets/import` |
| **Column mapping** | User-defined association between CSV header names and ticket fields (title, description, category, priority, assignee, status, optionally project, and **custom fields** by **key** when in scope). | `ColumnMapping` / `customFieldColumns`; import wizard step 2 |
| **Import row** | One CSV data row after column mapping, validated and stored before ticket creation. | `TicketImportRow`, `tb_ticket_import_rows` |
| **Ticket import batch** | Server-side persisted CSV upload with parsed rows awaiting mapping and execution. | `TicketImport`, `tb_ticket_imports` |
| **Custom field value** | The value stored on a **ticket** for one **custom field** (writable when in-scope; orphan former-workflow values retained read-only until cleared). | `TicketCustomFieldValue`, `tb_ticket_custom_field_values` |

### Notifications & real-time

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Notification** | Alert persisted for a subscriber about a ticket event. | `Notification`, `tb_notifications` |
| **Notification channel** | SSE stream registered by the client. | `GET /notifications/register` |
| **Unread count** | Number of unread notifications for the current user; drives the header badge (display capped at `99+`). | `GET /notifications/unread-count` |
| **Mark as read** | User acknowledges a single notification. | `POST /notifications/{id}/read` |
| **Mark all as read** | User acknowledges all of their unread notifications at once. | `POST /notifications/read-all` |
| **Ticket change email** | Transactional email when a subscribed ticket changes. | `MailerService`, Qute template `notifyTicketChange.html` |

### Analytics & views

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Kanban** | Board view grouping tickets by workflow status columns. | `/project/:projectId/kanban` |
| **Swimlane** | Optional Kanban row grouping by **assignee** or **priority** (toolbar **Agrupar por**; default none). View-only — not persisted. | Kanban toolbar |
| **WIP limit** | Optional maximum non-deleted tickets allowed in a **workflow status** for a given workflow; shared by all projects using that workflow. | `tb_workflow_wip_limits`; workflow form; Kanban `n/limit` |
| **Dashboard** | Project analytics page with charts and KPIs. | `/project/:projectId/dashboard` |
| **Dashboard widget** | Chart, table, or KPI visualization. | `DashboardType` enum |
| **Dashboard layout** | Ordered set of widgets for a user on a project dashboard; persisted server-side (not browser `localStorage`). | `tb_dashboard_layouts`; `GET/PUT …/dashboard/layout` |
| **Tickets by day** | Bar chart of ticket creation counts by day (UTC buckets). | `tickets-by-day`; [project-dashboard.md](../feature/project-dashboard.md) **FQ7** |
| **Tickets by status** (dashboard) | Pie of open (non-deleted) tickets by workflow status. | `tickets-by-status` |
| **Tickets by priority** | Pie of tickets by priority. | `tickets-by-priority` |
| **Recent tickets** | Table of the **20** most recently updated non-deleted tickets in the project. | `recent-tickets` |
| **Tickets por status** (KPI widget) | KPI-style summary: total non-deleted tickets + counts per status (widget id `performance-kpi`). | `performance-kpi`; **FQ8** — not throughput |
| **Burndown** | Project view of remaining **story points** for a **phase** vs an **ideal line** over the phase date range. Peer route of Kanban. | `/project/:projectId/burndown`; [feature/burndown.md](../feature/burndown.md) |
| **Story points** | Optional non-negative integer size/effort estimate on a ticket; used by burndown. | `Ticket.storyPoints`; UI **Story points** |
| **Ideal line** | Linear projection from remaining points at phase start date to zero at phase end date. | Burndown chart |
| **Canceled at** | Timestamp set when ticket reaches a **CANCELED** finish status; cleared when leaving canceled; used as burn day for burndown. | `Ticket.canceledAt` |
| **Search** | Full-text ticket search across projects. | `/search`, `GET /tickets/search` |
| **Query language** | Issues-native **plain text** search syntax, parsed with **ANTLR**; inspired by Jira JQL — **not JQL-compatible**; field predicates over tickets, comments, and **custom fields** via `cf.<key>`. | `POST /tickets/search/query`; `TicketQuery.g4` |
| **Saved query** | Named, persisted query text owned by a user; shareable via stable URL slug; optional **show at home** flag. | `tb_saved_queries`; `/search/q/:slug`, `/search/queries` |
| **Show at home** (saved query) | When enabled on edit, owned saved query renders as a ticket table section on the home screen. | Home `/` |
| **Clone saved query** | Non-owner copies another user's saved query into a new owned query (required before edit). | `POST …/saved-queries/{id}/clone` |
| **Home screen** | Post-login hub at `/` with **Tickets atuais**, **Tickets atribuídos**, **Atividade**, and owned **saved query** sections. | `home.component`; not the project picker |
| **Tickets atuais** (home) | All open (non-finished) tickets in the user's scope: member projects, or all projects for admin/project-manager. | Home section |
| **Tickets atribuídos** (home) | Open tickets in scope where the current user is **assignee**. | Home section |
| **Home activity** | Static snapshot of recent comments and status changes on tickets in scope; loaded once per visit (no SSE). | Home **Atividade** section |

---

## Business rules (invariants)

1. **Ticket identifier** — Auto-generated on create as `{project.prefix}-{zeroPaddedSeq}`; unique per project.
2. **Workflow enforcement** — `moveTicket` must validate that a transition exists in the project's workflow from current status to target status.
3. **Soft delete** — Deleted tickets are excluded from search and list queries; only admin and project-manager may delete. **Restore** — admin/PM may restore a soft-deleted ticket; it reappears in lists and search.
4. **History** — Create, field changes, assign, move, subscribe/unsubscribe, and delete actions are logged via `TicketHistoryService` as structured events. Comments appear in the activity feed only (not duplicated in history).
5. **Notifications** — Fired asynchronously on status move; delivered to ticket subscribers via SSE and optionally email.
6. **Roles** — Endpoint access enforced via `@RolesAllowed`; class-level `@DenyAll` on protected resources.
7. **Request/Response contract** — HTTP body types are records named `*Request` / `*Response` (ArchUnit enforced).
8. **Ticket template** — At most one template per project (embedded on `Project`). When enabled, only **configured** template fields pre-fill the create form; the user may submit without filling unconfigured template fields. Required ticket fields still follow `CreateTicketRequest` validation. Template may include **custom field** defaults for in-scope fields ([feature/custom-fields.md](../feature/custom-fields.md) **FQ5**).
9. **Project description** — Required on create and update (`CreateProjectRequest.description` must not be blank); may contain rich-text HTML from the shared editor (no separate plain-text max beyond non-blank).
10. **CSV import** — CSV parsed on the server (OpenCSV); upload and rows stored in `tb_ticket_imports` / `tb_ticket_import_rows` before mapping and execution. Upload is **chunked** (**init / part / complete**): max **5 MB** total, max **1 MB** per chunk, max **500** rows ([feature/ticket-import.md](../feature/ticket-import.md) **FQ1**, **FQ3**, **AQ1**). **Project-scoped** imports fix `project_id` on the batch; **global** imports leave `project_id` null and require a **project column** mapping — each row's project is resolved by name (case-insensitive). Author is the importing user; identifiers are always auto-generated (never read from CSV). Category resolved by name; assignee by email; status by workflow status name within the row's project workflow. Optional priority defaults to `MEDIUM`. Optional **story points** column maps to built-in `storyPoints` (≥ 0). Partial import (**FQ2**): valid rows are created; invalid rows are reported per row without rolling back siblings and may be **corrected in preview** (project, status, assignee, …) before re-validation / execute. Status on import: ticket is created at workflow start; if a different status is mapped, a direct transition from start to that status must exist (multi-hop paths are not supported).
11. **One active phase per project** — activating phase B **completes** the previously active phase; never two `ACTIVE` phases in the same project.
12. **Phase–ticket project match** — a ticket's phase must belong to the ticket's project.
13. **Assignable phases** — tickets may be assigned only to phases in `PLANNED` or `ACTIVE` status.
14. **Phases editable after completed** — completed phases may be updated (name, objective, deliverables, deliverable version, dates).
15. **Phase activation and status** — if workflow defines a **phase start status**, activating a phase moves each assigned ticket there **only when a valid transition exists**; other tickets keep their status; activation is never blocked by failed moves.
16. **Version labels SemVer** — version labels must be valid SemVer; unique per project.
17. **Version scope** — observed, target, and deliverable version references must belong to the ticket's or phase's project.
18. **Finish statuses** — each workflow defines finish statuses tagged `DONE` or `CANCELED`.
19. **Finish date** — moving to a `DONE` finish status sets `finished_at`; moving out of a `DONE` finish status clears `finished_at`. `CANCELED` does not set `finished_at` (uses `canceled_at` instead — see 19a). Mutual exclusivity: entering DONE clears `canceled_at`; entering CANCELED clears `finished_at`.
19a. **Canceled at** — moving to a `CANCELED` finish status sets `canceled_at`; moving out of `CANCELED` clears `canceled_at`. Used as burndown burn day for canceled tickets.
19b. **Story points** — optional non-negative integer on a ticket; null means unset. Burndown warns on unset in-scope tickets and treats them as 0 until set; setting points increases remaining (scope add).
19c. **Burndown** — phase-scoped remaining story points vs ideal line over phase start/end; chart disabled (not hidden) when dates incomplete; peer route of Kanban.
20. **Version changelog** — derived, not persisted separately. Includes non-canceled tickets linked by target version, observed version, or phase deliverable version. Excludes tickets in a `CANCELED` finish status; they reappear when moved out of canceled. Sorted by finish date ascending (nulls last) within grouped sections.
21. **Ticket create — no default phase** — new tickets have no phase unless the user selects one from **planned** and **active** phases in the create form.
22. **Phase/version history** — changes to phase, observed version, target version, due date, and finish date on tickets are logged via `TicketHistoryService`.
23. **Phase/version admin roles** — phase and version CRUD: `PROJECT_MANAGER` and `ADMIN`; version changelog **read** follows **project security level** (same as other gated surfaces).
24. **Project membership** — a user must be a **project member** to be set as ticket **assignee** on that project.
25. **Project owner** — each project has exactly one **project owner** with the project-manager role, assigned at creation. Only the project owner or an **admin** may update the project, manage allocation, or change project configuration. **Owner transfer:** admin or the current project owner may assign a new owner on edit; the new owner must have the project-manager role and need not already be a **project member** — they are added as a member when the transfer completes.
26. **Member removal** — a **project member** cannot be removed while they are **assignee** on a non-finished ticket in that project; tickets must be reassigned first.
27. **Home scope** — `user` role: home lists and activity include **member** projects only. **Project owner:** owned projects. **Admin:** all projects.
28. **Project hub access** — hub, Kanban, Burndown, backlog, dashboard, phases, and versions are **readable** according to the project’s **security level**. Project edit, allocation, and security-level change require **project owner** or **admin**.
29. **Project list (viewable)** — `GET /projects` / header **Projetos**: **admin** sees all; authenticated non-admin sees owned + member projects **plus** all **Internal** and **Public** projects; **Private** only if member/owner (or admin). Anonymous: **Public** projects only (architecture **AQ5**).
30. **Ticket search** — authenticated search results include only tickets the caller may **read** under each project’s **security level**. Soft-deleted tickets remain admin/PM-only. Anonymous global search is out of scope for v1.
30a. **Project security level** — every project has exactly one level: **Private** / **Internal** / **Public**. It gates **read** on tickets (including comments and history for allowed readers), Kanban, versions, hub, phases, backlog, burndown, dashboard, and filtered search. **Default** on create and migrate: **Internal**. **Writes** require membership (or manage); never anonymous.
31. **Saved query ownership** — each saved query has exactly one **owner**; only the owner may update or delete. Non-owners must **clone** another user's query before editing.
32. **Show at home** — optional per saved query (`show_at_home`); when enabled, the owner's query appears as a home section (one section per flagged query; snapshot per visit).
33. **Query language** — plain text query is parsed server-side with **ANTLR**; invalid syntax returns a validation error; soft-deleted tickets are excluded; global scope with optional project filter. Built-in fields include `points` / `storypoints` for story points.
34. **Search indexing** — PostgreSQL **`tsvector` + GIN** indexes on ticket and comment text columns (`search_vector`).
35. **Due date** — optional user-planned deadline on a ticket (`due_date`); independent of workflow **finish date** (`finished_at`).
36. **Project prefix immutability** — once a project has at least one ticket (including soft-deleted), its **prefix** cannot change.
37. **Category delete** — a category cannot be deleted while any ticket references it (`category_id` FK, including soft-deleted tickets), or while any project **ticket template** references it (`ticket_template_category_id`).
38. **Kanban drag validation** — client blocks drag/drop to columns with no valid workflow transition; server remains authoritative on `moveTicket`.
50. **WIP limit** — optional per workflow×status (`tb_workflow_wip_limits`); null/absent = unlimited. Count is non-deleted tickets in that status for the **project**. `moveTicket` into a status at or over its limit is rejected (400); client also blocks the drop. Ticket **create** and CSV **import** do not enforce WIP in the current product scope.
51. **Kanban swimlanes** — optional toolbar grouping by assignee or priority (default none); preference is not persisted server-side.
72. **Backlog membership** — project backlog lists non-deleted tickets that are not in a **DONE** finish status (`finished_at` is null). Soft-deleted and done tickets are excluded; canceled (no finish date) remain eligible.
73. **Backlog rank** — every ticket has a project-scoped integer `backlog_rank`. New tickets (create / CSV import) receive `max(rank)+1` in the project (append at end). Rank is independent of **Priority**.
74. **Backlog reorder** — only `PROJECT_MANAGER` and `ADMIN` may reorder; other authenticated users with project access may view. Reorder is relative (`beforeTicketId` or end); changes are audited as `FIELD_CHANGED` / `backlogRank`.
75. **Backlog pagination** — backlog list is paginated (default page size 20); UI uses infinite scroll.
39. **User removal** — a user cannot be deleted while they are **assignee** on tickets whose status is not workflow **start**, **done**, or **canceled** finish status.
40. **Self-registration** — new users may register via a public registration flow (default role `user`).
41. **Account profile** — authenticated users may update their own name, email, and **locale preference** (`pt` \| `en`) on account settings. **Locale preference** is seeded from the browser / active SPA locale on **register** and on **first provisioning** (LDAP/ENDPOINT user create); users may change it later in account settings. API errors and emails are not localized with the UI locale in v1. User-authored content (ticket fields, custom field labels, workflow status names, etc.) is not auto-translated.
42. **Notifications pagination** — notification list uses **infinite scroll** with paginated API; SSE client **auto-reconnects** after network drop.
63. **Unread badge** — header badge shows the server **unread count** for the current user (not only loaded list pages); display as `99+` when unread > 99; hidden when 0.
64. **Mark all as read** — marks every unread notification for the authenticated user only; after success the client reloads the first list page and unread count.
65. **Ticket type** — every ticket has type `EPIC`, `STORY`, or `TASK` (default `TASK`).
69. **Epic hierarchy** — only an **Epic** may be the parent of a `CHILD_OF` link; each child has at most one parent; depth is one (Epic → children); cycles are rejected.
70. **Ticket links** — peer and hierarchy links may connect tickets in **different projects**; the user must be able to view both ends; creating a link requires update rights on the source ticket’s project context.
71. **Epic finish with open children** — server allows moving an Epic to a done finish status while children are open; UI warns.
43. **Refresh token** — opaque server-side token in `tb_refresh_tokens`; issued on login; rotated on `POST /auth/refresh`; revoked on password change or reset. Access JWT TTL configured via `auth.access-token-minutes` (default 15 min).
44. **Dashboard layout** — one layout per user per project, stored server-side; browser `localStorage` layouts are not migrated.
45. **Recent tickets widget** — shows at most **20** non-deleted tickets ordered by `updated_at` descending.
46. **Single auth provider** — exactly one of LOCAL, LDAP, or ENDPOINT is active per deployment (`AUTH_PROVIDER`); login UI does not offer provider choice.
47. **External user provisioning** — LDAP/ENDPOINT success auto-creates a local **User** when none exists for the email; LDAP re-syncs roles from group→role map on every login; ENDPOINT assigns **USER** on create and does not overwrite roles later.
48. **Local password ops** — password recovery and change-password are allowed only when the active provider is LOCAL; non-LOCAL users may have a null `encoded_password`.
49. **Password policy** — local passwords (registration, change-password, reset confirm) must be 8–64 characters and include at least one uppercase letter, one lowercase letter, and one digit.
52. **Custom field definition before value** — a ticket may only store a writable **custom field value** for an **in-scope** (enabled) custom field. Orphan values from a former workflow are retained read-only until cleared.
53. **Custom field type validation** — values must match the field’s type (string ≤ min(configured, 255); text ≤ 1200; integer min/max; boolean; enum ∈ options). Key and type are immutable after create.
54. **Custom field required** — globally required fields enforced on create and update. **Status-required** workflow fields enforced on move into listed statuses and on create when start status is listed. Globally required workflow fields imply all statuses.
55. **Custom field key uniqueness** — keys unique per owner; for a project, project ∪ workflow keys must not collide (enforced on definition create and on project workflow change).
56. **Enum option in use** — an **enum option** cannot be removed while any ticket stores that option.
57. **Custom field delete** — hard delete blocked while any ticket has a value (including soft-deleted tickets); soft-disable (`enabled=false`) hides the field from forms and keeps values.
58. **Custom fields in import** — CSV **column mapping** maps headers to custom fields by **key**; row must resolve to a project that has that key in scope.
59. **Custom fields in query language** — predicates use `cf.<key>` with type-appropriate operators.
60. **Custom field history** — value changes logged as `FIELD_CHANGED` with `field` = custom field **key**.
61. **Custom field roles** — project definitions: project owner or admin; workflow definitions: project-manager or admin; values: same roles as ticket create/update.
62. **Custom field notifications** — custom field value changes do not trigger notifications or email in the current product scope.
66. **Personal API token** — secret shown once at create; stored hashed only; revoke disables Bearer auth immediately. Prefix `iss_pat_`. ([agentic-integration.md](../feature/agentic-integration.md).)
67. **Agent channel attribution** — mutations via API token persist `via_agent` and are shown as **Agente em nome de &lt;nome&gt;** (PAT: user name; service account: SA display name).
68. **Service account scope** — a service account belongs to exactly one project; tokens authorize **member-aligned** powers on that project only. Managed at `/projects/:projectId/service-accounts`. Prefix `iss_sat_`.
69. **Bearer auth** — `/api` accepts JWT session tokens or API tokens (`iss_pat_` / `iss_sat_`) on the same `Authorization: Bearer` header; API-token callers have the full update powers of the principal (user or project-scoped SA).
76. **Issues MCP deployment** — MCP is **external to Issues core**: a separate Quarkus project calling Issues HTTP APIs only (no shared persistence). The Issues repo may later become a **multi-module** reactor that includes MCP.
77. **Git repository association** — at most one remote per project in current scope; configurable by project owner or admin only. (Planned — [git-integration.md](../feature/git-integration.md).)
78. **Linked commit** — commits mentioning `{prefix}-{seq}` (subject or body) link to non-deleted tickets in the associated project; idempotent on `(ticket_id, sha)`; no subscriber notification and no auto-transition on link. (Planned.)
79. **Commit ingest auth** — forge push webhook verified with per-project HMAC secret; inbound API uses Bearer personal API token or project service account. (Planned.)
80. **Workflow status edit** — after create, statuses may be added, renamed (workflow-local detach/attach), or removed. Removing a status with tickets requires a replacement; remaps include soft-deleted tickets; orphan transitions and workflow CF status-required links to that status are dropped; remap writes history and does not notify.

---

## Aggregates (summary)

| Aggregate | Root | Consistency boundary |
|-----------|------|---------------------|
| User | `User` | Credentials, roles, profile, **personal API tokens** |
| Project | `Project` | Name, prefix, workflow assignment, phase template, **owner**, **members**, ticket template (incl. custom field defaults), **service accounts** |
| Personal API token | `ApiToken` | Hash, prefix, revoke, last-used; owned by one user |
| Service account | `ServiceAccount` | Display name, active flag, tokens; owned by one project |
| Workflow | `Workflow` | Statuses, transitions, phase start status, finish statuses |
| Custom field | `CustomField` | Key, label, type, required, enabled, owner (project XOR workflow), enum options, status-required links |
| Phase | `Phase` | Lifecycle, objective, deliverables, deliverable version |
| Version | `Version` | SemVer label, changelog (derived) |
| Ticket | `Ticket` | Title, status, assignee, phase, versions, finish date, subscribers, comments, **ticket type**, **custom field values**, **ticket links**, **linked commits** |
| Git repository association | Project git remote | One remote per project, webhook secret, ingest config |
| Linked commit | Ticket ↔ commit SHA | Idempotent `(ticket_id, sha)` |
| Notification | `Notification` | Read state per user per event |

---

## Mapping to code

| Domain term | Primary types |
|-------------|---------------|
| Ticket | `ticket.Ticket`, `ticket.TicketService` |
| Ticket type | `ticket.TicketType` |
| Ticket link | `ticket.link.TicketLink`, `ticket.link.TicketLinkService` |
| Workflow | `workflow.Workflow`, `workflow.WorkflowService` |
| Project | `project.Project`, `project.ProjectService` |
| Phase | `phase.Phase`, `phase.PhaseService` |
| Version | `phase.Version`, `phase.VersionService` |
| Notification | `notifications.Notification`, `notifications.NotificationsEndpoint` |
| Audit | `ticket.history.TicketHistoryService` |
| Personal API token | `auth.apitoken.ApiToken`, `ApiTokenService` |
| Service account | `project.serviceaccount.ServiceAccount`, `ServiceAccountService` |
| Ticket context | `ticket.context.TicketContextService` |
| Custom field | `customfield.CustomField`, `customfield.CustomFieldService` |
| Git repository association / linked commit | Planned — `git.*` ([feature/git-integration.md](../feature/git-integration.md)) |
| Issues MCP | External — [`issues-mcp/`](../issues-mcp/) (not a core package) |

---

## Checklist for changes

Before merging a domain-affecting change:

- [ ] Ubiquitous Language updated if new terms or rule changes
- [ ] Bounded context placement verified (package dependency direction)
- [ ] [ARCHITECTURE.md](../ARCHITECTURE.md) updated if structure or API surface changes
- [ ] [feature-catalog.md](feature-catalog.md) updated if routes or UI flows change
- [ ] Tests use domain vocabulary in method and variable names

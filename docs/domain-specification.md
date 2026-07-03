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
    User ||--o{ Notification : receives
```

---

## Bounded contexts

Issues is a **modular monolith**: one deployable, feature packages under `dev.vepo.issues.*`. Contexts communicate via CDI events, shared entities referenced by ID, and service calls at documented boundaries — not by reaching into another context's repositories from unrelated endpoints.

| Context | Packages | May depend on |
|---------|----------|---------------|
| **Platform** | `infra` | JDK/Jakarta only |
| **Identity & access** | `auth`, `user` | platform |
| **Project administration** | `project` | platform, identity, workflow |
| **Workflow configuration** | `workflow` | platform |
| **Phase & version planning** | `phase` | platform, project, workflow |
| **Ticket management** | `ticket`, `ticket.comments`, `ticket.history`, `ticket.business` | platform, identity, project, workflow, categories, phase |
| **Classification** | `categories` | platform |
| **Notifications** | `notifications` | platform, identity, ticket |
| **Analytics** | `dashboards` | platform, project, ticket, workflow |
| **Email** | `mailer` | platform, identity, ticket |

**Rules:**

- Feature packages must not depend on unrelated contexts (e.g. `categories` must not depend on `dashboards`).
- Cross-context reactions (notify subscribers on ticket move) use CDI events or dedicated services in the owning context.
- `infra` holds exception mappers, SPA routing, and dev setup — no domain logic.

---

## Ubiquitous Language

Terms below are the **only** approved names for aggregates, entities, states, actions, and user-visible labels unless this document is updated first.

### Platform & people

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Issues** | The product (change/ticket management). | UI title, email templates |
| **User** | Registered account with name, email, password, roles. | `User`, `tb_users` |
| **Role** | Platform capability assigned to a user (multi-role). | `Role` enum |
| **User** (role) | Default role; create and work on tickets. | `Role.USER` — label: "User" |
| **Administrator** | Full user management. | `Role.ADMIN` — label: "Admin" |
| **Project manager** | Create projects and workflows; soft-delete tickets. | `Role.PROJECT_MANAGER` |
| **Session** | Authenticated state via JWT Bearer token. | `AuthenticationEndpoint`, Angular `auth.service` |
| **Password recovery** | Self-service flow to reset password via email link. | `AuthenticationEndpoint` `/auth/recovery` |
| **Password reset token** | Single-use secret sent by email. | `PasswordResetToken` |

### Projects & workflows

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Project** | Bounded scope for tickets: name, prefix, required description, assigned workflow. | `Project`, `tb_projects` |
| **Project prefix** | Short uppercase code used in ticket identifiers (e.g. `ISS`). | `Project.prefix` |
| **Workflow** | Named state machine: start status, allowed statuses, transitions. | `Workflow`, `tb_workflows` |
| **Status** | Named step in a workflow (e.g. TODO, IN_PROGRESS, DONE). | `WorkflowStatus`, `tb_workflow_status` |
| **Transition** | Allowed move from one status to another within a workflow. | `WorkflowTransition` |
| **Start status** | Required initial status for every new ticket in a workflow. | `Workflow.start` / `start_id` |
| **Phase start status** | Optional status on a workflow; when a **phase** is **activated**, each assigned ticket moves here if a valid transition exists. | `Workflow.phaseStart`; UI **Status inicial da fase** |
| **Finish status** | Workflow status marked as terminal with outcome **done** or **canceled**. | `WorkflowFinishStatus`, `tb_workflow_finish_statuses` |
| **Finish outcome** | Classification of a finish status: `DONE` or `CANCELED`. | `FinishOutcome` enum |
| **Ticket template** | Optional default field values for new tickets in a project: title, description, category, priority. | Embedded on `Project`, `tb_projects` template columns |
| **Template enabled** | Project manager opted in; when true, all template fields are required and validated like `CreateTicketRequest`. | `Project.ticketTemplateEnabled`; UI checkbox **Usar template de ticket** |
| **Phase template objective** | Default plain-text **objective** copied into each new phase for the project. | `Project.phaseTemplateObjective` |
| **Phase template deliverable** | Default **deliverable** row copied into each new phase for the project. | `tb_project_phase_deliverable_templates` |

### Phases & versions

Methodology-neutral planning terms. UI labels in PT-BR until i18n.

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
| **Identifier** | Human-readable ticket key: `{project.prefix}-{seq}` (e.g. `ISS-003`). | `Ticket.identifier`, URL `/ticket/:ticketIdentifier` |
| **Title** | Short summary of the ticket. | `Ticket.title` |
| **Description** | Longer explanation of the work. | `Ticket.description` |
| **Category** | Classification label with display color (e.g. Feature, Bug). | `Category`, `tb_categories` |
| **Assignee** | User responsible for the ticket (optional). | `Ticket.assignee` |
| **Author** | User who created the ticket. | `Ticket.author` |
| **Current status** | Ticket's position in the project workflow. | `Ticket.status` → `WorkflowStatus` |
| **Priority** | Ticket urgency level. | `TicketPriority` enum: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`; `Ticket.priority` |
| **Soft delete** | Ticket marked deleted without physical removal. | `Ticket.deleted`; excluded from search |
| **Move (ticket)** | Change ticket status following workflow transition rules. | `POST /tickets/{id}/move`, `MoveTicketRequest` |
| **Phase (on ticket)** | Optional assignment of a ticket to a project phase. | `Ticket.phase`; UI **Fase** |
| **Observed version** | Version where the change was observed or shipped. | `Ticket.observedVersion`; UI **Versão observada** |
| **Target version** | Version where the change is intended to land. | `Ticket.targetVersion`; UI **Versão alvo** |
| **Finish date** | Timestamp set when ticket reaches a **done** finish status; cleared when leaving done. | `Ticket.finishedAt`; UI **Data de conclusão** |
| **Comment** | Text note attached to a ticket. | `Comment`, `tb_comments` |
| **Ticket history** | Immutable structured audit log of non-comment actions on a ticket (`action`, `field`, `oldValue`, `newValue`). | `TicketHistory`, `TicketHistoryService` |
| **Ticket history action** | Typed event: `CREATED`, `FIELD_CHANGED`, `STATUS_CHANGED`, `ASSIGNEE_CHANGED`, `SUBSCRIBED`, `UNSUBSCRIBED`, `DELETED`, `RESTORED`. | `TicketHistoryAction` |
| **Activity feed** | Unified chronological UI on ticket detail merging comments and history events. | Ticket detail **Atividade** section |
| **Subscriber** | User watching a ticket; receives notifications on changes. | `Ticket.subscribers`, M:N `tb_tickets_subscribers` |
| **Subscribe** | Add a user to ticket subscribers. | `PUT /tickets/{id}/subscribe` |
| **Unsubscribe** | Remove a subscriber from a ticket. | `DELETE /tickets/{id}/subscribe/{subscriberId}` |
| **CSV import** | Bulk creation of tickets from a CSV file. May be **project-scoped** (fixed project) or **global** (project resolved per row from a mapped column). | `POST /projects/{projectId}/tickets/import/upload` or `POST /tickets/import/upload`; UI at `/project/:projectId/tickets/import` or `/tickets/import` |
| **Column mapping** | User-defined association between CSV header names and ticket fields (title, description, category, priority, assignee, status, and optionally project). | `ColumnMapping`, import wizard step 2 |
| **Import row** | One CSV data row after column mapping, validated and stored before ticket creation. | `TicketImportRow`, `tb_ticket_import_rows` |
| **Ticket import batch** | Server-side persisted CSV upload with parsed rows awaiting mapping and execution. | `TicketImport`, `tb_ticket_imports` |

### Notifications & real-time

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Notification** | Alert persisted for a subscriber about a ticket event. | `Notification`, `tb_notifications` |
| **Notification channel** | SSE stream registered by the client. | `GET /notifications/register` |
| **Mark as read** | User acknowledges a notification. | `POST /notifications/{id}/read` |
| **Ticket change email** | Transactional email when a subscribed ticket changes. | `MailerService`, Qute template `notifyTicketChange.html` |

### Analytics & views

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Kanban** | Board view grouping tickets by workflow status columns. | `/project/:projectId/kanban` |
| **Dashboard** | Project analytics page with charts and KPIs. | `/project/:projectId/dashboard` |
| **Dashboard widget** | Chart, table, or KPI visualization. | `DashboardType` enum |
| **Tickets by day** | Pie chart of ticket creation over time. | `tickets-by-day` |
| **Tickets by status** | Pie chart of tickets grouped by status. | `tickets-by-status` |
| **Tickets by priority** | Pie chart of tickets grouped by priority. | `tickets-by-priority` |
| **Recent tickets** | Table of latest tickets. | `recent-tickets` |
| **Performance KPI** | Summary metrics for project throughput. | `performance-kpi` |
| **Search** | Full-text ticket search across projects. | `/search`, `GET /tickets/search` |

---

## Business rules (invariants)

1. **Ticket identifier** — Auto-generated on create as `{project.prefix}-{zeroPaddedSeq}`; unique per project.
2. **Workflow enforcement** — `moveTicket` must validate that a transition exists in the project's workflow from current status to target status.
3. **Soft delete** — Deleted tickets are excluded from search and list queries; only admin and project-manager may delete.
4. **History** — Create, field changes, assign, move, subscribe/unsubscribe, and delete actions are logged via `TicketHistoryService` as structured events. Comments appear in the activity feed only (not duplicated in history).
5. **Notifications** — Fired asynchronously on status move; delivered to ticket subscribers via SSE and optionally email.
6. **Roles** — Endpoint access enforced via `@RolesAllowed`; class-level `@DenyAll` on protected resources.
7. **Request/Response contract** — HTTP body types are records named `*Request` / `*Response` (ArchUnit enforced).
8. **Ticket template** — At most one template per project (embedded on `Project`). When enabled, title, description, category, and priority must satisfy the same constraints as `CreateTicketRequest`. The create-ticket UI pre-fills the form from the template; the user may edit before submit.
9. **Project description** — Required on create and update (`CreateProjectRequest.description` must not be blank).
10. **CSV import** — CSV parsed on the server (OpenCSV); upload and rows stored in `tb_ticket_imports` / `tb_ticket_import_rows` before mapping and execution. **Project-scoped** imports fix `project_id` on the batch; **global** imports leave `project_id` null and require a **project column** mapping — each row's project is resolved by name (case-insensitive). Author is the importing user; identifiers are always auto-generated (never read from CSV). Category resolved by name; assignee by email; status by workflow status name within the row's project workflow. Optional priority defaults to `MEDIUM`. Partial import: valid rows are created; invalid rows are reported per row without rolling back siblings. Status on import: ticket is created at workflow start; if a different status is mapped, a direct transition from start to that status must exist (multi-hop paths are not supported).
11. **One active phase per project** — activating phase B **completes** the previously active phase; never two `ACTIVE` phases in the same project.
12. **Phase–ticket project match** — a ticket's phase must belong to the ticket's project.
13. **Assignable phases** — tickets may be assigned only to phases in `PLANNED` or `ACTIVE` status.
14. **Phases editable after completed** — completed phases may be updated (name, objective, deliverables, deliverable version, dates).
15. **Phase activation and status** — if workflow defines a **phase start status**, activating a phase moves each assigned ticket there **only when a valid transition exists**; other tickets keep their status; activation is never blocked by failed moves.
16. **Version labels SemVer** — version labels must be valid SemVer; unique per project.
17. **Version scope** — observed, target, and deliverable version references must belong to the ticket's or phase's project.
18. **Finish statuses** — each workflow defines finish statuses tagged `DONE` or `CANCELED`.
19. **Finish date** — moving to a `DONE` finish status sets `finished_at`; moving out of a `DONE` finish status clears `finished_at`. `CANCELED` does not set finish date.
20. **Version changelog** — derived, not persisted separately. Includes non-canceled tickets linked by target version, observed version, or phase deliverable version. Excludes tickets in a `CANCELED` finish status; they reappear when moved out of canceled. Sorted by finish date ascending (nulls last) within grouped sections.
21. **Ticket create — no default phase** — new tickets have no phase unless the user selects one from **planned** and **active** phases in the create form.
22. **Phase/version history** — changes to phase, observed version, target version, and finish date on tickets are logged via `TicketHistoryService`.
23. **Phase/version admin roles** — phase and version CRUD: `PROJECT_MANAGER` and `ADMIN`; version changelog read: any authenticated user.

---

## Aggregates (summary)

| Aggregate | Root | Consistency boundary |
|-----------|------|---------------------|
| User | `User` | Credentials, roles, profile |
| Project | `Project` | Name, prefix, workflow assignment, phase template |
| Workflow | `Workflow` | Statuses, transitions, phase start status, finish statuses |
| Phase | `Phase` | Lifecycle, objective, deliverables, deliverable version |
| Version | `Version` | SemVer label, changelog (derived) |
| Ticket | `Ticket` | Title, status, assignee, phase, versions, finish date, subscribers, comments (via services) |
| Notification | `Notification` | Read state per user per event |

---

## Mapping to code

| Domain term | Primary types |
|-------------|---------------|
| Ticket | `ticket.Ticket`, `ticket.TicketService` |
| Workflow | `workflow.Workflow`, `workflow.WorkflowService` |
| Project | `project.Project`, `project.ProjectService` |
| Phase | `phase.Phase`, `phase.PhaseService` |
| Version | `phase.Version`, `phase.VersionService` |
| Notification | `notifications.Notification`, `notifications.NotificationsEndpoint` |
| Audit | `ticket.history.TicketHistoryService` |

---

## Checklist for changes

Before merging a domain-affecting change:

- [ ] Ubiquitous Language updated if new terms or rule changes
- [ ] Bounded context placement verified (package dependency direction)
- [ ] [ARCHITECTURE.md](../ARCHITECTURE.md) updated if structure or API surface changes
- [ ] [feature-catalog.md](feature-catalog.md) updated if routes or UI flows change
- [ ] Tests use domain vocabulary in method and variable names

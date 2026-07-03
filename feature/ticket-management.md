# Ticket management

**Feature version:** 3  
**Status:** planned  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Core ticket lifecycle: view and edit ticket fields (including optional **due date**), assign users, move status per workflow rules, soft-delete (admin/PM), add comments, audit history, subscribe/unsubscribe observers, and unified activity feed on ticket detail.

## Wireframe

**Guide:** layout reference for UI implementation — update when fields or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-03 |

### Screen: `/ticket/:ticketIdentifier`

| Region | Elements |
|--------|----------|
| Header | Identifier, title, status move, assignee, subscribe |
| Fields | Project, category, priority, description (rich text), **Data de vencimento** (optional), phase/versions when enabled |
| Actions | Save, delete (admin/PM), **Restaurar** on soft-deleted tickets (admin/PM), transition buttons |
| Comments | Add comment form + thread |
| Atividade | Merged feed: comments + history (`.activity-feed`) |

```
┌────────────────────────────────────────────────────────┐
│  PROJ-42  Title                    [Status ▼] [Assign] │
├───────────────────────────────┬────────────────────────┤
│  Fields + description         │  Comentários           │
│  [ Salvar ] [ Excluir ]       │  [ new comment… ]      │
├───────────────────────────────┴────────────────────────┤
│  Atividade (comments + history)                        │
└────────────────────────────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `ticket`, `ticket.comments`, `ticket.history`, `ticket.business`; reactions in `notifications`, `mailer` |
| Packages / files | `ticket.create`, `ticket.update`, `ticket.delete`, `ticket.restore`, `ticket.move`, `ticket.assign`, `ticket.find`, `ticket.list`, `ticket.subscribe`, `ticket.comments.*`, `ticket.history.*`, `TicketHistoryService` |
| API | `GET/POST /tickets`, `GET /tickets/{id}`, expanded find by id/identifier, `POST /tickets/{id}/move`, assign, delete, **restore**, subscribe/unsubscribe, comments, `GET /tickets/{id}/history` |
| UI | `/ticket/:ticketIdentifier`; `ticket-view`, `ticket-activity-feed`, `rich-text-editor` components; `ticket.service` |
| Schema / seed | `tb_tickets`, `tb_comments`, `tb_ticket_history`, `tb_tickets_subscribers`; sample tickets in `dev-import.sql` |
| Tests | `CreateTicketEndpointTest`, `UpdateTicketEndpointTest`, `DeleteTicketEndpointTest`, **`RestoreTicketEndpointTest`**, `MoveTicketEndpointTest`, `UpdateAssigneeEndpointTest`, `Find*EndpointTest`, `ListTicketsEndpointTest`, `AddCommentEndpointTest`, `ListCommentsEndpointTest`, `GetTicketHistoryEndpointTest`, `TicketHistoryServiceTest`, `SubscribeTicketEndpointTest`, `HistoryDisplayTest` |
| Docs | domain-spec (Ticket, Comment, History, Subscriber, Activity feed), feature-catalog (Ticket detail), README § Tickets & workflow |

### Risks

- Restore must re-include ticket in search/lists without breaking identifier uniqueness or history continuity.

### Feature questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ9 | Should deleted tickets be restorable from the UI? | answered | **Yes** — admin/PM may restore soft-deleted tickets from ticket detail |

### Feature questions (due date — v2)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Field type and nullability? | answered | Optional `LocalDate dueDate` (`due_date DATE`) |
| FQ2 | Distinction from finish date? | answered | **Due date** = user-planned deadline; **`finished_at`** = workflow DONE completion |
| FQ3 | Where editable? | answered | Create ticket + ticket detail update |
| FQ4 | CSV import mapping? | answered | Out of scope for this gap closure |
| FQ5 | Kanban badge? | answered | Out of scope for this gap closure |
| FQ6 | Query language? | answered | `dueDate` / `due` with date comparators + IS EMPTY / IS NOT EMPTY |
| FQ7 | History? | answered | `FIELD_CHANGED` field `"dueDate"` |
| FQ8 | UI label? | answered | PT-BR **Data de vencimento** |

## Changelog

### Initial implementation — baseline

**Version:** 1  
**Status:** done

**Description:** Full ticket CRUD (soft delete), workflow-validated moves, assignee updates, comments, structured history, subscribers, and merged Atividade feed on detail page.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Kanban board | Lists and moves tickets |
| Create ticket | Creates tickets via separate flow |
| Notifications | Fires on ticket changes for subscribers |
| Workflow configuration | Move validates against project workflow |
| Project administration | Tickets scoped to project |
| — | None identified beyond cross-context events |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Ticket detail matches **Wireframe** layout | Wireframe | ☑ |
| FC2 | Workflow-validated status moves | Summary | ☑ |
| FC3 | Comments and unified Atividade feed | Wireframe | ☑ |
| FC4 | Subscribe/unsubscribe observers | Summary | ☑ |
| FC5 | `domain-specification.md` — Ticket, Comment, History | Impact / Docs | ☑ |
| FC6 | `feature-catalog.md` — Ticket detail row | Impact / Docs | ☑ |

**Implementation notes:** `TicketHistoryService` logs CREATED, FIELD_CHANGED, STATUS_CHANGED, ASSIGNEE_CHANGED, SUBSCRIBED, UNSUBSCRIBED, DELETED; `ticket-view.component.ts` orchestrates detail UI.

### Ticket due date — 2026-07-03

**Version:** 2  
**Status:** done

**Development approval:** approved 2026-07-03 — tasks: T1–T12

**Description:** Close ARCHITECTURE §13 gap — optional planned **due date** on tickets, distinct from workflow **finish date**.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [create-ticket](create-ticket.md) | Optional due date on create form |
| [ticket-search](ticket-search.md) | Query language field `dueDate` / `due` |
| [kanban-board](kanban-board.md) | None (badge deferred) |
| [ticket-import](ticket-import.md) | None (CSV mapping deferred) |

## Architecture

| Layer | Change |
|-------|--------|
| Schema | `due_date DATE` on `tb_tickets` |
| Entity | `Ticket.dueDate` (`LocalDate`) |
| API | `CreateTicketRequest`, `UpdateTicketRequest`, `TicketResponse`, `TicketExpandedResponse` |
| Service | `TicketService` create/update + history |
| Query | `TicketQueryPredicateBuilder` — `dueDate` / `due` |
| UI | `ticket-form`, `ticket-view`; `query-language-reference.ts` |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | Changelog + FQ + Architecture + FC + test plan | ☑ |
| T2 | Flyway + JPA `dueDate` | ☑ |
| T3 | Extend Request/Response records | ☑ |
| T4 | `TicketService` persist + history | ☑ |
| T5 | Endpoint tests create/update | ☑ |
| T6 | Query language + tests | ☑ |
| T7 | `dev-import.sql` sample due dates | ☑ |
| T8 | Ticket detail UI | ☑ |
| T9 | Create ticket form UI | ☑ |
| T10 | OpenAPI codegen | ☑ |
| T11 | Query help reference + spec | ☑ |
| T12 | domain-spec, README, ARCHITECTURE §13 | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `CreateTicketEndpointTest` — due date on create | T4, T5 | ☑ |
| TC2 | `UpdateTicketEndpointTest` — due date update | T4, T5 | ☑ |
| TC3 | `TicketQueryLanguageServiceTest` — dueDate filter | T6 | ☑ |
| TC4 | `ArchitectureTest` | T3 | ☑ |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Ticket detail **Data de vencimento** matches **Wireframe** | Wireframe, FQ8 | ☑ |
| FC2 | Create ticket optional due date | FQ3 | ☑ |
| FC3 | History logs due date changes | FQ7 | ☑ |
| FC4 | Query language `dueDate` field | FQ6 | ☑ |
| FC5 | `domain-specification.md` — Due date term | Docs | ☑ |
| FC6 | ARCHITECTURE §13 gap closed | Docs | ☑ |

**Implementation notes:** `due_date` on `tb_tickets`; create/update API + UI; query language `dueDate`/`due`; history via `FIELD_CHANGED`. `mvn verify` + `npm run build` green (2026-07-03).

### Restore soft-deleted tickets — 2026-07-03

**Version:** 3  
**Status:** planned

**Description:** Allow admin and project-manager to **restore** soft-deleted tickets from the UI; ticket reappears in lists and search; history logs restoration.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [ticket-search](ticket-search.md) | Restored tickets included in query results again |
| [kanban-board](kanban-board.md) | Restored tickets visible on board |
| [notifications](notifications.md) | Optional notification on restore (TBD in architecture) |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | **Restaurar** action on soft-deleted ticket detail | Wireframe, FQ9 | ☐ |
| FC2 | Restored ticket visible in lists and search | FQ9 | ☐ |
| FC3 | History logs restore event | FQ9 | ☐ |
| FC4 | `domain-specification.md` — restore invariant | Docs | ☐ |

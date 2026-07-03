# Ticket management

**Feature version:** 3  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Core ticket lifecycle: view and edit ticket fields (including optional **due date**), assign users, move status per workflow rules, soft-delete (admin/PM), add comments, audit history, subscribe/unsubscribe observers, and unified activity feed on ticket detail.

## Wireframe

**Guide:** layout reference for UI implementation — update when fields or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-03 (restore wireframe) |

### Screen: `/ticket/:ticketIdentifier`

| Region | Elements |
|--------|----------|
| Header | Identifier, title, status move, assignee, subscribe |
| Fields | Project, category, priority, description (rich text), **Data de vencimento** (optional), phase/versions when enabled |
| Actions | Save, delete (admin/PM), **Restaurar** when deleted (admin/PM only); read-only fields when deleted |
| Comments | Add comment form + thread |
| Atividade | Merged feed: comments + history (`.activity-feed`) |

```
┌────────────────────────────────────────────────────────┐
│  PROJ-42  Title  [excluído]        (controls disabled) │
├───────────────────────────────┬────────────────────────┤
│  Fields + description (read)  │  Comentários (hidden)  │
│  [ Restaurar ]  (admin/PM)    │                        │
├───────────────────────────────┴────────────────────────┤
│  Atividade (comments + history incl. DELETED/RESTORED) │
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
**Status:** done

**Development approval:** approved 2026-07-03 — tasks: T2–T10

**Description:** Allow admin and project-manager to **restore** soft-deleted tickets from the UI; ticket reappears in lists and search; history logs restoration.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [ticket-search](ticket-search.md) | Restored tickets included in query results again |
| [kanban-board](kanban-board.md) | Restored tickets visible on board |
| [notifications](notifications.md) | **No** restore notification — history entry only (same as delete) |

## Architecture

**Scope:** changelog entry **Restore soft-deleted tickets — v3** only. Due-date architecture remains under v2 above.

| Area | Design |
|------|--------|
| Bounded contexts | `ticket`, `ticket.history` — no cross-context events |
| Packages / layers | `ticket.restore.RestoreTicketEndpoint` → `TicketService.restore` → `TicketRepository.restore`; find paths updated in `TicketService` + `ticket.find` |
| API | `POST /api/tickets/{id}/restore` — `@RolesAllowed(admin, project-manager)`; returns `TicketResponse` (200). `GET /api/tickets/{identifier}/expanded` returns deleted tickets for **admin/PM only**; regular `user` still gets 404 |
| Schema / seed | **No schema change** — reuse `tb_tickets.deleted` boolean |
| Response contract | Add `boolean deleted` to `TicketResponse` and `TicketExpandedResponse` so UI can render read-only deleted state |
| Service rules | `restore`: load ticket including deleted; reject if not deleted; set `deleted=false`; log `RESTORED` via existing `TicketHistoryService.logTicketRestored`. Mutations (`update`, `move`, `assign`, `delete`, comments, subscribe) reject deleted tickets with 400 |
| Frontend | `ticket-view`: when `deleted`, show badge, disable edit/move/comment/delete, show **Restaurar** for admin/PM; `ticket.service.restore(id)`; activity feed handles `RESTORED` action |
| Tests | `RestoreTicketEndpointTest`; extend find-expanded test for admin vs user on deleted ticket; `TicketHistoryServiceTest` or endpoint assertion for RESTORED entry; `ArchitectureTest` for new Response field |
| Docs | `feature-catalog.md` ticket detail row; domain-spec invariant already recorded |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | How can admin/PM open a deleted ticket for restore? | answered | **`GET …/expanded`** returns deleted tickets for **admin/PM**; regular users still 404 |
| AQ2 | Restore HTTP shape? | answered | **`POST /api/tickets/{id}/restore`** returning `TicketResponse` |
| AQ3 | Notification on restore? | answered | **No** — `RESTORED` history only (delete does not notify either) |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | Architecture + AQ + tasks + test plan on changelog v3 | ☑ |
| T2 | `TicketRepository.restore` + find-by-id/identifier including deleted | ☑ |
| T3 | `TicketService.restore` + reject mutations on deleted tickets + role-aware expanded find | ☑ |
| T4 | Add `deleted` to `TicketResponse` / `TicketExpandedResponse` | ☑ |
| T5 | `RestoreTicketEndpoint` — `POST /{id}/restore` | ☑ |
| T6 | `RestoreTicketEndpointTest` + find-expanded deleted visibility tests | ☑ |
| T7 | Ticket detail UI — deleted read-only state + **Restaurar** (admin/PM) | ☑ |
| T8 | Activity feed — `RESTORED` icon/summary | ☑ |
| T9 | OpenAPI codegen (`npm run generate:api`) | ☑ |
| T10 | `feature-catalog.md` + verify domain-spec invariant | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `RestoreTicketEndpointTest` — PM restores deleted ticket | T3, T5 | ☑ |
| TC2 | `RestoreTicketEndpointTest` — regular user forbidden | T5 | ☑ |
| TC3 | `RestoreTicketEndpointTest` — restore idempotent guard (not deleted → 400) | T3 | ☑ |
| TC4 | Find expanded by identifier — admin sees deleted; user gets 404 | T3 | ☑ |
| TC5 | `TicketHistoryServiceTest` or endpoint — RESTORED history entry | T3 | ☑ |
| TC6 | `ArchitectureTest` — Response records include `deleted` | T4 | ☑ |
| TC7 | `activity-feed.utils` spec — RESTORED summary | T8 | ☑ |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | **Restaurar** action on soft-deleted ticket detail | Wireframe, FQ9 | ☑ |
| FC2 | Restored ticket visible in lists and search | FQ9 | ☑ |
| FC3 | History logs restore event (`RESTORED`) | FQ9 | ☑ |
| FC4 | `domain-specification.md` — restore invariant | Docs | ☑ |
| FC5 | Deleted ticket detail read-only for all roles; restore admin/PM only | Wireframe, AQ1 | ☑ |
| FC6 | `feature-catalog.md` — restore step on ticket detail | Docs | ☑ |

**Implementation notes:** `POST /api/tickets/{id}/restore`; `deleted` on Response records; admin/PM expanded find includes soft-deleted tickets; ticket detail read-only + **Restaurar**; activity feed `RESTORED`. `mvn verify` + `npm run build` green (2026-07-03).

# Ticket management

**Feature version:** 1  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Core ticket lifecycle: view and edit ticket fields, assign users, move status per workflow rules, soft-delete (admin/PM), add comments, audit history, subscribe/unsubscribe observers, and unified activity feed on ticket detail.

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
| Fields | Project, category, priority, description (rich text), phase/versions when enabled |
| Actions | Save, delete (admin/PM), transition buttons |
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
| Packages / files | `ticket.create`, `ticket.update`, `ticket.delete`, `ticket.move`, `ticket.assign`, `ticket.find`, `ticket.list`, `ticket.subscribe`, `ticket.comments.*`, `ticket.history.*`, `TicketHistoryService` |
| API | `GET/POST /tickets`, `GET /tickets/{id}`, expanded find by id/identifier, `POST /tickets/{id}/move`, assign, delete, subscribe/unsubscribe, comments, `GET /tickets/{id}/history` |
| UI | `/ticket/:ticketIdentifier`; `ticket-view`, `ticket-activity-feed`, `rich-text-editor` components; `ticket.service` |
| Schema / seed | `tb_tickets`, `tb_comments`, `tb_ticket_history`, `tb_tickets_subscribers`; sample tickets in `dev-import.sql` |
| Tests | `CreateTicketEndpointTest`, `UpdateTicketEndpointTest`, `DeleteTicketEndpointTest`, `MoveTicketEndpointTest`, `UpdateAssigneeEndpointTest`, `Find*EndpointTest`, `ListTicketsEndpointTest`, `AddCommentEndpointTest`, `ListCommentsEndpointTest`, `GetTicketHistoryEndpointTest`, `TicketHistoryServiceTest`, `SubscribeTicketEndpointTest`, `HistoryDisplayTest` |
| Docs | domain-spec (Ticket, Comment, History, Subscriber, Activity feed), feature-catalog (Ticket detail), README § Tickets & workflow |

### Risks

- Due date field not implemented (ARCHITECTURE §13 known gap).

### Open questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| Q1 | Should deleted tickets be restorable from the UI? | open | |

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

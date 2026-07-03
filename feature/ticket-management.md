# Ticket management

**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Core ticket lifecycle: view and edit ticket fields, assign users, move status per workflow rules, soft-delete (admin/PM), add comments, audit history, subscribe/unsubscribe observers, and unified activity feed on ticket detail.

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

### Risks and open questions

- Due date field not implemented (ARCHITECTURE §13 known gap).
- Restore deleted ticket not exposed in UI.

## Changelog

### Initial implementation — baseline

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

**Implementation notes:** `TicketHistoryService` logs CREATED, FIELD_CHANGED, STATUS_CHANGED, ASSIGNEE_CHANGED, SUBSCRIBED, UNSUBSCRIBED, DELETED; `ticket-view.component.ts` orchestrates detail UI.

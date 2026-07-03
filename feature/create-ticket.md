# Create ticket

**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Create new tickets globally (`/tickets/new`) or within a project (`/project/:projectId/tickets/new`). Project-scoped creation pre-fills the form from the optional **ticket template** when enabled. Auto-generates human-readable identifiers (`{prefix}-{seq}`) and initial workflow status.

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `ticket`, `ticket.history`, `project` |
| Packages / files | `ticket.create.CreateTicketEndpoint`, `TicketHistoryService` |
| API | `POST /tickets` |
| UI | `/tickets/new`, `/project/:projectId/tickets/new`; `create-ticket`, `ticket-form` components |
| Schema / seed | `tb_tickets`; project template columns on `tb_projects` |
| Tests | `CreateTicketEndpointTest` |
| Docs | domain-spec (Identifier, Ticket template), feature-catalog (Create ticket rows), README § Tickets & workflow |

### Risks and open questions

- Template fields required when enabled; PM must configure project before team benefits.

## Changelog

### Initial implementation — baseline

**Status:** done

**Description:** Create ticket form with project selection, title, description, category, priority, assignee; project route pre-fills from template; history logs CREATED.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Project administration | Template configuration on project edit |
| Workflow configuration | Start status from project workflow |
| Kanban board | Novo ticket entry point |
| Ticket management | New tickets appear in lists and detail |
| Categories | Category picker uses category list |
| — | None identified |

**Implementation notes:** `create-ticket.component.ts`, `ticket-form.component.ts`; identifier generation in create flow.

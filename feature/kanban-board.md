# Kanban board

**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Project-scoped board view grouping tickets into columns by workflow status. Users drag or move tickets between columns; moves are validated against workflow transitions. Category colors display on cards.

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `ticket`, `project`, `workflow`, `categories` |
| Packages / files | `project.tickets.list.ListProjectTicketsEndpoint`, `project.status.ListProjectStatusesEndpoint`, `ticket.move.MoveTicketEndpoint` |
| API | `GET /projects/{id}/tickets`, `GET /projects/{id}/statuses`, `POST /tickets/{id}/move` |
| UI | `/project/:projectId/kanban`; `kanban` component |
| Schema / seed | `tb_tickets`, `tb_workflow_status`, `tb_workflow_transitions`; dev projects/tickets |
| Tests | `ListProjectTicketsEndpointTest`, `ListProjectStatusesEndpointTest`, `MoveTicketEndpointTest` |
| Docs | domain-spec (Kanban), feature-catalog (Kanban board), README § Views & analytics |

### Risks and open questions

- Drag-and-drop UX depends on client-side validation mirroring server rules.
- No swimlanes or WIP limits.

## Changelog

### Initial implementation — baseline

**Status:** done

**Description:** Kanban columns per project workflow status; ticket cards with category color; move between columns via workflow-validated API.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Ticket management | Move and list APIs shared |
| Create ticket | Novo ticket from Kanban |
| Ticket import | Importar CSV entry from Kanban |
| Project dashboard | Same project context |
| Notifications | Move triggers subscriber alerts |
| — | None identified |

**Implementation notes:** `kanban.component.ts`; loads project tickets and statuses; delegates moves to `ticket.service`.

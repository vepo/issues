# Kanban board

**Feature version:** 1  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Project-scoped board view grouping tickets into columns by workflow status. Users drag or move tickets between columns; moves are validated against workflow transitions. Category colors display on cards.

## Wireframe

**Guide:** layout reference for UI implementation — update when columns, filters, or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-03 |

### Screen: `/project/:projectId/kanban`

| Region | Elements |
|--------|----------|
| Toolbar | **Novo ticket**, **Importar CSV**, phase filter, links to Fases/Versões/Painel |
| Board | Columns per workflow status; draggable ticket cards |
| Card | Identifier, title, category color, phase badge (when enabled) |

```
┌──────────────────────────────────────────────────────────────┐
│  Kanban — Project X    [Fase ▼]  Novo │ Importar │ Fases …   │
├──────────┬──────────┬──────────┬──────────┬──────────────────┤
│ To Do    │ Doing    │ Review   │ Done     │                  │
│ ┌──────┐ │ ┌──────┐ │          │ ┌──────┐ │                  │
│ │card  │ │ │card  │ │          │ │card  │ │                  │
│ └──────┘ │ └──────┘ │          │ └──────┘ │                  │
└──────────┴──────────┴──────────┴──────────┴──────────────────┘
```

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

### Open questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| Q1 | Should Kanban support swimlanes or WIP limits? | open | |
| Q2 | How should client-side drag validation stay aligned with server move rules? | open | |

## Changelog

### Initial implementation — baseline

**Version:** 1  
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

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Kanban board matches **Wireframe** | Wireframe | ☑ |
| FC2 | Columns reflect workflow statuses | Summary | ☑ |
| FC3 | Moves validated against transitions | Summary | ☑ |
| FC4 | Category color on cards | Wireframe | ☑ |
| FC5 | `feature-catalog.md` — Kanban row | Impact / Docs | ☑ |

**Implementation notes:** `kanban.component.ts`; loads project tickets and statuses; delegates moves to `ticket.service`.

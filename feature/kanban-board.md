# Kanban board

**Feature version:** 2  
**Status:** planned  
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
| Toolbar | **Novo ticket**, **Importar CSV**, phase filter, swimlane selector (when configured), WIP limit indicators, links to Fases/Versões/Painel |
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

### Feature questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Should Kanban support swimlanes or WIP limits? | answered | **Yes** — add swimlanes and WIP limits to Kanban |
| FQ2 | How should client-side drag validation stay aligned with server move rules? | answered | **Keep current server validation**; client **blocks** drag/drop to columns with no valid transition (non-accepted target status) |

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

### Swimlanes, WIP limits, and drag validation — 2026-07-03

**Version:** 2  
**Status:** planned

**Description:** Kanban swimlanes and per-column WIP limits; client prevents drop on invalid transition targets before calling move API.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [workflow-configuration](workflow-configuration.md) | WIP limits may reference workflow status columns |
| [ticket-management](ticket-management.md) | Move API unchanged; client aligns with transitions |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Swimlanes render per **Wireframe** | Wireframe, FQ1 | ☐ |
| FC2 | WIP limit indicators and enforcement | FQ1 | ☐ |
| FC3 | Drag blocked when no valid transition to target column | FQ2 | ☐ |

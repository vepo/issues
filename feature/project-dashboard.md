# Project dashboard

**Feature version:** 2  
**Status:** planned  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Per-project analytics page with configurable widget layout: pie charts (tickets by day, status, priority), recent tickets table, and performance KPIs. Default widgets shown on first visit; users customize via **Editar layout**.

## Wireframe

**Guide:** layout reference for UI implementation — update when widgets or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-03 |

### Screen: `/project/:projectId/dashboard`

| Region | Elements |
|--------|----------|
| Header | Project name; **Editar layout** toggle |
| Grid | Draggable widget panels: pie charts, recent tickets table, KPI cards |

```
┌────────────────────────────────────────────────────────┐
│  Painel — Project X              [ Editar layout ]     │
├──────────────────────┬─────────────────────────────────┤
│  [Pie: por dia]      │  [Pie: por status]              │
├──────────────────────┼─────────────────────────────────┤
│  [Recent tickets]    │  [KPI performance]              │
└──────────────────────┴─────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `dashboards`, `ticket`, `project`, `workflow` |
| Packages / files | `dashboards.pie.LoadPieDashboardEndpoint`, `dashboards.table.LoadTableDashboardEndpoint`, `dashboards.kpi.LoadKpiDashboardEndpoint` |
| API | `GET /projects/{id}/dashboard/pie/{type}`, `/dashboard/table/{type}`, `/dashboard/kpi/{type}` |
| UI | `/project/:projectId/dashboard`; `dashboard` component (Chart.js/ng2-charts) |
| Schema / seed | Reads `tb_tickets`, workflow statuses; **`tb_dashboard_layouts`** (per user per project) |
| Tests | `LoadPieDashboardEndpointTest`, `LoadTableDashboardEndpointTest`, `LoadKpiDashboardEndpointTest` |
| Docs | domain-spec (Dashboard, Dashboard widget types), feature-catalog (Project dashboard), README § Views & analytics |

### Risks

- Large projects may need aggregated SQL or caching for chart endpoints (**FQ2**).

### Feature questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Should dashboard layout be persisted server-side? | answered | **Yes** — persist widget layout per user per project on server |
| FQ2 | Are chart query optimizations needed for large ticket volumes? | answered | **Yes** — optimize dashboard chart/table queries for scale |

## Changelog

### Initial implementation — baseline

**Version:** 1  
**Status:** done

**Description:** Dashboard with tickets-by-day, tickets-by-status, tickets-by-priority pie charts, recent-tickets table, and performance-kpi widget; editable layout.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Kanban board | Shared project navigation |
| Ticket management | Recent tickets link to detail |
| Project administration | Project must exist |
| — | None identified |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Dashboard grid matches **Wireframe** | Wireframe | ☑ |
| FC2 | Default widgets on first visit | Summary | ☑ |
| FC3 | Editar layout toggles customization | Wireframe | ☑ |
| FC4 | `feature-catalog.md` — Project dashboard row | Impact / Docs | ☑ |

**Implementation notes:** `dashboard.component.ts`; `DashboardType` enum drives widget types; Chart.js visualizations.

### Server layout persistence and query optimization — 2026-07-03

**Version:** 2  
**Status:** planned

**Description:** Replace `localStorage` layout with server persistence; optimize pie/table/KPI queries for large ticket volumes.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [kanban-board](kanban-board.md) | Shared project navigation unchanged |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Layout saved and restored from server per user/project | FQ1 | ☐ |
| FC2 | Chart endpoints perform acceptably on large datasets | FQ2 | ☐ |
| FC3 | ARCHITECTURE §13 gap closed | Docs | ☐ |

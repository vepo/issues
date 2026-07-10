# Project dashboard

**Feature version:** 2  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Per-project analytics page with configurable widget layout: pie charts (tickets by day, status, priority), recent tickets table, and performance KPIs. Default widgets shown on first visit; users customize via **Editar layout**. Layout is persisted **per user per project on the server**. Chart/table/KPI data is loaded via **aggregated SQL** (not full ticket lists in memory). Browser `localStorage` layouts are **not** migrated (legacy client storage discarded).

## Wireframe

**Guide:** layout reference for UI implementation — update when widgets or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-10 |

### Screen: `/project/:projectId/dashboard`

| Region | Elements |
|--------|----------|
| Header | Project name; **Editar layout** toggle |
| Grid | Draggable widget panels: pie charts, recent tickets table (max 20), KPI cards |

```
┌────────────────────────────────────────────────────────┐
│  Painel — Project X              [ Editar layout ]     │
├──────────────────────┬─────────────────────────────────┤
│  [Pie: por dia]      │  [Pie: por status]              │
├──────────────────────┼─────────────────────────────────┤
│  [Recent tickets]    │  [KPI performance]              │
└──────────────────────┴─────────────────────────────────┘
```

Layout load/save is transparent (no extra chrome beyond existing **Editar layout**).

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `dashboards` (owns layout + analytics reads); read-only `ticket`, `project` |
| Packages / files | Layout get/put endpoints; `DashboardLayout` entity/repo; aggregation queries; Angular drops `localStorage` |
| API | `GET/PUT /projects/{id}/dashboard/layout`; existing pie/table/kpi endpoints |
| UI | Same route; save/load layout via API |
| Schema / seed | `tb_dashboard_layouts` |
| Tests | Layout endpoint tests; recent table ≤ 20; Angular layout specs |
| Docs | domain-spec invariants **44–45**; feature-catalog; ARCHITECTURE §13 |

### Feature questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Should dashboard layout be persisted server-side? | answered | **Yes** — persist widget layout per user per project on server |
| FQ2 | Are chart query optimizations needed for large ticket volumes? | answered | **Yes** — optimize dashboard chart/table queries for scale |
| FQ3 | What happens to existing `localStorage` layouts when switching to server? | answered | **A** — discard browser layouts (legacy client storage); first visit uses defaults then saves to server. No one-time import. |
| FQ4 | How many rows should **Tickets Recentes** return? | answered | **A** — top **20** by `updated_at` |

## Architecture

### API surface

| Method | Path | Auth | Body / response |
|--------|------|------|-----------------|
| `GET` | `/api/projects/{projectId}/dashboard/layout` | USER, PM, ADMIN | `DashboardLayoutResponse` |
| `PUT` | `/api/projects/{projectId}/dashboard/layout` | same | `SaveDashboardLayoutRequest` `{ widgetIds: string[] }` |

Default widget ids: `tickets-by-status`, `tickets-by-priority`, `performance-kpi`, `recent-tickets`.

### Query optimization

| Widget | Implementation |
|--------|----------------|
| Pie / KPI | JPQL/native `GROUP BY` + `COUNT` on non-deleted tickets |
| Recent tickets | `ORDER BY updated_at DESC LIMIT 20` |

## Changelog

### Initial implementation — baseline

**Version:** 1  
**Status:** done

**Description:** Dashboard with tickets-by-day, tickets-by-status, tickets-by-priority pie charts, recent-tickets table, and performance-kpi widget; editable layout.

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Dashboard grid matches **Wireframe** | Wireframe | ☑ |
| FC2 | Default widgets on first visit | Summary | ☑ |
| FC3 | Editar layout toggles customization | Wireframe | ☑ |
| FC4 | `feature-catalog.md` — Project dashboard row | Impact / Docs | ☑ |

### Server layout persistence and query optimization — 2026-07-03

**Version:** 2  
**Status:** done

**Development approval:** approved 2026-07-10 — tasks: T1, T2, T3, T4, T5, T6

**Description:** Replace `localStorage` layout with server persistence per user/project (no browser import); optimize pie/table/KPI via SQL aggregations; recent tickets limited to 20.

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Layout saved and restored from server per user/project | FQ1 | ☑ |
| FC2 | Default layout when no server row | Architecture | ☑ |
| FC3 | No `localStorage` read/write (legacy discarded) | FQ3 | ☑ |
| FC4 | Pie/KPI use aggregation queries | FQ2 / AQ2 | ☑ |
| FC5 | Recent tickets ≤ 20; deleted excluded | FQ4 | ☑ |
| FC6 | UI still matches **Wireframe** | Wireframe | ☑ |
| FC7 | ARCHITECTURE §13 gap closed; domain-spec; feature-catalog | Docs | ☑ |
| FC8 | Layout + aggregation tests; Angular layout specs | Tests | ☑ |

#### Tasks

| ID | Deliverable | Done |
|----|-------------|------|
| T1 | Flyway baseline: `tb_dashboard_layouts`; entity + repository | ☑ |
| T2 | Layout get/save endpoints | ☑ |
| T3 | Aggregations + recent tickets LIMIT 20 | ☑ |
| T4 | Endpoint tests | ☑ |
| T5 | Angular API layout; remove localStorage | ☑ |
| T6 | Docs | ☑ |

#### Test coverage

| ID | Covers | Done |
|----|--------|------|
| TC1 | Layout GET/PUT tests | ☑ |
| TC2 | Table ≤20 + pie/KPI still green | ☑ |
| TC3 | Angular layout specs | ☑ |
| TC4 | Doc review | ☑ |

**Implementation notes:** `DashboardRepository` aggregations; `GET/PUT …/dashboard/layout`; Angular uses generated `DashboardApi`. `mvn verify` + Angular build/specs green.

# Project dashboard

**Feature version:** 3  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03); audit findings 2026-07-11

## Summary

Per-project analytics page (**Painel**) with configurable widget layout. Layout persisted **per user per project on the server**. Aggregated SQL for charts/KPI/table.

**v3 hardening:** fix SPA↔API type path (**AQ1**), enforce **requireView** (**AQ2**), distinct load/empty/error (**FQ6**), autosave + **Concluir** (**FQ5**), `tickets-by-day` as **bar** (**FQ7**), KPI label **Tickets por status** (**FQ8**), Angular facade (**AQ3**), live edit preview, typos/retry cleanup. New widgets remain out of scope.

## Wireframe

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-11 |

### Screen: `/project/:projectId/dashboard`

| Region | Elements | Notes |
|--------|----------|-------|
| Header | Project name; **Editar layout** ↔ **Concluir** | **FQ5** — autosave on drop/remove; toast on save ok/error; **Concluir** exits edit only |
| Grid (view + edit) | **Live** widgets (same renderers) | **U1** — no placeholder stubs |
| Widget states | Loading skeleton; **Sem dados**; error + **Tentar novamente** | **FQ6** — never fake pie |
| Available | Catalog to drag in | `tickets-by-day` as **bar** (**FQ7**); not in default layout |
| KPI widget | Title **Tickets por status** | **FQ8** — id stays `performance-kpi` |

```
┌────────────────────────────────────────────────────────┐
│  Painel — Project X         [ Editar layout | Concluir ]│
├──────────────────────┬─────────────────────────────────┤
│  [Pie: por status]   │  [Pie: por prioridade]          │
├──────────────────────┼─────────────────────────────────┤
│  [Recent tickets]    │  [Tickets por status — KPI]     │
│  (loading / empty / error per widget)                  │
└──────────────────────┴─────────────────────────────────┘
```

Default layout: `tickets-by-status`, `tickets-by-priority`, `performance-kpi`, `recent-tickets`.

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `dashboards` + `ProjectAccessService.requireView` on all ops |
| Packages / files | `DashboardType.fromString` dual accept; `DashboardService` membership; Angular `dashboard.service.ts` facade; component UX; optional bar chart for day widget |
| API | Unchanged paths; `{dashboardType}` accepts kebab **and** enum name (**AQ1 A**) |
| UI | Concluir, toasts, live edit, bar for day, KPI rename, distinct states |
| Schema / seed | Unchanged |
| Tests | Enum + kebab paths; membership 403; aggregates smoke; Angular states + facade |
| Docs | domain-spec widget labels; feature-catalog; ARCHITECTURE §13 |

### Risks

- Existing saved layouts with `performance-kpi` keep working (id unchanged; label only).
- ColorGenerator >15 labels still non-deterministic (**P3**) — defer unless trivial.

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Layout server-side? | answered | **Yes** |
| FQ2 | Query optimizations? | answered | **Yes** — SQL aggregations |
| FQ3 | localStorage migration? | answered | **Discard** |
| FQ4 | Recent tickets rows? | answered | **20** |
| FQ5 | Save UX? | answered | **Autosave** on drop/remove; button **Concluir** (exit only); toast on save success/error |
| FQ6 | Empty / loading / error? | answered | **Three distinct states:** skeleton while loading; **Sem dados** when empty; error + **Tentar novamente** (no fake pie) |
| FQ7 | `tickets-by-day` chart? | answered | **Bar**; keep in catalog; not in default layout |
| FQ8 | KPI title? | answered | UI label **Tickets por status**; widget id remains `performance-kpi` |

**Gate:** all blocking FQs answered.

## Code audit findings — 2026-07-11

*(Historical — decisions above close B1–B3 / U1–U6 / P1–P2 for v3; P3–P4 deferred.)*

| Id | Finding | v3 disposition |
|----|---------|----------------|
| **B1** | Enum path vs kebab | Fix via **AQ1 A** |
| **B2** | No membership | Fix via **AQ2** |
| **B3** | Thin tests | Tasks TC* |
| **U1** | Edit stubs | Live widgets in edit |
| **U2** | Misleading Salvar | **FQ5** |
| **U3** | Fake load/error | **FQ6** |
| **U4–U6** | Typos, dead retry, no facade | Tasks + **AQ3** |
| **P1** | KPI misnamed | **FQ8** |
| **P2** | Day as pie | **FQ7** bar |
| **P3–P4** | Colors / indexes | Deferred |

Suggested new widgets (WIP, throughput, …) → backlog idea; out of v3.

## Architecture

| Area | Design |
|------|--------|
| Bounded contexts | `dashboards`; depends on `project` access |
| Packages / layers | Endpoints → `DashboardService` → repos; inject `ProjectAccessService` |
| API | Same paths; auth via `requireView(projectId, username)` on layout + pie/table/kpi |
| Schema | Unchanged |
| Cross-context | Membership only — no ticket writes |
| Frontend | `services/dashboard.service.ts` wraps `DashboardApi`; component uses facade; Chart.js pie + **bar** for day |
| Tests | Path formats, 403, aggregates, Angular states |

### API surface

| Method | Path | Auth |
|--------|------|------|
| `GET/PUT` | `…/dashboard/layout` | `requireView` |
| `GET` | `…/dashboard/pie/{dashboardType}` | `requireView` |
| `GET` | `…/dashboard/table/{dashboardType}` | `requireView` |
| `GET` | `…/dashboard/kpi/{dashboardType}` | `requireView` |

### `DashboardType` (**AQ1**)

`fromString` accepts:

1. Kebab id (`tickets-by-status`, …) — existing tests  
2. Enum name (`TICKETS_BY_STATUS`, …) — OpenAPI / Angular client  

Invalid → 400 with corrected message (fix “bashboard” typo).

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Fix **B1** enum path? | answered | **(A)** `fromString` accepts kebab **and** enum name |
| AQ2 | Membership? | answered | **Yes** — `ProjectAccessService.requireView` on all layout + widget ops |
| AQ3 | Angular facade? | answered | **Yes** — `dashboard.service.ts` wrapping generated API |

**Gate:** blocking AQs answered → tasks below.

## Changelog

### Dashboard hardening (audit fixes) — 2026-07-11

**Version:** 3  
**Status:** done

**Description:** Harden Painel per accepted **FQ5–FQ8** / **AQ1–AQ3**: dual path parse, membership, UX states, Concluir + autosave, day bar chart, KPI label, facade, live edit, test gaps.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Burndown | Same `requireView` pattern |
| Home / hub | Painel entry unchanged |
| OpenAPI | Enum names still valid; kebab also valid |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | `fromString` accepts kebab + enum name; SPA charts load | **AQ1**, **B1** | ☑ |
| FC2 | Non-members 403 on layout + pie/table/kpi | **AQ2**, **B2** | ☑ |
| FC3 | Tests: both path forms, membership 403, basic aggregates | **B3** | ☑ |
| FC4 | Loading / Sem dados / error+retry per **FQ6** | **FQ6** | ☑ |
| FC5 | Autosave + **Concluir** + toast; live widgets in edit | **FQ5**, **U1** | ☑ |
| FC6 | `tickets-by-day` rendered as **bar** | **FQ7** | ☑ |
| FC7 | KPI title **Tickets por status** | **FQ8** | ☑ |
| FC8 | TS `dashboard.service` facade; typos/retry cleaned | **AQ3**, **U4–U6** | ☑ |
| FC9 | Wireframe match; domain-spec + feature-catalog + ARCHITECTURE | Docs | ☑ |

#### Tasks

| ID | Deliverable | Done |
|----|-------------|------|
| T1 | `DashboardType.fromString` dual accept + typo fix; endpoint tests for kebab **and** enum name | ☑ |
| T2 | `ProjectAccessService.requireView` in `DashboardService` (layout + pie/table/kpi); 403 tests | ☑ *(SEC7 / T6, 2026-07-11)* |
| T3 | Stronger backend tests: soft-delete exclusion or non-empty aggregate smoke | ☑ |
| T4 | Angular `dashboard.service.ts` facade; component uses it (**AQ3**) | ☑ |
| T5 | Widget states: loading / empty / error+retry (**FQ6**); remove fake pie + dead retry | ☑ |
| T6 | Autosave toast; **Concluir** label; live widgets in edit mode (**FQ5**, **U1**) | ☑ |
| T7 | `tickets-by-day` as bar chart (**FQ7**); KPI title **Tickets por status** (**FQ8**) | ☑ |
| T8 | Fix typos (`cdkDragclass`, naming); Angular specs for states / save UX | ☑ |
| T9 | Docs: domain-spec, feature-catalog, ARCHITECTURE §13 | ☑ |

#### Test coverage

| ID | Coverage | Done |
|----|----------|------|
| TC1 | Pie/kpi/table with `TICKETS_BY_*` and kebab paths — with T1 | ☑ |
| TC2 | Non-member 403 on layout get/put and one widget — with T2 | ☑ *(SEC7)* |
| TC3 | Aggregate or soft-delete smoke — with T3 | ☑ |
| TC4 | Angular: facade used; loading/empty/error; Concluir; bar day widget — with T4–T8 | ☑ |

**Development approval:** approved 2026-07-16 — tasks: T1, T2, T3, T4, T5, T6, T7, T8, T9

**Implementation notes (2026-07-16):** Dual `DashboardType.fromString`; access via `requireRead`/`requireView` (visibility); `DashboardAggregateSmokeTest`; Angular `dashboard.service.ts`; live edit + Concluir + toast; widget loading/empty/retry; day bar + KPI **Tickets por status**. `mvn verify` green; dashboard Angular specs 6/6.

**Catalog note:** feature-catalog Painel row restored to v3 UX (Concluir / Sem dados / KPI rename).

### Initial implementation — baseline

**Version:** 1  
**Status:** done

**Description:** Dashboard widgets + editable layout.

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1–FC4 | Wireframe, defaults, edit, catalog | — | ☑ |

### Server layout persistence and query optimization — 2026-07-03

**Version:** 2  
**Status:** done

**Development approval:** approved 2026-07-10 — tasks: T1–T6

**Description:** Server layout; SQL aggregations; recent ≤20.

**Implementation notes:** Soft-delete OK. Audit 2026-07-11 → v3.

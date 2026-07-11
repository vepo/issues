# Burndown

**Feature version:** 1  
**Status:** done  
**Requested:** 2026-07-11

## Summary

Add a **Burndown** project view — a dedicated route alongside **Kanban** — that charts remaining **story points** for a **phase** against an ideal completion line over the phase’s start/end dates. Phase management deferred this ([phase-management.md](phase-management.md) § Out of scope).

**v1:** always **non-negative integer story points** (**FQ1**, **FQ12**); built-in ticket field (**FQ10**); dedicated Kanban-peer UI (**FQ2**); chart control **disabled** + tooltip without phase dates (**FQ3**); label **Burndown** (**FQ7**); **canceled** counts as burned via `canceled_at` (**FQ8**, **FQ13**); missing points **warn** and contribute **0** until set — then count as **added points** on the actual line (**FQ11**).

## Scope

### In scope

| Id | Capability | Source |
|----|------------|--------|
| S1 | Metric = sum of **story points** (never ticket count) | **FQ1** |
| S2 | Dedicated `/project/:projectId/burndown`, peer of Kanban (hub + header nav) | **FQ2** |
| S3 | Chart enabled only when phase has both start and end; otherwise control disabled + tooltip | **FQ3** |
| S4 | Product label **Burndown** | **FQ7** |
| S5 | **CANCELED** tickets burn points (`canceled_at`) | **FQ8**, **FQ13** |
| S6 | **Phase-only** scope in v1 (no version burndown) | **FQ6** |
| S7 | Built-in **story points** on ticket (`INTEGER`, nullable, ≥ 0) | **FQ10**, **FQ12** |
| S8 | Warn when in-scope tickets lack points; null = 0 until set; setting points increases remaining (scope add) | **FQ11** |
| S9 | Scope = tickets **currently** assigned to the selected phase | **FQ4** |
| S10 | No daily snapshot table; actual line may rise above ideal when points/tickets added | **FQ5** |
| S11 | Default phase selector = **active** phase; if none, empty selection + prompt | **FQ14** |

### Out of scope

| Item | Reason |
|------|--------|
| Ticket-count burndown | **FQ1** |
| Dashboard / phase-detail as primary UI | **FQ2** |
| Version-scoped burndown | **FQ6** |
| Time spent / hours tracking | Backlog `time-tracking` |
| Daily snapshot persistence | **FQ5** |

### Ubiquitous language

| Term | Meaning | Avoid |
|------|---------|-------|
| **Burndown** | Project view: remaining story points over a phase date range vs ideal line | Sprint burndown only |
| **Story points** | Optional non-negative integer size/effort on a ticket | Hours, decimals |
| **Ideal line** | Straight line from remaining points at phase `start_date` → `0` at `end_date` | — |
| **Remaining points** | Sum of story points on in-scope tickets not yet burned as of day `d` | Open ticket count |
| **Burned** | Ticket in **DONE** (`finished_at`) or **CANCELED** (`canceled_at`) — points leave remaining | — |
| **Missing points warning** | In-scope ticket with null story points; contributes 0 until set | Silent zero without warn |

## Wireframe

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-11 |

### Screen: `/project/:projectId/burndown`

| Region | Elements | Notes |
|--------|----------|-------|
| Nav | Peer of **Kanban** / **Painel** on hub + project header | **FQ2** |
| Title | **Burndown** | **FQ7** |
| Phase | Selector; default **active** phase | **FQ14** |
| Warnings | Banner listing tickets missing story points (link to ticket) | **FQ11** |
| Chart control | Enabled only if phase has start **and** end; else **disabled** + tooltip | **FQ3** — not hidden |
| Chart | Ideal vs Remaining (points) | Chart.js |
| Legend | Ideal, Remaining | |

```
┌──────────────────────────────────────────────────────────────┐
│  Project X     [ Kanban ] [ Burndown ] [ Painel ] …          │
├──────────────────────────────────────────────────────────────┤
│  Burndown                                                    │
│  Fase: [ Active phase ▾ ]                                    │
│  ⚠ PROJ-12, PROJ-18 sem story points                         │
│  [ Ver burndown ]  ← disabled + tooltip if dates incomplete  │
│  Points … Ideal / Remaining chart …                          │
└──────────────────────────────────────────────────────────────┘
```

**Tooltip (FQ3):** “Defina data de início e fim na fase para habilitar o Burndown.”

### Ticket form / detail

| Region | Elements |
|--------|----------|
| Field | **Story points** — optional non-negative integer |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | **Ticket** (points + `canceled_at`); **Analytics** (`dashboards`) owns burndown series; **Phase** read for dates/active; project hub/nav |
| Packages / files | `ticket` entity/API/forms/history/move; `dashboards.burndown.*`; Angular burndown page; hub/header links |
| API | Ticket CRUD + responses include `storyPoints`; move sets/clears `canceledAt`; `GET /projects/{id}/burndown?phaseId=` |
| UI | `/project/:projectId/burndown`; ticket form field; warnings banner; disabled chart + tooltip |
| Schema / seed | `tb_tickets.story_points`, `tb_tickets.canceled_at`; `dev-import` samples |
| Tests | Points validation; cancel burn; series math; warnings; Angular page specs |
| Docs | domain-spec; feature-catalog; README; ARCHITECTURE §13 |
| Cross-feature | CSV import column; query language field; time-tracking backlog narrowed (points ship here) |

### Risks

- Retroactive reconstruction with **current** assignment applies today’s points to past days unless point-set history is used for join day — v1 accepts this; scope adds still raise remaining (**FQ5**, **FQ11**).
- Ideal line uses remaining at `start_date` (reconstructed); mid-phase adds make actual go above ideal (expected).

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Metric for v1? | answered | **Always points** |
| FQ2 | Primary UI? | answered | **New window**, peer of Kanban |
| FQ3 | Missing phase dates? | answered | Require start/end; **disable** + **tooltip**; do not hide |
| FQ4 | Scope set? | answered | **Default:** tickets **currently** on the selected phase |
| FQ5 | Mid-phase scope change? | answered | **Default:** no snapshots; actual line may **rise** when tickets/points added; ideal stays linear from start remaining → 0 |
| FQ6 | Version burndown in v1? | answered | **Default:** **phase-only** |
| FQ7 | UI label? | answered | **Burndown** |
| FQ8 | Canceled? | answered | **Count as burned** |
| FQ9 | Feature doc home? | answered | New `burndown` |
| FQ10 | Points storage? | answered | **Default:** built-in ticket field with this feature |
| FQ11 | Missing points? | answered | **Warn**; null contributes **0**; when points are **added**, count as **added points** (remaining increases) |
| FQ12 | Points type? | answered | **Non-negative integer** |
| FQ13 | Cancel burn day? | answered | **Default:** `canceled_at` mirroring `finished_at` |
| FQ14 | Default phase? | answered | **Default:** **active** phase; if none, empty + prompt |

**Gate:** all blocking FQs answered.

## Architecture

| Area | Design |
|------|--------|
| Bounded contexts | `ticket` owns `storyPoints` + `canceledAt`; `dashboards` owns burndown series read; `phase` for phase dates/active lookup; `project` membership auth |
| Packages / layers | `TicketService` / `TicketRepository` for points + cancel timestamps; `dashboards.BurndownService` + `dashboards.burndown.LoadBurndownEndpoint`; no cross-context repository calls |
| API | See below |
| Schema / seed | `story_points INTEGER NULL CHECK (story_points IS NULL OR story_points >= 0)`; `canceled_at TIMESTAMP NULL`; seed points on sample tickets |
| Cross-context | On move: extend finish handling — **DONE** ↔ `finished_at`; **CANCELED** ↔ `canceled_at` (set/clear like done). BurndownService reads tickets via repository queries in dashboards (or ticket query API used only inside service layer owned by dashboards injecting TicketRepository — **allowed** Analytics → ticket per domain map) |
| Frontend | Route `project/:projectId/burndown`; `BurndownApi` via codegen; hub + header link; ticket form `storyPoints` |
| Tests | `LoadBurndownEndpointTest`; move cancel sets `canceledAt`; create/update points validation; Angular `burndown.component.spec.ts` |

### API surface

| Method | Path | Auth | Request / response |
|--------|------|------|--------------------|
| `GET` | `/api/projects/{projectId}/burndown?phaseId={id}` | project member / admin | `BurndownResponse` |
| (existing) | Ticket create/update/get | unchanged roles | add `storyPoints` (`Integer`, optional, ≥ 0) |
| (existing) | Ticket move | unchanged | side effect: `canceledAt` |

**`BurndownResponse`** (sketch):

- `phaseId`, `phaseName`, `startDate`, `endDate`, `datesComplete` (boolean)
- `series`: `[{ date, ideal, remaining }]` — empty if `!datesComplete`
- `warnings`: `[{ ticketId, identifier, code: "MISSING_STORY_POINTS" }]`
- `commitmentPoints` (remaining at start), `remainingPoints` (today)

### Series algorithm (**AQ2**)

Given phase with both dates and tickets currently on phase (non-deleted):

1. For each day `d` from `startDate` to `min(today, endDate)`:
2. **Remaining(d)** = Σ `coalesce(story_points, 0)` for tickets where burn day is null or burn day date `> d`.
3. **Burn day** = `finished_at` if DONE path, else `canceled_at` if set (canceled); if both somehow set, earlier date wins (should not happen in normal moves).
4. **Ideal(d)** = linear from `Remaining(startDate)` to `0` at `endDate` (inclusive end).
5. Tickets with null points appear in `warnings`; contribute 0 until updated.

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Own burndown under `dashboards` or `phase`? | answered | **`dashboards`** (analytics view); phase data read-only |
| AQ2 | Series reconstruction details? | answered | Current phase assignment + `finished_at`/`canceled_at` + coalesce points; ideal from start remaining → 0; no snapshot table |
| AQ3 | CSV import + query language for points in v1? | answered | **Yes** — map optional CSV column; query field `points` / `storypoints` |
| AQ4 | Path shape? | answered | `GET /projects/{projectId}/burndown?phaseId=` |

**Gate:** blocking AQs answered → task break.

## Changelog

### Points burndown dedicated view — 2026-07-11

**Version:** 1  
**Status:** done  
**Change name:** Burndown (story points, Kanban-peer page)  
**Impact on other features:** Ticket forms/API/import/query; phase dates UX; project hub/nav; move/cancel lifecycle; time-tracking backlog (points delivered here)

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Metric is story points only | **FQ1**, S1 | ☑ |
| FC2 | Built-in non-negative integer `storyPoints` on tickets | **FQ10**, **FQ12** | ☑ |
| FC3 | `/project/:projectId/burndown` peer of Kanban (hub + nav) | **FQ2**, Wireframe | ☑ |
| FC4 | Chart control disabled + tooltip without phase dates; not hidden | **FQ3** | ☑ |
| FC5 | UI label Burndown | **FQ7** | ☑ |
| FC6 | Canceled burns via `canceled_at` | **FQ8**, **FQ13** | ☑ |
| FC7 | Missing-points warnings; null=0; added points raise remaining | **FQ11** | ☑ |
| FC8 | Phase-only; current assignment; active phase default | **FQ4**, **FQ6**, **FQ14** | ☑ |
| FC9 | Series API + algorithm per **Architecture** | Architecture, **AQ2** | ☑ |
| FC10 | CSV + query language for points | **AQ3** | ☑ |
| FC11 | Wireframe match (page + warnings + ticket field) | Wireframe | ☑ |
| FC12 | domain-spec + feature-catalog + README | Docs | ☑ |
| FC13 | All FQs/AQs answered | FQ/AQ tables | ☑ |

#### Tasks

| ID | Deliverable | Done |
|----|-------------|------|
| T1 | Flyway baseline: `story_points`, `canceled_at` on `tb_tickets`; entity fields | ☑ |
| T2 | Ticket create/update/response + validation (≥ 0); history on points change; `dev-import` samples | ☑ |
| T3 | Move lifecycle: set/clear `canceled_at` for CANCELED (mirror DONE/`finished_at`) | ☑ |
| T4 | `BurndownService` series + warnings; `LoadBurndownEndpoint` + `BurndownResponse` | ☑ |
| T5 | Endpoint tests: series math, missing dates empty series, warnings, canceled burn | ☑ |
| T6 | CSV import optional points column + query language `points`/`storypoints` | ☑ |
| T7 | Angular: ticket form/detail story points field + specs | ☑ |
| T8 | Angular: burndown page (phase picker, disabled+tooltip, chart, warnings) + route | ☑ |
| T9 | Project hub + header nav link **Burndown** | ☑ |
| T10 | Docs: domain-spec, feature-catalog, README, ARCHITECTURE §13 | ☑ |

#### Test coverage

| ID | Covers | Tasks | Done |
|----|--------|-------|------|
| TC1 | Points validation + persistence on create/update | T2 | ☑ |
| TC2 | `canceled_at` set/cleared on move | T3 | ☑ |
| TC3 | Burndown series ideal/remaining; canceled burned; warnings | T4, T5 | ☑ |
| TC4 | Burndown disabled path when dates incomplete (`datesComplete=false`) | T5 | ☑ |
| TC5 | CSV points mapping + query language filter | T6 | ☑ |
| TC6 | Angular ticket points field | T7 | ☑ |
| TC7 | Angular burndown page (tooltip/disabled, warnings, chart data) | T8 | ☑ |
| TC8 | Doc review checklist | T10 | ☑ |

**Development approval:** approved 2026-07-11 — tasks: T1, T2, T3, T4, T5, T6, T7, T8, T9, T10

**Implementation notes:** Built-in `story_points` + `canceled_at` on `tb_tickets`; `BurndownService`/`LoadBurndownEndpoint` under `dashboards.burndown`; Angular `/project/:projectId/burndown` with Chart.js; CSV `storyPointsColumn`; query `points`/`storypoints`. Verified with `mvn verify`, `npm run build`, and burndown/ticket-form Angular specs.

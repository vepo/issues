# Kanban board

**Feature version:** 2  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Project-scoped board view grouping tickets into columns by workflow status. Users drag tickets between columns; moves are validated against workflow transitions (server authoritative; client connects only to `moveable` targets). Optional **swimlanes** (toolbar: none / assignee / priority; default none) and per-workflow-status **WIP limits** with hard enforcement on move. Category colors display on cards.

## Wireframe

**Guide:** layout reference for UI implementation — update when columns, filters, or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-10 |

### Screen: `/project/:projectId/kanban`

| Region | Elements |
|--------|----------|
| Toolbar | **Novo ticket**, **Importar CSV**, phase filter, **Faixa** select (`Nenhuma` / `Responsável` / `Prioridade`, default `Nenhuma`), links to Fases/Versões/Painel |
| Board (no swimlanes) | One row of columns per workflow status |
| Board (swimlanes on) | Horizontal lane bands; each lane is a row of status cells; lane label = assignee name / “Sem responsável”, or priority label |
| Column / cell header | Status name; count; when WIP set: `n/limit` (over-limit styling when `n >= limit`) |
| Card | Identifier, title, category color, phase badge (when enabled) |
| Drag | Only to transition-valid targets; enter/drop blocked when target column is at/over WIP |

```
┌──────────────────────────────────────────────────────────────┐
│  Kanban — Project X  [Fase ▼] [Faixa: Nenhuma ▼]  Novo …    │
├──────────┬──────────┬──────────┬──────────┬──────────────────┤
│ To Do 2/5│ Doing 3/3│ Review   │ Done     │                  │
│ ┌──────┐ │ ┌──────┐ │          │          │  (no lanes)      │
│ │card  │ │ │card  │ │          │          │                  │
└──────────┴──────────┴──────────┴──────────┴──────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  … [Faixa: Responsável ▼]                                    │
├──────────┬──────────┬──────────┬──────────┬──────────────────┤
│ To Do    │ Doing 3/3│ Review   │ Done     │                  │
│ ═════════ Alice ═══════════════════════════════════════════  │
│ ┌──────┐ │ ┌──────┐ │          │          │                  │
│ ═════════ Sem responsável ═════════════════════════════════  │
│ ┌──────┐ │          │          │ ┌──────┐ │                  │
└──────────┴──────────┴──────────┴──────────┴──────────────────┘
```

### Screen: `/workflows/new` and `/workflows/:workflowId` (cross-feature)

| Region | Elements |
|--------|----------|
| Status table | Existing status names + optional **WIP** numeric input (empty = unlimited) |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `ticket` (move WIP), `workflow` (WIP config), `project` (status list exposes WIP); UI `kanban` + workflow form |
| Packages / files | `workflow` create/update/response; `ProjectStatusResponse`; move path in `ticket`; `kanban.component.*`; `workflow-form` |
| API | `CreateWorkflowRequest` / `UpdateWorkflowRequest` + `WorkflowResponse` carry optional WIP per status; `ProjectStatusResponse.wipLimit`; `moveTicket` **400** when target at/over WIP |
| UI | Kanban Faixa selector + lane layout; WIP badges + hard drop block; workflow form WIP column |
| Schema | `tb_workflow_wip_limits (workflow_id, status_id, wip_limit)` — absence = unlimited |
| Tests | Workflow create/update WIP; move over-limit; Kanban connectedTo / enterPredicate / swimlanes / WIP UI |
| Docs | domain-spec (swimlane, WIP, invariants); feature-catalog Kanban + workflow; README; ARCHITECTURE §13 |

### Feature questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Should Kanban support swimlanes or WIP limits? | answered | **Yes** — add swimlanes and WIP limits |
| FQ2 | How should client-side drag validation stay aligned with server move rules? | answered | **Keep server validation**; client **blocks** drop to columns with no valid transition |
| FQ3 | What do swimlanes group by? | answered | **D** — toolbar selector: **none** (default) / **assignee** / **priority** |
| FQ4 | Where is WIP limit configured? | answered | **A** — per **workflow status** (shared by all projects using that workflow); edit on workflow create/edit |
| FQ5 | WIP enforcement when column is at/over limit? | answered | **B** — **hard** — client blocks drop; server rejects move with **400** |
| FQ6 | Does WIP apply to ticket **create** / CSV **import** into a status? | answered | **Moves only** for this changelog — create and import do not check WIP (opened by **FQ5**; can revisit later) |

## Architecture

### Bounded contexts

| Context | Role |
|---------|------|
| Workflow | Persist and expose WIP limits per workflow×status |
| Project | `listProjectStatuses` includes `wipLimit` from project’s workflow |
| Ticket | `moveTicket` enforces WIP (hard) |
| Frontend | Swimlanes (client-only); WIP UI; drag rules |

### Schema

```sql
CREATE TABLE tb_workflow_wip_limits (
    workflow_id BIGINT NOT NULL REFERENCES tb_workflows,
    status_id   BIGINT NOT NULL REFERENCES tb_workflow_status,
    wip_limit   INTEGER NOT NULL CHECK (wip_limit >= 1),
    PRIMARY KEY (workflow_id, status_id)
);
```

Null/absent row = unlimited (**AQ2**). Do not put WIP on global `tb_workflow_status` (name catalog is shared across workflows).

### Packages / layers

| Operation | Endpoint | Service | Repository |
|-----------|----------|---------|------------|
| Create/update workflow WIP | existing create/update workflow endpoints | `WorkflowService` | `WorkflowRepository` (+ WIP entity/table) |
| List project statuses | `ListProjectStatusesEndpoint` | `ProjectService` | reads workflow WIP |
| Move ticket | existing move endpoint | ticket move service | count non-deleted tickets in project+status |

### API surface

| Type | Change |
|------|--------|
| `StatusWipRequest` / response field | `{ status: string, wipLimit: Integer \| null }` — null clears / unlimited |
| `CreateWorkflowRequest` | optional `List<StatusWipRequest> wipLimits` (default empty) |
| `UpdateWorkflowRequest` | optional `List<StatusWipRequest> wipLimits` — replace set for workflow (statuses still fixed) |
| `WorkflowResponse` | `List<StatusWipResponse> wipLimits` (only statuses with a limit, or all with nullable — prefer all workflow statuses with nullable `wipLimit` for form binding) |
| `ProjectStatusResponse` | `Integer wipLimit` (nullable) |
| Move | **400** if target status has limit and `count(non-deleted tickets in project with that status) >= wipLimit` **before** adding the moved ticket (same-status no-op unchanged) |

### Swimlanes (**FQ3** D)

- Toolbar `Faixa`: `none` \| `assignee` \| `priority`; default `none`; **not persisted** (**AQ1**).
- Assignee lanes: one per distinct assignee among `visibleTickets()`, plus **Sem responsável**.
- Priority lanes: one per priority value present (stable order HIGH → MEDIUM → LOW or enum order).
- Drop cells: status×lane; `cdkDropListConnectedTo` = valid transition targets’ cells (all lanes) + same-status other lanes if needed for cross-lane same-status moves (assignee/priority change is **not** via drag — drag only changes status; card stays in lane matching ticket fields after refresh).

**Note:** Dragging does not reassign assignee/priority; after a status move the card remains in the lane implied by ticket data.

### WIP hard enforcement (**FQ5** B)

| Layer | Behaviour |
|-------|-----------|
| UI | Show `n/limit`; `cdkDropListEnterPredicate` (or equivalent) false when target column count ≥ limit (moving within same status allowed) |
| API | Reject move into over-capacity status with 400 + clear message |
| Count | Non-deleted tickets only (**AQ3**); project-scoped |

### FQ2 — drag validation

Keep `cdkDropListConnectedTo` ← `moveable`. Add specs; optional muted style for non-connected columns.

### Cross-context

- No ticket repository calls from workflow package.
- Move service loads WIP from workflow association for the ticket’s project.

### Frontend

| Surface | Work |
|---------|------|
| `kanban.component` | Faixa select; lane layout; WIP badges; enter predicate |
| `workflow-form` | WIP input per status row (create + edit) |
| Codegen | After OpenAPI change |

### Tests

| Area | Asserts |
|------|---------|
| Workflow create/update | Persist/clear WIP; response includes limits |
| `listProjectStatuses` | `wipLimit` present |
| Move | 400 when at limit; success when under; unlimited null OK |
| Kanban | `connectedTo`; enter blocked at WIP; swimlane grouping for assignee/priority/none |

### Architecture questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Swimlanes need persistence? | answered | **No** — toolbar state only |
| AQ2 | WIP null means unlimited? | answered | **Yes** — absent row / null |
| AQ3 | Count deleted tickets toward WIP? | answered | **No** |
| AQ4 | Schema placement for WIP? | answered | **`tb_workflow_wip_limits`** keyed by workflow×status (**FQ4** A) |
| AQ5 | Server enforce WIP? | answered | **Yes** on `moveTicket` (**FQ5** B) |

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

**Implementation notes:** `kanban.component.ts`; `cdkDropListConnectedTo` already uses `moveable`.

### Swimlanes, WIP limits, and drag validation — 2026-07-03

**Version:** 2  
**Status:** done

**Description:** Toolbar swimlanes (none / assignee / priority); WIP limits on workflow statuses with hard client+server enforcement on move; verify transition-only drag connectivity.

**Development approval:** approved 2026-07-10 — tasks: T1, T2, T3, T4, T5, T6, T7, T8, T9

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [workflow-configuration](workflow-configuration.md) | Status table gains optional WIP; create/update API carries limits |
| [ticket-management](ticket-management.md) | `moveTicket` rejects when target WIP exceeded |
| [project-administration](project-administration.md) | None — WIP not on project (**FQ4** A) |
| Create ticket / CSV import | No WIP check this version (**FQ6**) |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Faixa selector + swimlane layouts match **Wireframe** (none / assignee / priority) | FQ3 / Wireframe | ☑ |
| FC2 | WIP configured on workflow status table; `n/limit` on Kanban | FQ4 / Wireframe | ☑ |
| FC3 | Hard WIP: client blocks drop; server 400 on move | FQ5 | ☑ |
| FC4 | Drag only to transition-valid columns (specs) | FQ2 | ☑ |
| FC5 | Create/import do not enforce WIP | FQ6 | ☑ |
| FC6 | domain-spec invariants + terms; feature-catalog; README; ARCHITECTURE §13 | Docs | ☑ |
| FC7 | Tests: workflow WIP, move over-limit, Kanban lanes/WIP/drag | Tests | ☑ |

#### Tasks

| ID | Deliverable | Done |
|----|-------------|------|
| T1 | Flyway baseline: `tb_workflow_wip_limits`; JPA entity + repository access | ☑ |
| T2 | `StatusWipRequest` / response fields; create + update workflow persist/replace WIP; `WorkflowResponse` exposes limits | ☑ |
| T3 | `ProjectStatusResponse.wipLimit` from project workflow | ☑ |
| T4 | `moveTicket` hard WIP check (non-deleted count); endpoint tests | ☑ |
| T5 | Workflow form: WIP input per status (create + edit); Angular specs as needed | ☑ |
| T6 | Kanban: Faixa selector + assignee/priority/none swimlane layout | ☑ |
| T7 | Kanban: WIP `n/limit` + enter/drop block when at/over limit | ☑ |
| T8 | Kanban specs: `connectedTo` / moveable; swimlanes; WIP block | ☑ |
| T9 | Docs: domain-spec, feature-catalog, README, ARCHITECTURE §13; `npm run generate:api` after backend | ☑ |

#### Test coverage

| ID | Covers | Tests | Done |
|----|--------|-------|------|
| TC1 | T1–T2 | `CreateWorkflowEndpointTest` / `UpdateWorkflowEndpointTest` — WIP persist, clear, response | ☑ |
| TC2 | T3 | `ListProjectStatusesEndpointTest` assert `wipLimit` | ☑ |
| TC3 | T4 | `MoveTicketEndpointTest` over WIP | ☑ |
| TC4 | T5 | Workflow form WIP inputs wired through create/edit | ☑ |
| TC5 | T6–T8 | `kanban.component.spec.ts` — lanes, connectedTo, WIP enter block | ☑ |

**Implementation notes:** `tb_workflow_wip_limits`; `WorkflowWipLimit`; create/update `wipLimits`; `ProjectStatusResponse.wipLimit`; hard WIP in `TicketService.moveTicket`; Kanban Faixa + swimlanes + `cdkDropListEnterPredicate`; workflow form WIP column.

# Workflow configuration

**Feature version:** 2  
**Status:** tasks-ready  
**Requested:** retrospective baseline (documented 2026-07-03); editable statuses 2026-07-11

## Summary

Project managers and admins define workflows: name, start status, status list, and allowed transitions. Workflows are assigned to projects and govern ticket moves on Kanban and API.

**v1 (shipped):** create with full status list; edit updates name, start status, transitions, phase start, finish statuses, and WIP — status membership fixed after create.

**v2 (tasks-ready):** allow **add / rename / remove** of statuses on edit. Removing a status that still has tickets requires a **replacement status**; tickets (including soft-deleted) are remapped in the same transaction. Orphan transitions and status-required CF links for the removed status are dropped. Rename is **workflow-local** (detach/attach name rows). Remap logs **STATUS_CHANGED** history; **no** subscriber notify.

## Decisions

| ID | Decision | Source |
|----|----------|--------|
| D1 | Status list editable after create (add, rename, remove) | **FQ1** |
| D2 | Remove with tickets → require replacement status; remap in same update | **FQ2** |
| D3 | Auto-delete transitions referencing a removed status | **FQ3** |
| D4 | Soft-deleted tickets remapped with the same replacement | **FQ4** |
| D5 | Remap: **STATUS_CHANGED** history per ticket; **no** notify | **FQ5** |
| D6 | Start / phase-start / finish must be reassigned in the same request if those statuses leave the list | **FQ6** |
| D7 | Rename is workflow-local: detach old global name row / attach (find-or-create) new name — other workflows unaffected | **FQ7** |
| D8 | Drop custom-field status-required links to the removed status (workflow-owned fields) | **FQ8** |
| D9 | Single-save API with `statusReplacements` map; direct status set (no transition validation); one DB transaction | **AQ1–AQ3** |

## Wireframe

**Guide:** layout reference for UI implementation — update when Scope or **FQ*n*** decisions change.

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-11 (FQ3–FQ8 accepted) |

### Screen: `/workflows`

| Region | Elements |
|--------|----------|
| List | Workflow name, start status; **Editar** per row |
| Actions | **Novo processo** |

### Screen: `/workflows/new` and `/workflows/:workflowId`

| Region | Elements | Notes |
|--------|----------|-------|
| Form | Name, start status | Start dropdown limited to statuses remaining after edits |
| Status table | Add / rename / remove; WIP editable | Edit unlocked (**FQ1**) |
| Remove with tickets | Dialog: **Mover tickets para** [ status ▼ ] | Required when any ticket (incl. soft-deleted) counts (**FQ2**, **FQ4**) |
| Transitions | From → To; UI drops edges for removed statuses | Server also auto-deletes (**FQ3**) |
| Extensions | Phase start; finish statuses | Must point at remaining statuses (**FQ6**) |

```
┌─────────────────────────────────────────────────────────────┐
│  Editar processo                                            │
├─────────────────────────────────────────────────────────────┤
│  Nome  [ … ]     Status inicial [ To Do ▼ ]                 │
│                                                             │
│  Status                          WIP                        │
│  To Do                           [  ]                       │
│  In Progress                     [ 5 ]                      │
│  Review          [ Remover ]     ← 3 tickets                │
│  Done                            [  ]                       │
│  [ + Status ]                                               │
│                                                             │
│  ┌─ Remover “Review” ─────────────────────────────────────┐ │
│  │  3 tickets neste status. Mover para: [ Done ▼ ]        │ │
│  │  [ Cancelar ]  [ Confirmar ]                           │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                             │
│  Transições …                    [ Salvar ]                 │
└─────────────────────────────────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `workflow` owns definition update + orchestrates remap; `ticket` / `ticket.history` for status + history; `customfield` for dropping status-required links |
| Packages / files | `UpdateWorkflowRequest`, `WorkflowService.update`; ticket remap helper (service in ticket or called from workflow); Angular `workflow-form` |
| API | `POST /workflows/{id}` gains `statuses` + `statusReplacements` (removed name → replacement name) |
| UI | Unlock status table on edit; remove dialog with replacement picker; prune transitions client-side |
| Schema / seed | No new tables; membership join + ticket `status_id` + CF status-required rows |
| Tests | Add/rename/remove; remap incl. soft-deleted; reject missing replacement; transition/CF cleanup; no notify; cross-workflow rename isolation |
| Docs | domain-spec invariants; feature-catalog; ARCHITECTURE §13 close gap |

### Risks

- Large remaps in one transaction (many tickets) — acceptable for v2; monitor timeout.
- Shared global status rows: rename must not mutate another workflow’s membership (**FQ7** / **D7**).
- Finish/start misconfiguration on same save — validate before apply (**FQ6**).

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Should workflow statuses be editable after create? | answered | **Yes** — add, rename, and remove on edit |
| FQ2 | Removal when tickets still in that status? | answered | **Require replacement status**; remap tickets as part of the update |
| FQ3 | Orphan transitions when status removed? | answered | **Auto-delete** transitions that reference the removed status |
| FQ4 | Soft-deleted tickets remapped? | answered | **Yes** — same replacement |
| FQ5 | History / notify on bulk remap? | answered | **STATUS_CHANGED** history **yes**; notify **no** |
| FQ6 | Remove start / phase-start / finish status? | answered | Must **reassign** start / phase-start / finish set in the **same request** so no dangling refs |
| FQ7 | Rename global vs workflow-local? | answered | **Workflow-local** — detach old name / attach find-or-create new name; other workflows unaffected |
| FQ8 | Custom field status-required on removed status? | answered | **Drop** those status-required rows for fields on this workflow |

**Gate:** blocking FQs answered → Architecture below.

## Architecture

| Area | Design |
|------|--------|
| Bounded contexts | `workflow` orchestrates update; injects ticket repository/service for remap + `TicketHistoryService` for STATUS_CHANGED; `customfield` repository/service to drop status-required links. No notifications CDI. |
| Packages / layers | `UpdateWorkflowEndpoint` → `WorkflowService.update` → repositories; remap logic in WorkflowService or dedicated `WorkflowStatusRemap` helper in `workflow` calling ticket persistence (not Endpoint). |
| API | Extend existing `POST /workflows/{id}` |
| Schema | Unchanged tables; membership + ticket status + CF links mutate |
| Cross-context | Direct ticket status set (**AQ2**); history write; no notify events (**FQ5**) |
| Frontend | `workflow-form` edit: enable status FormArray; track removals → replacements; send `statuses` + `statusReplacements` |
| Tests | `UpdateWorkflowEndpointTest` extensions; Angular `workflow-form` edit specs |

### API surface

| Method | Path | Auth | Body |
|--------|------|------|------|
| `POST` | `/api/workflows/{id}` | PROJECT_MANAGER / ADMIN (existing) | Extended `UpdateWorkflowRequest` |

**`UpdateWorkflowRequest`** (extended):

- Existing: `name`, `start`, `transitions`, `phaseStart`, `finishStatuses`, `wipLimits`
- New: `statuses` — `List<String>` full desired membership (order preserved if product cares; min 2)
- New: `statusReplacements` — `Map<String, String>` or list of `{ from, to }` for each **removed** status that has tickets (incl. soft-deleted). Required iff count &gt; 0 for that status across projects using this workflow; empty/absent if no tickets.

**Validation (before mutate):**

1. `statuses` size ≥ 2; unique names; `start` ∈ statuses; phase-start ∈ statuses or null; finish statuses ⊆ statuses.
2. For every status currently on the workflow but not in new `statuses`: if any ticket (deleted or not) on any project with this workflow has that status → `statusReplacements` must map it to a name ∈ new `statuses` (≠ removed).
3. Transitions in request must only reference names in new `statuses` (server may also strip orphans from persisted set per **FQ3**).

**Apply (one transaction — AQ3):**

1. Remap tickets: for each replacement, set `status_id` to target; write `STATUS_CHANGED` history (actor = current user); skip notify.
2. Drop transitions involving removed statuses (**FQ3**); apply request transitions.
3. Drop WIP for removed statuses; apply new WIP.
4. Drop CF status-required links to removed status ids for workflow-owned custom fields (**FQ8**).
5. Recompute membership: for rename, remove old join + resolveStatuses(new name); do **not** rename shared `tb_workflow_status.name` in place (**FQ7**).
6. Set start / phase-start / finish from request (**FQ6**).

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Remap API shape? | answered | **Single save** — `statuses` + `statusReplacements` on `UpdateWorkflowRequest` |
| AQ2 | Transition-valid move vs direct set? | answered | **Direct status set** + history (workflow restructure privilege) |
| AQ3 | Transaction boundary? | answered | **One** DB transaction for membership + remaps + CF/transition cleanup |

## Scope (v2)

| ID | Scope item | Notes |
|----|------------|-------|
| S1 | Edit status list (add / rename / remove) | FQ1, FQ7 |
| S2 | Replacement required when tickets present; remap incl. soft-deleted | FQ2, FQ4 |
| S3 | Auto-drop orphan transitions; drop CF status-required | FQ3, FQ8 |
| S4 | History on remap; no notify | FQ5 |
| S5 | Same-request reassignment of start / phase-start / finish | FQ6 |
| S6 | Angular unlock + remove dialog | Wireframe |
| S7 | Docs | domain-spec, feature-catalog, ARCHITECTURE §13 |

## Changelog

### Editable workflow statuses after create — 2026-07-11

**Version:** 2  
**Status:** tasks-ready  
**Change name:** Editable workflow statuses (add / rename / remove + ticket remap)

**Description:** Unlock status list on workflow edit. Removing a status that still has tickets requires a replacement status; tickets (including soft-deleted) are remapped with history and without notifications. Orphan transitions and status-required CF links are dropped. Rename is workflow-local.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Kanban board | Columns change; remapped tickets change column |
| Ticket management | Bulk status + STATUS_CHANGED history |
| Ticket import | Status names must match post-edit workflow |
| Custom fields | Status-required links dropped for removed statuses |
| Notifications | **None** on remap (**FQ5**) |
| Project administration | Unchanged |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Status list editable on edit (add / rename / remove) | FQ1, S1, Wireframe | ☐ |
| FC2 | Remove with tickets requires replacement; tickets remapped | FQ2, Wireframe | ☐ |
| FC3 | Orphan transitions auto-deleted | FQ3 | ☐ |
| FC4 | Soft-deleted tickets remapped | FQ4 | ☐ |
| FC5 | STATUS_CHANGED history; no notify | FQ5 | ☐ |
| FC6 | Start / phase-start / finish reassigned in same request | FQ6 | ☐ |
| FC7 | Rename does not mutate other workflows’ status membership | FQ7 | ☐ |
| FC8 | Status-required CF links dropped for removed status | FQ8 | ☐ |
| FC9 | Wireframe: remove dialog **Mover tickets para** | Wireframe | ☐ |
| FC10 | API: `statuses` + `statusReplacements`; one transaction; direct set | Architecture, AQ1–3 | ☐ |
| FC11 | domain-spec + feature-catalog + ARCHITECTURE §13 | Docs | ☐ |
| FC12 | All FQs/AQs answered | FQ/AQ tables | ☑ |

#### Tasks

| ID | Deliverable | Done |
|----|-------------|------|
| T1 | Extend `UpdateWorkflowRequest` with `statuses` + `statusReplacements`; validation messages | ☐ |
| T2 | `WorkflowService.update`: membership add/rename/remove (workflow-local rename); orphan transition + WIP cleanup | ☐ |
| T3 | Ticket remap (incl. soft-deleted) + `STATUS_CHANGED` history; no notify; same transaction | ☐ |
| T4 | Drop workflow CF status-required links for removed statuses; start/phase-start/finish guards | ☐ |
| T5 | Endpoint tests: add/rename/remove; remap; missing replacement → 400; cross-workflow rename isolation; no notification side effect | ☐ |
| T6 | Angular `workflow-form` edit: enable statuses; remove dialog with replacement; send new fields + specs | ☐ |
| T7 | Docs: domain-spec invariants, feature-catalog, ARCHITECTURE §13, README if needed | ☐ |

#### Test coverage

| ID | Covers | Tasks | Done |
|----|--------|-------|------|
| TC1 | Request validation + statuses on update | T1, T5 | ☐ |
| TC2 | Add / rename / remove membership; cross-workflow rename isolation | T2, T5 | ☐ |
| TC3 | Remap active + soft-deleted; history; reject without replacement | T3, T5 | ☐ |
| TC4 | Orphan transitions + CF status-required dropped; start/finish guards | T2, T4, T5 | ☐ |
| TC5 | Angular edit unlock + remove dialog + payload | T6 | ☐ |
| TC6 | Doc review | T7 | ☐ |

**Development approval:** pending — approve task IDs (e.g. “Approve T1–T7”) to start phase 5.

**Implementation notes:** (fill after done)

---

### Initial implementation — baseline

**Version:** 1  
**Status:** done

**Description:** Workflow builder UI with status table and transitions table on create; edit updates name, start status, and transitions.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Project administration | Each project references one workflow |
| Kanban board | Columns derived from workflow statuses; WIP limits configured here ([kanban-board](kanban-board.md) v2) |
| Ticket management | Move validates transitions |
| Create ticket | Initial status from workflow start status |
| Ticket import | Status column must match workflow |
| — | None identified |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Workflow list matches **Wireframe** | Wireframe | ☑ |
| FC2 | Create/edit form with status + transition tables | Wireframe | ☑ |
| FC3 | Start status and transitions govern ticket moves | Summary | ☑ |
| FC4 | `feature-catalog.md` — Workflow rows | Impact / Docs | ☑ |

**Implementation notes:** `workflow-form.component.ts`; `UpdateWorkflowEndpoint` partial update scope documented in ARCHITECTURE §13.

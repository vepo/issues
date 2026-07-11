# Project administration

**Feature version:** 3  
**Status:** tasks-ready  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Project managers create and edit projects: name, prefix, required description, assigned workflow, and optional **ticket template** (default title, description, category, priority for new tickets). Projects scope all tickets and Kanban boards. Once a project has tickets, its **prefix** is immutable so ticket identifiers stay stable.

**v3 (catalog compliance):** Align **admin** project update with hub/`canManage` and allocation APIs — `UpdateProjectEndpoint` must allow `ADMIN` (not only `PROJECT_MANAGER`). Tighten Angular `roleGuard` on edit/allocation so non-owner PMs are not offered a page that only 403s ([feature-catalog review](../reports/feature-catalog-review-1-11-07-2026-16-27-54.md)).

**Related:** Project **security level** (Private / Internal / Public) is analyzed in [project-visibility.md](project-visibility.md) — set on create/edit; gates ticket and project-surface reads (SEC1).

## Wireframe

**Guide:** layout reference for UI implementation — update when project form or **FQ*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-10 |

### Screen: `/projects`

| Region | Elements |
|--------|----------|
| List | `.data-table`: name, prefix, workflow; row → edit |
| Actions | **Novo projeto** |

### Screen: `/projects/new` and `/projects/:projectId/edit`

| Region | Elements | Notes |
|--------|----------|-------|
| Form | Name, prefix, description, workflow, ticket template, phase template, owner (edit) | Create: prefix editable |
| Prefix (edit) | `mat-form-field` Prefixo | When `prefixLocked`: input **disabled** (read-only); optional hint that prefix cannot change after tickets exist |
| Template | Checkbox **Usar template de ticket**; default field values | Unchanged |

```
┌─────────────────────────────────────────────┐
│  Editar projeto                              │
├─────────────────────────────────────────────┤
│  Nome                                       │
│  Processo                                   │
│  Prefixo  [ISS        ] (disabled if locked)│
│  Descrição                                  │
│  … template / owner …                       │
│              [Cancelar]  [Salvar]           │
└─────────────────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `project` (owns rule); reads ticket existence via `TicketRepository.countProjectTickets` (same boundary as `ProjectMemberService`) |
| Packages / files | `ProjectService.update` / `findById` / `listAll` / `create`; `ProjectResponse`; `project-edit` component |
| API | `ProjectResponse` gains `prefixLocked`; `POST /projects/{id}` rejects prefix change when locked (400) |
| UI | `/projects/:projectId/edit` — disable prefix when `prefixLocked` |
| Schema / seed | None — no Flyway change |
| Tests | `UpdateProjectEndpointTest` (prefix lock); Angular `project-edit.component.spec.ts`; `ArchitectureTest` if response shape changes |
| Docs | domain-spec invariant 36 (already present — verify); feature-catalog Edit project step; ARCHITECTURE §13 if listed as gap |

### Risks

- Soft-deleted tickets still hold identifiers with the old prefix — they must lock the prefix (**FQ2**).
- Clients that omit UI gating can still hit the API; server must enforce.

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Should project prefix be immutable after tickets exist? | answered | **Yes** — prefix cannot change once the project has tickets |
| FQ2 | Do soft-deleted tickets lock the prefix? | answered | **Yes** — any ticket row for the project (including soft-deleted) locks the prefix; identifiers retain the prefix |
| FQ3 | Will projects ever support multiple workflows? | not valid | One workflow per project is the current invariant |
| FQ4 | May admin-without-PM update a project via API? | answered | **Yes** — include `ADMIN` on `UpdateProjectEndpoint` (and any sibling update path), matching allocation + catalog/hub **Editar** |
| FQ5 | Should SPA edit/allocation routes match `requireManage` (owner PM or admin)? | answered | **Yes** — avoid non-owner PM opening pages that only get API 403 |

## Architecture

**Guide:** technical design for changelog v2 — Immutable project prefix.

| Area | Design |
|------|--------|
| Bounded contexts | Project administration owns the rule. Ticket existence via existing `TicketRepository.countProjectTickets` (counts all tickets for project, no `deleted = false` filter) — same cross-read pattern as `ProjectMemberService` |
| Packages / layers | `UpdateProjectEndpoint` → `ProjectService.update`; `FindProjectByIdEndpoint` / list / create → `ProjectResponse` with `prefixLocked` |
| API | No new path. Extend `ProjectResponse` with `boolean prefixLocked`. Update rejects when prefix string differs and `countProjectTickets(projectId) > 0` |
| Schema / seed | No change |
| Cross-context | No CDI events. Inject `TicketRepository` into `ProjectService` (or shared helper) for count only |
| Frontend | After codegen: map `prefixLocked`; disable `prefix` control on edit when true; create always editable |
| Tests | Endpoint: change prefix with tickets → 400; same prefix with tickets → 201; change with zero tickets → 201; soft-deleted only → 400. Angular: disabled when locked |

### Packages / layers

| Layer | Type | Responsibility |
|-------|------|----------------|
| Endpoint | `project.update.UpdateProjectEndpoint` | Unchanged path; delegates to service |
| Service | `ProjectService.update` | If `request.prefix()` differs from current and ticket count > 0 → `BadRequestException`; else apply fields including prefix when allowed |
| Service | `ProjectService` response mapping | `prefixLocked = countProjectTickets(id) > 0` on create/update/find/list responses |
| Repository | `TicketRepository.countProjectTickets` | Existing — reuse; no new query required |

### API surface

| Method | Path | Auth | Success | Failure |
|--------|------|------|---------|---------|
| `POST` | `/api/projects/{id}` | manage (PM owner / admin) | `201` + `ProjectResponse` incl. `prefixLocked` | `400` when prefix changes and tickets exist; `403`/`404` unchanged |
| `GET` | `/api/projects/{id}` | view | `ProjectResponse` with `prefixLocked` | unchanged |
| `GET` | `/api/projects` | authenticated | list items include `prefixLocked` | unchanged |
| `POST` | `/api/projects` | PM | create; `prefixLocked` always `false` | unchanged |

Error message (suggested): `Project prefix cannot be changed while the project has tickets`

`ProjectResponse` fields: existing + `boolean prefixLocked`.

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | How does the edit UI know the prefix is locked? | answered | Add `prefixLocked` on `ProjectResponse` (true when ticket count > 0) |
| AQ2 | Reject same-prefix update when locked? | answered | **No** — only reject when the requested prefix **differs** from the stored value |
| AQ3 | New repository method vs reuse count? | answered | Reuse `TicketRepository.countProjectTickets` (already includes soft-deleted) |

### v3 — Admin project update authz + route guards

| Area | Design |
|------|--------|
| Bounded contexts | `project` |
| Packages / layers | `UpdateProjectEndpoint` `@RolesAllowed`; service still `requireManage`; Angular routes for edit + allocation |
| API | Add `Roles.ADMIN` alongside `PROJECT_MANAGER` on update endpoint annotations; business rule stays owner-or-admin via `requireManage` |
| Frontend | Align edit/allocation `canActivate` with hub `canManage` (admin or owner PM) |
| Tests | Admin-without-PM update succeeds; non-owner PM 403; Angular guard specs |

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ4 | Annotation-only ADMIN vs service-only? | answered | **Both** — annotation allows ADMIN JWT into method; `requireManage` still enforces owner-or-admin |
| AQ5 | How to express manage on SPA routes? | answered | Mirror hub: admin **or** (project-manager **and** owner) |

## Changelog

### Catalog compliance — admin update + manage guards — 2026-07-11

**Version:** 3  
**Status:** tasks-ready

**Description:** Fix admin-without-PM project update 403; align edit/allocation SPA reachability with backend `requireManage`. Source: [feature-catalog-review](../reports/feature-catalog-review-1-11-07-2026-16-27-54.md).

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Project allocation | Same manage rule / guard |
| Project hub | **Editar** already shown for admin — API will match |
| Feature catalog | Clear interim “API today PM-only” note when done |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | `UpdateProjectEndpoint` allows ADMIN role into handler | FQ4, AQ4 | ☐ |
| FC2 | Admin-without-PM can update when `requireManage` passes | FQ4 | ☐ |
| FC3 | Non-owner PM still cannot update (403) | Regression | ☐ |
| FC4 | Edit/allocation routes do not invite non-manage users into guaranteed-403 UX | FQ5, AQ5 | ☐ |
| FC5 | Endpoint + Angular tests | Tests | ☐ |
| FC6 | `feature-catalog.md` Edit project Steps updated | Docs | ☐ |

#### Tasks

| ID | Deliverable | Done |
|----|-------------|------|
| T1 | Add `ADMIN` to `@RolesAllowed` on `UpdateProjectEndpoint` (and siblings if needed) | ☑ |
| T2 | `UpdateProjectEndpointTest` — `tech_lead`-style admin-without-PM can update; non-owner PM cannot | ☐ |
| T3 | Align Angular edit + allocation `canActivate` / load with `requireManage` semantics | ☐ |
| T4 | Angular specs for guard/redirect behaviour | ☐ |
| T5 | Docs: feature-catalog Edit project + allocation notes | ☐ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | Admin without PROJECT_MANAGER role → update 201 when manage | T1, T2 | ☐ |
| TC2 | Non-owner PM → update 403 | T2 | ☐ |
| TC3 | Non-manage user blocked from edit/allocation route or CTA | T3, T4 | ☐ |

**Development approval:** — (awaiting explicit task IDs)

### Initial implementation — baseline

**Version:** 1  
**Status:** done

**Description:** Project CRUD for project-manager role; workflow assignment; optional ticket template with **Usar template de ticket** checkbox.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Create ticket | Template pre-fill on project-scoped create |
| Kanban board | Project context for board |
| Ticket import | Global import maps project column |
| Workflow configuration | Project references workflow |
| — | None identified |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Project list matches **Wireframe** | Wireframe | ☑ |
| FC2 | Create/edit form matches **Wireframe** | Wireframe | ☑ |
| FC3 | Optional ticket template on project | Summary | ☑ |
| FC4 | `feature-catalog.md` — Project rows | Impact / Docs | ☑ |

**Implementation notes:** `project-edit.component.ts`; template fields embedded on `Project` entity.

### Immutable project prefix — 2026-07-03

**Version:** 2  
**Status:** done

**Description:** Reject prefix changes on update when the project has one or more tickets (including soft-deleted); UI shows prefix read-only via `prefixLocked` on `ProjectResponse`.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [ticket-management](ticket-management.md) | Identifiers remain stable; soft-deleted tickets keep locking prefix |
| [create-ticket](create-ticket.md) | Identifier generation unchanged |
| [ticket-import](ticket-import.md) | Creating tickets via import locks prefix thereafter |
| — | None identified |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Edit form: prefix disabled when `prefixLocked` | Wireframe, FQ1, AQ1 | ☑ |
| FC2 | API rejects prefix **change** when tickets exist (400) | FQ1, AQ2 | ☑ |
| FC3 | Soft-deleted tickets alone still lock prefix | FQ2 | ☑ |
| FC4 | Same prefix on update succeeds when locked | AQ2 | ☑ |
| FC5 | Empty project (no tickets) can still change prefix | FQ1 | ☑ |
| FC6 | `domain-specification.md` invariant 36 accurate (incl. soft-delete) | Docs, FQ2 | ☑ |
| FC7 | `feature-catalog.md` — Edit project notes read-only prefix when tickets exist | Impact / Docs | ☑ |
| FC8 | OpenAPI / Angular client includes `prefixLocked` | Architecture | ☑ |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | Extend `ProjectResponse` with `prefixLocked`; map from ticket count in `ProjectService` | ☑ |
| T2 | `ProjectService.update` — reject prefix change when `countProjectTickets > 0` | ☑ |
| T3 | `UpdateProjectEndpointTest` — lock, soft-delete, same-prefix, empty-project cases | ☑ |
| T4 | Regenerate API client (`npm run generate:api`) | ☑ |
| T5 | Angular `project-edit`: disable prefix when `prefixLocked`; optional hint | ☑ |
| T6 | `project-edit.component.spec.ts` — prefix disabled when locked | ☑ |
| T7 | Docs: domain-spec (soft-delete clarity on invariant 36), feature-catalog Edit project step | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `UpdateProjectEndpointTest` — change prefix with tickets → 400 | T2, T3 | ☑ |
| TC2 | `UpdateProjectEndpointTest` — soft-deleted ticket only → 400 | T2, T3, FQ2 | ☑ |
| TC3 | `UpdateProjectEndpointTest` — same prefix with tickets → 201 | T2, T3, AQ2 | ☑ |
| TC4 | `UpdateProjectEndpointTest` — change prefix with zero tickets → 201 | T2, T3 | ☑ |
| TC5 | Find/list response includes `prefixLocked` true/false | T1 | ☑ |
| TC6 | `project-edit.component.spec.ts` — prefix control disabled when locked | T5, T6 | ☑ |

**Development approval:** approved 2026-07-10 — tasks: T1, T2, T3, T4, T5, T6, T7

**Implementation notes:** `ProjectResponse.prefixLocked` from `TicketRepository.countProjectTickets`; `ProjectService.update` rejects differing prefix when count > 0. Angular disables prefix + hint. Also fixed `WorkflowService.applyWipLimits` clear/re-add Hibernate conflict (needed for `mvn verify`). Tests: `UpdateProjectEndpointTest`, find/list prefixLocked, `project-edit.component.spec.ts`; `mvn verify` + `npm run build` + Angular specs green (2026-07-10).

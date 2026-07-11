# Project navigation (header)

**Feature version:** 2  
**Status:** tasks-ready  
**Requested:** 2026-07-10

## Summary

Add a global header **Projetos** labeled button visible to **all authenticated users** (any role). The control opens a menu of **viewable** projects so every user can open their project **Kanban** in one click from any page. When the user has no accessible projects, the button is **disabled** and a tooltip shows the empty message. Closes the navigation gap left when the home project grid was removed ([home-screen](home-screen.md) **Q5** / **Q15**) without restoring a home project picker.

Distinct from [project-administration](project-administration.md) (`/projects` CRUD list). Conta **Projetos** and header menu footer **Gerenciar projetos** both remain for PM/admin (**FQ5**).

## Wireframe

**Guide:** layout reference for UI implementation — update when Scope or **FQ*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-10 |

### Widget: global header — Projetos menu (no dedicated route)

| Region | Elements | Notes |
|--------|----------|-------|
| Trigger | Labeled **Projetos** button (`.btn-header` / outlined) in `.header-actions`, left of notifications | All authenticated users (**FQ2**) |
| Menu | One `mat-menu-item` per viewable project (name) → Kanban | **FQ1**, **FQ3** |
| Footer | Divider + **Gerenciar projetos** → `/projects` | PM/admin only (**FQ5**) |
| Empty | Button **disabled**; `matTooltip` = empty message (e.g. Nenhum projeto) | **FQ4** — no menu open |
| Conta | Keep **Projetos** → `/projects` for PM/admin | **FQ5** both |

```
┌─ Header ──────────────────────────────────────────────────────────┐
│ Issues │ [search] │ Novo │ Importar │ … │ [Projetos ▾] │ 🔔 │ 👤 │
└───────────────────────────────────────────────────────────────────┘
                                              │
                    (has projects)            ▼
                                    ┌─────────────────────┐
                                    │ Project Alpha       │  → /project/:id/kanban
                                    │ Project Beta        │  → /project/:id/kanban
                                    │ ─────────────────── │
                                    │ Gerenciar projetos  │  → /projects (PM/admin)
                                    └─────────────────────┘

                    (zero projects)  [Projetos] disabled + tooltip “Nenhum projeto”
```

Header layout otherwise unchanged (brand, ticket search, Novo, Importar, notifications, Conta).

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `project` (list scope); Angular shell |
| Packages / files | `ProjectAccessService.listProjectsForUser`; `ListProjectsEndpointTest`; `ProjectsService`; `project-menu` shell component; `app.html` / `app.ts`; Conta menu unchanged for **Projetos** |
| API | Same `GET /projects` (`listProjects`); **response set changes** for PM: owned ∪ member (viewable), not owned-only |
| UI | Header **Projetos** menu → Kanban; disabled+tooltip when empty; Conta **Projetos** kept; menu footer **Gerenciar projetos** for PM/admin |
| Schema / seed | None |
| Tests | `ListProjectsEndpointTest` (viewable scope, incl. PM-as-member); `project-menu` / `app` Angular specs |
| Docs | feature-catalog, ui-elements-gallery §1.1/§8, domain-spec (Project navigation menu), README § Features, ARCHITECTURE §13 |

### Risks

- Header density — another labeled control next to Novo/Importar.
- `/projects` admin list for PM expands to **member ∪ owned** (same API) — rows may include projects the PM cannot edit; **Editar** already fails for non-owners via API (pre-existing UX gap on that page).
- Disabled button + tooltip: wrapper span so tooltip works when the control is disabled.

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Clicking a project in the header menu opens **Kanban** or **project hub**? | answered | **Kanban** — `/project/:id/kanban` |
| FQ2 | Trigger placement: labeled **Projetos** button or icon-only? | answered | **Labeled button** in header actions (left of notifications) |
| FQ3 | Menu list scope: **viewable** or current `listProjects` (PM = owned only)? | answered | **Viewable** — admin: all; others: member ∪ owned. For **all users**, not PM-only |
| FQ4 | Zero accessible projects behaviour? | answered | Button **disabled**; empty message as **tooltip** (no menu) |
| FQ5 | Conta **Projetos** vs header footer **Gerenciar projetos**? | answered | **Both** — keep Conta item; also footer link in header menu (PM/admin) |

## Architecture

| Area | Design |
|------|--------|
| Bounded contexts | `project` owns list scope; shell consumes via existing API — no new context |
| Packages / layers | `ProjectAccessService.listProjectsForUser` → `ProjectService.listAll` → `ListProjectsEndpoint` (unchanged path). Frontend: `ProjectsService.findAll()` → `ProjectMenuComponent` in shell |
| API | `GET /api/projects` — roles unchanged (`user`, `project-manager`, `admin`). Semantics: return **viewable** projects (align with `canViewProject` / `projectScopeIds`: admin all; else member ∪ owned). No new endpoint (**AQ1**) |
| Schema / seed | None |
| Cross-context | None |
| Frontend | `project-menu` component (`app-project-menu`) in `.header-actions` before `app-notification`. Loads projects when authenticated. Items → `routerLink` `/project/:id/kanban`. Footer **Gerenciar projetos** if `project-manager` or `admin`. Empty: `[disabled]` + `matTooltip`. Conta **Projetos** stays in `app.html` |
| Tests | Extend `ListProjectsEndpointTest` for PM member-of-non-owned; Angular `project-menu.component.spec.ts` (items → kanban, disabled+tooltip, footer visibility) |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | New nav-only endpoint vs change shared `listProjects` scope? | answered | **Change** `listProjectsForUser` to viewable (member ∪ owned; admin all). One API for header and `/projects`. Opened by **FQ3** |
| AQ2 | Inline menu in `AppComponent` vs dedicated shell component? | answered | Dedicated **`ProjectMenuComponent`** (same pattern as notifications) for isolation and specs |
| AQ3 | When to load project list? | answered | On component init while authenticated; reuse `ProjectsService.findAll()` — no polling |

## Changelog

### Catalog compliance — Conta Projetos for admin — 2026-07-11

**Version:** 2  
**Status:** tasks-ready

**Description:** Conta → **Projetos** is `project-manager` only; catalog and **FQ5** promise PM/admin. Admin-without-PM must reach `/projects` via Conta as well as header **Gerenciar projetos**. Source: [feature-catalog-review](../reports/feature-catalog-review-1-11-07-2026-16-27-54.md).

**Impact on other features:** [project-administration](project-administration.md) list entry.

#### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ6 | Show Conta → **Projetos** for admin (not only PM)? | answered | **Yes** — matches original **FQ5** and catalog |

#### Architecture

| Area | Design |
|------|--------|
| Frontend | Conta menu item role: `admin` \|\| `project-manager` |
| Tests | App/shell spec |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Admin-without-PM sees Conta → Projetos | FQ6 | ☐ |
| FC2 | PM still sees Conta → Projetos | Regression | ☐ |
| FC3 | feature-catalog Project list Steps updated | Docs | ☐ |

#### Tasks

| ID | Deliverable | Done |
|----|-------------|------|
| T1 | Conta **Projetos** `*role` includes admin | ☐ |
| T2 | Spec for admin visibility | ☐ |
| T3 | feature-catalog Project list note cleanup | ☐ |

**Development approval:** — (awaiting explicit task IDs)

### Header Projetos menu for all members — 2026-07-10

**Version:** 1  
**Status:** done

**Description:** All authenticated users get a header **Projetos** labeled button listing **viewable** projects; each item opens **Kanban**. Empty: disabled + tooltip. Conta **Projetos** and menu footer **Gerenciar projetos** both for PM/admin. `GET /projects` list scope aligned to viewable.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [home-screen](home-screen.md) | Complements hub path (**Q15**); does not restore home project grid (**Q5**) |
| [kanban-board](kanban-board.md) | New global entry point to board for every user |
| [project-administration](project-administration.md) | `GET /projects` for PM includes memberships (**FQ3** / **AQ1**); Conta **Projetos** kept (**FQ5**) |
| [notifications](notifications.md) | Header adjacency — Projetos left of bell |
| — | None identified |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Labeled **Projetos** button in header for all authenticated users | FQ2, Summary | ☑ |
| FC2 | Menu lists **viewable** projects (admin all; else member ∪ owned) | FQ3, AQ1 | ☑ |
| FC3 | Each project item navigates to `/project/:id/kanban` | FQ1, Wireframe | ☑ |
| FC4 | Zero projects: button disabled + tooltip empty message | FQ4 | ☑ |
| FC5 | Conta **Projetos** kept for PM/admin | FQ5 | ☑ |
| FC6 | Header menu footer **Gerenciar projetos** → `/projects` for PM/admin | FQ5, Wireframe | ☑ |
| FC7 | Header menu matches **Wireframe** | Wireframe | ☑ |
| FC8 | `GET /projects` / `listProjectsForUser` uses viewable scope | AQ1, Architecture | ☑ |
| FC9 | `feature-catalog.md` — shell Projetos → Kanban path | Impact / Docs | ☑ |
| FC10 | `ui-elements-gallery.md` — header Projetos menu | Impact / Docs | ☑ |
| FC11 | `domain-specification.md` — Project navigation menu term | Impact / Docs | ☑ |
| FC12 | `README.md` — feature bullet for header Projetos | Impact / Docs | ☑ |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | Change `ProjectAccessService.listProjectsForUser` to viewable scope (admin: all; else member ∪ owned, deduped); update `ListProjectsEndpointTest` (incl. PM sees project as member only) | ☑ |
| T2 | Add `ProjectMenuComponent` (load via `ProjectsService`, menu items → Kanban, disabled+tooltip when empty, **Gerenciar projetos** for PM/admin) + unit specs | ☑ |
| T3 | Wire `app-project-menu` into `app.html` header actions (before notifications); keep Conta **Projetos** | ☑ |
| T4 | Docs: feature-catalog, ui-elements-gallery, domain-spec, README | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `ListProjectsEndpointTest` — viewable scope (user member; PM owned; PM as member of non-owned; non-member excluded) | T1 | ☑ |
| TC2 | `project-menu.component.spec.ts` — items link to kanban; empty disabled+tooltip; footer role-gated | T2 | ☑ |
| TC3 | Conta **Projetos** still present for PM (`app.html`) | T3, FQ5 | ☑ |

**Development approval:** approved 2026-07-10 — tasks: T1, T2, T3, T4

**Implementation notes:** `ProjectAccessService.listProjectsForUser` unions member + owned (deduped, name-sorted). Shell `ProjectMenuComponent` in header. `mvn clean verify` green; `npm run build` green; Angular `project-menu` + `app` specs green (2026-07-10).

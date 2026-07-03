# Feature change requests

One markdown file per **high-level capability**: `feature/<feature-slug>.md` (kebab-case).

**Mandatory process:** [development-process.mdc](../.cursor/rules/development-process.mdc) — feature analysis → architecture design → task break → **explicit task approval** → TDD. Answering FQ/AQ is not approval. No code before approved task IDs.

## Resolving `<feature-slug>`

See rule § **Resolve `<feature-slug>`** for the full gate. Summary:

1. Name the **capability** (not the task).
2. Derive a 2–4 word kebab-case slug aligned with [feature-catalog.md](../docs/feature-catalog.md).
3. Search `feature/*.md` and the catalog for a related doc — **extend** if it exists.
4. If multiple slugs fit or scope is ambiguous → **ask the user**; do not pick silently.

| Request | Slug | Related? |
|---------|------|----------|
| Add export to ticket list | `ticket-search` or `ticket-export` | Check `feature/*.md`; ask if both could apply |
| Fix CSV column mapping on import | `ticket-import` | Extend existing import doc |
| New password policy on settings page | `account-settings` | Extend if `feature/account-settings.md` exists |

## Template

Copy into `feature/<feature-slug>.md` and fill in **before** architecture design (phase 1). Phases 2–5 add Architecture, Tasks, approval, and implementation to each changelog entry.

```markdown
# <Human-readable feature name>

**Feature version:** 1  
**Status:** planned | in-progress | done  
**Requested:** YYYY-MM-DD

## Summary

One paragraph: what is being asked and why.

## Wireframe

**Guide:** layout reference for UI implementation — **update whenever** Scope, routes, or **FQ*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)). Phase 5 Angular work must match this section unless the wireframe is revised here first.

| Field | Value |
|-------|-------|
| **Source** | [Excalidraw](https://excalidraw.com/#…) · ASCII below · N/A — no dedicated UI page |
| **Last updated** | YYYY-MM-DD |

### Screen: `/example-route`

| Region | Elements | Notes |
|--------|----------|-------|
| Header | (global shell — out of scope if unchanged) | |
| Body | … | Gallery classes per [ui-elements-gallery.md](../docs/ui-elements-gallery.md) |

```
┌────────────────────────────────────────┐
│  Example layout (ASCII)                │
└────────────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | e.g. `ticket`, `workflow` |
| Packages / files | Main touch points |
| API | New/changed endpoints, Request/Response records |
| UI | Routes, components, services |
| Schema / seed | `V1.0.0__Database_Creation.sql`, `dev-import.sql` |
| Tests | Endpoint tests, Angular specs, ArchUnit |
| Docs | domain-spec, feature-catalog, README, ARCHITECTURE |

### Risks

Non-question risks and known gaps (optional bullet list).

### Feature questions (FQ*n*)

Product, scope, UX, and domain decisions. Reference by **FQ*n*** in tasks, changelog, and discussion. Status: `open` | `answered` | `not valid`.

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | e.g. Should export include soft-deleted tickets? | open | |
| FQ2 | e.g. Max CSV file size for import? | answered | 10 MB; enforced in upload endpoint |

**Gate:** phase 2 (architecture design) requires no blocking **FQ*n*** — resolve or mark `not valid` first.

**Impact review:** after each answered feature question, update Impact / Risks / **Wireframe** / **Feature checklist** / tasks and domain spec as needed; new **FQ*n*** or **AQ*n*** may be added — see [development-process.mdc](../.cursor/rules/development-process.mdc) § Impact review when answering open questions.

## Architecture

**Guide:** technical design for the current changelog entry — **update whenever** **AQ*n*** answers or footprint-changing **FQ*n*** answers change ([architecture-design.mdc](../.cursor/rules/architecture-design.mdc)). Phase 5 backend work must match this section unless revised here first.

| Area | Design |
|------|--------|
| Bounded contexts | e.g. `ticket`, `project` — dependency direction |
| Packages / layers | Endpoint → Service → Repository per operation |
| API | Paths, Request/Response records, roles |
| Schema / seed | Tables, columns, Flyway baseline changes |
| Cross-context | CDI events, shared services |
| Frontend | Routes, services, API codegen |
| Tests | Endpoint tests, ArchUnit, Angular specs |

### Architecture questions (AQ*n*)

Technical design decisions. Reference by **AQ*n*** in tasks, **Architecture**, and **Feature checklist**. Status: `open` | `answered` | `not valid`.

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | e.g. New `HomeService` vs extend `TicketService`? | open | |
| AQ2 | e.g. Composite key vs surrogate id for project members? | answered | Composite `(project_id, user_id)` |

**Gate:** phase 3 (task break) requires no blocking **AQ*n*** — resolve or mark `not valid` first. Phase 4 requires explicit task ID approval — answering FQ/AQ is **not** approval.

## Changelog

### <Change name> — YYYY-MM-DD

**Version:** 1  
**Status:** planned | architecture-ready | tasks-ready | approved | in-progress | done

**Description:** What this specific change request does.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| e.g. Ticket board | Column filter behaviour unchanged |
| e.g. Notifications | New event on export completion |
| — | None identified |

#### Feature checklist (phase 1 — build; update through phase 5; recheck before done)

Verifiable **product and scope** criteria — not implementation steps. Build from **Scope**, **Decisions**, **Impact** (API, UI, schema, docs), and **Impact on other features**. One row per user-visible behaviour, invariant, doc deliverable, or cross-feature obligation.

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | e.g. Export includes only non-deleted tickets | FQ1, S2 | ☐ |
| FC2 | e.g. `feature-catalog.md` — export happy path | Impact / Docs | ☐ |
| FC3 | e.g. `domain-specification.md` — Export term | Impact / Docs | ☐ |

**Lifecycle:** create or extend in phase 1; add rows when **FQ/AQ** answers or scope change (impact review); align with tasks in phase 3. During phase 5, mark **Done** only after verifying the criterion in code or docs. **Before `done`:** recheck every row — re-read criterion against implementation; uncheck and fix gaps; do not mark `done` with open FC items.

#### Tasks (phase 3 — required before approval)

| ID | Task | Done |
|----|------|------|
| T1 | e.g. Add `Phase` entity + Flyway baseline | ☐ |
| T2 | e.g. `CreatePhaseEndpoint` + test | ☐ |

#### Test coverage (phase 3 — required before done)

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `CreatePhaseEndpointTest` | T1, T2 | ☐ |
| TC2 | `phase.service.spec.ts` | T3 | ☐ |

**Development approval:** pending | approved YYYY-MM-DD — tasks: T1, T2

Required before phase 5. User must name task IDs (e.g. "Approve T1–T8"). Answering feature or architecture questions alone does **not** satisfy this line.

**Implementation notes:** (fill after done — key files, tests run)
```

## Feature index (baseline)

| Capability | File | Feature version | Status |
|------------|------|-----------------|--------|
| Authentication (login, password recovery) | `feature/authentication.md` | 1 | done |
| Account settings | `feature/account-settings.md` | 1 | done |
| Ticket management (detail, comments, history, subscribe) | `feature/ticket-management.md` | 1 | done |
| Ticket search (query language, saved queries) | `feature/ticket-search.md` | 2 | tasks-ready |
| Create ticket | `feature/create-ticket.md` | 1 | done |
| Import tickets (CSV) | `feature/ticket-import.md` | 1 | done |
| Kanban board | `feature/kanban-board.md` | 1 | done |
| Project dashboard | `feature/project-dashboard.md` | 1 | done |
| Project administration | `feature/project-administration.md` | 1 | done |
| User management | `feature/user-management.md` | 1 | done |
| Workflow configuration | `feature/workflow-configuration.md` | 1 | done |
| Categories | `feature/categories.md` | 1 | done |
| Notifications (SSE, in-app) | `feature/notifications.md` | 1 | done |
| Email delivery | `feature/email-delivery.md` | 1 | done |
| Phase and version management | `feature/phase-management.md` | 4 | planned |
| UI design system (class consistency) | `feature/ui-design-system.md` | 1 | done |
| Home screen (personal work hub) | `feature/home-screen.md` | 1 | tasks-ready |

## Versioning

- **Feature version** (document header) — highest changelog **Version** number; increment when adding a changelog entry.
- **Changelog Version** — sequential integer per entry (`1`, `2`, `3`, …); stable once published; never renumber.
- **Feature questions** — sequential **FQ1**, **FQ2**, … per feature file; product scope and UX; cite as **FQ3** in tasks and approval notes.
- **Architecture questions** — sequential **AQ1**, **AQ2**, … per feature file; technical design; cite as **AQ2** in tasks and **Architecture** section.
- **Legacy Q*n*** — treated as **FQ*n*** until the feature doc is updated.
- **Feature checklist** — sequential `FC1`, `FC2`, … per changelog entry; cite scope (**S2**), **FQ/AQ**, Wireframe, Architecture, or Impact in **Source**; recheck all rows before `done`.
- **Wireframe** — top-level `## Wireframe` on every feature doc; revise on scope/UI changes; cite in FC rows as **Wireframe**.
- **Architecture** — top-level `## Architecture` on every feature doc with an in-progress changelog entry; revise on **AQ*n*** or footprint-changing **FQ*n*** answers; cite in FC rows as **Architecture**.
- **Approval vs questions** — answering FQ/AQ updates the feature doc only; phase 4 requires explicit task ID approval ([development-process.mdc](../.cursor/rules/development-process.mdc) § Strict phase gate).

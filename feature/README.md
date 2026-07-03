# Feature change requests

One markdown file per **high-level capability**: `feature/<feature-slug>.md` (kebab-case).

**Mandatory process:** [development-process.mdc](../.cursor/rules/development-process.mdc) — feature analysis → task break → **explicit task approval** → TDD. Answering Q*n* is not approval. No code before approved task IDs.

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

Copy into `feature/<feature-slug>.md` and fill in **before** task break (phase 1). Phases 2–4 add Tasks, approval, and implementation to each changelog entry.

```markdown
# <Human-readable feature name>

**Feature version:** 1  
**Status:** planned | in-progress | done  
**Requested:** YYYY-MM-DD

## Summary

One paragraph: what is being asked and why.

## Wireframe

**Guide:** layout reference for UI implementation — **update whenever** Scope, routes, or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)). Phase 4 Angular work must match this section unless the wireframe is revised here first.

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

### Open questions

Reference by **Q*n*** in tasks, changelog, and discussion. Status: `open` | `answered` | `not valid`.

| # | Question | Status | Answer |
|---|----------|--------|--------|
| Q1 | e.g. Should export include soft-deleted tickets? | open | |
| Q2 | e.g. Max CSV file size for import? | answered | 10 MB; enforced in upload endpoint |

**Gate:** phase 2 (task break) requires no `open` questions that block scope — resolve or mark `not valid` first. Phase 3 requires explicit task ID approval — answering Q*n* is **not** approval.

**Impact review:** after each answered question, update Impact / Risks / **Wireframe** / **Feature checklist** / tasks and domain spec as needed; new **Q*n*** may be added — see [development-process.mdc](../.cursor/rules/development-process.mdc) § Impact review when answering open questions.

## Changelog

### <Change name> — YYYY-MM-DD

**Version:** 1  
**Status:** planned | tasks-ready | approved | in-progress | done

**Description:** What this specific change request does.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| e.g. Ticket board | Column filter behaviour unchanged |
| e.g. Notifications | New event on export completion |
| — | None identified |

#### Feature checklist (phase 1 — build; update through phase 4; recheck before done)

Verifiable **product and scope** criteria — not implementation steps. Build from **Scope**, **Decisions**, **Impact** (API, UI, schema, docs), and **Impact on other features**. One row per user-visible behaviour, invariant, doc deliverable, or cross-feature obligation.

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | e.g. Export includes only non-deleted tickets | Q1, S2 | ☐ |
| FC2 | e.g. `feature-catalog.md` — export happy path | Impact / Docs | ☐ |
| FC3 | e.g. `domain-specification.md` — Export term | Impact / Docs | ☐ |

**Lifecycle:** create or extend in phase 1; add rows when **Q*n*** answers or scope change (impact review); align with tasks in phase 2. During phase 4, mark **Done** only after verifying the criterion in code or docs. **Before `done`:** recheck every row — re-read criterion against implementation; uncheck and fix gaps; do not mark `done` with open FC items.

#### Tasks (phase 2 — required before approval)

| ID | Task | Done |
|----|------|------|
| T1 | e.g. Add `Phase` entity + Flyway baseline | ☐ |
| T2 | e.g. `CreatePhaseEndpoint` + test | ☐ |

#### Test coverage (phase 2 — required before done)

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `CreatePhaseEndpointTest` | T1, T2 | ☐ |
| TC2 | `phase.service.spec.ts` | T3 | ☐ |

**Development approval:** pending | approved YYYY-MM-DD — tasks: T1, T2

Required before phase 4. User must name task IDs (e.g. "Approve T1–T8"). Answering open questions alone does **not** satisfy this line.

**Implementation notes:** (fill after done — key files, tests run)
```

## Feature index (baseline)

| Capability | File | Feature version | Status |
|------------|------|-----------------|--------|
| Authentication (login, password recovery) | `feature/authentication.md` | 1 | done |
| Account settings | `feature/account-settings.md` | 1 | done |
| Ticket management (detail, comments, history, subscribe) | `feature/ticket-management.md` | 1 | done |
| Ticket search | `feature/ticket-search.md` | 1 | done |
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
- **Open questions** — sequential `Q1`, `Q2`, … per feature file; numbers are never reused; cite as **Q3** in tasks and approval notes.
- **Feature checklist** — sequential `FC1`, `FC2`, … per changelog entry; cite scope (**S2**), **Q*n***, Wireframe, or Impact in **Source**; recheck all rows before `done`.
- **Wireframe** — top-level `## Wireframe` on every feature doc; revise on scope/UI changes; cite in FC rows as **Wireframe**.
- **Approval vs questions** — answering Q*n* updates the feature doc only; phase 3 requires explicit task ID approval ([development-process.mdc](../.cursor/rules/development-process.mdc) § Strict phase gate).

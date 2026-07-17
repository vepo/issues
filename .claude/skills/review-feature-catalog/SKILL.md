---
name: review-feature-catalog
description: Walk every feature-catalog row against routes, roles, and happy paths; report compliance gaps and backlog suggestions. Read-only. User-invoked only (/review-feature-catalog).
disable-model-invocation: true
---

You are the **Product Owner** for Issues running a **full feature-catalog review**. Act as described in `.claude/agents/product-owner.md`. Produce a **read-only product audit** — do **not** change code or docs unless the user explicitly asks to apply fixes afterward.

**Prerequisites:** Read `docs/feature-catalog.md`, `docs/domain-specification.md`, and `docs/backlog.md`. Skim Angular routes under `src/main/webui/` and relevant endpoints when verifying a row.

## Scope

Default: **every** row in the feature catalog (UI table + API-only table) and **Dev personas**.

If the user names a Feature name, route prefix, or `feature/<slug>.md`, restrict to that subset but still note cross-feature navigation links (e.g. hub → Kanban).

## Output

Write one report:

`reports/feature-catalog-review-{sequential}-{dd-MM-yyyy-HH-mm-ss}.md`

Severity: `critical` | `major` | `minor` | `suggestion`.

**Do not ask for confirmation** before starting. **Do not** edit `src/**`, catalog, or backlog in this command.

---

## Phase 1 — Inventory

1. List all catalog **Feature** rows (UI + API-only) with Route / API and Roles.
2. Inventory Angular routes (router config) and primary shell menus.
3. Note matching `feature/*.md` files where a capability has analysis.

## Phase 2 — Route and role compliance

For each catalog row:

| Check | Pass criteria |
|-------|----------------|
| Route exists | Path in router matches **Route** (params allowed) |
| Reachable | Menu or documented entry points match **Steps** |
| Roles | Guards / menu visibility / API roles align with **Roles** |
| Dev persona | At least one persona can exercise the happy path |

Mark **missing implementation**, **extra undocumented route**, or **role mismatch**.

## Phase 3 — Happy-path steps

For each UI row, verify **Steps** against UI labels and flows:

- Step verbs and screen names exist (Portuguese labels as in catalog)
- Linked features (e.g. Burndown from Kanban) still exist
- Capability gates (`GET /auth/capabilities`, LOCAL-only) match catalog notes
- Broken or impossible steps → finding with evidence

Sample deeply where risk is high (auth, ticket detail, import, admin); lighter check for obvious list/detail CRUD if routes and labels match.

## Phase 4 — Feature docs and backlog

1. Catalog capabilities with behaviour gaps → suggest `feature/<slug>` analysis or extend existing doc (do not create files in this command).
2. `feature/*.md` marked `done` but catalog/README stale → finding.
3. Implemented flows missing from catalog → catalog-add suggestions.
4. Product gaps (no catalog row, clear user need) → backlog-shaped proposals (Idea, why, slug, suggested Order) — do not edit `docs/backlog.md` until the user accepts.

## Phase 5 — Cross-checks

- Domain terms in catalog Steps match `docs/domain-specification.md`
- README Features bullets vs catalog (gross mismatches only)
- API-only rows still used by a UI surface or intentionally headless

## Report template

```markdown
# Feature catalog review — Issues

## Summary
(2–3 sentences: overall compliance)

## Coverage
| Catalog feature | Route / API | Status | Notes |
|-----------------|-------------|--------|-------|
| … | … | ok / gap / stale | … |

## Findings

### Critical
- ...

### Major
- ...

### Minor / suggestions
- ...

## Catalog fix list
- Row "…" — change …

## Backlog proposals (not applied)
| Suggested Order | Idea | Slug | Why |
|----------------:|------|------|-----|
| … | … | … | … |

## Recommended next steps
1. …
```

Start the full-catalog review now.

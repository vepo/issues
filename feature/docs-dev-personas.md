# Docs & dev personas sync

**Feature version:** 1  
**Status:** tasks-ready  
**Requested:** 2026-07-11 (from [feature-catalog-review](../reports/feature-catalog-review-1-11-07-2026-16-27-54.md))

## Summary

Keep **Dev personas**, README logins, and `dev-import.sql` aligned so local happy-path exploration matches documentation. Critical finding: catalog/README listed `@issues.vepo.dev` emails that are not seeded; seed uses `@issues.ui`. Also fix orphan saved-query owner email referencing the missing user.

## Wireframe

N/A — docs and seed only (no new UI).

## Impact

| Area | Effect |
|------|--------|
| Docs | `feature-catalog.md` § Dev personas; `README.md` login table |
| Seed | `dev-import.sql` saved-query owner email |
| Tests | Dev smoke / Given builders if they hardcode old emails |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Canonical persona domain? | answered | **`@issues.ui`** as in current `dev-import.sql` — update docs to seed, not invent missing users |

## Architecture

| Area | Design |
|------|--------|
| Schema | No Flyway change |
| Seed | Change saved-query `WHERE email = …` to an existing `@issues.ui` user (e.g. `junior_dev@issues.ui`) |
| Docs | Catalog + README already partially updated in review follow-up; finish interim notes when code tasks elsewhere land |

## Changelog

### Sync personas and orphan seed email — 2026-07-11

**Version:** 1  
**Status:** tasks-ready

**Description:** Align exploration docs with seed; fix orphan saved-query owner.

**Impact on other features:** Ticket search saved queries seed; onboarding exploration.

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Catalog Dev personas = `dev-import.sql` emails/roles | FQ1, Review critical | ☑ *(docs applied 2026-07-11)* |
| FC2 | README login table matches seed personas | Review critical | ☑ *(docs applied 2026-07-11)* |
| FC3 | Saved-query seed references existing user email | Review critical | ☐ |
| FC4 | Grep repo for stale `@issues.vepo.dev` persona docs | Docs | ☐ |

#### Tasks

| ID | Deliverable | Done |
|----|-------------|------|
| T1 | Catalog + README persona sync | ☑ |
| T2 | Fix `dev-import.sql` saved-query owner (`user@issues.vepo.dev` → existing `@issues.ui`) | ☐ |
| T3 | Grep/fix remaining stale persona emails in docs/tests/Given if any | ☐ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | Dev start loads seed without missing-user FK/no-op for saved query | T2 | ☐ |

**Development approval:** — (awaiting T2–T3; T1 already applied as docs-only)

## Implementation notes

- Catalog + README updated from review on 2026-07-11 before code approval.
- Do not reintroduce `@issues.vepo.dev` admin/pm/user personas unless also added to seed.

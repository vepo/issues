# Feature change requests

One markdown file per **high-level capability**: `feature/<feature-slug>.md` (kebab-case).

**Mandatory process:** [development-process.mdc](../.cursor/rules/development-process.mdc) — feature analysis → task break → development approved → TDD. No code before approval.

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

**Status:** planned | in-progress | done  
**Requested:** YYYY-MM-DD

## Summary

One paragraph: what is being asked and why.

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

### Risks and open questions

- …

## Changelog

### <Change name> — YYYY-MM-DD

**Status:** planned | tasks-ready | approved | in-progress | done

**Description:** What this specific change request does.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| e.g. Ticket board | Column filter behaviour unchanged |
| e.g. Notifications | New event on export completion |
| — | None identified |

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

**Implementation notes:** (fill after done — key files, tests run)
```

## Feature index (baseline)

| Capability | File | Status |
|------------|------|--------|
| Authentication (login, password recovery) | `feature/authentication.md` | done |
| Account settings | `feature/account-settings.md` | done |
| Ticket management (detail, comments, history, subscribe) | `feature/ticket-management.md` | done |
| Ticket search | `feature/ticket-search.md` | done |
| Create ticket | `feature/create-ticket.md` | done |
| Import tickets (CSV) | `feature/ticket-import.md` | done |
| Kanban board | `feature/kanban-board.md` | done |
| Project dashboard | `feature/project-dashboard.md` | done |
| Project administration | `feature/project-administration.md` | done |
| User management | `feature/user-management.md` | done |
| Workflow configuration | `feature/workflow-configuration.md` | done |
| Categories | `feature/categories.md` | done |
| Notifications (SSE, in-app) | `feature/notifications.md` | done |
| Email delivery | `feature/email-delivery.md` | done |
| Phase and version management | `feature/phase-management.md` | planned |

Home (`/`) is a landing shell only — no separate feature doc.

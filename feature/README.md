# Feature change requests

One markdown file per **high-level capability**: `feature/<feature-slug>.md` (kebab-case).

Create or update **before** writing code. Rule: [change-request-analysis.mdc](../.cursor/rules/change-request-analysis.mdc).

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

Copy into `feature/<feature-slug>.md` and fill in before implementation:

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

**Status:** planned | in-progress | done

**Description:** What this specific change request does.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| e.g. Ticket board | Column filter behaviour unchanged |
| e.g. Notifications | New event on export completion |
| — | None identified |

**Implementation notes:** (fill after done — key files, tests run)
```

## Example slugs

| Capability (catalog) | File |
|----------------------|------|
| Import tickets (CSV) | `feature/ticket-import.md` |
| Account settings | `feature/account-settings.md` |
| Kanban board | `feature/kanban-board.md` |

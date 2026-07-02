# Conventions checklist

Tracks gaps between documentation and code, and agentic development setup status.

## Cursor / agent setup

| Item | Status |
|------|--------|
| `AGENTS.md` | Done |
| `ARCHITECTURE.md` | Done |
| `docs/domain-specification.md` | Done |
| `docs/feature-catalog.md` | Done |
| `.cursor/rules/` (pillars + file-scoped) | Done |
| `.cursor/agents/` (TDD + domain + docs) | Done |
| `.cursor/commands/` (fix tests, Sonar, coverage, structure review) | Done |
| Legacy `.cursor/rules/vo-naming-pattern.mdc` | Replaced by `issues-http-contract.mdc` |
| Legacy `.cursor/rules/always-write-tests.mdc` | Replaced by `issues-model.mdc` § TDD + `issues-tests.mdc` |

## Project rename (morpho-board → issues)

| Item | Status |
|------|--------|
| Maven artifact `issues` | Done |
| Package `dev.vepo.issues` | Done |
| Sonar project key `vepo_issues` | Done |
| Application class `IssuesApplication` | Done |
| UI product name "Issues" | Done |
| GitHub repo rename (`vepo/issues`) | External — update remote if needed |
| Workspace folder rename | Optional — rename checkout directory to `issues` |

## Doc debt

| Item | Notes |
|------|-------|
| Endpoint security gaps | `CategoryEndpoint`, `StatusEndpoint`, `ProjectTicketEndpoint` lack `@DenyAll` / `@RolesAllowed` |
| Frontend API base URL | Hardcoded `http://localhost:8080/api` in Angular services |
| `application-guidelines.md` | Not created — use `feature-catalog.md` for route flows |
| OpenAPI as user doc | Live at `/openapi` when running; no static export in `docs/` |

## When closing a gap

1. Fix code or docs.
2. Update this checklist row to Done.
3. Cross-link from `ARCHITECTURE.md` §13 if it was a known gap.

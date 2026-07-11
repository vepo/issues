# Conventions checklist

Tracks gaps between documentation and code, and agentic development setup status.

## Cursor / agent setup

| Item | Status |
|------|--------|
| `.cursor/skills/issues-agent/` | Done — backup MCP/PAT skill ([agentic-integration](../../feature/agentic-integration.md) **FQ8**) |
| `AGENTS.md` | Done |
| `ARCHITECTURE.md` | Done |
| `docs/domain-specification.md` | Done |
| `docs/feature-catalog.md` | Done |
| `.cursor/rules/` (pillars + file-scoped) | Done |
| `change-request-analysis.mdc` + `feature/` change docs | Done |
| `architecture-design.mdc` (phase 2 architecture design) | Done |
| `development-process.mdc` (five-phase gate + TDD) | Done |
| `.cursor/agents/` (TDD + domain + docs) | Done |
| `.cursor/commands/` (fix tests, Sonar, coverage, structure review) | Done |
| `readme.mdc` (keep README features current) | Done |
| `issues-ux.mdc` (Nielsen heuristics + flat UI design) | Done |
| `docs/ui-elements-gallery.md` | Done |
| `docs/ui-nielsen-audit.md` | Done |
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
| Endpoint security gaps | Done — all 39 endpoints use `@DenyAll` + `@RolesAllowed` |
| Frontend API base URL | Done — relative `/api` via generated client + facades |
| OpenAPI → TypeScript codegen | Done — `mvn test` + `npm run generate:api`; output gitignored |
| `application-guidelines.md` | Not created — use `feature-catalog.md` for route flows |
| OpenAPI as user doc | Live at `/openapi`; test export at `target/openapi/openapi.yaml` |

## When closing a gap

1. Fix code or docs.
2. Update this checklist row to Done.
3. Cross-link from `ARCHITECTURE.md` §13 if it was a known gap.

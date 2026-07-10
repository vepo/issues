# Agent instructions (Issues)

Read these before changing code or tests:

| Document | Purpose |
|----------|---------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Stack, packages, patterns, API map, naming, feature workflow |
| [docs/domain-specification.md](docs/domain-specification.md) | Ubiquitous language, bounded contexts, invariants |
| [docs/feature-catalog.md](docs/feature-catalog.md) | UI routes and navigation paths |
| [docs/backlog.md](docs/backlog.md) | Ordered product backlog (ideas and priority) |
| [docs/ui-elements-gallery.md](docs/ui-elements-gallery.md) | UI element catalog — flat UI principles, properties, style, behavior |
| [docs/conventions-checklist.md](docs/conventions-checklist.md) | Doc debt and agent setup status |
| [feature/](feature/) | Feature analysis, tasks, approval, and changelog per capability |
| [.cursor/rules/](.cursor/rules/) | Four pillars + file-scoped detail |
| [.cursor/agents/](.cursor/agents/) | Project subagents (specialized behaviour) |

**Not in production yet**: This project is not in production yet. There is no need to keep legacy or update any production environment. Schema changes: amend `V1.0.0__Database_Creation.sql` only — see [issues-flyway.mdc](.cursor/rules/issues-flyway.mdc).

**Development process:** [development-process.mdc](.cursor/rules/development-process.mdc) — (1) feature analysis → (2) architecture design → (3) task break → (4) **explicit task approval** → (5) TDD. Each feature doc has **Wireframe** and **Architecture** sections; each changelog entry maintains a **Feature checklist** (**FC*n***) **rechecked before `done`**. Two question kinds: **FQ*n*** (product) and **AQ*n*** (technical). Answering FQ/AQ triggers a mandatory **impact review** (not approval). No code before approved task IDs. **Never end with non-working code** — `mvn verify` green before stopping.

**API codegen:** after backend endpoint changes, run `mvn test` then `cd src/main/webui && npm run generate:api`. Endpoints live in `{context}.{action}` subpackages — one HTTP method per class (e.g. `user.create.CreateUserEndpoint`).

## Agents vs commands

| Surface | Location | Purpose |
|---------|----------|---------|
| **Subagents** | `.cursor/agents/*.md` | Specialized system prompts — TDD, domain modeling, API contract review. Delegate by name or let Cursor route from `description`. |
| **Commands** | `.cursor/commands/*.md` | Repeatable workflows you slash-invoke — fix all tests, Sonar loop, coverage loop, structure review. |

## Rules — four pillars (always on)

| Pillar | Rule | Covers |
|--------|------|--------|
| 1. Building the model | [issues-model.mdc](.cursor/rules/issues-model.mdc) | Domain language, architecture, packages, **TDD guidance**, doc triggers |
| 2. Testing | [issues-testing.mdc](.cursor/rules/issues-testing.mdc) | Tiered Maven/Angular commands, impact map, failure workflow |
| 3. Coding quality | [issues-quality.mdc](.cursor/rules/issues-quality.mdc) | Finish gate, ReadLints, `mvn verify`, standards index |
| 4. Platform usage | [issues-platform.mdc](.cursor/rules/issues-platform.mdc) | Java 21, Quarkus, Angular, approved libraries, tooling boundaries |

Additional always-on rules: [development-process.mdc](.cursor/rules/development-process.mdc) (five-phase gate + TDD), [change-request-analysis.mdc](.cursor/rules/change-request-analysis.mdc) (phase 1 feature analysis), [architecture-design.mdc](.cursor/rules/architecture-design.mdc) (phase 2 architecture design), [backlog-management.mdc](.cursor/rules/backlog-management.mdc) (ordered product backlog), [issues-core.mdc](.cursor/rules/issues-core.mdc), [domain-model.mdc](.cursor/rules/domain-model.mdc), [issues-layered-architecture.mdc](.cursor/rules/issues-layered-architecture.mdc), [issues-bounded-contexts.mdc](.cursor/rules/issues-bounded-contexts.mdc), [static-analysis.mdc](.cursor/rules/static-analysis.mdc), [development-experience.mdc](.cursor/rules/development-experience.mdc), [feature-catalog.mdc](.cursor/rules/feature-catalog.mdc), [readme.mdc](.cursor/rules/readme.mdc) (keep README features and quick start current).

No content is duplicated across pillars — each hub links to file-scoped rules for detail.

## File-scoped rules

| Rule | Globs | Topic |
|------|-------|-------|
| [issues-http-contract.mdc](.cursor/rules/issues-http-contract.mdc) | `**/*Endpoint.java`, `**/*Request.java`, `**/*Response.java` | Request/Response records; no VO/DTO suffix |
| [issues-java.mdc](.cursor/rules/issues-java.mdc) | `**/*.java` | Style, logging, `var`, streams |
| [issues-format-imports.mdc](.cursor/rules/issues-format-imports.mdc) | `**/*.java` | Imports and formatter |
| [issues-strings.mdc](.cursor/rules/issues-strings.mdc) | `src/main/java/**/*.java` | String building |
| [issues-jpa.mdc](.cursor/rules/issues-jpa.mdc) | `*Repository.java` | EntityManager queries |
| [issues-tests.mdc](.cursor/rules/issues-tests.mdc) | `src/test/**` | REST Assured, Given, ArchUnit, domain narrative |
| [issues-angular.mdc](.cursor/rules/issues-angular.mdc) | `src/main/webui/**` | Components, services, Material |
| [issues-ux.mdc](.cursor/rules/issues-ux.mdc) | `src/main/webui/**` | Nielsen heuristics, gallery gate, flat UI design |
| [issues-test-failure-diagnosis.mdc](.cursor/rules/issues-test-failure-diagnosis.mdc) | `src/test/**` | Failure classification and reports |
| [documentation.mdc](.cursor/rules/documentation.mdc) | `docs/**`, `README.md` | User-facing docs maintenance |
| [readme.mdc](.cursor/rules/readme.mdc) | always on | README features, stack, quick start |
| [dev-import-sql-safety.mdc](.cursor/rules/dev-import-sql-safety.mdc) | `dev-import.sql`, migrations | Safe dev seed changes |
| [issues-flyway.mdc](.cursor/rules/issues-flyway.mdc) | always on | Pre-production: amend `V1.0.0` only, no `V1.0.x` files |

## Project subagents (`.cursor/agents/`)

| Subagent | When to delegate |
|----------|------------------|
| [tdd-red](.cursor/agents/tdd-red.md) | New behaviour — **create** a failing test only (no production code) |
| [tdd-green](.cursor/agents/tdd-green.md) | After Red — minimal production code to pass the test |
| [tdd-refactor](.cursor/agents/tdd-refactor.md) | After Green — design cleanup, tests stay green |
| [domain-model](.cursor/agents/domain-model.md) | Before coding — domain-spec and vocabulary |
| [api-compliance](.cursor/agents/api-compliance.md) | Before merge — REST contract and ArchUnit rules |
| [docs-sync](.cursor/agents/docs-sync.md) | After API/behaviour change — architecture and feature catalog |

**TDD cycle (phase 5 only):** feature analysis → architecture design → task break → user approval → `tdd-red` → `tdd-green` → `tdd-refactor` per approved task.

Example: *"Use tdd-red to create a test for …"*

## Built-in Task subagents

| Subagent | When to use |
|----------|-------------|
| `explore` | Map codebase before a large feature |
| `shell` | Maven/npm loops, git, CI reproduction |
| `ci-investigator` | Single failing PR check |
| `bugbot` | User-requested diff review |
| `security-review` | User-requested security review |
| `generalPurpose` | Multi-step work outside specialists |

## Commands (workflows only)

| Command | Purpose |
|---------|---------|
| [fix_tests.md](.cursor/commands/fix_tests.md) | Loop until tests pass |
| [fix_sonar_issues.md](.cursor/commands/fix_sonar_issues.md) | Static analysis fixes |
| [increase_coverage.md](.cursor/commands/increase_coverage.md) | Coverage improvements |
| [review_code_structure.md](.cursor/commands/review_code_structure.md) | Responsibilities, boundaries, duplication audit |

## Stack-specific workflow

**Backend:** entity → repository → service (if non-trivial) → `*Endpoint` with `*Request`/`*Response` records → `@QuarkusTest` + REST Assured.

**Frontend:** service → component → route → `*.spec.ts` when behaviour is non-trivial.

**Full-stack:** align API contract with Angular service; update [feature-catalog.md](docs/feature-catalog.md) when adding routes.

**Frontend finish gate:** `npm run build` (regenerates API client via `prebuild`) — not bare `ng build`.

**Tests:** use `Given` for seed data; run `mvn test` for backend, `npm test` in `src/main/webui` for frontend.

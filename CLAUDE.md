# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

@AGENTS.md

## Mandatory development process — read before touching `src/`

This repo enforces a **strict five-phase gate** on every non-trivial change (new behaviour, API, UI route, schema, multi-package refactor, or cross-feature bug fix). Full detail: `.cursor/rules/development-process.mdc`.

```
1. Feature analysis → 2. Architecture design → 3. Task break → 4. Explicit task approval → 5. TDD implementation
```

- **No edits to `src/main/**`, `src/test/**`, or `src/main/webui/**` before phase 4.** Phases 1–3 only touch `feature/<slug>.md`, `docs/**`, and `ARCHITECTURE.md`.
- Track open decisions as **FQ*n*** (feature questions, phase 1) and **AQ*n*** (architecture questions, phase 2) in the feature doc. **Answering a question is not approval** — after every FQ/AQ answer, run an impact review (update Impact, Wireframe, Architecture, Feature checklist, tasks) and stop.
- Phase 4 requires **explicit task-ID approval** ("Approve T1–T3", "Go ahead with T1"). "Implement it" / "yes" / "do the analysis" do **not** count — present the task table and ask which IDs to approve instead.
- Phase 5 is TDD only, per approved task: Red (failing test, no `src/main` change) → Green (minimal code) → Refactor. Optionally delegate to `.cursor/agents/tdd-red.md` / `tdd-green.md` / `tdd-refactor.md`.
- Skip the gate only for: typo, comment-only, formatter-only, or docs-only edits.
- **Never end a session with broken code.** Before stopping (task `done` or still `in-progress`), the tree must compile and touched tests must pass — fix forward or revert, don't leave red.
- Not in production yet — no legacy/back-compat concerns. Schema changes amend `V1.0.0__Database_Creation.sql` directly (see `.cursor/rules/issues-flyway.mdc`); never add new `V1.0.x` migration files.

## Commands

```bash
# Backend
mvn quarkus:dev                          # run app (backend :8080 + Angular dev server via Quinoa on :4200)
mvn test                                 # all backend tests
mvn test -Dtest=TicketEndpointTest#methodName   # single test
mvn test -Dtest=ArchitectureTest         # ArchUnit rules
mvn formatter:format                     # Java formatting
mvn verify                               # full backend gate — required before marking a task done

# After backend endpoint changes, regenerate the Angular API client
mvn test && cd src/main/webui && npm run generate:api

# Frontend (src/main/webui)
npm run build                            # production build; runs generate:api via prebuild — use this, not bare `ng build`
npm test -- --no-watch --browsers=ChromeHeadless   # headless test run
npm run lint
```

Run the smallest test scope that covers your change while iterating; run `mvn verify` (+ `npm run build` and Angular tests if UI changed) once before calling a task done — see `.cursor/rules/issues-testing.mdc` for the full tiered ladder and change→test impact map.

## Architecture essentials

- **Stack:** Java 21 / Quarkus 3.30 backend (`src/main/java/dev/vepo/issues/`, root package `dev.vepo.issues`) + Angular 20 SPA (`src/main/webui/`), bundled into one deployable via Quarkus Quinoa. PostgreSQL + Flyway. SmallRye JWT (RS256) auth. SSE for real-time notifications.
- **Backend pattern:** entity → repository → service (if non-trivial) → `*Endpoint` with `*Request`/`*Response` records (never `VO`/`DTO` suffix) → `@QuarkusTest` + REST Assured tests. Endpoints live in `{context}.{action}` subpackages, one HTTP method per class (e.g. `user.create.CreateUserEndpoint`).
- **Frontend pattern:** service → component → route → `*.spec.ts` for non-trivial behaviour. Generated OpenAPI clients live in `src/app/generated/` (gitignored) and are wrapped by hand-written facades in `services/`. UI copy uses `@jsverse/transloco` runtime PT/EN catalogs on canonical routes (no locale-prefixed routes/builds); dates/numbers/Material labels follow the active `UiLocaleService` locale.
- **Package placement:** `auth` (JWT/login), `user`, `project`, `workflow` (statuses/transitions), `ticket`/`ticket.*` (incl. comments/history), `categories`, `notifications` (SSE), `dashboards`, `mailer`, `infra` (cross-cutting HTTP/errors).
- **Canonical docs:** `ARCHITECTURE.md` (packages §5, naming §11, feature workflow §12), `docs/domain-specification.md` (ubiquitous language, invariants), `docs/feature-catalog.md` (UI routes), `docs/backlog.md` (ordered product backlog).
- **Logging:** SLF4J only — never `System.out`.

## Rules and subagents

`.cursor/rules/*.mdc` are the authoritative, file-scoped source of truth (always-on "four pillars": `issues-model.mdc`, `issues-testing.mdc`, `issues-quality.mdc`, `issues-platform.mdc`, plus glob-scoped rules for Java, Angular, JPA, HTTP contracts, tests, etc. — see the table in `AGENTS.md`). They were authored for Cursor's subagent/command UI (`.cursor/agents/*.md`, `.cursor/commands/*.md`), which Claude Code doesn't invoke natively — but the workflows they describe (TDD red/green/refactor cycle, docs-sync, API-compliance review) are still the expected way to work; follow them manually or via the `Agent` tool as needed.

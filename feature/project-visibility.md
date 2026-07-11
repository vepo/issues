# Project visibility (security level)

**Feature version:** 1  
**Status:** planned  
**Requested:** 2026-07-11

## Summary

Introduce a per-project **security level** (visibility) set at **create** (and editable by project owner or admin). It replaces the current implicit “any authenticated user can read all tickets” behaviour ([security audit SEC1](../reports/security-audit-1-11-07-2026-16-38-26.md)) with explicit levels:

| Level | Code / PT-BR UI | Who may **read** gated surfaces |
|-------|-----------------|----------------------------------|
| **Private** | `PRIVATE` / **Privado** | Project **members** and **admin** only |
| **Internal** | `INTERNAL` / **Interno** | Any **authenticated** user |
| **Public** | `PUBLIC` / **Público** | **Anonymous** and authenticated users |

**Default** on create and for existing projects after migrate: **Internal** (**FQ2**).

**Writes** (create/update/move/comment/import/reorder/…): always require project **membership** (or manage roles); never anonymous — even on Public (**FQ3**).

**Read surfaces** (**FQ4**): tickets, Kanban, versions, hub, phases, backlog, burndown, dashboard, and global search results (filtered). CSV import remains a **write** path (members only).

## Wireframe

**Guide:** layout for create/edit project and Public anonymous chrome.

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-11 (FQ1–FQ9 answered) |

### Screen: `/projects/new` and `/projects/:projectId/edit`

| Region | Elements | Notes |
|--------|----------|-------|
| Form | Existing fields + **Nível de segurança** | Required on create; editable by project owner or admin (**FQ8**) |
| Control | Radio group with three options + short help text | Labels **FQ1**; default selected: **Interno** (**FQ2**) |

```
┌─────────────────────────────────────────────────────────┐
│  Novo projeto / Editar projeto                           │
├─────────────────────────────────────────────────────────┤
│  Nome / Prefixo / Processo / Descrição / …              │
│                                                         │
│  Nível de segurança *                                   │
│  ○ Privado — só membros do projeto                      │
│  ● Interno — qualquer usuário autenticado  (padrão)     │
│  ○ Público — também visitante sem login                 │
│                                                         │
│                         [Cancelar]  [Salvar]            │
└─────────────────────────────────────────────────────────┘
```

### Screen: Public / anonymous read (when Public) — **FQ6**

| Region | Elements | Notes |
|--------|----------|-------|
| Surfaces | Ticket by identifier, Kanban, versions, hub, phases, backlog, burndown, dashboard | Read-only; same set as **FQ4** minus mutative actions |
| Shell | Limited header (no Novo / Importar / admin / allocation) | Login CTA |
| Out of scope (v1) | Anonymous global search / home | Authenticated search only (**FQ4**); anonymous uses deep links / known project routes |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | **`project`** owns security level; **`ticket`**, **`phase`**, **`dashboards`**, search, import consume via `ProjectAccessService` |
| Packages / files | `Project` + create/update Request/Response; extend `canView` / `canRead`; ticket find/list/search/create/update/move/comment; Kanban; version/phase; backlog/burndown/dashboard; project list |
| API | `securityLevel` on create/update/response; `@PermitAll` **read** only when Public (**AQ**); authenticated Internal/Private checks; writes always membership |
| UI | Create/edit radio (default Interno); optional hub badge; anonymous-capable routes for Public (**FQ6**) |
| Schema / seed | Column on `tb_projects` (enum); **default Internal** for new + existing (**FQ2**); `dev-import.sql` examples of each level |
| Tests | Access matrix: anonymous / non-member / member / admin × Private/Internal/Public × FQ4 surfaces |
| Docs | domain-spec (UL + invariants 23, 28–30a); feature-catalog; README; SEC1 closure |
| Cross-features | [project-administration](project-administration.md), [ticket-management](ticket-management.md), [kanban-board](kanban-board.md), [phase-management](phase-management.md), [ticket-search](ticket-search.md), [ticket-import](ticket-import.md), [project-dashboard](project-dashboard.md), [burndown](burndown.md), [ticket-backlog](ticket-backlog.md), [agentic-integration](agentic-integration.md) |

### Risks

- Broad change: every project-scoped read must call one access helper — easy to miss an endpoint (SEC1 class of bugs).
- **Internal default** (**FQ2**) preserves today’s open authenticated read for migrated projects; **Private** must be chosen explicitly to close IDOR for sensitive projects — document for operators.
- Public + rich text (**SEC4**) increases anonymous XSS/scraping surface — sanitize before enabling Public in production.
- Search must filter by readable projects (**FQ4**); home already membership-scoped for `user` (**invariant 27**) — leave as-is unless product expands later.
- Agentic PAT/SA: read respects security level; write stays member-aligned (**FQ3**).

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Official names for the three levels (code enum + PT-BR UI labels)? | answered | **Private** / `PRIVATE` / **Privado**; **Internal** / `INTERNAL` / **Interno**; **Public** / `PUBLIC` / **Público** |
| FQ2 | Default on **create** and for **existing** projects after migrate? | answered | **Internal** (create + migrate existing rows) |
| FQ3 | Do **writes** always require project **membership** (or manage), even when Internal/Public? | answered | **Yes** — visibility is read-only openness; never anonymous writes |
| FQ4 | Which surfaces inherit the level for **read**? | answered | Tickets, Kanban, versions, hub, phases, backlog, burndown, dashboard, global search (filtered). CSV import = write path (members only) |
| FQ5 | Does **admin** always bypass Private (see all projects)? | answered | **Yes** |
| FQ6 | For **Public**, which anonymous routes/UI exist? | answered | Read-only: ticket by identifier, Kanban, versions, hub, phases, backlog, burndown, dashboard; limited shell + Login CTA. No anonymous global search/home in v1 |
| FQ7 | May Internal/Public non-members see **comments**, **history**, **subscribers**, soft-deleted tickets? | answered | Comments + history **yes** for read; subscribers visible with ticket read; soft-deleted still admin/PM only |
| FQ8 | Who may **change** security level after create? | answered | Same as project edit — project owner or admin |
| FQ9 | Header **Projetos** / `GET /projects` list — include Internal/Public projects for non-members? | answered | **Yes** — discoverability for Internal/Public; Private only members + admin |

**Gate:** blocking **FQ1–FQ9** answered — proceed to phase 2 (architecture).

## Architecture

*(Phase 2 — draft after FQ answers; open **AQ*n*** below.)*

| Area | Design |
|------|--------|
| Bounded contexts | **`project`** owns `securityLevel`; all other contexts call `ProjectAccessService` — no cross-context repository reads for authz |
| Packages / layers | Entity `Project.securityLevel`; Request/Response field; `ProjectAccessService.canRead` / `requireRead` / existing `canView`/`requireManage`; one Endpoint per HTTP op unchanged |
| API surface | `securityLevel` on project create/update/response; ticket and peer read endpoints enforce `requireRead`; Public reads may use `@PermitAll` + service check (**AQ1**); writes keep `@RolesAllowed` + membership |
| Schema | `tb_projects.security_level` VARCHAR/ENUM NOT NULL DEFAULT `'INTERNAL'`; backfill existing = Internal |
| Cross-context | Search/home query filters; Kanban/backlog/burndown/dashboard/phase/version list gate on read |
| Frontend | Create/edit radio; anonymous route guards for Public deep links (**AQ2**); project list shows Internal/Public to non-members |
| Tests | Matrix tests per FQ4 × actor × level |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | How do **anonymous** Public reads authenticate in Quarkus (PermitAll + service check vs guest principal)? | open | Suggestion: `@PermitAll` on specific read endpoints + `ProjectAccessService` allows anonymous only when `PUBLIC` |
| AQ2 | SPA routing for anonymous Public — reuse authenticated components read-only, or separate public routes? | open | Suggestion: reuse same routes with auth guard allowing anonymous when project is Public (resolve project first) |
| AQ3 | Persist `security_level` as PostgreSQL ENUM type or VARCHAR + Java enum? | open | Suggestion: VARCHAR + Java `SecurityLevel` enum (matches other string enums in baseline) |
| AQ4 | Field name on API: `securityLevel` vs `visibility`? | open | Suggestion: `securityLevel` (matches domain term **project security level**) |
| AQ5 | Should `GET /projects` for anonymous return Public projects only (for nav), or empty until login? | open | Suggestion: Public projects only when anonymous; after login FQ9 applies |

## Changelog

### Project security levels (visibility) — 2026-07-11

**Version:** 1  
**Status:** planned

**Description:** Add project **security level** on create/edit; enforce read access for tickets and related project surfaces per level; default **Internal**; Private closes SEC1 for sensitive projects. Remediates [security-audit SEC1](../reports/security-audit-1-11-07-2026-16-38-26.md).

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Project administration | Create/edit **Nível de segurança**; default Interno; owner/admin may change |
| Ticket management / search | Read gated by level; search filtered; writes membership-only |
| Kanban / versions / phases / backlog / burndown / dashboard / hub | Read gated by level (**FQ4**) |
| Auth / anonymous SPA | Public read routes (**FQ6**); **AQ1–AQ2**, **AQ5** |
| Project list / shell menu | Non-members see Internal + Public (**FQ9**) |
| Agentic | PAT/SA read respects level; write membership-aligned |
| Ticket import | Members only (write) |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Create/edit project exposes **Nível de segurança** per **Wireframe** (default Interno) | Wireframe, FQ1, FQ2, FQ8 | ☐ |
| FC2 | Levels Private / Internal / Public behave per FQ1–FQ3 | FQ1–FQ3 | ☐ |
| FC3 | Private: non-member authenticated cannot read FQ4 surfaces; admin can (**FQ5**) | SEC1, FQ4, FQ5 | ☐ |
| FC4 | Internal: non-member authenticated can **read** FQ4 surfaces; cannot write | FQ3, FQ4 | ☐ |
| FC5 | Public: anonymous can **read** FQ6 surfaces; limited shell; no anonymous search/home | FQ6 | ☐ |
| FC6 | New + existing projects default to **Internal** | FQ2 | ☐ |
| FC7 | `GET /projects` / Projetos menu: Internal+Public visible to non-members; Private members+admin | FQ9 | ☐ |
| FC8 | Non-members see comments+history on readable tickets; soft-deleted admin/PM only | FQ7 | ☐ |
| FC9 | domain-spec + feature-catalog + README updated | Docs | ☐ |
| FC10 | Access matrix tests green | Tests | ☐ |

#### Tasks

*(Phase 3 — after architecture; blocking **AQ1–AQ5** resolved.)*

#### Test coverage

*(Phase 3.)*

**Development approval:** —

**Impact review (2026-07-11):** FQ1–FQ9 answered — Internal default (**FQ2**); accepted suggestions for remaining FQs. Wireframe default + FQ6 surfaces filled; Risks note Internal migrate; FC rows expanded (FC7–FC8); Architecture draft + **AQ1–AQ5** opened. Domain-spec UL/invariants updated.

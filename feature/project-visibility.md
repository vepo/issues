# Project visibility (security level)

**Feature version:** 1  
**Status:** done  
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
| **Last updated** | 2026-07-16 (AQ1–AQ5 accepted) |

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
| Bounded contexts | **`project`** owns `securityLevel`; **`ticket`**, **`phase`**, **`dashboards`**, search, import consume via `ProjectAccessService` — no cross-context repository authz |
| Packages / files | `SecurityLevel` enum; `Project.securityLevel`; `CreateProjectRequest` / `ProjectResponse`; extend `ProjectAccessService` (`canRead` / `requireRead` / list filters); read endpoints `@PermitAll` + service check; write paths stay membership; Angular create/edit radio + public-read guard |
| API | Field **`securityLevel`** (**AQ4**) on create/update/response; Public reads: `@PermitAll` + `requireRead` (**AQ1**); writes `@RolesAllowed` + membership (**FQ3**); anonymous `GET /projects` → Public only (**AQ5**) |
| UI | Create/edit radio (default Interno); reuse same FQ6 routes with public-read guard (**AQ2**); limited shell + Login CTA; Projetos menu per FQ9 / AQ5 |
| Schema / seed | `tb_projects.security_level VARCHAR(16) NOT NULL DEFAULT 'INTERNAL'` (**AQ3**); existing rows = Internal (**FQ2**); `dev-import.sql` examples of each level |
| Tests | Access matrix: anonymous / non-member / member / admin × Private/Internal/Public × FQ4 surfaces; list-projects anonymous vs auth |
| Docs | domain-spec (UL + invariants 23, 28–30a); feature-catalog; README; ARCHITECTURE §13; SEC1 closure |
| Cross-features | [project-administration](project-administration.md), [ticket-management](ticket-management.md), [kanban-board](kanban-board.md), [phase-management](phase-management.md), [ticket-search](ticket-search.md), [ticket-import](ticket-import.md), [project-dashboard](project-dashboard.md), [burndown](burndown.md), [ticket-backlog](ticket-backlog.md), [agentic-integration](agentic-integration.md) |

### Risks

- Broad change: every project-scoped read must call one access helper — easy to miss an endpoint (SEC1 class of bugs).
- **Internal default** (**FQ2**) preserves today’s open authenticated read for migrated projects; **Private** must be chosen explicitly to close IDOR for sensitive projects — document for operators.
- Public + rich text (**SEC4**) increases anonymous XSS/scraping surface — sanitize before enabling Public in production.
- Search must filter by readable projects (**FQ4**); home already membership-scoped for `user` (**invariant 27**) — leave as-is unless product expands later.
- Agentic PAT/SA: read respects security level; write stays member-aligned (**FQ3**).
- Today’s `canViewProject` / `requireView` mean **membership** (or admin) — rename or split carefully so write paths keep membership while reads use `canRead` / `requireRead`.

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

| Area | Design |
|------|--------|
| Bounded contexts | **`project`** owns `securityLevel`; all other contexts call `ProjectAccessService` — no cross-context repository reads for authz |
| Packages / layers | Java enum `SecurityLevel` (`PRIVATE` / `INTERNAL` / `PUBLIC`); entity field on `Project`; `CreateProjectRequest` / `ProjectResponse` field `securityLevel`; `ProjectAccessService.canRead` / `requireRead` (level-based) distinct from membership `canViewProject` / write checks; `ProjectService` create/update/list; one Endpoint per HTTP op unchanged |
| API surface | **`securityLevel`** on create/update/response (**AQ4**). Public-capable **read** endpoints: `@PermitAll` + service `requireRead` (anonymous allowed only when `PUBLIC`) (**AQ1**). Writes keep `@RolesAllowed` + membership/manage. Soft-deleted tickets remain admin/PM-only (**FQ7**). |
| Schema | `tb_projects.security_level VARCHAR(16) NOT NULL DEFAULT 'INTERNAL'` — **VARCHAR + Java enum**, not PostgreSQL ENUM (**AQ3**); backfill existing = Internal |
| List projects | Authenticated: admin → all; else membership/owned ∪ Internal ∪ Public; Private only if member/owner (**FQ9**). Anonymous: **Public only** (**AQ5**); `ListProjectsEndpoint` `@PermitAll` |
| Cross-context | Ticket find/list/search/comments/history; Kanban; phase/version list+find; backlog; burndown; dashboard; project find/hub — all gate with `requireRead`. Writes (create/update/move/comment/import/reorder/links) keep membership. Search filters by readable project ids. |
| Frontend | Create/edit radio default Interno. **Reuse same routes** for Public anonymous read; replace/extend `authGuard` with a public-read guard that allows anonymous when project resolves as Public (**AQ2**). Limited shell + hide mutative actions; Login CTA. Projetos menu uses list API (FQ9 / AQ5). |
| Tests | Matrix tests per FQ4 × actor × level; anonymous list Public-only; Private denies non-member; Internal allows authenticated non-member read; writes still membership; Angular radio + guard specs |

### Access helper (target behaviour)

| Actor × level | `canRead` | Write (membership) |
|---------------|-----------|-------------------|
| Anonymous × Private / Internal | no | n/a |
| Anonymous × Public | yes | no |
| Authenticated non-member × Private | no (admin yes) | no |
| Authenticated non-member × Internal / Public | yes | no |
| Member / owner | yes | yes (per role) |
| Admin | yes (all levels) | manage / as today |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | How do **anonymous** Public reads authenticate in Quarkus (PermitAll + service check vs guest principal)? | answered | `@PermitAll` on specific read endpoints + `ProjectAccessService` allows anonymous only when `PUBLIC` |
| AQ2 | SPA routing for anonymous Public — reuse authenticated components read-only, or separate public routes? | answered | Reuse same routes with auth/public-read guard allowing anonymous when project is Public (resolve project first) |
| AQ3 | Persist `security_level` as PostgreSQL ENUM type or VARCHAR + Java enum? | answered | VARCHAR + Java `SecurityLevel` enum (matches other string enums in baseline) |
| AQ4 | Field name on API: `securityLevel` vs `visibility`? | answered | `securityLevel` (matches domain term **project security level**) |
| AQ5 | Should `GET /projects` for anonymous return Public projects only (for nav), or empty until login? | answered | Public projects only when anonymous; after login FQ9 applies |

**Gate:** blocking **AQ1–AQ5** answered — phase 3 task break complete below.

## Changelog

### Project security levels (visibility) — 2026-07-11

**Version:** 1  
**Status:** done

**Description:** Add project **security level** on create/edit; enforce read access for tickets and related project surfaces per level; default **Internal**; Private closes SEC1 for sensitive projects. Remediates [security-audit SEC1](../reports/security-audit-1-11-07-2026-16-38-26.md).

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Project administration | Create/edit **Nível de segurança**; default Interno; owner/admin may change |
| Ticket management / search | Read gated by level; search filtered; writes membership-only |
| Kanban / versions / phases / backlog / burndown / dashboard / hub | Read gated by level (**FQ4**) |
| Auth / anonymous SPA | Public read via `@PermitAll` + public-read guard (**AQ1**, **AQ2**); anonymous list Public only (**AQ5**) |
| Project list / shell menu | Non-members see Internal + Public (**FQ9**); anonymous sees Public only |
| Agentic | PAT/SA read respects level; write membership-aligned |
| Ticket import | Members only (write) |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Create/edit project exposes **Nível de segurança** per **Wireframe** (default Interno) | Wireframe, FQ1, FQ2, FQ8 | ☑ |
| FC2 | Levels Private / Internal / Public behave per FQ1–FQ3 | FQ1–FQ3 | ☑ |
| FC3 | Private: non-member authenticated cannot read FQ4 surfaces; admin can (**FQ5**) | SEC1, FQ4, FQ5 | ☑ |
| FC4 | Internal: non-member authenticated can **read** FQ4 surfaces; cannot write | FQ3, FQ4 | ☑ |
| FC5 | Public: anonymous can **read** FQ6 surfaces; limited shell; no anonymous search/home | FQ6 | ☑ |
| FC6 | New + existing projects default to **Internal** | FQ2 | ☑ |
| FC7 | `GET /projects` / Projetos menu: Internal+Public visible to non-members; Private members+admin | FQ9 | ☑ |
| FC8 | Non-members see comments+history on readable tickets; soft-deleted admin/PM only | FQ7 | ☑ |
| FC9 | domain-spec + feature-catalog + README + ARCHITECTURE §13 updated | Docs | ☑ |
| FC10 | Access matrix tests green | Tests | ☑ |
| FC11 | API field is `securityLevel` on create/update/response | AQ4 | ☑ |
| FC12 | Persist as VARCHAR + Java `SecurityLevel` enum (default `INTERNAL`) | AQ3, FQ2 | ☑ |
| FC13 | Public reads use `@PermitAll` + `ProjectAccessService.requireRead` (anonymous only when PUBLIC) | AQ1 | ☑ |
| FC14 | SPA reuses same FQ6 routes; public-read guard allows anonymous when project is Public | AQ2, FQ6 | ☑ |
| FC15 | Anonymous `GET /projects` returns Public projects only | AQ5 | ☑ |

#### Tasks

| ID | Deliverable | Done |
|----|-------------|------|
| T1 | Flyway baseline: `tb_projects.security_level VARCHAR(16) NOT NULL DEFAULT 'INTERNAL'`; Java `SecurityLevel` enum; `Project` entity field | ☑ |
| T2 | `ProjectAccessService`: `canRead` / `requireRead` (Optional user + level rules); list readable projects for auth + anonymous; keep membership helpers for writes | ☑ |
| T3 | `securityLevel` on `CreateProjectRequest` / `ProjectResponse`; create/update persist (default Internal); owner/admin may change | ☑ |
| T4 | Wire `requireRead` on ticket find/list/search/comments/history/links-read; `@PermitAll` on Public-capable ticket/project read endpoints (**AQ1**) | ☑ |
| T5 | Wire `requireRead` on Kanban, phases, versions, backlog, burndown, dashboard, project find/hub | ☑ |
| T6 | `ListProjectsEndpoint` `@PermitAll`: anonymous → Public only (**AQ5**); authenticated → FQ9 filter | ☑ |
| T7 | Angular project create/edit **Nível de segurança** radio per Wireframe + facade/codegen | ☑ |
| T8 | Angular public-read guard (reuse routes, **AQ2**); limited shell + hide mutative actions for anonymous/non-member readers | ☑ |
| T9 | Confirm writes (create/update/move/comment/import/reorder/…) stay membership/manage; soft-deleted admin/PM only | ☑ |
| T10 | `dev-import.sql` sample projects for Private / Internal / Public | ☑ |
| T11 | Docs: feature-catalog, README, ARCHITECTURE §13; close SEC1 note; regenerate API client | ☑ |

#### Test coverage

| ID | Covers | Tasks | Done |
|----|--------|-------|------|
| TC1 | Schema default Internal; create/update round-trip `securityLevel` | T1, T3 | ☑ |
| TC2 | Access matrix: anonymous / non-member / member / admin × Private/Internal/Public on ticket + peer reads | T2, T4, T5 | ☑ |
| TC3 | Writes denied for anonymous and non-members even on Public/Internal | T9 | ☑ |
| TC4 | `GET /projects` anonymous Public-only; authenticated FQ9 (Internal+Public; Private members/admin) | T6 | ☑ |
| TC5 | Soft-deleted tickets remain admin/PM-only under readable projects | T4, T9 | ☑ |
| TC6 | Angular create/edit radio default Interno | T7 | ☑ |
| TC7 | Angular public-read guard allows anonymous Public; redirects/login for Private/Internal | T8 | ☑ |
| TC8 | Doc review checklist (catalog, README, ARCHITECTURE, domain-spec) | T11 | ☑ |

**Development approval:** approved 2026-07-16 — tasks: T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11

**Implementation notes:** Shipped 2026-07-16. `SecurityLevel` + `tb_projects.security_level` (default INTERNAL); `ProjectAccessService.canRead`/`requireRead` vs membership `requireView`; `@PermitAll` on Public-capable reads; Angular create/edit radio + `publicReadGuard`; dev-import Private/Public demos. `mvn verify` green (341 tests).

**Impact review (2026-07-11):** FQ1–FQ9 answered — Internal default (**FQ2**); accepted suggestions for remaining FQs. Wireframe default + FQ6 surfaces filled; Risks note Internal migrate; FC rows expanded (FC7–FC8); Architecture draft + **AQ1–AQ5** opened. Domain-spec UL/invariants updated.

**Impact review (2026-07-16):** AQ1–AQ5 accepted as suggested. Architecture finalized (`@PermitAll` + `requireRead`, reused SPA routes, VARCHAR + `SecurityLevel`, API `securityLevel`, anonymous list Public-only). Impact/Risks updated (`canView` vs `canRead` split). FC11–FC15 added. Tasks T1–T11 + TC1–TC8 written; status `tasks-ready`. No new open FQ/AQ.

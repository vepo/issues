# Feature catalog review — Issues

**Verdict:** mixed — almost all catalog routes exist and most happy paths are reachable, but Dev personas / README logins are stale vs `dev-import.sql`, User admin routes lack frontend role guards, and Project dashboard catalog steps describe unshipped v3 UX.

**Date:** 2026-07-11  
**Scope:** Full UI table + API-only table + Dev personas  
**Sources:** `docs/feature-catalog.md`, `docs/backlog.md`, `docs/domain-specification.md`, `README.md`, `src/main/webui/src/app/app.routes.ts`, shell menus, `dev-import.sql`, sampled components/endpoints, `feature/*.md`

## Summary

Route coverage is strong: every catalog UI path maps to `app.routes.ts`, and primary labels (Kanban, Burndown, Backlog, Painel, Vínculos, Importar CSV, etc.) match the live UI. Compliance gaps cluster around **stale exploration docs** (personas), **role/reachability mismatches** (users, admin-only menus, edit-project JWT role), and **aspirational catalog copy** for Project dashboard v3 (still `tasks-ready`). Agentic PAT APIs are partially in code without catalog rows; backlog still lists that work as awaiting approval.

## Coverage

| Catalog feature | Route / API | Status | Notes |
|-----------------|-------------|--------|-------|
| Login | `/login` | ok | Capabilities gate Criar conta / Recuperar senha |
| Register | `/login/register` | ok | Public route; links LOCAL-only |
| Password reset request | `/login/reset-password` | gap | Steps say "Forgot password"; UI is **Recuperar senha** |
| Password reset confirm | `/login/reset-password/:token` | ok | |
| Home | `/` | ok | Tickets atuais / atribuídos / Atividade + saved-query sections |
| Project hub | `/projects/:projectId` | ok | Kanban, Burndown, Backlog, Painel, Versões, Fases; Nova fase/versão when `canManage` |
| Project backlog | `/project/:projectId/backlog` | ok | Drag reorder gated; link to Kanban |
| Project allocation | `/projects/:projectId/allocation` | ok | Hub CTA owner/admin; API `requireManage`; route `roleGuard` any PM/admin |
| Account settings | `/account/settings` | ok | Change password via capabilities |
| Kanban board | `/project/:projectId/kanban` | ok | Phase filter, Agrupar por, WIP, Burndown/Importar/Versões/Fases |
| Burndown | `/project/:projectId/burndown` | ok | Default active phase; Ver burndown; missing points warning |
| Project dashboard | `/project/:projectId/dashboard` | stale | Catalog describes v3 (Concluir, Sem dados, KPI **Tickets por status**); live UI differs |
| Version catalog | `/project/:projectId/versions` | ok | Hub + Kanban → Versões |
| Create version | `…/versions/new` | ok | roleGuard admin/PM |
| Version detail | `…/versions/:versionId` | ok | |
| Phase list / create / detail | `…/phases`… | ok | |
| Ticket detail | `/ticket/:ticketIdentifier` | ok | Type badge, Vínculos, Subtarefas, Restaurar |
| Ticket search (simple) | `/search` | ok | |
| Advanced search | `/search/advanced` | ok | Executar + query help |
| Saved queries list/edit/shared | `/search/queries`… `/search/q/:slug` | ok | Exibir na página inicial |
| Create ticket (global/project) | `/tickets/new`, `…/tickets/new` | ok | |
| Import CSV (global/project) | `/tickets/import`, `…/tickets/import` | ok | Header Importar + menu Importar CSV |
| User list / create / edit | `/users`… | gap | Catalog **admin**; routes **authGuard only** (no `roleGuard`) |
| Project list | `/projects` | ok | Conta→Projetos is PM-only; admin uses Gerenciar projetos |
| Header Projetos menu | (shell) | ok | Kanban targets; Gerenciar for admin/PM |
| Create project | `/projects/new` | ok | PM-only guard matches catalog |
| Edit project | `/projects/:projectId/edit` | gap | Catalog includes **admin**; API `@RolesAllowed(PROJECT_MANAGER)` only |
| Workflow list/create/edit | `/workflows`… | gap | Menu **Processos** gated `*role="'project-manager'"` only; route allows admin |
| Category list | `/categories` | ok | admin roleGuard |
| Notifications (SSE) | (global) | ok | Marcar todas como lidas present |
| List statuses | `GET /status` | ok | `WorkflowPaths.STATUS`; used by resolvers |
| Confirm password reset | `POST /auth/recovery/confirm` | ok | Backs confirm page |
| Change password | `POST /auth/change-password` | ok | Account settings |
| Auth capabilities | `GET /auth/capabilities` | ok | Login + account gates |
| Dev personas | (seed) | stale | Catalog/README emails ≠ `dev-import.sql` |

### Extra / undocumented (not catalog rows)

| Surface | Evidence | Suggestion |
|---------|----------|------------|
| Context hints (onboarding) | `feature/onboarding-hints.md` **done**; `hintId` on home/kanban/advanced-search | Add catalog row or note under Home / Kanban / Advanced search |
| Personal API tokens | `GET/POST/DELETE /account/api-tokens` (`auth.apitoken.*`); agentic **in-progress** | API-only row now; UI rows when T10 ships |
| Service accounts UI | Planned `/projects/:projectId/service-accounts` — not in router yet | Catalog when implemented |

### Matching `feature/*.md` (high level)

| Slug | Feature status | Catalog alignment |
|------|----------------|-------------------|
| Most shipped (`kanban-board`, `burndown`, `ticket-backlog`, `ticket-links`, `notifications`, `custom-fields`, …) | `done` | Rows present |
| `project-dashboard` | v3 `tasks-ready` | Catalog already reads as if v3 shipped |
| `agentic-integration` | `approved` / `in-progress` | No catalog rows; backlog still says await approval |
| `git-integration`, `ticket-import` v2, `i18n` | `tasks-ready` | Correctly absent from catalog as product surfaces |

## Findings

### Critical

- **Dev personas unusable as documented** — Catalog (`feature-catalog.md` § Dev personas) and README login table list `admin@issues.vepo.dev`, `pm@issues.vepo.dev`, `user@issues.vepo.dev`. Seed inserts only `@issues.ui` users (`cto@issues.ui`, `junior_dev@issues.ui`, `project_lead@issues.ui`, `director_projects@issues.ui`, `tech_lead@issues.ui`, `senior_dev@issues.ui`, `guest@issues.ui`). Happy-path exploration per catalog personas fails. Severity amplified by `dev-import.sql:514–516` still seeding a saved query for `user@issues.vepo.dev` (no-op).

### Major

- **User admin routes lack frontend role guard** — Catalog Roles: **admin**. `app.routes.ts` `/users`, `/users/new`, `/users/:userId` use only `authGuard`. Menu hides via `*role="'admin'"`, but any authenticated user can open the URLs. Mutating APIs are `@RolesAllowed(ADMIN)`; list/find allow broader roles — UI implies admin-only management.

- **Project dashboard catalog is aspirational (v3)** — Row claims **Editar layout** (autosave; **Concluir** exits), **Sem dados**, KPI titled **Tickets por status**. Live UI: toggle **Editar layout** / **Salvar layout**, empty copy **Nenhum widget…**, KPI title **KPIs de Performance**, chart **Tickets por Status**. Matches `feature/project-dashboard.md` v3 FQ5–FQ8 (still `tasks-ready`), not current behaviour.

- **Edit project: catalog/admin UI vs API role** — Catalog and hub `canManage()` treat **admin** as manager. `UpdateProjectEndpoint` is `@RolesAllowed(PROJECT_MANAGER_ROLE)` only. Admin without `project-manager` (e.g. seed `tech_lead@issues.ui`) sees **Editar** but update API rejects. Allocation APIs correctly allow ADMIN.

- **Backlog stale for agentic** — `docs/backlog.md` Order 2 still “await task approval”; `feature/agentic-integration.md` has **Development approval** and status **in-progress**, with PAT endpoints already in `src/main/java/.../auth/apitoken/`. Catalog has no API-only/UI rows yet (FC10 unchecked).

### Minor / suggestions

- Password reset request Steps use English **"Forgot password"**; UI and domain language use **Recuperar senha**.
- Conta → **Projetos** is `*role="'project-manager'"` only; catalog Project list also cites Conta for admin — admins reach list via header **Gerenciar projetos**.
- Administração → **Processos** menu is PM-only; workflows routes allow admin — admin-without-PM cannot discover Processos from shell.
- Project allocation / edit: Angular `roleGuard` allows any PM; backend `requireManage` is owner-or-admin — non-owner PM can open page then get API 403 (catalog Roles are closer to backend than to the route guard).
- Onboarding hints shipped (`feature/onboarding-hints.md`) but omitted from catalog.
- Register / reset routes remain publicly reachable by URL when capabilities hide links (documented LOCAL-only entry; acceptable if intentional).
- Duplicate empty `path: ''` redirect in `app.routes.ts` is dead code (first `''` wins) — not a catalog issue.

## Catalog fix list

- **Dev personas** — Replace table with `dev-import.sql` emails/roles; map exploration intents (full admin → `cto@` or `director_projects@`; PM → `project_lead@` / `senior_dev@`; user → `junior_dev@`; empty roles → `guest@`). Fix README login table the same way.
- **Password reset request** — Steps: Login → **Recuperar senha** (LOCAL only) → …
- **Project dashboard** — Revert Steps to current UX (**Salvar layout** / **Editar layout**; KPI **KPIs de Performance** or chart **Tickets por Status**; drop Concluir / Sem dados until v3 ships), *or* mark row “planned hardening” explicitly.
- **User list / create / edit** — Keep Roles **admin**; note menu-only until `roleGuard(['admin'])` is added (implementation follow-up).
- **Edit project** — Align Roles/Steps with reality: either document “admin must also hold project-manager for update API” or fix endpoint to include ADMIN (product decision).
- **Workflow list** — Note Administração → Processos requires **project-manager** in menu; admin-only users need direct URL or menu fix.
- **Project list** — Steps: prefer Header **Projetos** → **Gerenciar projetos** for admin; Conta → Projetos for PM.
- **API-only (optional now)** — `GET/POST /account/api-tokens`, revoke — used by agentic in-progress; UI deferred.
- **Optional catalog add** — Onboarding context hints on Home, Kanban, Advanced search (dismissible).

## Backlog proposals (not applied)

| Suggested Order | Idea | Slug | Why |
|----------------:|------|------|-----|
| (after current promoted) | **Catalog & seed persona sync** — align catalog, README, `dev-import-sql-safety` protected emails with `@issues.ui` seed; fix orphan saved-query owner email | `docs-dev-personas` | Blocks reliable local happy-path review |
| (pair with user-management) | **Harden user admin SPA guards** — `roleGuard(['admin'])` on `/users*` | `user-management` | Matches catalog Roles; closes URL bypass |
| (pair with project-admin) | **Admin project update authz** — include ADMIN on `UpdateProjectEndpoint` or hide Editar for admin-without-PM | `project-administration` | Catalog + hub already promise admin manage |
| (keep Order 1) | Approve **project-dashboard** T1–T9 | `project-dashboard` | Catalog already documents target UX; ship or revert catalog |
| (keep Order 2) | Continue **agentic** T1–T15; add catalog rows with T14 | `agentic-integration` | Backlog status wrong; partial API without catalog |
| — | (existing ideas 6–23 remain valid; no new product gaps beyond attachments / export already listed) | — | — |

## Recommended next steps

1. **Docs-only:** sync Dev personas + README logins + fix `dev-import.sql` saved-query owner email; rewrite Project dashboard catalog Steps to match live UI until v3 approval.
2. **Product decisions:** admin-without-PM for project update; whether user routes get `roleGuard` (recommend yes).
3. **Process:** approve `project-dashboard` T1–T9 *or* stop documenting Concluir/Sem dados/KPI rename as shipped; update backlog Order 2 to reflect agentic `in-progress`.
4. **When applying catalog fixes:** use **docs-sync**; do not start phase 5 from this report alone — task-break + explicit task IDs for any code changes.
)

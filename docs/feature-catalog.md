# Feature catalog

UI feature index for Issues. Update when routes, menu items, or primary user flows change (see [.cursor/rules/feature-catalog.mdc](../.cursor/rules/feature-catalog.mdc)).

| Feature | Route | Roles | Steps (happy path) |
|---------|-------|-------|-------------------|
| Login | `/login` | public | Open app → enter email/password → submit → redirect home; **Criar conta** / **Recuperar senha** only when `GET /auth/capabilities` reports LOCAL password recovery |
| Register | `/login/register` | public | Login → Criar conta (LOCAL only) → username, name, email, password (8+ with upper/lower/digit) → submit → login |
| Password reset request | `/login/reset-password` | public | Login → **Recuperar senha** (LOCAL only) → enter email → submit |
| Password reset confirm | `/login/reset-password/:token` | public | Open email link → enter new password twice → submit → login |
| Home | `/` | authenticated | Login → personal hub: **Tickets atuais**, **Tickets atribuídos**, **Atividade** (static snapshot); optional sections for owned saved queries with **Exibir na página inicial**; dismissible **context hint** (onboarding) |
| Project hub | `/projects/:projectId` | authenticated (project member or admin) | Home → project name → hub: **Kanban**, **Burndown**, **Backlog**, **Painel**, **Versões**, **Fases**; lists phases and versions; owner PM or admin also **Editar**, **Alocação**, **Nova fase** / **Nova versão** |
| Project backlog | `/project/:projectId/backlog` | authenticated (project member or admin); reorder: project-manager, admin | Project hub → **Backlog** → ranked list (infinite scroll); PM/admin drag to reorder; excludes done/deleted; link to **Kanban** |
| Project allocation | `/projects/:projectId/allocation` | project owner PM, admin | Project hub → **Alocação** → list members → add user → remove (blocked when member has open assigned tickets; UI lists those tickets) |
| Account settings | `/account/settings` | authenticated | Menu → Conta → edit name/email, save profile → change password (LOCAL only, via capabilities) or use recovery link → **Conectar agente** (preset + **Gerar token e configuração**, copy MCP snippet) → **Tokens de API** (list, create, revoke; secret once) |
| Project service accounts | `/projects/:projectId/service-accounts` | project-manager, admin | Project hub / admin → **Contas de serviço** → list → **Nova conta de serviço** (display name) → **Gerar token** / **Desativar** |
| Kanban board | `/project/:projectId/kanban` | authenticated (project member or admin) | Header **Projetos** → project name → board; or Project hub → **Kanban** → view columns by status → drag/move ticket; **filter by phase** (all / active / unplanned / **pick any phase**); **Agrupar por** swimlanes (none / assignee / priority); WIP `n/limit` on columns; phase badge on cards; header link to **Burndown**; dismissible **context hint** (onboarding) |
| Burndown | `/project/:projectId/burndown` | authenticated (project member or admin) | Project hub / Kanban → **Burndown** → select phase (default **active**) → warnings for tickets missing story points → **Ver burndown** enabled only when phase has start and end (else disabled + tooltip) → Ideal vs Remaining chart |
| Project dashboard | `/project/:projectId/dashboard` | authenticated (project member or admin) | Hub → **Painel** → default widgets; toggle **Editar layout** / **Salvar layout**; empty **Nenhum widget…**; KPI titled **KPIs de Performance** (chart **Tickets por Status**); recent tickets ≤20. *(v3 hardening — Concluir / Sem dados / KPI rename — planned in [project-dashboard.md](../feature/project-dashboard.md), not shipped)* |
| Version catalog | `/project/:projectId/versions` | authenticated | Kanban → Versões → list SemVer labels → open changelog |
| Create version | `/project/:projectId/versions/new` | project-manager, admin | Versões → Nova versão → enter SemVer label + description → save |
| Version detail / changelog | `/project/:projectId/versions/:versionId` | authenticated | Versões → select version → view grouped changelog (Planejado / Entregue / Via fase); PM can edit label |
| Phase list | `/project/:projectId/phases` | authenticated | Kanban → Fases → list phases with status badges |
| Create phase | `/project/:projectId/phases/new` | project-manager, admin | Fases → Nova fase → objective, deliverables, deliverable version → save |
| Phase detail | `/project/:projectId/phases/:phaseId` | authenticated | Fases → select phase → edit; PM can **Ativar** (planned) or **Concluir** (active) |
| Ticket detail | `/ticket/:ticketIdentifier` | authenticated | Kanban or search → click ticket → see **type** badge (Épico/História/Tarefa); edit built-in fields (incl. **Tipo**, rich-text **Descrição**) and **Campos personalizados** (Text CF rich text; orphan former-workflow values read-only), assign, move status (Epic warns when moving to DONE with open **Subtarefas**), delete (admin/PM), **restore deleted ticket (admin/PM)**, **Vínculos** (grouped peer/hierarchy links; add/remove; cross-project search), **Subtarefas** on Epics (progress `n/m` + **Nova subtarefa**), comments, observe; **Atividade** shows **Agente em nome de …** when history/comments were made via API token (`via_agent`) |
| Ticket search (simple) | `/search` | authenticated | Menu → search → enter term → open ticket |
| Advanced search (query language) | `/search/advanced` | authenticated | Search → **Busca avançada** → enter query (incl. `cf.<key>` predicates) → **Executar**; open query help for field/operator reference; dismissible **context hint** (onboarding) |
| Saved queries list | `/search/queries` | authenticated | **Minhas consultas** → open, edit, delete |
| Saved query edit | `/search/queries/new`, `/search/queries/:id/edit` | authenticated | Name, query text, **Exibir na página inicial**, save |
| Shared saved query | `/search/q/:slug` | authenticated | Copy link; owner **Editar**; others **Clonar** |
| Create ticket | `/tickets/new` | authenticated | Header → Novo → select project → fill form (**Tipo** Épico/História/Tarefa, rich-text **Descrição** + **Campos personalizados** from in-scope defs; Text CF rich text) → optional **fase** → create |
| Create ticket (project) | `/project/:projectId/tickets/new` | authenticated | Kanban → Novo ticket → form pre-filled from template (built-ins + custom field defaults; rich-text Description / Text CF; **Tipo**) → optional **fase** → create |
| Import tickets (CSV, project) | `/project/:projectId/tickets/import` | authenticated | Kanban → Importar CSV → upload file → map built-in columns and **custom field keys** → preview (correct invalid rows in place) → import valid rows (partial; no sibling rollback) |
| Import tickets (CSV, global) | `/tickets/import` | authenticated | Header **Importar** or user menu → Importar CSV → upload file → map project + built-in columns and **custom field keys** (row invalid if project lacks that key) → preview (correct invalid rows) → import valid rows |
| User list | `/users` | admin | Menu → users → list; Editar or Excluir (blocked when assignee on open tickets). *SPA `roleGuard(['admin'])` planned — [user-management.md](../feature/user-management.md) v3; until then menu-only (URL reachable if authenticated)* |
| Create user | `/users/new` | admin | Users → new → fill form → save. *Same SPA guard note as User list* |
| Edit user | `/users/:userId` | admin | Users → select user → edit → save. *Same SPA guard note as User list* |
| Project list | `/projects` | project-manager, admin | Admin: Header **Projetos** → **Gerenciar projetos**; PM: Conta → **Projetos** or header footer. List: **Abrir** hub, **Fases**, **Versões**, **Editar** per row (list scope: viewable projects — member ∪ owned for non-admin; all for admin). *Conta → Projetos for admin-without-PM planned — [project-navigation.md](../feature/project-navigation.md) v2* |
| Header Projetos menu | (global shell) | authenticated | Header **Projetos** → pick project → Kanban; empty: button disabled + tooltip; PM/admin also **Gerenciar projetos** in menu footer |
| Create project | `/projects/new` | project-manager | Projects → new → fill form (rich-text project description; **Nível de segurança** Private/Internal/Public, default Internal; optional ticket template with rich-text template description; creator becomes owner and member) → save |
| Edit project | `/projects/:projectId/edit` | project owner PM, admin | Project hub → **Editar** → update fields, **Nível de segurança**, and **owner** (admin or current owner); **prefix** read-only when the project has tickets; manage **Campos personalizados (projeto)** (add/edit/disable/delete) and template custom defaults → save. *API today: `@RolesAllowed(PROJECT_MANAGER)` only — admin-without-PM sees hub **Editar** but update fails; fix planned — [project-administration.md](../feature/project-administration.md) v3* |
| Workflow list | `/workflows` | project-manager, admin | Menu → Administração → **Processos** → list → Editar. *Menu today gated `project-manager` only; admin-without-PM needs direct URL until shell fix — [workflow-configuration.md](../feature/workflow-configuration.md) v3* |
| Create workflow | `/workflows/new` | project-manager, admin | Workflows → Novo processo → status table (optional WIP) + transitions table → **Campos personalizados (processo)** visible but **Adicionar campo** disabled (save first) → save |
| Edit workflow | `/workflows/:workflowId` | project-manager, admin | Workflows → Editar → change name, **statuses** (add/rename/remove), start, transitions, WIP; removing a status with tickets asks **Mover tickets para**; manage **Campos personalizados (processo)** incl. status-required |
| Category list | `/categories` | admin | Menu → Administração → Categorias → list; Nova categoria or Editar dialog with color picker; Excluir with confirm (blocked when tickets or project templates reference the category) |
| Notifications (SSE) | (global, background) | authenticated | Login → SSE registers (live events) → badge shows server unread (`99+` when > 99) → dropdown loads paginated list with infinite scroll; **Marcar todas como lidas** when unread > 0; SSE auto-reconnects and refreshes unread + page 0 |

## API-only features (no dedicated UI page)

| Feature | API | Notes |
|---------|-----|-------|
| List statuses | `GET /status` | Used by filters and admin |
| Confirm password reset | `POST /auth/recovery/confirm` | LOCAL only; used by password reset confirm page |
| Change password | `POST /auth/change-password` | LOCAL only; used by account settings |
| Auth capabilities | `GET /auth/capabilities` | Public; drives login/account password UI (`passwordRecovery`, `changePassword`) |
| Agent setup config | `GET /agent/setup-config?preset=` | User JWT; returns paste-ready MCP JSON using `issues.public-base-url` / `issues.mcp-public-base-url` |
| Ticket context | `GET /tickets/{id}/context` | JWT or API token; composite detail + transitions + in-scope custom fields for agents |
| Service account API | `/projects/{id}/service-accounts` (+ `…/tokens`) | PM/admin; UI at `/projects/:projectId/service-accounts` |
| Personal API tokens | `GET/POST/DELETE /account/api-tokens` | User JWT; UI under Account **Tokens de API** / **Conectar agente** |

## Dev personas (from `dev-import.sql`)

| Email | Roles | Use for |
|-------|-------|---------|
| cto@issues.ui | admin, project-manager, user | Full admin menu, workflows, projects, user admin |
| director_projects@issues.ui | admin, project-manager | Admin + PM without `user` role edge cases |
| tech_lead@issues.ui | admin, user | Admin **without** project-manager (user admin, ticket delete; project edit API gap until v3) |
| project_lead@issues.ui | project-manager | Project CRUD / hub manage |
| senior_dev@issues.ui | project-manager, user | PM + member flows |
| junior_dev@issues.ui | user | Ticket CRUD, Kanban, search |
| guest@issues.ui | *(none)* | Empty roles / guard edge cases |

Default dev password: see `application.properties` (`password.default`).

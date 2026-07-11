# Feature catalog

UI feature index for Issues. Update when routes, menu items, or primary user flows change (see [.cursor/rules/feature-catalog.mdc](../.cursor/rules/feature-catalog.mdc)).

| Feature | Route | Roles | Steps (happy path) |
|---------|-------|-------|-------------------|
| Login | `/login` | public | Open app → enter email/password → submit → redirect home; **Criar conta** / **Recuperar senha** only when `GET /auth/capabilities` reports LOCAL password recovery |
| Register | `/login/register` | public | Login → Criar conta (LOCAL only) → username, name, email, password (8+ with upper/lower/digit) → submit → login |
| Password reset request | `/login/reset-password` | public | Login → "Forgot password" (LOCAL only) → enter email → submit |
| Password reset confirm | `/login/reset-password/:token` | public | Open email link → enter new password twice → submit → login |
| Home | `/` | authenticated | Login → personal hub: **Tickets atuais**, **Tickets atribuídos**, **Atividade** (static snapshot); optional sections for owned saved queries with **Exibir na página inicial** |
| Project hub | `/projects/:projectId` | authenticated (project member or admin) | Home → project name → hub: **Kanban**, **Burndown**, **Backlog**, **Painel**, **Versões**, **Fases**; lists phases and versions; owner PM or admin also **Editar**, **Alocação**, **Nova fase** / **Nova versão** |
| Project backlog | `/project/:projectId/backlog` | authenticated (project member or admin); reorder: project-manager, admin | Project hub → **Backlog** → ranked list (infinite scroll); PM/admin drag to reorder; excludes done/deleted; link to **Kanban** |
| Project allocation | `/projects/:projectId/allocation` | project owner PM, admin | Project hub → **Alocação** → list members → add user → remove (blocked when member has open assigned tickets; UI lists those tickets) |
| Account settings | `/account/settings` | authenticated | Menu → Conta → edit name/email, save profile → change password (LOCAL only, via capabilities) or use recovery link |
| Kanban board | `/project/:projectId/kanban` | authenticated (project member or admin) | Header **Projetos** → project name → board; or Project hub → **Kanban** → view columns by status → drag/move ticket; **filter by phase** (all / active / unplanned / **pick any phase**); **Agrupar por** swimlanes (none / assignee / priority); WIP `n/limit` on columns; phase badge on cards; header link to **Burndown** |
| Burndown | `/project/:projectId/burndown` | authenticated (project member or admin) | Project hub / Kanban → **Burndown** → select phase (default **active**) → warnings for tickets missing story points → **Ver burndown** enabled only when phase has start and end (else disabled + tooltip) → Ideal vs Remaining chart |
| Project dashboard | `/project/:projectId/dashboard` | authenticated (project member or admin) | Project hub → **Painel** → default widgets on first visit; **Editar layout** customizes and saves layout per user on the server; recent tickets capped at 20 |
| Version catalog | `/project/:projectId/versions` | authenticated | Kanban → Versões → list SemVer labels → open changelog |
| Create version | `/project/:projectId/versions/new` | project-manager, admin | Versões → Nova versão → enter SemVer label + description → save |
| Version detail / changelog | `/project/:projectId/versions/:versionId` | authenticated | Versões → select version → view grouped changelog (Planejado / Entregue / Via fase); PM can edit label |
| Phase list | `/project/:projectId/phases` | authenticated | Kanban → Fases → list phases with status badges |
| Create phase | `/project/:projectId/phases/new` | project-manager, admin | Fases → Nova fase → objective, deliverables, deliverable version → save |
| Phase detail | `/project/:projectId/phases/:phaseId` | authenticated | Fases → select phase → edit; PM can **Ativar** (planned) or **Concluir** (active) |
| Ticket detail | `/ticket/:ticketIdentifier` | authenticated | Kanban or search → click ticket → see **type** badge (Épico/História/Tarefa); edit built-in fields (incl. **Tipo**, rich-text **Descrição**) and **Campos personalizados** (Text CF rich text; orphan former-workflow values read-only), assign, move status (Epic warns when moving to DONE with open **Subtarefas**), delete (admin/PM), **restore deleted ticket (admin/PM)**, **Vínculos** (grouped peer/hierarchy links; add/remove; cross-project search), **Subtarefas** on Epics (progress `n/m` + **Nova subtarefa**), comments, observe |
| Ticket search (simple) | `/search` | authenticated | Menu → search → enter term → open ticket |
| Advanced search (query language) | `/search/advanced` | authenticated | Search → **Busca avançada** → enter query (incl. `cf.<key>` predicates) → **Executar**; open query help for field/operator reference |
| Saved queries list | `/search/queries` | authenticated | **Minhas consultas** → open, edit, delete |
| Saved query edit | `/search/queries/new`, `/search/queries/:id/edit` | authenticated | Name, query text, **Exibir na página inicial**, save |
| Shared saved query | `/search/q/:slug` | authenticated | Copy link; owner **Editar**; others **Clonar** |
| Create ticket | `/tickets/new` | authenticated | Header → Novo → select project → fill form (**Tipo** Épico/História/Tarefa, rich-text **Descrição** + **Campos personalizados** from in-scope defs; Text CF rich text) → optional **fase** → create |
| Create ticket (project) | `/project/:projectId/tickets/new` | authenticated | Kanban → Novo ticket → form pre-filled from template (built-ins + custom field defaults; rich-text Description / Text CF; **Tipo**) → optional **fase** → create |
| Import tickets (CSV, project) | `/project/:projectId/tickets/import` | authenticated | Kanban → Importar CSV → upload file → map built-in columns and **custom field keys** → preview (correct invalid rows in place) → import valid rows (partial; no sibling rollback) |
| Import tickets (CSV, global) | `/tickets/import` | authenticated | Header **Importar** or user menu → Importar CSV → upload file → map project + built-in columns and **custom field keys** (row invalid if project lacks that key) → preview (correct invalid rows) → import valid rows |
| User list | `/users` | admin | Menu → users → list; Editar or Excluir (blocked when assignee on open tickets) |
| Create user | `/users/new` | admin | Users → new → fill form → save |
| Edit user | `/users/:userId` | admin | Users → select user → edit → save |
| Project list | `/projects` | project-manager, admin | Header **Projetos** → **Gerenciar projetos**, or Conta → **Projetos** → list; **Abrir** hub, **Fases**, **Versões**, **Editar** per row (list scope: viewable projects — member ∪ owned for non-admin; all for admin) |
| Header Projetos menu | (global shell) | authenticated | Header **Projetos** → pick project → Kanban; empty: button disabled + tooltip; PM/admin also **Gerenciar projetos** in menu footer |
| Create project | `/projects/new` | project-manager | Projects → new → fill form (rich-text project description; optional ticket template with rich-text template description; creator becomes owner and member) → save |
| Edit project | `/projects/:projectId/edit` | project owner PM, admin | Project hub → **Editar** → update fields and **owner** (admin or current owner); **prefix** read-only when the project has tickets; manage **Campos personalizados (projeto)** (add/edit/disable/delete) and template custom defaults → save |
| Workflow list | `/workflows` | project-manager, admin | Menu → Administração → Processos → list workflows → Editar |
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

## Dev personas (from `dev-import.sql`)

| Email | Roles | Use for |
|-------|-------|---------|
| cto@issues.ui | admin, project-manager, user | Full admin menu, workflows, projects |
| admin@issues.vepo.dev | admin | User admin, ticket delete |
| pm@issues.vepo.dev | project-manager | Project CRUD |
| user@issues.vepo.dev | user | Ticket CRUD, Kanban |

Default dev password: see `application.properties` (`password.default`).

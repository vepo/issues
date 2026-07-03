# Feature catalog

UI feature index for Issues. Update when routes, menu items, or primary user flows change (see [.cursor/rules/feature-catalog.mdc](../.cursor/rules/feature-catalog.mdc)).

| Feature | Route | Roles | Steps (happy path) |
|---------|-------|-------|-------------------|
| Login | `/login` | public | Open app → enter email/password → submit → redirect home |
| Password reset request | `/login/reset-password` | public | Login → "Forgot password" → enter email → submit |
| Password reset confirm | `/login/reset-password/:token` | public | Open email link → enter new password twice → submit → login |
| Home | `/` | authenticated | Login → land on home |
| Account settings | `/account/settings` | authenticated | Menu → Conta → view profile → change password (current + new) or use recovery link |
| Kanban board | `/project/:projectId/kanban` | authenticated | Home → select project → view columns by status → drag/move ticket; **filter by phase** (all / active / unplanned); phase badge on cards |
| Project dashboard | `/project/:projectId/dashboard` | authenticated | Home → select project → dashboard shows default widgets on first visit; Editar layout to customize |
| Version catalog | `/project/:projectId/versions` | authenticated | Kanban → Versões → list SemVer labels → open changelog |
| Create version | `/project/:projectId/versions/new` | project-manager, admin | Versões → Nova versão → enter SemVer label + description → save |
| Version detail / changelog | `/project/:projectId/versions/:versionId` | authenticated | Versões → select version → view grouped changelog (Planejado / Entregue / Via fase); PM can edit label |
| Phase list | `/project/:projectId/phases` | authenticated | Kanban → Fases → list phases with status badges |
| Create phase | `/project/:projectId/phases/new` | project-manager, admin | Fases → Nova fase → objective, deliverables, deliverable version → save |
| Phase detail | `/project/:projectId/phases/:phaseId` | authenticated | Fases → select phase → edit; PM can **Ativar** (planned) or **Concluir** (active) |
| Ticket detail | `/ticket/:ticketIdentifier` | authenticated | Kanban or search → click ticket → edit fields (incl. phase, planned/shipped version), assign, move status, delete (admin/PM), comments, observe |
| Ticket search | `/search` | authenticated | Menu → search → enter term → open ticket |
| Create ticket | `/tickets/new` | authenticated | Header → Novo → select project → fill form → optional **fase** → create |
| Create ticket (project) | `/project/:projectId/tickets/new` | authenticated | Kanban → Novo ticket → form pre-filled from template → optional **fase** → create |
| Import tickets (CSV, project) | `/project/:projectId/tickets/import` | authenticated | Kanban → Importar CSV → upload file → map columns → preview → import valid rows |
| Import tickets (CSV, global) | `/tickets/import` | authenticated | Header **Importar** or user menu → Importar CSV → upload file → map project + columns → preview → import valid rows |
| User list | `/users` | admin | Menu → users → list |
| Create user | `/users/new` | admin | Users → new → fill form → save |
| Edit user | `/users/:userId` | admin | Users → select user → edit → save |
| Project list | `/projects` | project-manager+ | Menu → projects → list |
| Create project | `/projects/new` | project-manager | Projects → new → fill form (optional ticket template) → save |
| Edit project | `/projects/:projectId` | project-manager | Projects → select project → edit template/workflow/**phase template** → save |
| Workflow list | `/workflows` | project-manager, admin | Menu → Administração → Processos → list workflows → Editar |
| Create workflow | `/workflows/new` | project-manager, admin | Workflows → Novo processo → status table + transitions table → save |
| Edit workflow | `/workflows/:workflowId` | project-manager, admin | Workflows → Editar → change name, start status, transitions (status names fixed) |
| Category list | `/categories` | admin | Menu → Administração → Categorias → list; Nova categoria or Editar dialog with color picker |
| Notifications (SSE) | (global, background) | authenticated | Login → SSE registers → badge updates on ticket changes |

## API-only features (no dedicated UI page)

| Feature | API | Notes |
|---------|-----|-------|
| List statuses | `GET /status` | Used by filters and admin |
| Confirm password reset | `POST /auth/recovery/confirm` | Used by password reset confirm page |
| Change password | `POST /auth/change-password` | Used by account settings while logged in |

## Dev personas (from `dev-import.sql`)

| Email | Roles | Use for |
|-------|-------|---------|
| cto@issues.ui | admin, project-manager, user | Full admin menu, workflows, projects |
| admin@issues.vepo.dev | admin | User admin, ticket delete |
| pm@issues.vepo.dev | project-manager | Project CRUD |
| user@issues.vepo.dev | user | Ticket CRUD, Kanban |

Default dev password: see `application.properties` (`password.default`).

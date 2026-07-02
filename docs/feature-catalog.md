# Feature catalog

UI feature index for Issues. Update when routes, menu items, or primary user flows change (see [.cursor/rules/feature-catalog.mdc](../.cursor/rules/feature-catalog.mdc)).

| Feature | Route | Roles | Steps (happy path) |
|---------|-------|-------|-------------------|
| Login | `/login` | public | Open app → enter email/password → submit → redirect home |
| Password reset request | `/login/reset-password` | public | Login → "Forgot password" → enter email → submit |
| Password reset confirm | `/login/reset-password/:token` | public | Open email link → enter new password → submit |
| Home | `/` | authenticated | Login → land on home |
| Kanban board | `/project/:projectId/kanban` | authenticated | Home → select project → view columns by status → drag/move ticket |
| Project dashboard | `/project/:projectId/dashboard` | authenticated | Home → select project → dashboard → view charts/KPIs |
| Ticket detail | `/ticket/:ticketIdentifier` | authenticated | Kanban or search → click ticket → view/edit/comment/move |
| Ticket search | `/search` | authenticated | Menu → search → enter term → open ticket |
| User list | `/users` | admin | Menu → users → list |
| Create user | `/users/new` | admin | Users → new → fill form → save |
| Edit user | `/users/:userId` | admin | Users → select user → edit → save |
| Project list | `/projects` | project-manager+ | Menu → projects → list |
| Create project | `/projects/new` | project-manager | Projects → new → fill form → save |
| Edit project | `/projects/:projectId` | project-manager | Projects → select project → edit → save |
| Notifications (SSE) | (global, background) | authenticated | Login → SSE registers → badge updates on ticket changes |

## API-only features (no dedicated UI page)

| Feature | API | Notes |
|---------|-----|-------|
| Create workflow | `POST /workflows` | admin, project-manager |
| List categories | `GET /categories` | Used by ticket forms |
| List statuses | `GET /status` | Used by filters and admin |

## Dev personas (from `dev-import.sql`)

| Email | Roles | Use for |
|-------|-------|---------|
| admin@issues.vepo.dev | admin | User admin, ticket delete |
| pm@issues.vepo.dev | project-manager | Project CRUD |
| user@issues.vepo.dev | user | Ticket CRUD, Kanban |

Default dev password: see `application.properties` (`password.default`).

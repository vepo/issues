# Nielsen Heuristic Audit — Issues UI

Per-page review of every screen against [Nielsen's 10 usability heuristics](https://www.nngroup.com/articles/ten-usability-heuristics/).  
**Audit date:** 2026-07-02 · **Scope:** `src/main/webui/` after corporate uniform layout pass.

Legend: ✅ Compliant · ⚠️ Partial · ❌ Gap · **Fix** = action taken or recommended.

---

## Heuristic summary (application-wide)

| # | Heuristic | Status | Notes |
|---|-----------|--------|-------|
| 1 | Visibility of system status | ⚠️ | Toasts, notification badge, kanban drag OK; some async widgets show generic "Carregando…" |
| 2 | Match system / real world | ✅ | PT-BR, domain terms (Ticket, Projeto, Kanban) |
| 3 | User control & freedom | ✅ | Dialog cancel, auth back links, logout in menu |
| 4 | Consistency & standards | ✅ | Gallery classes + tokens; single `data-table` and `filter-chip--active` |
| 5 | Error prevention | ✅ | Form validation on create/edit flows |
| 6 | Recognition over recall | ✅ | Project cards, visible nav, status filter in header |
| 7 | Flexibility & efficiency | ⚠️ | Header search + Enter; role chips on users; no keyboard shortcuts doc |
| 8 | Aesthetic & minimalist | ✅ | Flat UI (Metro/Fluent-influenced): bold color blocks, typography hierarchy, minimal ornament |
| 9 | Help users recover from errors | ⚠️ | `mat-error` on forms; password-reset-request error branch still silent |
| 10 | Help & documentation | ⚠️ | Footer OpenAPI link; no in-app help beyond empty states |

---

## App shell (`app.html`)

| Element | H1–H10 review | Status |
|---------|---------------|--------|
| Brand "Issues" | H4 consistent chrome; H6 home recognition | ✅ |
| Search bar (pill) | H1 submit navigates; H7 Enter/search button | ✅ |
| Status filter | H6 visible filter; H4 matches search page | ✅ |
| + Novo | H4 primary action; H3 opens dismissible dialog | ✅ |
| Notification icon + badge | H1 unread count | ✅ |
| Hamburger menu | H6 role-gated items; H4 icon pattern | ✅ |
| Footer | H10 API docs link | ✅ |

---

## Login (`/login`)

| Element | Review | Status |
|---------|--------|--------|
| `.page-auth` + `.auth-card` | H8 centered, minimal | ✅ |
| Email / password (outline) | H5 required validation | ✅ |
| Entrar (primary) | H1 disabled while invalid | ✅ |
| Esqueci minha senha | H3 escape hatch | ✅ |
| Error message area | H9 visible login failure | ✅ **Fix:** uses `.error` block |

---

## Password reset request (`/password-reset-request`)

| Element | Review | Status |
|---------|--------|--------|
| Auth card layout | H4 matches login | ✅ |
| Email field + submit | H5 validation | ✅ |
| Success message | H1 confirms email sent | ✅ |
| HTTP error handling | H9 | ⚠️ **Remaining:** empty error subscribe — add toast or inline error |
| Voltar ao login | H3 | ✅ |

---

## Password reset confirm (`/password-reset`)

| Element | Review | Status |
|---------|--------|--------|
| Stub empty state | H9 explains unsupported flow | ✅ **Fix:** added with back link |
| Implementation | H2/H9 | ❌ **Remaining:** full token form not built |

---

## Home / projects (`/`)

| Element | Review | Status |
|---------|--------|--------|
| `.page-header` title + subtitle | H8 hierarchy | ✅ |
| `.project-grid` / `.project-card` | H6 scannable projects | ✅ |
| Card links (Kanban, Painel) | H4 consistent actions | ✅ |
| Empty state | H9 guidance when no projects | ✅ |

---

## Kanban (`/project/:id/kanban`)

| Element | Review | Status |
|---------|--------|--------|
| Page title with project name | H1/H6 context | ✅ |
| Column headers + `.column-count` | H1 ticket counts per status | ✅ **Fix:** count badge added |
| Ticket cards | H2 status columns familiar | ✅ |
| Drag-and-drop | H1 visual move feedback | ✅ |
| Sub-header filters | H6 filter labels | ✅ **Fix:** `--filter` variant styled |

---

## Ticket detail (`/ticket/:id`)

| Element | Review | Status |
|---------|--------|--------|
| Page header + identifier | H6 recognition | ✅ |
| `.detail-list` metadata | H8 structured read-only fields | ✅ |
| Tabs (Detalhes / Comentários / Histórico) | H4 matches gallery `.tabs` | ✅ |
| Comments section | H8 panel in `.page-panel` | ✅ |
| Assignee / status actions | H3 user can change assignee | ✅ |
| `.text-muted` for empty assignee | H8 de-emphasized placeholder | ✅ |

---

## Search (`/search`)

| Element | Review | Status |
|---------|--------|--------|
| Page header reflects query | H1 status from URL | ✅ |
| Results table | H4 same `.data-table` as admin lists | ✅ |
| Empty state with guidance | H9 suggests changing filters | ✅ |

---

## Users list (`/users`)

| Element | Review | Status |
|---------|--------|--------|
| Page header + Novo usuário | H4 primary top-right | ✅ |
| Name/email `.filter-input` | H7 quick filter | ✅ |
| Role chips (`.role-filters`) | H6 visible active filters | ✅ |
| Data table | H4 consistent | ✅ |
| Edit link per row | H6 | ✅ |
| Native inputs vs Material | H4 | ⚠️ **Remaining:** migrate filters to `mat-form-field` outline |

---

## User edit (`/users/:id`)

| Element | Review | Status |
|---------|--------|--------|
| Page header with user name | H6 | ✅ |
| Outline form fields | H4 | ✅ |
| Salvar / Cancelar | H4 filled + outlined cancel | ✅ |
| Role checkboxes | H5 | ✅ |

---

## Projects list (`/projects`)

| Element | Review | Status |
|---------|--------|--------|
| Page header | H8 | ✅ |
| Data table in `.page-panel` | H4 | ✅ |
| Edit navigation | H6 | ✅ |

---

## Project edit (`/projects/:id`)

| Element | Review | Status |
|---------|--------|--------|
| Page layout + form | H4 | ✅ |
| Workflow association | H2 domain match | ✅ |
| Actions row | H3 cancel returns | ✅ |

---

## Dashboard (`/project/:id/dashboard`)

| Element | Review | Status |
|---------|--------|--------|
| `.page` + header | H4 now aligned with other pages | ✅ **Fix:** layout pass |
| Editar layout toggle | H3 reversible edit mode | ✅ |
| Widget grid | H8 content-focused | ✅ |
| Loading placeholders | H1 | ⚠️ Generic "Carregando gráfico…" for table/KPI too |

---

## Create ticket dialog (modal)

| Element | Review | Status |
|---------|--------|--------|
| Dialog title | H6 | ✅ |
| Outline `mat-form-field` | H4 | ✅ **Fix:** appearance outline |
| Validation errors | H5/H9 | ✅ |
| Cancelar (outlined) / Criar (filled) | H4 button hierarchy | ✅ **Fix:** cancel uses outlined |

---

## Notifications (header component)

| Element | Review | Status |
|---------|--------|--------|
| Icon-only trigger | H8 minimal chrome | ✅ |
| Badge count | H1 | ✅ |
| Panel list | H6 | ✅ |

---

## Rich text editor (ticket comments)

| Element | Review | Status |
|---------|--------|--------|
| Toolbar + editor | H7 power users | ✅ |
| Submit comment | H1 | ⚠️ No explicit loading state on submit |

---

## Cross-cutting improvements (this pass)

1. **Flat UI design system** — Metro/Fluent-influenced: minimal ornament, bold color blocks, typography hierarchy, `$radius-none` geometry (see [ui-elements-gallery.md](ui-elements-gallery.md) § Flat UI).
2. **Uniform page scaffold** — `.page`, `.page-header`, `.page-title`, `.page-subtitle`, `.page-panel` on all routes.
3. **Auth screens** — `.page-auth` + `.auth-card` for login and password flows.
4. **Button hierarchy** — primary `filled` + `.btn`, secondary `outlined` + `.btn-secondary` / `.btn-cancel`.
5. **Form fields** — `appearance="outline"` on auth, edit, and create-ticket forms.
6. **Empty states** — `.empty-state` with dashed border and muted copy.
7. **Kanban** — column count badges for status visibility.

---

## Remaining backlog

| Item | Heuristic | Priority |
|------|-----------|----------|
| In-app help / onboarding tooltip | H10 | Low |

Most items from the 2026-07-02 audit are addressed (password confirm, workflows UI, account page, per-widget loading copy).

---

## Verification

After UI changes, manually verify at `mvn quarkus:dev` → `http://localhost:8080/` with dev persona `cto@issues.ui` / `qwas1234`:

1. Login → home → project card → kanban → ticket → back
2. Header search, status filter, + Novo dialog
3. Menu → Users, Projects, Dashboard
4. Logout → password reset request

Automated: `cd src/main/webui && npm run build && npm test -- --no-watch --browsers=ChromeHeadless`

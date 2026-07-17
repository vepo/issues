# UI Elements Gallery — Issues

Canonical catalog of every reusable UI element in the Angular SPA (`src/main/webui/`).

**Governance:** Before adding or changing any UI element, read this document. Reuse an existing element with the documented properties, style, and behavior. If nothing fits, extend the gallery in the same PR.

Related: [feature-catalog.md](feature-catalog.md) (routes), [colors.scss](../src/main/webui/src/colors.scss) (tokens), [styles.scss](../src/main/webui/src/styles.scss) (global classes), [issues-ux.mdc](../.cursor/rules/issues-ux.mdc) (Nielsen + flat UI).

---

## Flat UI design principles

Issues uses **flat UI design** — minimal ornament, bold color blocks, and typography-driven hierarchy. The approach draws from Microsoft's Metro design language (2010) and modern [Fluent Design](https://www.microsoft.design/) guidance for productivity software.

**Density:** **Comfortable compact** — mild global density (~15% less chrome than the original airy scale). Prefer showing more content per viewport without shrinking below the floors below. Flat principles unchanged (no elevation growth, square corners, token-driven spacing).

| Principle | Gallery expression |
|-----------|-------------------|
| **Simple, minimal interfaces** | `.page` / `.page-panel` layout; whitespace via `$space-*`; no decorative borders or textures |
| **Clean sans-serif typography** | Roboto; `.page-title` / `.page-subtitle`; hierarchy by size, weight, color — not by frames |
| **Bold, vibrant colors** | Navy chrome (`$base-background-color`), primary blue actions (`$base-active-color`), semantic feedback colors |
| **Flat iconography** | Material Icons — geometric, scalable; `.btn-icon-header` for header actions |
| **Simple buttons** | `.btn` solid fills, `$radius-none` corners, light `$shadow-card` only on panels — not on every button |
| **Minimal illustrations** | `.empty-state` text panels; no hero imagery in app chrome |
| **Mobile-ready** | Header wraps; reduced margins at `max-width: 750px` |

**Anti-patterns (not flat):** gradients, glossy/glass effects, heavy drop shadows, card lift or shadow growth on hover, skeuomorphic controls, pill/circular chrome unless explicitly added to this gallery.

**Hover pattern:** use `border-color` change (`flat-hover-border` mixin) — not increased `box-shadow`.

---

## Design tokens

| Token | Value | Use |
|-------|-------|-----|
| `$base-background-color` | `#023164` | Header, footer, table chrome, modal header |
| `$base-active-color` | `#1565C0` | Primary buttons, links, active nav, focus ring |
| `$base-active-hover-color` | `#0D47A1` | Primary hover |
| `$base-secondary-bg-color` | `#E8EEF4` | Secondary buttons, label chips |
| `$base-secondary-color` | `#023164` | Secondary button text |
| `$base-error-color` | `#C62828` | Cancel/destructive, errors, unread badge |
| `$surface-page` | `#F5F6F8` | Page background |
| `$surface-card` | `#FFFFFF` | Cards, columns, panels |
| `$surface-muted` | `#FAFAFA` | Empty states, zebra rows |
| `$border-subtle` | `#E0E3E8` | Card/column borders |
| `$text-primary` | `#1A2332` | Headings, body |
| `$text-secondary` | `#5A6472` | Descriptions, metadata |
| `$text-muted` | `#8A929E` | Placeholders, empty hints |
| `$shadow-card` | `0 1px 2px rgba(0,0,0,.06)` | Light elevation on panels only |
| `$shadow-toast` | `0 1px 3px rgba(0,0,0,.1)` | Toast feedback |
| `$radius-none` | `0` | Square corners — Metro-influenced flat geometry |
| `$space-sm` / `$space-md` / `$space-lg` / `$space-xl` | 8 / **12** / **16** / **24** px | Comfortable-compact spacing grid (`$space-xs` = 4px) |
| `$panel-padding` | `$space-lg` (**16px**) | `.page-panel`, `.comment-form`, `form.edit.page-panel` |
| `$table-cell-padding-x` / `$table-cell-padding-y` | **12px** / **6px** | `.data-table`, `.inline-table` cells |
| `$shell-padding-x` | `0.75rem` (**12px**) | Horizontal inset for header, footer, main, context bar — full-width chrome |
| `$control-height` | `2.25rem` (**36px**) | `.btn`, `.form-field--control`, `.filter-chip` — toolbar / action rows |
| `$control-font-size` | `0.875rem` | Label text on toolbar controls |
| `$control-padding-x` | `0.625rem` | Horizontal padding on toolbar controls |

**Density floors:** control height ≥ 36px; body/table text ≥ 0.8125rem; icon hit targets ≥ 36×36; focus ring 2px.

Material theme uses `density: -1` and primary CSS variables aligned to `$base-active-color` in `styles.scss`. Button and compact field heights use `$control-height` via Material overrides and `.form-field--control`.

Typography (page): `.page-title` **1.25rem**; `.page-subtitle` **0.875rem**.
---

## 1. App shell

### 1.1 Header (`.main-header`)

| Property | Value |
|----------|-------|
| **Location** | `app/app.html` |
| **Visibility** | Always; search/create only when authenticated |

**Style:** Navy background (`$base-background-color`), white text, flat rectangular controls. Full-width bar; content in `.shell-inner` with horizontal padding `$shell-padding-x` only (GitHub-style — no max-width cap on chrome).

**Layout (left → right, inside `.shell-inner`):**
- **Brand** (`.brand`) — ticket icon + "Issues" wordmark → `/`
- **Search** (`.search-bar`) — flat rectangular input; submit on Enter (authenticated)
- **Novo** — compact primary button
- **Importar** — compact outlined header button (authenticated)
- **Actions** (`.header-actions`) — **Projetos** menu (`app-project-menu`) + notification icon + user menu icon

**Behavior:**
- **Brand** → `/` with `brand-active` on home
- **Search** — `.search-bar` form; Enter → `/search` with `q` query param only
- **Novo** — compact primary button → `/tickets/new`
- **Projetos** — labeled outlined button (`app-project-menu`); menu lists viewable projects → `/project/:id/kanban`; disabled + tooltip when empty; PM/admin footer **Gerenciar projetos** → `/projects`
- **Notificações** — icon-only `matIconButton` + badge (`app-notification`)
- **User menu** — `person` icon + `mat-menu`: email header, Conta, **Projetos** (PM/admin → `/projects`), Administração submenu (role-gated), Sair
- **Acessar** — shown when logged out → `/login`

**Status filter:** moved to search results page (`.filter-chips` on `/search`), not in global header.

### 1.1.1 Project menu (`app-project-menu`)

| Property | Value |
|----------|-------|
| **Location** | `.header-actions`, left of notifications |
| **Trigger** | Labeled **Projetos** button (`.btn.btn-compact.btn-header`, `matButton="outlined"`) |
| **Menu** | `mat-menu.menu-panel` — one item per viewable project (name + kanban icon) → Kanban route |
| **Empty** | Trigger **disabled**; wrapper `matTooltip` = “Nenhum projeto” |
| **Footer** | Divider + **Gerenciar projetos** → `/projects` when user has `project-manager` or `admin` |
| **API** | `ProjectsService.findAll()` → `GET /projects` (viewable scope) |

### 1.2 Context bar (`.context-bar`)

| Property | Value |
|----------|-------|
| **Location** | `app-context-bar` between header and `main` |
| **Visibility** | Authenticated; project kanban/dashboard and ticket routes only |

**Style:** `$surface-toolbar` background, bottom border `$border-subtle`, reuses `.breadcrumb` inside `.shell-inner`.

**Breadcrumbs:**
- `/project/:id/kanban` — Início › {project} › Kanban
- `/project/:id/dashboard` — Início › {project} › Painel
- `/ticket/:id` — Início › {project} › #{identifier}

### 1.3 Main content (`main.container`)

Flex-grow content area; horizontal padding `$shell-padding-x` (reduced on mobile).

### 1.4 Footer (`.main-footer`)

Copyright + **Documentação da API** link (`/openapi`). Navy background, white text. Uses `.shell-inner` for alignment with header.

---

## 2. Buttons

All action buttons use `matButton` (or `matButton="filled"`) **plus** a gallery class. Do **not** rely on bare `button {}` global styles.

### 2.1 Primary (`.btn`)

| Property | Value |
|----------|-------|
| **Style** | `$base-active-color` solid fill, white label/icon, `$radius-none`, no gradient |
| **Height** | `$control-height` (36px) |
| **Hover** | `$base-active-hover-color` |
| **Disabled** | Gray, 60% opacity |
| **Focus** | 2px `$focus-ring-color` outline |

**Used in:** header (Novo ticket), home (Kanban, Painel), modals (Criar), ticket actions, list tables (Novo, Editar), comments.

### 2.2 Secondary (`.btn-secondary`)

| Property | Value |
|----------|-------|
| **Style** | `$base-secondary-bg-color` fill, `$base-secondary-color` text |

**Used in:** login (Recuperar Senha), password reset (Voltar).

### 2.3 Cancel / destructive (`.btn-cancel`)

| Property | Value |
|----------|-------|
| **Style** | `$base-error-color` solid fill |
| **Use** | Irreversible or high-risk actions only |

**Used in:** flows that need explicit destructive emphasis (not routine dialog dismiss).

### 2.4 Dismiss / back (`.btn-secondary` + `outlined`)

| Property | Value |
|----------|-------|
| **Use** | Cancel on edit forms, close modals, back navigation |

**Used in:** create-ticket page, user/project edit, password reset, login secondary actions.

### 2.5 Header icon button (`.btn-icon-header`)

| Property | Value |
|----------|-------|
| **Directive** | `matIconButton` |
| **Style** | White icon on navy; flat square hover (`$radius-none`) |
| **Used in** | User menu, notification bell |

### 2.6 Brand link (`.brand`)

| Property | Value |
|----------|-------|
| **Markup** | `mat-icon` + `.brand-name` "Issues" |
| **Style** | White wordmark; subtle underline when `brand-active` on home |
| **Behavior** | `routerLink="/"` |

### 2.7 Primary / secondary / cancel

| Class | `matButton` variant |
|-------|---------------------|
| `.btn` | `"filled"` — primary via `mat.button-overrides` |
| `.btn-secondary` | `"outlined"` — dismiss, back, secondary navigation |
| `.btn-cancel` | `"filled"` with red CSS custom properties — destructive only |

### 2.8 Tab button (`.tab-button`)

| Property | Value |
|----------|-------|
| **Location** | `ticket-view.component.html` / `.scss` |
| **Style** | Borderless; active = bottom border `$base-active-color` |
| **Active class** | `.tab-button--active` (alias: `.active`) |
| **Behavior** | Toggles `activeTab` (`history` \| `comments`); no route change |

### 2.9 Icon-only remove (`.btn-remove`)

Dashboard widget header; borderless, muted text, red on hover.

---

## 3. Form controls

### 3.1 Standard field (`mat-form-field.form-field`, `appearance="outline"`)

| Property | Value |
|----------|-------|
| **Width** | 100% in forms |
| **Style** | Flat outline field; `$radius-none` via Material theme overrides |
| **Validation** | `mat-error` below field; red `$base-error-color` |
| **Behavior** | Reactive (`formControlName`) or template-driven (`ngModel`) |

**Used in:** login, password reset, create-ticket page, user edit, project edit.

### 3.1a Toolbar control (`mat-form-field.form-field--control`, `appearance="outline"`)

| Property | Value |
|----------|-------|
| **Height** | `$control-height` (36px) — same as `.btn`; only the outer wrapper is fixed-height (infix/trigger flex) so the label is not clipped |
| **Width** | Auto; `min-width` 9rem, `max-width` 13rem typical; **Kanban** `.board-phase-filter` uses `min-width` 11rem / `max-width` 22rem for long phase names |
| **Style** | Compact outline select/input; no subscript area |
| **Used in** | Kanban phase / swimlane filters in `.page-header__actions`; ticket actions |
| **Panel** | Kanban selects use `panelClass="board-phase-filter-panel"` — wrapping options, wider panel |

Pair with inline label (e.g. `.board-phase-filter__label`) in action rows — not for full-page forms.

### 3.2 Search bar (`.search-bar`)

| Property | Value |
|----------|-------|
| **Markup** | `form` with leading icon, `input[type="search"]`; submit on Enter |
| **Style** | White flat bar, light shadow, focus ring on `:focus-within`; `$radius-none` |
| **Behavior** | Form submit or Enter → `/search` with `q` query param |

### 3.3 Status filter chips (`.filter-chips` / `.filter-chip`)

| Property | Value |
|----------|-------|
| **Location** | `search-tickets.component.html`, `users-view.component.html` |
| **Active class** | `.filter-chip--active` — filled primary blue (canonical; do not use `.filter-chip.active`) |
| **Markup** | `.filter-chip` buttons including **Todos** (`statusId = -1`) |
| **Behavior** | Click updates `/search` query params (`q`, optional `status`) |

**Note:** Status filtering was removed from the global header (formerly `.toolbar-select`).

### 3.4 Native filter input (`input[type="text"]` in tables)

**Used in:** `users-view` filter row only.  
**Style:** Global `input` rules — white bg, `$border-default` border.  
**Tech debt:** migrate to `mat-form-field` or shared `.filter-input` for consistency.

### 3.5 Checkbox

Native checkbox in users list (filter + read-only role display).  
**Tech debt:** prefer `mat-checkbox` like `users-edit`.

### 3.6 Rich text editor (`app-rich-text-editor`)

| Property | Value |
|----------|-------|
| **Inputs** | `value`, `disabled`, `placeholder` |
| **Outputs** | `valueChange` |
| **Forms** | `ControlValueAccessor` — use with `formControlName` / `formControl` |
| **Style** | `.rich-text-editor` — toolbar + contenteditable; wrapper `.form-field--rich-text` + `.form-label` |
| **Display** | Read-only HTML via `.rich-text-display` + `[innerHTML]` (Angular sanitization) |
| **Length** | Plain-text length helpers in `core/plain-text-length.ts` (align with server `PlainTextLength`) |
| **Behavior** | Toolbar formatting; emits HTML string |

**Used in:** ticket comments; ticket Description (create + edit); Text custom fields; project description; ticket template description.

---

## 4. Layout blocks

### 4.1 Page scaffold (`.page`)

Standard wrapper for every authenticated route.

| Class | Purpose |
|-------|---------|
| `.page` | Full-width content within `main` shell padding; vertical padding |
| `.page-header` | Flex row: title block + `.page-header__actions` |
| `.page-title` | H1 — primary screen title |
| `.page-subtitle` | Muted one-line context |
| `.page-panel` | White flat panel; light `$shadow-card`; tables, tabs, forms |
| `.page-panel--flush` | Panel without inner padding (embedded tables) |

**Used in:** home, kanban, search, users, projects, ticket view, dashboard, edit forms.

### 4.2 Auth layout (`.page-auth`)

| Class | Purpose |
|-------|---------|
| `.page-auth` | Centers content vertically on login/password routes |
| `.auth-card` | White card, shadow, max-width 420px |
| `.form-actions` | Right-aligned button row in auth and modals |

**Used in:** login, password-reset-request, password-reset.

### 4.3 Home hub (`.home-hub`)

Personal work screen at `/` — two-column ticket tables (`.home-hub__tickets`) plus full-width activity feed. Ticket lists use `div.data-table.data-table--cols-home-tickets` inside `.home-hub__panel` (`max-height` + `overflow: auto`); activity uses `.activity-feed` in `.home-hub__panel--activity` with the same scroll pattern.

### 4.4 Project allocation

Member table on `/projects/:projectId/allocation` — add via user select, remove with guard UI listing open assigned tickets when blocked.

### 4.5 Project grid (`.project-grid` / `.project-card`) — legacy

Former home project picker; retained in styles for reference. New navigation uses **project hub** instead.

### 4.6 Detail list (`.detail-list`)

Definition list for read-only ticket metadata (label / value rows).

### 4.5 Tabs (`.tabs`)

Horizontal tab bar inside `.page-panel`; active tab underline uses `$base-active-color`.

### 4.6 Filters

| Class | Use |
|-------|-----|
| `.filter-input` | Compact text filter in list headers |
| `.role-filters` | Toggle chips for role filtering (users list) |

### 4.7 Empty state (`.empty-state`)

Dashed muted panel with centered italic copy — search, lists, password-reset stub.

### 4.8 Legacy (avoid in new screens)

| Class | Notes |
|-------|-------|
| `.centered` | Superseded by `.page` |
| `.form-header` | Superseded by `.auth-card` title inside card |

### 4.9 Edit form (`form.edit`)

| Property | Value |
|----------|-------|
| **Max-width** | 960px inside `.page-panel` |
| **Style** | Outline fields, `.form-actions` footer |
| **Used in** | user edit, project edit |

### 4.10 Card (`.card`)

| Variant | Style | Behavior |
|---------|-------|----------|
| Default | White, border, shadow; pad `$space-sm`; `min-width` 220px; title `0.9375rem`; description clamp 2 | Hover: border highlight (no lift) |
| `.empty` | Dashed border, muted text | Non-interactive empty state |
| In `.box` | Wider padding | Legacy project home (prefer `.project-card`) |

**Used in:** kanban tickets, legacy layouts.

### 4.11 Filter summary (`.filter-summary`)

Flat toolbar showing active search criteria (term, status). Replaces legacy `.parameters-box` on search results.

### 4.12 Parameters summary (`.parameters-box`) — legacy

Table layout showing active search filters (term, status). Muted toolbar background. Prefer `.filter-summary`.

### 4.13 Ticket actions toolbar (`.ticket-actions`)

| Property | Value |
|----------|-------|
| **Location** | `ticket-view.component.html` |
| **Style** | Full-width flex row; two `.ticket-actions__group` columns (50% each); `.form-field--control` grows inside each group; buttons fixed width |
| **Behavior** | Update assignee; move ticket to allowed status |

### 4.14 Version changelog section (`.changelog-section`)

| Property | Value |
|----------|-------|
| **Location** | `version-detail.component.html` |
| **Style** | Section heading + nested `.data-table` per changelog group |
| **Behavior** | Read-only release notes grouped by section name |

### 4.15 Inline editable table (`.inline-table`)

| Property | Value |
|----------|-------|
| **Location** | `workflow-form.component.html` |
| **Structure** | `.inline-table__header`, `__body`, `__row`, `__col`, `__col--action`; variant `--readonly` |
| **Style** | Same chrome as `.data-table` header/zebra rows; embeds `form-field--table` in cells |
| **Behavior** | Add/remove rows for statuses, transitions, finish statuses |

---

## 5. Kanban (`.board`)

| Element | Class | Behavior |
|---------|-------|----------|
| Board row | `.board` | Horizontal scroll; gap `$space-sm` |
| Column | `.column` | CDK `cdkDropList`; connected lists; pad `$space-sm` |
| Column title | `.header` | Status name (normalized); `0.875rem` |
| Ticket card | `.card` + `cdkDrag` | Links to `/ticket/:identifier` — see §4.10 |
| Empty column | `.card.empty` | Placeholder text |
| Drop target | `.cdk-drop-list-receiving` | Light navy tint border |

**API:** drag-drop calls ticket move endpoint.

---

## 6. Data tables

### 6.1 Page and admin list table (`div.data-table`)

| Property | Value |
|----------|-------|
| **Chrome** | Light gray header (`.header`); zebra rows in `.body` |
| **Layout** | Two layout modifiers — never mix on one table: `data-table--layout-grid` (home hub ticket columns via grid tracks) and `data-table--layout-table` (admin CRUD lists via CSS table; action column shrink-wraps) |
| **Structure** | `.header-cell`, `.row.even` / `.row.odd`, `.cell-actions` (table layout only), `.data-table--empty` |
| **Modifiers** | Grid: `data-table--cols-home-tickets`, `data-table--cols-home-saved-query`. Table: `data-table--cols-id-name-color-actions`, `data-table--cols-projects-list`, `data-table--cols-search-results`, `data-table--cols-query-help` |

**Used in:** users, projects, categories, workflows, versions, ticket history, dashboard widgets.

### 6.2 ~~Page table (`div.table`)~~ — removed

Navy chrome table was removed from `styles.scss`. Do not reintroduce — use **`div.data-table`** (§6.1).

Note: ticket detail activity uses `.activity-feed` (see §10.1), not `div.data-table` for the merged timeline.

---

## 7. Dialogs

### 7.1 Modal pattern (`.modal` inside `MatDialog`)

| Section | Class | Style |
|---------|-------|-------|
| Title bar | `.header` | Navy, white text |
| Body | `.body` | min-width 500px, form fields |
| Actions | `.actions` | Right-aligned; Cancel + primary |

**Used in:** category edit dialog; create-ticket uses the page pattern (`.page` + `.page-panel`) instead.

---

## 8. Navigation & menus

### 8.1 User menu (`mat-menu.menu-panel`)

White panel, divider lines, hover `$base-secondary-bg-color`. Role directive gates admin/PM items. Email header, Conta, Importar CSV, **Projetos** (PM/admin → `/projects`), Administração submenu, Sair.

### 8.1.1 Project navigation menu (`app-project-menu`)

See §1.1.1 — header **Projetos** → Kanban per project; optional **Gerenciar projetos** footer.

### 8.2 Notification menu (`mat-menu.menu-panel.notifications`)

Header row: title **Notificações** + text action **Marcar todas como lidas** (shown when server unread > 0). Unread badge (`.notification-badge`) uses server unread count — `$base-error-color`; display `99+` when unread > 99; hidden at 0. Items navigate on click; single mark-read via service; mark-all reloads page 0 + unread.

### 8.3 Ticket export menu (`mat-menu.menu-panel`)

Compact result-level download action used on `/search`, `/search/advanced`, and `/search/q/:slug`.

| Property | Value |
|----------|-------|
| **Trigger** | Secondary outlined `.btn.btn-secondary` labeled **Exportar** / **Export** |
| **Menu** | `mat-menu.menu-panel` with text items **CSV** and **JSON** |
| **Placement** | Simple and saved-query page-header actions; advanced-search form actions after a valid query has run |
| **Loading** | Disable the trigger while the selected export is pending to prevent duplicate downloads |
| **Errors** | Accessible translated inline `.error[role="alert"]`; restore the trigger after a failed request |
| **Behavior** | Re-submit the current server-side search criteria and download the attachment filename from `Content-Disposition`; never serialize visible result rows |
| **Locale** | Trigger and errors react to the active PT/EN catalog; file keys and values remain locale-neutral |

---

## 9. Feedback

### 9.0 Context hint (`.context-hint`)

| Property | Value |
|----------|-------|
| **Purpose** | Dismissible onboarding banner below page header |
| **Markup** | `<aside class="context-hint">` + `.context-hint__message` + `.context-hint__close` button |
| **Behavior** | **Fechar** sets `localStorage` key `issues.hint.dismissed.{hintId}` |
| **Screens** | Home, Kanban, Advanced search |

### 9.1 Toast (`app-toast`)

| Type | Class | Style |
|------|-------|-------|
| Success | `.toast-success` | Green semantic palette |
| Error | `.toast-error` | Red semantic palette |
| Warning | `.toast-warning` | Amber semantic palette |
| Info | `.toast-info` | Blue semantic palette |

**Behavior:** `role="alert"`, `aria-live="assertive"`; auto-dismiss via `ToastService`; close button optional.  
**Position:** `.toast-container` fixed (corners/center per config).

### 9.2 Inline error (`.error`)

Italic red text below forms (login).

### 9.3 Loading copy

Plain text: `Carregando...`, `Enviando...` on buttons and dashboard widgets.

---

## 10. Ticket detail

| Element | Behavior |
|---------|----------|
| Title row | `#identifier - title` + Observar/Ignorar toggle |
| Metadata | Projeto, Categoria, Descrição, Autor, Responsável, Status |
| Subscriber chips | `span.labels > span.label` |
| Activity section | `.activity-section` — comment form + `.filter-chip` filters + `.activity-feed` timeline |
| Comment form | Rich text + submit; disabled while posting |

### 10.1 Activity feed (`.activity-feed`)

| Property | Value |
|----------|-------|
| **Location** | `ticket-activity-feed.component.html`, `ticket-view.component.html` |
| **Structure** | `.activity-item` rows with `.activity-item__icon`, actor, summary, timestamp |
| **Comment row** | `.activity-item--comment` + `.activity-item__comment` (rich HTML) |
| **Change row** | `.activity-item__change` with strikethrough old → new values |
| **Filters** | `.filter-chips` → `.filter-chip` (`Todos` / `Comentários` / `Alterações`) |
| **Behavior** | Merges ticket history API + comments; newest first; SSE refresh on status move |

---

## 11. Dashboard (`.dashboard-configurator`)

Wrapped in `.page` with standard `.page-header`. Inner `.page-panel` contains `.dashboard-container` (edit + view modes).

| Mode | UI |
|------|-----|
| View | Widget grid with charts (Chart.js `baseChart`), KPI `data-table`, ticket `data-table` |
| Edit | Widget palette + CDK drag-drop layout; Salvar/Editar Layout toggle |

---

## 12. Material components inventory

| Component | Usage |
|-----------|-------|
| `MatButton` | All buttons |
| `MatIcon` | Icons (`fontIcon`) |
| `MatFormField` / `MatInput` | Forms, header search |
| `MatSelect` / `MatOption` | Status filter, dropdowns |
| `MatDialog` | Create ticket |
| `MatMenu` / `MatMenuItem` | User + notification menus |
| `MatCheckbox` | User edit roles |
| `MatStepper` | CSV ticket import wizard |
| `CdkDrag` / `CdkDropList` | Kanban, dashboard |
| `baseChart` (ng2-charts) | Dashboard pie charts |

| `MatStepper` | `ticket-import-wizard` | Linear wizard: upload → column mapping → preview → results |
| Hidden file input | `ticket-import-wizard` | `.btn` label wrapping `<input type="file" accept=".csv">`; chunked upload via init/part/complete; shows `N/M partes` progress |

---

## 12. Account settings

| Element | Class / component | Properties | Style | Behavior |
|---------|-------------------|------------|-------|----------|
| Language select | `account-settings` `mat-select` `locale` | Português (`pt`) \| English (`en`) | Outline field under profile | Saves with profile; activates the matching Transloco runtime catalog and updates copy, date/number formatting, and Material labels immediately without navigation or reload |

---

## 13. CSV import wizard

| Element | Class / component | Properties | Style | Behavior |
|---------|-------------------|------------|-------|----------|
| Wizard shell | `mat-stepper` linear | 4 steps | Inside `.page-panel` | Step 1: file; 2: mapping; 3: preview table; 4: results |
| Preview table | `.import-preview-table` | Row #, title, category, status, validation | Bordered table, `.import-row-invalid` for errors | Populated from `POST .../import/preview` |
| Step actions | `.step-actions` | Back / Next / Import | `.btn` + `.btn-secondary` | Import advances to results on success |

---

## 14. Style review findings

### 2026-07-11 — comfortable compact density

| Area | Finding | Status |
|------|---------|--------|
| Spacing scale | Airy 16/24/32 md/lg/xl limited content density | Fixed — 12/16/24px; floors documented |
| Controls | 40px / Material density 0 | Fixed — 36px / `density: -1` |
| Page type | Title 1.5rem / subtitle 0.95rem | Fixed — 1.25rem / 0.875rem |
| Kanban | Tall cards (pad 16, clamp 3, min-width 250) | Fixed — pad `$space-sm`, clamp 2, min-width 220 |
| Toolbar select | Phase filter clipped at 13rem / stacked 36px heights | Fixed — `.board-phase-filter` wider; infix/trigger flex; `board-phase-filter-panel` |
| Docs | Gallery tokens stale | Fixed — density note + token table |

### 2026-07-03 — design system consolidation

| Area | Finding | Status |
|------|---------|--------|
| Filter chips | Duplicate `.filter-chip` blocks with conflicting `.active` / `--active` | Fixed — single block; `.filter-chip--active` only |
| Tables | `div.table` unused; all lists on `div.data-table` | Fixed — removed `div.table` from CSS |
| Workflow form | Component-local table BEM duplicated data-table | Fixed — global `.inline-table` |
| Ticket view | `.edit-form`, `.ticket-actions` undocumented | Fixed — `form.edit`, `.ticket-actions` in global CSS |
| Toasts | Inline component styles with raw hex | Fixed — moved to `styles.scss` semantic tokens |
| Layout tokens | Panel/table padding drift | Fixed — `$panel-padding`, `$table-cell-padding-*` |
| i18n | Systematic pass | Implemented — 617-key PT/EN runtime catalogs; canonical routes and immediate same-screen switching |

### 2026-07-02 — flat UI pass

| Area | Finding | Status |
|------|---------|--------|
| Corners | All surfaces use `$radius-none` | ✅ |
| Hover | Project cards / kanban cards used shadow growth on hover | Fixed — border highlight |
| Search bar | Heavy drop shadow + glow focus ring | Fixed — light shadow, border focus |
| Auth card | Used `$shadow-card-hover` at rest | Fixed — `$shadow-card` |
| Edit forms | `form.edit` duplicated `page-panel` styles | Fixed — `edit page-panel` combo |
| Create ticket cancel | `outlined` variant inconsistent with gallery | Fixed — `filled` + `.btn-cancel` |
| Password reset SCSS | Legacy `form` border conflicted with `.auth-card` | Fixed — removed |
| Toasts | Slide animation + heavy shadow | Fixed — opacity fade, light shadow |
| Dashboard widgets | Shadow hover on palette items | Fixed — border highlight |
| Users filters | Native `.filter-input` missing explicit border | Fixed |
| Users filters | Native inputs vs Material elsewhere | Open — tech debt |

### Earlier audit (2026-07-02)

| Area | Finding | Status |
|------|---------|--------|
| Theme | M3 default primary clashed with flat UI palette | Fixed |
| Buttons | `matButton` variant inconsistency | Fixed |
| Header | Toolbar controls replaced tall `mat-form-field` | Fixed |
| Users list | Debug template leak | Fixed |
| Dashboard | Typo `dashboar table` | Fixed |
| Password reset page | Stub component | Done |
| Workflows / account routes | Menu links may lack routes | Done |

---

## 15. Adding a new element — checklist

1. Search this gallery — can an existing element be reused?
2. If new: add SCSS to `colors.scss` / `styles.scss` (not ad-hoc hex in components).
3. Document the element in this file (properties, style, behavior, used-in).
4. Walk [issues-ux.mdc](../.cursor/rules/issues-ux.mdc) Nielsen checklist.
5. Update [feature-catalog.md](feature-catalog.md) if routes or labels change.
6. Add/update `*.spec.ts` when behavior is non-trivial.

# UI Elements Gallery — Issues

Canonical catalog of every reusable UI element in the Angular SPA (`src/main/webui/`).

**Governance:** Before adding or changing any UI element, read this document. Reuse an existing element with the documented properties, style, and behavior. If nothing fits, extend the gallery in the same PR.

Related: [feature-catalog.md](feature-catalog.md) (routes), [colors.scss](../src/main/webui/src/colors.scss) (tokens), [styles.scss](../src/main/webui/src/styles.scss) (global classes), [issues-ux.mdc](../.cursor/rules/issues-ux.mdc) (Nielsen checklist).

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
| `$shadow-card` | `0 1px 3px rgba(0,0,0,.08)` | Cards, columns |
| `$space-sm` / `$space-md` / `$space-lg` | 8 / 16 / 24 px | Spacing grid |

Material theme CSS variables are aligned to `$base-active-color` in `styles.scss`.

---

## 1. App shell

### 1.1 Header (`.main-header`)

| Property | Value |
|----------|-------|
| **Location** | `app/app.html` |
| **Visibility** | Always; search/status/create only when authenticated |

**Style:** Navy background (`$base-background-color`), white text, flex row, wraps on mobile.

**Layout (left → right):**
- **Brand** (`.brand`) — ticket icon + "Issues" wordmark → `/` (replaces "Início" text link)
- **Search** (`.search-bar`) — pill input + circular search button (authenticated)
- **Status** (`.toolbar-select`) — pill dropdown on navy chrome
- **Novo** — compact primary button
- **Actions** (`.header-actions`) — notification icon + hamburger menu icon

**Behavior:**
- **Brand** → `/` with `brand-active` on home
- **Search** — `.search-bar` form; submit via button or Enter → `/search`
- **Status** — `.toolbar-select`; change navigates to `/search` with query params
- **Novo** — opens `CreateTicketModalComponent` dialog
- **Notificações** — icon-only `matIconButton` + badge (`app-notification`)
- **Menu** — icon-only hamburger `matIconButton` + `mat-menu`; role-gated items
- **Acessar** — shown when logged out → `/login`

### 1.2 Main content (`main.container`)

Flex-grow content area; horizontal margin `$space-xl` (reduced on mobile).

### 1.3 Footer (`.main-footer`)

Copyright + **Documentação da API** link (`/openapi`). Navy background, white text.

---

## 2. Buttons

All action buttons use `matButton` (or `matButton="filled"`) **plus** a gallery class. Do **not** rely on bare `button {}` global styles.

### 2.1 Primary (`.btn`)

| Property | Value |
|----------|-------|
| **Style** | `$base-active-color` fill, white label/icon, 4px radius |
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
| **Style** | `$base-error-color` fill |

**Used in:** create-ticket modal, user/project edit forms.

### 2.4 Header icon button (`.btn-icon-header`)

| Property | Value |
|----------|-------|
| **Directive** | `matIconButton` |
| **Style** | White icon on navy; circular hover |
| **Used in** | Hamburger menu, notification bell |

### 2.5 Brand link (`.brand`)

| Property | Value |
|----------|-------|
| **Markup** | `mat-icon` + `.brand-name` "Issues" |
| **Style** | White wordmark; subtle underline when `brand-active` on home |
| **Behavior** | `routerLink="/"` |

### 2.6 Primary / secondary / cancel

| Class | `matButton` variant |
|-------|---------------------|
| `.btn` | `"filled"` — corporate primary via `mat.button-overrides` |
| `.btn-secondary` | `"outlined"` |
| `.btn-cancel` | `"filled"` with red CSS custom properties |

### 2.7 Tab button (`.tab-button`)

| Property | Value |
|----------|-------|
| **Location** | `ticket-view.component.html` / `.scss` |
| **Style** | Borderless; active = bottom border `$base-active-color` |
| **Behavior** | Toggles `activeTab` (`history` \| `comments`); no route change |

### 2.8 Icon-only remove (`.btn-remove`)

Dashboard widget header; borderless, muted text, red on hover.

---

## 3. Form controls

### 3.1 Standard field (`mat-form-field.form-field`, `appearance="fill"`)

| Property | Value |
|----------|-------|
| **Width** | 100% in forms |
| **Validation** | `mat-error` below field; red `$base-error-color` |
| **Behavior** | Reactive (`formControlName`) or template-driven (`ngModel`) |

**Used in:** login, password reset, create-ticket modal, user edit, project edit.

### 3.2 Search bar (`.search-bar`)

| Property | Value |
|----------|-------|
| **Markup** | `form` with leading icon, `input[type="search"]`, circular `.search-bar__action` submit |
| **Style** | White pill (`border-radius: 999px`), shadow, focus ring on `:focus-within` |
| **Behavior** | Submit button or implicit form submit → `/search` with query params |

### 3.3 Header toolbar select (`.toolbar-select`)

| Property | Value |
|----------|-------|
| **Markup** | Native `select` with `[ngValue]` for `Status` objects |
| **Width** | min 120px (100% on mobile) |
| **Behavior** | Change triggers search navigation |

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
| **Style** | `.rich-text-editor` — toolbar + contenteditable area |
| **Behavior** | Toolbar formatting; emits HTML string |

**Used in:** ticket comments.

---

## 4. Layout blocks

### 4.1 Page scaffold (`.page`)

Standard wrapper for every authenticated route.

| Class | Purpose |
|-------|---------|
| `.page` | Max-width content, vertical padding, `$surface-page` background |
| `.page--wide` | Full-width variant (kanban, dashboard) |
| `.page-header` | Flex row: title block + `.page-header__actions` |
| `.page-title` | H1 — primary screen title |
| `.page-subtitle` | Muted one-line context |
| `.page-panel` | White card panel with shadow (tables, tabs, forms) |
| `.page-panel--flush` | Panel without inner padding (embedded tables) |

**Used in:** home, kanban, search, users, projects, ticket view, dashboard, edit forms.

### 4.2 Auth layout (`.page-auth`)

| Class | Purpose |
|-------|---------|
| `.page-auth` | Centers content vertically on login/password routes |
| `.auth-card` | White card, shadow, max-width 420px |
| `.form-actions` | Right-aligned button row in auth and modals |

**Used in:** login, password-reset-request, password-reset.

### 4.3 Project grid (`.project-grid` / `.project-card`)

Home page project picker — responsive grid of linked cards with Kanban/Dashboard actions.

### 4.4 Detail list (`.detail-list`)

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
| Default | White, border, shadow | Hover: slightly stronger shadow (no lift) |
| `.empty` | Dashed border, muted text | Non-interactive empty state |
| In `.box` | Wider padding | Legacy project home (prefer `.project-card`) |

**Used in:** kanban tickets, legacy layouts.

### 4.11 Parameters summary (`.parameters-box`)

Table layout showing active search filters (term, status). Muted toolbar background.

---

## 5. Kanban (`.board`)

| Element | Class | Behavior |
|---------|-------|----------|
| Board row | `.board` | Horizontal scroll |
| Column | `.column` | CDK `cdkDropList`; connected lists |
| Column title | `.header` | Status name (normalized) |
| Ticket card | `.card` + `cdkDrag` | Links to `/ticket/:identifier` |
| Empty column | `.card.empty` | Placeholder text |
| Drop target | `.cdk-drop-list-receiving` | Light navy tint border |

**API:** drag-drop calls ticket move endpoint.

---

## 6. Data tables

### 6.1 Page table (`div.table`)

| Property | Value |
|----------|-------|
| **Chrome** | Navy header row (`.header`), navy sub-header (`.sub-header`) |
| **Body** | CSS `display: table`; rows `.row.even` / `.row.odd` neutral zebra |
| **Empty** | `div.table.empty` — dashed muted panel |

**Used in:** users list, projects list, ticket history.

### 6.2 Embedded data table (`div.data-table`)

| Property | Value |
|----------|-------|
| **Chrome** | Light gray header; fits inside dashboard widgets |
| **Structure** | Same `.header` / `.body` / `.row` pattern as page table |

**Used in:** dashboard KPI and table widgets.

---

## 7. Dialogs

### 7.1 Modal pattern (`.modal` inside `MatDialog`)

| Section | Class | Style |
|---------|-------|-------|
| Title bar | `.header` | Navy, white text |
| Body | `.body` | min-width 500px, form fields |
| Actions | `.actions` | Right-aligned; Cancel + primary |

**Used in:** `CreateTicketModalComponent` (`disableClose: true`).

---

## 8. Navigation & menus

### 8.1 User menu (`mat-menu.menu-panel`)

White panel, divider lines, hover `$base-secondary-bg-color`. Role directive gates admin/PM items.

### 8.2 Notification menu (`mat-menu.menu-panel.notifications`)

Unread count badge (`.unread-box` / `.unread`) — `$base-error-color`. Items navigate on click; mark read via service.

---

## 9. Feedback

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
| Tabs | Histórico / Comentários |
| Comment form | Rich text + submit; disabled while posting |

---

## 11. Dashboard (`.dashboard-configurator`)

Wrapped in `.page.page--wide` with standard `.page-header`. Inner `.page-panel` contains `.dashboard-container` (edit + view modes).

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
| `CdkDrag` / `CdkDropList` | Kanban, dashboard |
| `baseChart` (ng2-charts) | Dashboard pie charts |

---

## 13. Live review findings (2026-07-02, `http://localhost:8080/`)

Issues observed in running dev environment and code audit:

| Area | Finding | Status |
|------|---------|--------|
| Theme | M3 default primary clashed with corporate palette | Fixed — `mat.button-overrides` + CSS vars |
| Buttons | `matButton` without variant rendered text buttons; custom CSS fought Material | Fixed — `matButton="filled"` / `"outlined"` |
| Header | `mat-form-field` too tall on navy chrome | Fixed — `.toolbar-search` / `.toolbar-select` |
| Header chrome | Menu/notification buttons low contrast on navy | Fixed — `.btn-header` outlined variant |
| Nav active | Bright blue pill on Início | Fixed — subtle white overlay |
| Users list | Debug `{{ filter.name }}` leaked in template | Fixed |
| Dashboard | Typo class `dashboar table` broke widget tables | Fixed → `.data-table` |
| i18n | English labels on user/project forms | Fixed → PT-BR |
| Users filters | Native inputs vs Material elsewhere | Documented tech debt |
| Password reset page | Stub `PasswordResetComponent` | Open — route exists, minimal UI |
| Workflows / account routes | Linked in menu, routes may be missing | Open — verify `app.routes.ts` |

---

## 14. Adding a new element — checklist

1. Search this gallery — can an existing element be reused?
2. If new: add SCSS to `colors.scss` / `styles.scss` (not ad-hoc hex in components).
3. Document the element in this file (properties, style, behavior, used-in).
4. Walk [issues-ux.mdc](../.cursor/rules/issues-ux.mdc) Nielsen checklist.
5. Update [feature-catalog.md](feature-catalog.md) if routes or labels change.
6. Add/update `*.spec.ts` when behavior is non-trivial.

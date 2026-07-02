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

**Style:** Navy background (`$base-background-color`), white text, flex row, wraps on mobile (`max-width: 750px`).

**Behavior:**
- Nav link **Início** → `/` with `routerLinkActive`
- **Buscar tickets** — compact `mat-form-field` (`.header-field`); search on **Enter** only
- **Status** — `mat-select` (`.header-field.status-field`); change navigates to `/search` with query params
- **Novo ticket** — opens `CreateTicketModalComponent` dialog
- **Notificações** — SSE-backed menu (`app-notification`)
- **Menu** — role-gated items (Conta, Usuários, Processos, Projetos, Sair)
- **Acessar** — shown when logged out → `/login`

**Chrome buttons** (notification, menu, login): `button.mat-mdc-button` without `.btn` — use `.btn-header` styling (translucent on navy).

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

### 2.4 Header chrome (`.btn-header`)

| Property | Value |
|----------|-------|
| **Style** | Semi-transparent white on navy; no primary blue fill |
| **Used in** | Notification trigger, Menu, Acessar |

### 2.5 Tab button (`.tab-button`)

| Property | Value |
|----------|-------|
| **Location** | `ticket-view.component.html` / `.scss` |
| **Style** | Borderless; active = bottom border `$base-active-color` |
| **Behavior** | Toggles `activeTab` (`history` \| `comments`); no route change |

### 2.6 Icon-only remove (`.btn-remove`)

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

### 3.2 Header toolbar field (`.header-field`)

| Property | Value |
|----------|-------|
| **Appearance** | `outline`, `subscriptSizing="dynamic"`; labels hidden via `.header-field` CSS |
| **Width** | 220px search / 160px status (100% on mobile) |
| **Style** | White field background on navy header; no subscript area |

### 3.3 Native filter input (`input[type="text"]` in tables)

**Used in:** `users-view` filter row only.  
**Style:** Global `input` rules — white bg, `$border-default` border.  
**Tech debt:** migrate to `mat-form-field` or shared `.filter-input` for consistency.

### 3.4 Checkbox

Native checkbox in users list (filter + read-only role display).  
**Tech debt:** prefer `mat-checkbox` like `users-edit`.

### 3.5 Rich text editor (`app-rich-text-editor`)

| Property | Value |
|----------|-------|
| **Inputs** | `value`, `disabled`, `placeholder` |
| **Outputs** | `valueChange` |
| **Style** | `.rich-text-editor` — toolbar + contenteditable area |
| **Behavior** | Toolbar formatting; emits HTML string |

**Used in:** ticket comments.

---

## 4. Layout blocks

### 4.1 Page title area (`.centered`)

Centers content; used on home, lists, ticket view, search.

### 4.2 Form header (`.form-header`)

Navy banner above auth forms (login, password reset). White heading text.

### 4.3 Edit form (`form.edit`)

| Property | Value |
|----------|-------|
| **Max-width** | 960px |
| **Style** | White card, shadow, toolbar footer (`.actions`) |
| **Used in** | user edit, project edit |

### 4.4 Card (`.card`)

| Variant | Style | Behavior |
|---------|-------|----------|
| Default | White, border, shadow | Hover: slightly stronger shadow (no lift) |
| `.empty` | Dashed border, muted text | Non-interactive empty state |
| In `.box` | Wider padding | Project home grid |

**Used in:** home projects, search results, kanban tickets, parameters display.

### 4.5 Parameters summary (`.parameters-box`)

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
| Theme | M3 default primary (`#343dff`) clashed with corporate `#1565C0` | Fixed — CSS variable overrides |
| Buttons | Global `button {}` styled tabs, menu items, toolbar | Fixed — scoped to `.btn` |
| Header | Bulky labeled fields on navy chrome | Fixed — compact `floatLabel="never"` |
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

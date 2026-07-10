# Categories

**Feature version:** 2  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Administrators manage ticket categories: name and display color. Categories classify tickets on create, edit, Kanban cards, and imports. List is available to all authenticated users for pickers; admin CRUD at `/categories`. Unused categories may be deleted; deletion is rejected while any ticket (including soft-deleted) or any project ticket template references the category.

## Wireframe

**Guide:** layout reference for UI implementation — update when category UI or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-10 |

### Screen: `/categories`

| Region | Elements |
|--------|----------|
| List | `.data-table`: name, color swatch |
| Actions | **Nova categoria**; row **Editar** opens dialog; row **Excluir** with confirm |
| Dialog | Name field; color picker |

```
┌─────────────────────────────────────────────┐
│  Categorias              [ Nova categoria ]   │
├─────────────────────────────────────────────┤
│  ■ Bug    │ Editar │ Excluir                │
│  ■ Feature│ Editar │ Excluir                │
└─────────────────────────────────────────────┘
```

### Confirm: delete category

| Region | Elements |
|--------|----------|
| Dialog | Confirm delete; **Cancelar** / **Excluir** |

```
┌─────────────────────────────────────────────┐
│  Excluir categoria?                         │
│  Esta ação não pode ser desfeita.           │
│  [ Cancelar ]  [ Excluir ]                  │
└─────────────────────────────────────────────┘
```

When delete is rejected (referenced by tickets or project templates): show error message — category cannot be deleted while in use.

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `categories` (Classification); read checks against tickets and projects (no cross-context write) |
| Packages / files | `categories.delete.DeleteCategoryEndpoint`, `CategoryService.delete`, `CategoryRepository`; Angular `categories-view`, `category.service` |
| API | Existing: `GET/POST /categories`, `PUT /categories/{id}`; **new:** `DELETE /categories/{id}` (admin) |
| UI | `/categories` — **Excluir** + confirm; error when referenced |
| Schema / seed | No schema change — FKs already on `tb_tickets.category_id` and `tb_projects.ticket_template_category_id` |
| Tests | `DeleteCategoryEndpointTest`; `categories-view.component.spec.ts` |
| Docs | domain-spec invariant **36**; feature-catalog Category list; ARCHITECTURE.md API map |

### Feature questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Should categories be deletable when referenced by tickets? | answered | **No** — forbid delete when any ticket references the category |
| FQ2 | Does category rename need explicit UX beyond FK propagation? | answered | **No special UX** — tickets reference category **by id** (`category_id`), not name; rename updates display only |
| FQ3 | If a project **ticket template** references the category (`ticket_template_category_id`) but no ticket does, what happens on delete? | answered | **A** — block delete (same as tickets) |

## Architecture

### Bounded contexts

| Context | Role |
|---------|------|
| Classification (`categories`) | Owns delete API, service rule, repository remove |
| Ticket management | Read-only: any ticket with `category_id` (including soft-deleted) |
| Project administration | Read-only: any project with `ticket_template_category_id` |

Dependency: count queries in `CategoryRepository` (JPQL on `Ticket` / `Project` entities). Do **not** call `TicketService` / `ProjectService` for the guard.

### Packages / layers

| Layer | Type | Responsibility |
|-------|------|----------------|
| Endpoint | `categories.delete.DeleteCategoryEndpoint` | `DELETE /categories/{id}` → `CategoryService.delete` |
| Service | `CategoryService.delete(long id)` | Load category; if ticket or template reference → `BadRequestException`; else remove |
| Repository | `CategoryRepository` | `delete(Category)`; `countTicketsByCategoryId(long)`; `countProjectsByTemplateCategoryId(long)` |

### API surface

| Method | Path | Auth | Success | Failure |
|--------|------|------|---------|---------|
| `DELETE` | `/api/categories/{id}` | `ADMIN` | `204 No Content` | `404` unknown id; `400` referenced by tickets or project templates |

`operationId`: `deleteCategory`. Tag: `Category`.

Error messages (distinct for clarity):
- Tickets: `Category cannot be deleted while tickets reference it`
- Templates: `Category cannot be deleted while project ticket templates reference it`

### Schema / persistence

- No Flyway change.
- Soft-deleted tickets **count** as references.
- Project template category references **block** delete (**FQ3** A).

### Cross-context integration

- No CDI events.
- Regenerate Angular API client after endpoint add.

### Frontend

| Piece | Change |
|-------|--------|
| `CategoryService` | `delete(id)` → generated `deleteCategory` |
| `categories-view` | **Excluir** per row; confirm dialog; on 400 show error; refresh list on success |

### Tests

| Test | Asserts |
|------|---------|
| `DeleteCategoryEndpointTest` | Unused → 204; ticket ref → 400; soft-deleted ticket → 400; template ref → 400; unknown → 404; non-admin → 403 |
| Angular | Delete calls API; list refreshes; error path |

### Architecture questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Hard delete vs soft-delete for categories? | answered | **Hard delete** — categories have no `deleted` flag; unused rows are removed |
| AQ2 | HTTP status when referenced? | answered | **400 Bad Request** — consistent with other domain rule violations (`BadRequestException`) |
| AQ3 | How to detect ticket references? | answered | Count tickets by `category_id` (include soft-deleted); reject if count > 0 |
| AQ4 | How to detect template references? | answered | Count projects by `ticket_template_category_id`; reject if count > 0 (**FQ3** A) |

## Changelog

### Initial implementation — baseline

**Version:** 1  
**Status:** done

**Description:** Admin category list with Nova categoria and Editar dialog; color picker for card/display styling; public list endpoint for forms.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Create ticket | Category picker |
| Ticket management | Category on ticket detail |
| Kanban board | Category color on cards |
| Ticket import | Category column mapping |
| Project administration | Template default category |
| — | None identified |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Category list matches **Wireframe** | Wireframe | ☑ |
| FC2 | Create/edit dialog with color picker | Wireframe | ☑ |
| FC3 | Public list endpoint for pickers | Summary | ☑ |
| FC4 | `feature-catalog.md` — Category list row | Impact / Docs | ☑ |

**Implementation notes:** `categories-view.component.ts`; `ListCategoriesEndpoint` used by filters and ticket forms.

### Category delete guard — 2026-07-03

**Version:** 2  
**Status:** done

**Development approval:** approved 2026-07-10 — tasks: T1, T2, T3, T4, T5, T6

**Description:** Add admin delete for categories; reject delete when any ticket (including soft-deleted) or any project ticket template references the category. Rename remains id-based (no bulk ticket update).

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [ticket-import](ticket-import.md) | Import still resolves category by name at import time |
| [ticket-management](ticket-management.md) | Category picker by id; delete does not cascade |
| [project-administration](project-administration.md) | Template `categoryId` blocks category delete until cleared |
| [create-ticket](create-ticket.md) | Partial template category pre-fill unaffected until category deleted |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | `DELETE /categories/{id}` succeeds when unused | Architecture | ☑ |
| FC2 | Delete rejected (400) when tickets reference category | FQ1 | ☑ |
| FC3 | Soft-deleted tickets still block delete | Architecture / AQ3 | ☑ |
| FC4 | Delete rejected (400) when project templates reference category | FQ3 | ☑ |
| FC5 | UI **Excluir** + confirm; error when referenced | Wireframe | ☑ |
| FC6 | Rename does not require ticket bulk update (id-based) | FQ2 | ☑ |
| FC7 | `domain-specification.md` invariant **36** + feature-catalog / ARCHITECTURE API map | Docs | ☑ |
| FC8 | `DeleteCategoryEndpointTest` + Angular coverage | Tests | ☑ |

#### Tasks

| ID | Deliverable | Done |
|----|-------------|------|
| T1 | `CategoryRepository` — `countTicketsByCategoryId`, `countProjectsByTemplateCategoryId`, `delete` | ☑ |
| T2 | `CategoryService.delete` — 404 if missing; 400 if ticket or template reference; else hard delete | ☑ |
| T3 | `DeleteCategoryEndpoint` — `DELETE /categories/{id}`, admin, `operationId=deleteCategory`, 204 | ☑ |
| T4 | `DeleteCategoryEndpointTest` — unused 204; ticket ref 400; soft-deleted ticket 400; template ref 400; 404; 403 | ☑ |
| T5 | Angular: `CategoryService.delete`, codegen; `categories-view` **Excluir** + confirm + error; specs | ☑ |
| T6 | Docs: feature-catalog Category list steps; ARCHITECTURE.md API map; confirm domain invariant **36** | ☑ |

#### Test coverage

| ID | Covers | Tests | Done |
|----|--------|-------|------|
| TC1 | T1–T4 | `DeleteCategoryEndpointTest` — unused / ticket / soft-deleted / template / 404 / 403 | ☑ |
| TC2 | T5 | `categories-view.component.spec.ts` — delete success refresh; error on 400 | ☑ |
| TC3 | T6 | Doc review — catalog + ARCHITECTURE + invariant **36** | ☑ |

**Implementation notes:** `DeleteCategoryEndpoint` + repository reference counts; UI confirm dialog and toast on 400. `mvn verify` green; Angular build + categories-view specs green.

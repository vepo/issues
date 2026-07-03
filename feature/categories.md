# Categories

**Feature version:** 2  
**Status:** planned  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Administrators manage ticket categories: name and display color. Categories classify tickets on create, edit, Kanban cards, and imports. List is available to all authenticated users for pickers; admin CRUD at `/categories`.

## Wireframe

**Guide:** layout reference for UI implementation — update when category UI or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-03 |

### Screen: `/categories`

| Region | Elements |
|--------|----------|
| List | `.data-table`: name, color swatch |
| Actions | **Nova categoria**; row **Editar** opens dialog |
| Dialog | Name field; color picker |

```
┌─────────────────────────────────────────────┐
│  Categorias              [ Nova categoria ]   │
├─────────────────────────────────────────────┤
│  ■ Bug    │ Editar                          │
│  ■ Feature│ Editar                          │
└─────────────────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `categories` (Classification) |
| Packages / files | `categories.create`, `categories.update`, `categories.list` |
| API | `GET /categories`, `POST /categories`, `POST /categories/{id}` |
| UI | `/categories`; `categories-view` component (list, create/edit dialog with color picker) |
| Schema / seed | `tb_categories`; sample categories in `dev-import.sql` |
| Tests | `ListCategoriesEndpointTest`, `CreateCategoryEndpointTest`, `UpdateCategoryEndpointTest` |
| Docs | domain-spec (Category), feature-catalog (Category list), README § Tickets & workflow |

### Feature questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Should categories be deletable when referenced by tickets? | answered | **No** — forbid delete when any ticket references the category |
| FQ2 | Does category rename need explicit UX beyond FK propagation? | answered | **No special UX** — tickets reference category **by id** (`category_id`), not name; rename updates display only |

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
**Status:** planned

**Description:** Enforce non-deletion of categories referenced by tickets; confirm tickets use `category_id` FK (already id-based).

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [ticket-import](ticket-import.md) | Import still resolves category by name at import time |
| [ticket-management](ticket-management.md) | Category picker by id |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Delete rejected when tickets reference category | FQ1 | ☐ |
| FC2 | Rename does not require ticket bulk update | FQ2 | ☐ |
| FC3 | `domain-specification.md` — category id reference | Docs | ☐ |

# Categories

**Feature version:** 1  
**Status:** done  
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

### Open questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| Q1 | Should categories be deletable when referenced by tickets? | open | |
| Q2 | Does category rename need explicit UX beyond FK propagation? | open | |

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

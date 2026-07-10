# Create ticket

**Feature version:** 2  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Create new tickets globally (`/tickets/new`) or within a project (`/project/:projectId/tickets/new`). Project-scoped creation pre-fills the form from the optional **ticket template** when enabled. Auto-generates human-readable identifiers (`{prefix}-{seq}`) and initial workflow status.

## Wireframe

**Guide:** layout reference for UI implementation — update when form fields or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-03 |

### Screen: `/tickets/new` and `/project/:projectId/tickets/new`

| Region | Elements |
|--------|----------|
| Form | Project (global route only), title, description, category, priority, assignee, optional phase |
| Actions | **Criar** primary; cancel |

```
┌─────────────────────────────────────────────┐
│  Novo ticket                                │
├─────────────────────────────────────────────┤
│  Projeto [▼]  (hidden when project route)   │
│  Título [………………………………]                     │
│  Descrição [rich text]                      │
│  Categoria │ Prioridade │ Responsável       │
│  Fase [▼] optional                          │
│  [ Criar ]  [ Cancelar ]                    │
└─────────────────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `ticket`, `ticket.history`, `project` |
| Packages / files | `ticket.create.CreateTicketEndpoint`, `TicketHistoryService` |
| API | `POST /tickets` |
| UI | `/tickets/new`, `/project/:projectId/tickets/new`; `create-ticket`, `ticket-form` components |
| Schema / seed | `tb_tickets`; project template columns on `tb_projects` |
| Tests | `CreateTicketEndpointTest` |
| Docs | domain-spec (Identifier, Ticket template), feature-catalog (Create ticket rows), README § Tickets & workflow |

### Feature questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Should ticket template be optional when enabled but PM has not configured all fields? | answered | **Yes** — when template is enabled, only configured fields pre-fill; user may submit without filling template-only fields |

## Changelog

### Initial implementation — baseline

**Version:** 1  
**Status:** done

**Description:** Create ticket form with project selection, title, description, category, priority, assignee; project route pre-fills from template; history logs CREATED.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Project administration | Template configuration on project edit |
| Workflow configuration | Start status from project workflow |
| Kanban board | Novo ticket entry point |
| Ticket management | New tickets appear in lists and detail |
| Categories | Category picker uses category list |
| — | None identified |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Create form matches **Wireframe** | Wireframe | ☑ |
| FC2 | Project route pre-fills from ticket template | Summary | ☑ |
| FC3 | Identifier auto-generated on create | Summary | ☑ |
| FC4 | `feature-catalog.md` — Create ticket rows | Impact / Docs | ☑ |

**Implementation notes:** `create-ticket.component.ts`, `ticket-form.component.ts`; identifier generation in create flow.

### Partial ticket template pre-fill — 2026-07-03

**Version:** 2  
**Status:** done

**Development approval:** approved 2026-07-03 — tasks: T1–T6

**Description:** When **Usar template de ticket** is enabled, pre-fill only fields the PM configured; do not require template-only fields on create.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [project-administration](project-administration.md) | Template fields remain optional individually |
| [ticket-management](ticket-management.md) | Create validation unchanged for required ticket fields |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Create succeeds with partial template pre-fill | FQ1 | ☑ |
| FC2 | `domain-specification.md` — ticket template invariant updated | Docs | ☑ |

**Implementation notes:** `ProjectService.validateTicketTemplate` requires at least one configured field; blank template values stored as null. `create-ticket` and `ticket-form` apply partial defaults only. Project edit allows optional template fields with **Nenhuma** priority option. Tests: `CreateProjectEndpointTest`, `create-ticket.component.spec.ts`, `ticket-form.component.spec.ts`.

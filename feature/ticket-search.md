# Ticket search

**Feature version:** 1  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Global full-text search across tickets in all projects. Authenticated users enter a search term and open matching tickets from results. Deleted tickets are excluded.

## Wireframe

**Guide:** layout reference for UI implementation — update when search UX or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-03 |

### Screen: `/search`

| Region | Elements |
|--------|----------|
| Search bar | Term input; filter chips (status, etc. when present) |
| Results | Table: identifier (link), title, project, status |

```
┌─────────────────────────────────────────────┐
│  Buscar                                     │
│  [ search term………………… ]  [filter chips]     │
├─────────────────────────────────────────────┤
│  Identifier │ Title │ Project │ Status      │
│  PROJ-1     │ …     │ …       │ …           │
└─────────────────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `ticket` |
| Packages / files | `ticket.search.SearchTicketsEndpoint`, `TicketRepository` search query |
| API | `GET /tickets/search` |
| UI | `/search`; `search-tickets` component |
| Schema / seed | `tb_tickets` (title, description, identifier); relies on existing seed data |
| Tests | `SearchTicketsEndpointTest` |
| Docs | domain-spec (Search), feature-catalog (Ticket search), README § Views & analytics |

### Open questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| Q1 | Should search support filters (project, status, assignee)? | open | |
| Q2 | Is dedicated full-text indexing required at scale? | open | |

## Changelog

### Initial implementation — baseline

**Version:** 1  
**Status:** done

**Description:** Menu-accessible global search page with term input and navigation to ticket detail by identifier.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Ticket management | Opens ticket detail from results |
| Kanban board | Alternative entry path to tickets |
| — | None identified |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Search page matches **Wireframe** | Wireframe | ☑ |
| FC2 | Soft-deleted tickets excluded from results | Summary | ☑ |
| FC3 | Results link to ticket detail | Wireframe | ☑ |
| FC4 | `feature-catalog.md` — Ticket search row | Impact / Docs | ☑ |

**Implementation notes:** `search-tickets.component.ts`; `SearchTicketsEndpoint` excludes soft-deleted tickets.

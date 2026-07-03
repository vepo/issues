# Ticket search

**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Global full-text search across tickets in all projects. Authenticated users enter a search term and open matching tickets from results. Deleted tickets are excluded.

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

### Risks and open questions

- No advanced filters (project, status, assignee) in search UI.
- Search performance at scale not indexed beyond DB full-text/query approach.

## Changelog

### Initial implementation — baseline

**Status:** done

**Description:** Menu-accessible global search page with term input and navigation to ticket detail by identifier.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Ticket management | Opens ticket detail from results |
| Kanban board | Alternative entry path to tickets |
| — | None identified |

**Implementation notes:** `search-tickets.component.ts`; `SearchTicketsEndpoint` excludes soft-deleted tickets.

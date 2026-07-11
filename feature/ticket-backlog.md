# Ticket backlog

**Feature version:** 1  
**Status:** done  
**Requested:** 2026-07-11

## Summary

Add a **project backlog** view: a ranked, **paginated** list of tickets that teams can see in order and that **project-managers and admins** can **reorder**. Ordering uses an explicit **backlog rank**, separate from the **Priority** urgency enum (`LOW` / `MEDIUM` / `HIGH` / `CRITICAL`).

Today tickets have no project-wide rank; Kanban groups by status; search lists do not support user-defined order. This feature fills the planning gap between “what exists” and “what we do next.”

**Distinct from:** **Priority** (urgency label), **Kanban** (status columns + moves), **Phase** (time box), **Ticket search** (query language, not ranked planning).

## Scope

| ID | In scope | Notes |
|----|----------|-------|
| S1 | Project-scoped backlog only | **FQ1** — no global backlog |
| S2 | Persist and display **backlog rank** | **FQ2** — distinct from Priority |
| S3 | Reorder via drag-and-drop (PM/admin) | **FQ4** — members read-only |
| S4 | List excludes soft-deleted and **DONE** finish statuses | **FQ3** |
| S5 | Navigation from project hub; Projetos menu keeps Kanban | **FQ9** |
| S6 | New tickets (create / import) append at **end** of rank | **FQ5** |
| S7 | **Paginated** list (infinite scroll UX) | **FQ10** |

| ID | Out of scope for v1 | Notes |
|----|---------------------|-------|
| O1 | Changing Priority enum when reordering | **FQ2** |
| O2 | Sprint / iteration containers | Later |
| O3 | Cross-project / personal global backlog | **FQ1** |
| O4 | Kanban column order follows backlog rank | **FQ7** — later |
| O5 | Backlog filters (phase / status / assignee) | **FQ6** — none in v1 |
| O6 | CSV import/export of rank | Later |
| O7 | Query-language `ORDER BY rank` | Later |

## Decisions (from FQs)

| Topic | Decision |
|-------|----------|
| Scope | Project-only |
| Ordering field | New **backlog rank**; Priority unchanged |
| Membership | Non-deleted; exclude **DONE** finish statuses (`finished_at` set). Soft-deleted excluded. Canceled (no finish date) remain listed |
| Reorder roles | **PROJECT_MANAGER** and **admin** only; other members view |
| New ticket rank | Append at end (`max(rank)+1` in project) |
| Filters | None in v1 |
| Kanban sort | Independent for now |
| UI label | **Backlog** |
| Projetos menu | Still opens **Kanban** |
| List size | Paginated (default page size 20; infinite scroll) |

## Ubiquitous language

| Term | Meaning |
|------|---------|
| **Backlog** | Project-scoped ordered list of non-deleted, non-done tickets for planning “what’s next.” |
| **Backlog rank** | Relative position of a ticket in the project (`tb_tickets.backlog_rank`; lower = higher in the list). Distinct from **Priority**. |
| **Reorder (backlog)** | Change a ticket’s backlog rank relative to peers (PM/admin). |

## Wireframe

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-11 |

### Screen: `/project/:projectId/backlog`

| Region | Elements | Notes |
|--------|----------|-------|
| Header | Project name + **Backlog** | PT-BR label **Backlog** (**FQ8**) |
| Toolbar | Link to **Kanban** / project hub | No filter controls in v1 (**FQ6**) |
| List | Ranked rows: drag handle (PM/admin only), identifier, title, status chip, priority chip, assignee | Order = backlog rank |
| Drag | Reorder within list; persist on drop | Hidden / disabled for non-PM (**FQ4**) |
| Pagination | Infinite scroll loads next page | **FQ10**; same pattern as notifications |
| Empty | Message when no backlog tickets | |

```
┌──────────────────────────────────────────────────────────────────┐
│  Backlog — Project X                              [Kanban]       │
├──────────────────────────────────────────────────────────────────┤
│  ≡  ISS-12  Auth API tokens        TODO        HIGH    Alice    │
│  ≡  ISS-08  Fix login redirect     IN PROGRESS MEDIUM  Bob      │
│  ≡  ISS-03  CSV import polish      TODO        LOW     —        │
│  ≡  ISS-15  Dashboard widgets      REVIEW      MEDIUM  Alice    │
│  … (scroll for more)                                             │
└──────────────────────────────────────────────────────────────────┘
```

### Navigation (shell / hub)

| Surface | Change |
|---------|--------|
| Project hub `/projects/:projectId` | Add **Backlog** alongside Kanban / Painel / … |
| Header **Projetos** menu | Unchanged — still opens **Kanban** (**FQ9**) |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `ticket` (rank, list, reorder); project hub / nav UI only |
| Packages / files | `ticket.backlog.list`, `ticket.backlog.reorder`, `BacklogService`; Angular `backlog` component; create/import assign initial rank |
| API | `GET /projects/{id}/backlog?page&size`; `POST /projects/{id}/backlog/reorder` |
| UI | `/project/:projectId/backlog`; hub link; drag for PM/admin |
| Schema / seed | `tb_tickets.backlog_rank`; index; `dev-import.sql` ranks |
| Tests | List page order/membership; reorder auth + persistence; create appends end; Angular scroll + drag |
| Docs | domain-spec; feature-catalog; README § Views |

### Risks

- Concurrent PM reorders — server applies relative placement transactionally (**AQ2**).
- Gaps in ranks when done tickets retain ranks — list still `ORDER BY backlog_rank`; acceptable.
- Rank ≠ Priority confusion — label view **Backlog**; keep Priority chip as urgency only.

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Project-scoped only vs also global? | answered | **Project-scoped only** (default) |
| FQ2 | New backlog rank vs Priority enum? | answered | **New backlog rank**; Priority unchanged (default/recommended) |
| FQ3 | Which tickets appear by default? | answered | **B — exclude DONE** finish statuses; also exclude soft-deleted (default for backlog planning) |
| FQ4 | Who may reorder? | answered | **PM and admin only**; other members read-only |
| FQ5 | Where do new tickets land? | answered | **A — end** of backlog (default) |
| FQ6 | Filters in v1? | answered | **A — none** (default) |
| FQ7 | Kanban column order follows rank? | answered | **C — later** (default / prefer defer) |
| FQ8 | PT-BR UI label? | answered | **A — Backlog** (default) |
| FQ9 | Projetos menu opens? | answered | **Kanban** (current / default) |
| FQ10 | List size? | answered | **B — paginate** (infinite scroll) |

## Architecture

### Bounded contexts

| Context | Role |
|---------|------|
| Ticket management | Owns `backlog_rank`; list backlog; reorder; assign rank on create/import |
| Project administration | Hub link only (no new project domain rules) |
| Identity | Roles: view = authenticated member/admin; reorder = `PROJECT_MANAGER` + `ADMIN` |

### Packages / layers

| Operation | Endpoint | Service | Repository |
|-----------|----------|---------|------------|
| List backlog page | `ticket.backlog.list.ListProjectBacklogEndpoint` | `BacklogService.list(projectId, page, size, username)` | `TicketRepository` page query |
| Reorder | `ticket.backlog.reorder.ReorderProjectBacklogEndpoint` | `BacklogService.reorder(...)` | `TicketRepository` update ranks |
| Create / import | existing create/import services | Call `BacklogService.nextRank(projectId)` (or equivalent) | persist `backlog_rank` |

Dependency: Endpoint → `BacklogService` → `TicketRepository`. No cross-context repository access.

### Schema

Amend `V1.0.0__Database_Creation.sql` on `tb_tickets`:

```sql
backlog_rank INT NOT NULL DEFAULT 0
```

Index (non-unique; gaps allowed):

```sql
CREATE INDEX idx_tickets_project_backlog_rank
    ON tb_tickets (project_id, backlog_rank)
    WHERE deleted = false;
```

Existing rows / `dev-import.sql`: assign dense ranks per project (`ROW_NUMBER` by `id` or `created_at`). Done tickets keep a rank but are omitted from the backlog query.

### API surface

| Method | Path | Auth | Body / query | Response |
|--------|------|------|--------------|----------|
| `GET` | `/projects/{projectId}/backlog` | `USER`, `ADMIN`, `PROJECT_MANAGER` (must access project) | `page` (default 0), `size` (default 20, max 100) | `BacklogPageResponse` |
| `POST` | `/projects/{projectId}/backlog/reorder` | `ADMIN`, `PROJECT_MANAGER` | `ReorderBacklogRequest` | `BacklogTicketResponse` (moved ticket) or 204 |

**Records:**

- `BacklogTicketResponse` — id, identifier, title, status, priority, assignee summary, backlogRank, … (static `load`)
- `BacklogPageResponse` — items, total, page, size, hasMore (same shape as notifications)
- `ReorderBacklogRequest` — `ticketId`, `beforeTicketId` (`Long`, null = move to end of backlog among eligible tickets)

**List query rules:** `project_id` match; `deleted = false`; not in DONE finish status (`finished_at IS NULL`); `ORDER BY backlog_rank ASC, id ASC`; offset pagination.

**Reorder rules:** caller must be admin or project-manager; ticket must belong to project and be backlog-eligible; `beforeTicketId` if set must be same project and backlog-eligible; server shifts ranks in a transaction; log `FIELD_CHANGED` / `backlogRank` via `TicketHistoryService`.

OpenAPI `@Tag(name = "Backlog")`; operationIds `listProjectBacklog`, `reorderProjectBacklog`.

### Cross-context

- No CDI event required for v1 (history covers audit; no notification on reorder).
- Kanban unchanged (**FQ7**).

### Frontend

| Piece | Detail |
|-------|--------|
| Route | `/project/:projectId/backlog` |
| Component | `components/backlog/` — list, infinite scroll, CDK drag for PM/admin |
| Service | facade → generated API client after codegen |
| Hub | **Backlog** link on project hub |
| Roles | Hide drag handle when user lacks PM/admin |

### Tests

| Area | Coverage |
|------|----------|
| `ListProjectBacklogEndpointTest` | Order by rank; excludes deleted/done; pagination; membership access |
| `ReorderProjectBacklogEndpointTest` | PM/admin success; user 403; relative before/end; persistence |
| Create/import tests | New ticket gets max+1 rank |
| `ArchitectureTest` | Request/Response naming |
| Angular | `backlog.component.spec.ts` — load page, scroll, drag calls reorder |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Rank storage strategy? | answered | **Integer `backlog_rank`** with gaps allowed; reorder shifts affected peers in a transaction |
| AQ2 | Reorder API shape? | answered | **Relative** — `beforeTicketId` optional (`null` = end) |
| AQ3 | Default / max page size? | answered | **Default 20, max 100** (align with notifications) |
| AQ4 | History on reorder? | answered | **Yes** — `FIELD_CHANGED` field `backlogRank` |

## Changelog

### Add project ticket backlog with reorder — 2026-07-11

**Version:** 1  
**Status:** done

**Description:** Project backlog view: paginated list by backlog rank; PM/admin reorder; members read-only; excludes deleted and DONE tickets; new tickets append at end.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Kanban board | Hub sibling only; column order unchanged (**FQ7**) |
| Create ticket / Import | Assign initial `backlog_rank` at end (**FQ5**) |
| Ticket management | History may show `backlogRank` changes |
| Ticket search | No change |
| Project hub / navigation | New **Backlog** entry; Projetos → Kanban unchanged |
| Priority field | Unchanged |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Project members can open backlog and see non-done tickets in rank order | S1, S2, FQ3 | ☑ |
| FC2 | List is paginated (infinite scroll); default page size 20 | FQ10, AQ3 | ☑ |
| FC3 | PM/admin can reorder; order persists; non-PM cannot reorder | S3, FQ4 | ☑ |
| FC4 | Soft-deleted and DONE tickets excluded from backlog | FQ3 | ☑ |
| FC5 | New tickets (create/import) get rank at end of project backlog | FQ5 | ☑ |
| FC6 | UI matches Wireframe (Backlog label, list, drag for PM, Kanban link, no filters) | Wireframe, FQ6, FQ8 | ☑ |
| FC7 | Priority enum unchanged by reorder | FQ2 | ☑ |
| FC8 | Projetos menu still opens Kanban | FQ9 | ☑ |
| FC9 | `domain-specification.md` — Backlog / Backlog rank + invariants | Impact / Docs | ☑ |
| FC10 | `feature-catalog.md` — backlog route + steps | Impact / Docs | ☑ |
| FC11 | README § Views — backlog bullet | Impact / Docs | ☑ |
| FC12 | Reorder writes ticket history `backlogRank` | AQ4 | ☑ |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | Schema: `backlog_rank` on `tb_tickets` + index; seed ranks in `dev-import.sql` | ☑ |
| T2 | Entity + `TicketRepository` backlog page query and rank helpers | ☑ |
| T3 | `BacklogService` — list (membership, filters, pagination) + reorder + `nextRank` | ☑ |
| T4 | `ListProjectBacklogEndpoint` + `BacklogPageResponse` / `BacklogTicketResponse` + tests | ☑ |
| T5 | `ReorderProjectBacklogEndpoint` + `ReorderBacklogRequest` + auth/history tests | ☑ |
| T6 | Create ticket + CSV import assign end-of-backlog rank + tests | ☑ |
| T7 | Angular backlog route/component (infinite scroll + drag) + service + specs | ☑ |
| T8 | Project hub **Backlog** link; feature-catalog + README + domain-spec sync | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `ListProjectBacklogEndpointTest` — order, exclude done/deleted, pages | T2–T4 | ☑ |
| TC2 | `ReorderProjectBacklogEndpointTest` — PM/admin ok, user forbidden, before/end | T3, T5 | ☑ |
| TC3 | Create/import tests assert `backlog_rank` at end | T6 | ☑ |
| TC4 | History assertion on reorder (`backlogRank`) | T5 | ☑ |
| TC5 | `backlog.component.spec.ts` — load, scroll, reorder call | T7 | ☑ |
| TC6 | `ArchitectureTest` still green for new Request/Response | T4, T5 | ☑ |

**Development approval:** approved 2026-07-11 — tasks: T1, T2, T3, T4, T5, T6, T7, T8

**Implementation notes:**

- Backend: `backlog_rank` + index; `ticket.backlog` list/reorder endpoints; create/import/create-child assign `nextRank`; history `FIELD_CHANGED` / `backlogRank`.
- Frontend: `/project/:projectId/backlog` infinite scroll + CDK drag for PM/admin; hub + Kanban **Backlog** links.
- Docs: domain-spec invariants 72–75; feature-catalog; README; ARCHITECTURE §13; product backlog row done.


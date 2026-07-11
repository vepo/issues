# Ticket links, epics & subtasks

**Feature version:** 1  
**Status:** done  
**Requested:** 2026-07-11

## Summary

Add **typed relationships between tickets** so teams can decompose and connect work without overloading **phases** (time-boxed planning) or **categories** (classification labels).

v1 covers:

1. **Peer links** — `BLOCKS`, `RELATES_TO`, `DUPLICATES`, `DERIVED_FROM`, `REMAINING_WORK_OF` (**FQ3**).
2. **Hierarchy** — `CHILD_OF` under an **Epic** only; single parent; depth 1 (**FQ5**, **FQ14**).
3. **Ticket type** — `EPIC` / `STORY` / `TASK` (**FQ12**); Epic is the feature umbrella (**FQ4**).
4. **Ticket detail UI** — **Vínculos**, **Subtarefas** with progress + **Nova subtarefa** (**FQ8**, **FQ9**, **FQ11**).
5. **History** on link add/remove.
6. **Cross-project** links allowed (**FQ6**, **FQ15**).

**Distinct from:** **Phase** (time box), **Version** (release), **Category** (Feature/Bug label).

**Out of scope for v1:** admin-configurable link types (**FQ2**); query-language / Kanban epic filters (**FQ10**); CSV import of links; auto-close parent when children finish; notifications on link create.

## Scope

| ID | In scope | Notes |
|----|----------|-------|
| S1 | Typed ticket↔ticket links | Fixed enum (**FQ2**, **FQ3**) |
| S2 | Hierarchy Epic → children | `CHILD_OF`; Epic parent only (**FQ4**, **FQ5**, **FQ14**) |
| S3 | Ticket detail: Vínculos / Subtarefas | Wireframe |
| S4 | Create/remove links | Cross-project (**FQ6**); view both ends (**FQ15**) |
| S5 | History on link add/remove | `LINK_ADDED` / `LINK_REMOVED` |
| S6 | Soft-delete | Hide deleted ends; keep edges; restore restores navigation |
| S7 | Nova subtarefa | Create child under Epic (**FQ8**) |
| S8 | Progress rollup | `n/m concluídas` on Epic (**FQ9**) |
| S9 | Ticket type field | `EPIC` / `STORY` / `TASK` (**FQ12**) |

| ID | Out of scope for v1 | Notes |
|----|---------------------|-------|
| O1 | Configurable link-type admin UI | **FQ2** fixed |
| O2 | Depth-2 story→subtask | Reconciled with **FQ14** — depth 1 only |
| O3 | Query language predicates | **FQ10** defer |
| O4 | Kanban epic filter/swimlane | **FQ10** defer |
| O5 | CSV import/export of links | Later |
| O6 | Auto-close / hard-block parent DONE | **FQ7** warn UI, allow server |
| O7 | Webhooks / email on link events | Later |

## Ubiquitous language

| Term | Meaning |
|------|---------|
| **Ticket type** | `EPIC`, `STORY`, or `TASK` — designation of the ticket’s role in planning (**FQ12**). |
| **Epic** | Feature-level ticket (`type=EPIC`) that groups many children; delivery work is on children; may span many **phases**/sprints. Distinct from **Phase** and **Category**. |
| **Ticket link** | Directed association between two tickets with a **link type**. |
| **Link type** | Fixed enum: `BLOCKS`, `RELATES_TO`, `DUPLICATES`, `DERIVED_FROM`, `REMAINING_WORK_OF`, `CHILD_OF`. |
| **Parent ticket** | Epic that is the target of one or more `CHILD_OF` edges. |
| **Child ticket** / **Subtask** | Ticket with a `CHILD_OF` link to an Epic (UI **Subtarefas**). |
| **Blocks** / **Blocked by** | Inverse display of `BLOCKS`. |
| **Duplicates** / **Duplicated by** | Inverse display of `DUPLICATES`. |
| **Derived from** / **Origem de** | Inverse display of `DERIVED_FROM`. |
| **Remaining work of** / **Tem trabalho restante** | Inverse display of `REMAINING_WORK_OF`. |
| **Relates to** | Symmetric-style peer link (`RELATES_TO`). |

## Wireframe

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-11 |

### Screen: `/ticket/:ticketIdentifier`

| Region | Elements | Notes |
|--------|----------|-------|
| Header | Identifier, title, **type** badge (Épico/História/Tarefa), status, assignee | Type editable on create/edit |
| Fields | Built-ins + custom fields | Epic may have optional **phase** (**FQ13**) |
| **Vínculos** | Grouped by PT-BR type; row: label, identifier, project prefix, title, status; **Remover** | Peer + hierarchy; cross-project |
| **Subtarefas** | Children of Epic only; `n/m concluídas`; **Nova subtarefa** | Hidden if not Epic |
| **Add link** | Type ▾ + search (all viewable projects) + **Vincular** | **FQ6**, **FQ15** |
| Atividade | Link added/removed history | |

```
┌──────────────────────────────────────────────────────────────┐
│  ISS-42  [Épico] Implement ticket links   [status] [assignee]│
├───────────────────────────────┬──────────────────────────────┤
│  Fields / description         │  Comentários                 │
├───────────────────────────────┴──────────────────────────────┤
│  Vínculos                                                    │
│  ┌ Bloqueia ──────────────────────────────────────────────┐  │
│  │ AUTH-50  Auth token API     IN_PROGRESS        [×]      │  │
│  └────────────────────────────────────────────────────────┘  │
│  [ Tipo ▾ ] [ Buscar ticket…          ] [ Vincular ]         │
├──────────────────────────────────────────────────────────────┤
│  Subtarefas                                    2/4 concluídas│
│  │ ISS-43  Schema + entity     DONE                        │  │
│  │ ISS-44  Link API            IN_PROGRESS                 │  │
│  [ Nova subtarefa ]                                          │
├──────────────────────────────────────────────────────────────┤
│  Atividade (vínculo adicionado / removido)                   │
└──────────────────────────────────────────────────────────────┘
```

### Dialog: Nova subtarefa (**FQ8**)

| Region | Elements | Notes |
|--------|----------|-------|
| Form | Title *; optional description; inherits Epic’s project; type default `TASK`; parent fixed | |
| Actions | **Criar** / **Cancelar** | Creates ticket + `CHILD_OF` → Epic |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | Ticket management (`ticket`, `ticket.link`, `ticket.history`) |
| Packages / files | `TicketType` enum; `TicketLink` entity; `ticket.link.*` endpoints; `TicketLinkService`; history actions; Angular ticket-view + create/edit type |
| API | Links CRUD under ticket; create-child; expand response includes links + children progress; ticket type on create/update |
| UI | Vínculos / Subtarefas; type on forms; cross-project search; warn on Epic DONE with open children |
| Schema / seed | `ticket_type` on `tb_tickets`; `tb_ticket_links`; `dev-import.sql` samples |
| Tests | Link CRUD, cross-project, Epic-only parent, cycles, depth, soft-delete, history; Angular specs |
| Docs | domain-spec, feature-catalog, README, ARCHITECTURE, gallery if needed |

### Risks

- Epic ≠ Phase copy discipline.
- Cross-project auth/search (**FQ15**).
- Cycles on `CHILD_OF` / peer graphs.
- Soft-deleted link targets.

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | One doc vs split hierarchy? | answered | **One** capability (`ticket-links`) |
| FQ2 | Fixed vs configurable types? | answered | **Fixed enum** for v1 |
| FQ3 | Link-type set? | answered | `BLOCKS`, `RELATES_TO`, `DUPLICATES`, `DERIVED_FROM`, `REMAINING_WORK_OF`, `CHILD_OF` (+ inverse UI labels) |
| FQ4 | What is an Epic? | answered | Feature-level ticket grouping many children; work on children; spans many phases/sprints; ≠ Phase/Category |
| FQ5 | Depth / parents? | answered | **Single parent**; **depth 1** (Epic → children). Depth 2 deferred — reconciled with **FQ14** (only Epics are parents) |
| FQ6 | Cross-project? | answered | **Allowed** |
| FQ7 | Parent DONE with open children? | answered | **Warn in UI**, **allow** on server |
| FQ8 | Nova subtarefa in v1? | answered | **Yes** |
| FQ9 | Progress `n/m`? | answered | **Yes** |
| FQ10 | Query / Kanban epic? | answered | **Defer** |
| FQ11 | PT-BR labels? | answered | **Vínculos** / **Subtarefas**; Bloqueia / Bloqueado por; Relacionado a; Duplicata de / Duplicado por; Derivado de / Origem de; Trabalho restante de / Tem trabalho restante; Filho de / Pai de (Épico); types Épico / História / Tarefa |
| FQ12 | Epic designation? | answered | **(A)** ticket **type** field: `EPIC` / `STORY` / `TASK` |
| FQ13 | Epic × Phase? | answered | Epic **may** have optional phase; children keep their own |
| FQ14 | Who may be hierarchy parent? | answered | **Only Epics** |
| FQ15 | Cross-project permissions? | answered | User must **view** both tickets; create/delete link requires ability to update the **source** ticket’s project context (authenticated member/admin as for ticket update) |

**Impact review (batch defaults — 2026-07-11):** All blocking FQs answered. FQ5 reconciled to depth 1 under FQ14. Ticket type column in scope (S9). Cross-project + Epic type expand create/edit forms and expanded ticket payload.

## Architecture

| Area | Design |
|------|--------|
| Bounded contexts | Ticket management only |
| Packages / layers | `ticket.link` Endpoint → `TicketLinkService` → `TicketLinkRepository`; type on `Ticket` / `TicketService`; history via `TicketHistoryService` |
| API | See API surface |
| Schema | `tb_tickets.ticket_type`; `tb_ticket_links` |
| Cross-context | Project access for auth only; no Phase FK on links |
| Frontend | Ticket detail sections; type on create/edit; link search via existing ticket search |
| Tests | Endpoint + Angular as in Test coverage |

### Packages / layers

| Layer | Type | Responsibility |
|-------|------|----------------|
| Enum | `ticket.TicketType` | `EPIC`, `STORY`, `TASK` (default `TASK`) |
| Enum | `ticket.link.TicketLinkType` | Fixed link kinds |
| Entity | `ticket.link.TicketLink` | source, target, type, createdAt, createdBy |
| Repository | `TicketLinkRepository` | CRUD, find by ticket either end, cycle helpers |
| Service | `TicketLinkService` | validate type/parent/cycle/auth; create-child; progress |
| Endpoints | `ticket.link.list/create/delete`, `ticket.link.createchild` | One HTTP method each |
| History | `TicketHistoryAction.LINK_ADDED`, `LINK_REMOVED` | Structured |

### Edge semantics (**AQ2**)

- Store **one directed** edge: for `CHILD_OF`, **source = child**, **target = parent (Epic)**.
- For `BLOCKS`, source blocks target; API returns inverse label when viewing from the other end.
- `RELATES_TO` stored once (canonical order e.g. lower id → higher id) or directed with same label both ways — pick canonical lower→higher id.

### Hierarchy rules

- `CHILD_OF` target must have `type=EPIC`.
- Child has at most one `CHILD_OF` (single parent).
- Depth 1 only — child of an Epic cannot itself be parent via `CHILD_OF`.
- Cycles rejected.
- Cross-project `CHILD_OF` allowed (**FQ6**) — child may live in another project than the Epic.

### Parent finish (**FQ7**)

- Server: allow Epic move to DONE with open children.
- UI: warning dialog when saving/moving Epic to done/finish while `doneCount < totalChildren`.

### Soft-delete (**S6**)

- List endpoints omit or mark links whose other end is soft-deleted; do not 500.
- Edges remain in DB; restore restores navigable link.

### API surface

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| `GET` | `/api/tickets/{id}/links` | view ticket | All links from this ticket’s perspective (outbound + inbound with display labels) |
| `POST` | `/api/tickets/{id}/links` | update source ticket | Body: `CreateTicketLinkRequest(targetTicketId, linkType)` — `{id}` is source |
| `DELETE` | `/api/tickets/{id}/links/{linkId}` | update source or either end TBD — prefer user who can update ticket that “owns” the edge from UI context | |
| `POST` | `/api/tickets/{id}/children` | update Epic | Body: `CreateChildTicketRequest(title, description?)`; `{id}` must be Epic |
| expand | existing GET expanded | — | Include `ticketType`, `links`, `childrenSummary { total, done }` |

Request/Response records: `CreateTicketLinkRequest`, `TicketLinkResponse`, `CreateChildTicketRequest`, `ChildrenSummaryResponse` (or embed).

Roles: same authenticated roles as ticket read/update (`USER` / `PROJECT_MANAGER` / `ADMIN`) plus project viewability checks (**FQ15**).

### Schema

```sql
-- on tb_tickets
ticket_type VARCHAR(16) NOT NULL DEFAULT 'TASK'
  CHECK (ticket_type IN ('EPIC', 'STORY', 'TASK'));

CREATE TABLE tb_ticket_links (
  id BIGSERIAL PRIMARY KEY,
  source_ticket_id BIGINT NOT NULL REFERENCES tb_tickets,
  target_ticket_id BIGINT NOT NULL REFERENCES tb_tickets,
  link_type VARCHAR(32) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  created_by BIGINT NOT NULL REFERENCES tb_users,
  CONSTRAINT uk_ticket_link UNIQUE (source_ticket_id, target_ticket_id, link_type),
  CONSTRAINT ck_ticket_link_not_self CHECK (source_ticket_id <> target_ticket_id)
);
CREATE INDEX idx_ticket_links_source ON tb_ticket_links (source_ticket_id);
CREATE INDEX idx_ticket_links_target ON tb_ticket_links (target_ticket_id);
```

Pre-production: amend `V1.0.0__Database_Creation.sql` only.

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Single links table vs parent_ticket_id? | answered | **Single** `tb_ticket_links` including `CHILD_OF` |
| AQ2 | Directed vs both directions? | answered | **One directed** edge; inverse labels in API/UI |
| AQ3 | History actions? | answered | **`LINK_ADDED` / `LINK_REMOVED`** |
| AQ4 | Links on expanded GET? | answered | **Yes** — embed links + children summary; keep list endpoint for refresh |
| AQ5 | Package? | answered | **`ticket.link`** |
| AQ6 | Default ticket type for existing rows? | answered | **`TASK`** |
| AQ7 | Create-child default type? | answered | **`TASK`** |

## Changelog

### Ticket links, epics & subtasks — 2026-07-11

**Version:** 1  
**Status:** done

**Description:** Typed ticket links (peer + Epic hierarchy), ticket type (`EPIC`/`STORY`/`TASK`), ticket-detail Vínculos/Subtarefas UI, create-child, progress rollup, cross-project links, history.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Ticket management | Detail + type field; history actions |
| Create ticket | Ticket type selector; no parent on global create (use Nova subtarefa) |
| Kanban / query | Unchanged (**FQ10**) |
| Phase management | Epic may have phase; ≠ Epic concept |
| Categories | Unchanged |
| Agentic / git | Future consumers of links; not this changelog |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Create/remove typed links on ticket detail | S1, S3, S4 | ☑ |
| FC2 | Link types match **FQ3** | FQ3 | ☑ |
| FC3 | Epic type + Epic-only parent + depth 1 | FQ4, FQ5, FQ12, FQ14 | ☑ |
| FC4 | Cross-project links + **FQ15** auth | FQ6, FQ15 | ☑ |
| FC5 | Warn UI on Epic DONE with open children; server allows | FQ7 | ☑ |
| FC6 | Nova subtarefa on Epic | FQ8 | ☑ |
| FC7 | Progress `n/m` on Epic | FQ9 | ☑ |
| FC8 | Query/Kanban epic deferred | FQ10 | ☑ |
| FC9 | Soft-deleted ends handled | S6 | ☑ |
| FC10 | LINK_ADDED / LINK_REMOVED history | S5, AQ3 | ☑ |
| FC11 | UI matches Wireframe | Wireframe | ☑ |
| FC12 | PT-BR labels per **FQ11** | FQ11 | ☑ |
| FC13 | Epic ≠ Phase in UI/docs | FQ4, FQ13 | ☑ |
| FC14 | domain-spec updated | Docs | ☑ |
| FC15 | feature-catalog updated | Docs | ☑ |
| FC16 | README Features bullet | Docs | ☑ |
| FC17 | Cycles / invalid parents rejected | Architecture | ☑ |
| FC18 | Ticket type on create/edit/expand | FQ12, S9 | ☑ |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | Schema: `ticket_type` on `tb_tickets` + `tb_ticket_links`; entity `TicketType`, `TicketLink` | ☑ |
| T2 | `TicketLinkRepository` + cycle / single-parent / Epic-parent queries | ☑ |
| T3 | `TicketLinkService` — create/delete/list; auth **FQ15**; history LINK_* | ☑ |
| T4 | Endpoints: list / create / delete links | ☑ |
| T5 | Endpoint: create child under Epic | ☑ |
| T6 | Ticket create/update/expand: `ticketType`; embed links + children summary | ☑ |
| T7 | Endpoint tests: peer link, cross-project, Epic parent, reject non-Epic parent, cycle, soft-delete, history | ☑ |
| T8 | Angular: type on create/edit; Vínculos / Subtarefas / add link / Nova subtarefa / progress / DONE warn | ☑ |
| T9 | Angular specs for links UI + type | ☑ |
| T10 | `dev-import.sql` sample Epic + children + peer link; docs (domain-spec, catalog, README, ARCHITECTURE) | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | Create/list/delete peer link | T3, T4, T7 | ☑ |
| TC2 | Cross-project link succeeds when both viewable | T3, T7 | ☑ |
| TC3 | CHILD_OF requires Epic parent; rejects second parent; rejects cycle | T2, T3, T7 | ☑ |
| TC4 | Create-child creates TASK + CHILD_OF | T5, T7 | ☑ |
| TC5 | History LINK_ADDED / LINK_REMOVED | T3, T7 | ☑ |
| TC6 | Expanded response includes type, links, childrenSummary | T6, T7 | ☑ |
| TC7 | Angular: render links; add/remove; subtarefas progress | T8, T9 | ☑ |

**Development approval:** approved 2026-07-11 — tasks: T1, T2, T3, T4, T5, T6, T7, T8, T9, T10

**Implementation notes:**

- Backend T1–T7: schema, `ticket.link` package, endpoints, ticket type on create/update/expand, `TicketLinkEndpointTest` green.
- Frontend T8–T9: `TicketService` link facades; **Tipo** on create/edit; ticket detail **Vínculos** / **Subtarefas**; Nova subtarefa dialog; Epic DONE warn; history PT-BR labels; Angular specs.
- T10: `dev-import.sql` ISS-018 Epic + ISS-019/020 children + BLOCKS/RELATES peer samples; domain-spec, feature-catalog, README, ARCHITECTURE updated.

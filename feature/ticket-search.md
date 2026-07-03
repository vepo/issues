# Ticket search

**Feature version:** 2  
**Status:** tasks-ready  
**Requested:** retrospective baseline (documented 2026-07-03); query language extension [GitHub issue #4](https://github.com/vepo/issues/issues/4) (2026-07-03)

## Summary

Global ticket search for authenticated users. **Version 1** (done): simple term search on title and description with optional status filter. **Version 2** (this request): **Issues query language** (syntax **inspired by** [Jira JQL](https://support.atlassian.com/jira-service-management-cloud/docs/use-advanced-search-with-jira-query-language-jql/) — **not compatible**); searchable **ticket fields including comments**; **saved queries** with shareable links; **owner-only edit** and delete; non-owners **clone** only when opening another user's saved query; optional **show at home** per query on the edit page; **list all saved queries** page.

Header simple term search is **retained** (**FQ9**).

## Wireframe

**Guide:** layout reference for UI implementation — update when search UX or **FQ*n*** / **AQ*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-03 |

### Screen: `/search` — simple search (v1, retained)

| Region | Elements |
|--------|----------|
| Search bar | Term input; link **Busca avançada** → advanced mode |
| Filters | Status chips (optional; retained from v1) |
| Results | Cards/table: identifier (link), title, project, status |

```
┌─────────────────────────────────────────────┐
│  Resultados da busca                        │
│  [ termo………………… ]  [ Busca avançada ]       │
│  [ status chips ]                           │
├─────────────────────────────────────────────┤
│  #PROJ-1  Title…  Project · Status        │
└─────────────────────────────────────────────┘
```

### Screen: `/search/advanced` — query language editor (ad-hoc)

| Region | Elements |
|--------|----------|
| Query input | Multiline **query language** text; **Executar** |
| Actions | **Salvar consulta** → navigates to new saved-query edit; field reference helper (collapsible) |
| Results | Same result layout as `/search` |
| Errors | Inline parse/validation message |

```
┌─────────────────────────────────────────────┐
│  Busca avançada                             │
│  ┌─────────────────────────────────────┐    │
│  │ project = "Alpha" AND status = TODO │    │
│  │ AND assignee = currentUser()        │    │
│  └─────────────────────────────────────┘    │
│  [ Executar ]  [ Salvar consulta ]          │
├─────────────────────────────────────────────┤
│  (results)                                  │
└─────────────────────────────────────────────┘
```

### Screen: `/search/queries` — list all saved queries (required)

| Region | Elements |
|--------|----------|
| Header | **Minhas consultas**; **Nova consulta** → edit page |
| Table | Name, query preview, **Exibir na home** (yes/no), updated; actions: **Abrir**, **Editar**, **Excluir** |
| Empty | Link to **Busca avançada** |

```
┌─────────────────────────────────────────────┐
│  Minhas consultas              [ Nova ]     │
├─────────────────────────────────────────────┤
│  Name │ Preview │ Home │ Updated │ Actions │
│  …    │ …       │ ☑    │ …       │ …       │
└─────────────────────────────────────────────┘
```

### Screen: `/search/queries/new` and `/search/queries/:id/edit` — query edit

| Region | Elements |
|--------|----------|
| Form | **Nome** (required); query text (required); **Exibir na página inicial** checkbox (**FQ10**) |
| Actions | **Salvar**; **Excluir** (existing queries, owner only); **Cancelar** |
| Non-owner | Not reachable for edit — shared view uses **Clonar** instead (**FQ7**) |

```
┌─────────────────────────────────────────────┐
│  Editar consulta                            │
│  Nome [………………………………………]                     │
│  ☐ Exibir na página inicial                 │
│  ┌─────────────────────────────────────┐    │
│  │ query text…                         │    │
│  └─────────────────────────────────────┘    │
│  [ Salvar ]  [ Excluir ]  [ Cancelar ]      │
└─────────────────────────────────────────────┘
```

### Screen: `/search/q/:slug` — shared saved query (read)

| Region | Elements |
|--------|----------|
| Header | Query **name**; owner display name; read-only query text (collapsible) |
| Actions (owner) | **Editar** → `/search/queries/:id/edit` |
| Actions (non-owner) | **Clonar** → creates owned copy, opens edit (**FQ7**) |
| Share | **Copiar link** (stable URL `/search/q/:slug`) |
| Results | Ticket list for stored query |

```
┌─────────────────────────────────────────────┐
│  Minhas tarefas abertas          by Alice   │
│  project = "Alpha" AND assignee = me()      │
│  [ Copiar link ]  [ Editar | Clonar ]       │
├─────────────────────────────────────────────┤
│  (results)                                  │
└─────────────────────────────────────────────┘
```

### Screen: `/` (home) — saved query sections (cross-feature)

One section per owned saved query with **show at home** enabled (**FQ8**, **FQ10**):

| Region | Elements |
|--------|----------|
| Section title | Saved query **name** (link → `/search/q/:slug`) |
| Table | Identifier, title, project, status, priority, updated |
| Empty | Section omitted when query returns no rows (or empty-state per gallery) |

```
┌─────────────────────────────────────────────────────────┐
│  … Tickets atuais │ Tickets atribuídos …                 │
├─────────────────────────────────────────────────────────┤
│  Minhas tarefas abertas (saved query)                   │
│  Identifier │ Title │ Project │ Status │ Updated        │
└─────────────────────────────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `ticket.search` (ANTLR query language + saved queries); `home` (sections); read-only cross-context: `project`, `workflow`, `user`, `categories`, `phase`, `ticket.comments` |
| Packages / files | See **Architecture** — ANTLR grammar, parser visitor, executor, saved-query CRUD, home extension |
| API | `POST /tickets/search/query`; saved-query CRUD + clone; `GET /home/saved-queries` (single response) |
| UI | `/search/advanced`, `/search/queries`, `/search/queries/:id/edit`, `/search/q/:slug`; home sections; header search unchanged |
| Schema / seed | `tb_saved_queries`; PostgreSQL `tsvector` + GIN (**AQ1**); dev seed sample queries |
| Build | **ANTLR 4** Maven plugin + runtime dependency in `pom.xml` |
| Tests | ANTLR/parser unit tests; query + saved-query endpoint tests; home section tests; Angular specs |
| Docs | domain-spec, feature-catalog, README, ARCHITECTURE §13; cross-note in [home-screen](home-screen.md) |

### Risks

- **ANTLR build** — grammar changes require Maven generate-sources in CI; keep grammar tests green.
- **Query language scope** — all ticket fields + comments increases parser and test surface; ship documented field/operator reference.
- **Performance** — comment predicates + home batch execution in one API call (**AQ3**).

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Should search support filters (project, status, assignee)? | answered | **Superseded by v2** — query language covers structured filters |
| FQ2 | Is dedicated full-text indexing required at scale? | answered | **Yes** — PostgreSQL full-text indexes for text search paths |
| FQ3 | JQL compatibility vs Issues-native dialect? | answered | **Not JQL-compatible** — Issues query language, JQL-**inspired** syntax; search by **field** |
| FQ4 | Which fields queryable in v2? | answered | **All** ticket fields **including comments** |
| FQ5 | Result scope? | answered | **Global** for any authenticated user; **project** usable as filter (`project = "X"` or project set with `=`) |
| FQ6 | Share link access? | answered | **Authenticated only** |
| FQ7 | When is clone required? | answered | **Only** when a user opens **another user's** saved query and wants to edit — not for own queries |
| FQ8 | Home layout for saved queries? | answered | **One section per** saved query (with show-at-home enabled) |
| FQ9 | Keep header simple term search? | answered | **Yes** — coexist with advanced query language |
| FQ10 | Show at home mandatory or optional? | answered | **Optional** — checkbox on query **edit** page (**Exibir na página inicial**) |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Full-text indexing strategy for **FQ2**? | answered | PostgreSQL **`tsvector` + GIN`** on `tb_tickets` (title, description) and `tb_comments` (content); structured fields via JPA criteria |
| AQ2 | Ad-hoc query execution HTTP shape? | answered | **`POST /api/tickets/search/query`** with `SearchTicketsByQueryRequest { query }` — plain text query in body |
| AQ3 | Home saved-query sections — API shape? | answered | **`GET /api/home/saved-queries`** — **one API call** returning `{ savedQuery, tickets[] }[]` for all show-at-home owned queries |
| AQ4 | Query language parser implementation? | answered | **ANTLR 4** — grammar in `src/main/antlr4/`; accepts **plain text** query strings (stored and submitted as text) |

**Gate:** all **FQ*n*** and **AQ*n*** resolved — ready for task break.

## Architecture

**Guide:** technical design for v2 — update when **AQ*n*** or footprint-changing **FQ*n*** answers change ([architecture-design.mdc](../.cursor/rules/architecture-design.mdc)).

| Field | Value |
|-------|-------|
| **Last updated** | 2026-07-03 |
| **Status** | complete |

### Bounded contexts

| Context | Role |
|---------|------|
| `ticket.search` | Query language parse/execute; **SavedQuery** aggregate; saved-query HTTP API |
| `ticket` | `TicketRepository` criteria execution; existing simple search unchanged |
| `ticket.comments` | Comment field resolution (`comment ~ "text"`) — read via service/repository in ticket context, not from `home` |
| `home` | New endpoint for saved-query sections (`showAtHome = true`, owner = current user) |
| `project`, `workflow`, `user`, `categories`, `phase` | Field resolution only — injected services/repositories from owning context |

Dependency direction: `home` → `ticket.search` services; `ticket.search` → `ticket` repository layer; no cross-context repository calls from endpoints.

### Packages and layers

```
dev.vepo.issues.ticket.search
├── query/
│   ├── antlr/                         # generated: TicketQueryLexer, TicketQueryParser
│   ├── TicketQuery.g4                 # → src/main/antlr4/TicketQuery.g4
│   ├── TicketQueryLanguageService     # plain text → ANTLR parse → AST visitor → tickets
│   ├── TicketQueryCriteriaBuilder     # AST → JPA Criteria + tsvector predicates
│   └── SearchTicketsByQueryEndpoint   # POST …/tickets/search/query
├── saved/
│   ├── SavedQuery                     # entity; query_text plain TEXT
│   ├── SavedQueryRepository
│   ├── SavedQueryService              # CRUD, clone, ownership rules
│   ├── create/CreateSavedQueryEndpoint
│   ├── list/ListSavedQueriesEndpoint
│   ├── find/FindSavedQueryBySlugEndpoint
│   ├── update/UpdateSavedQueryEndpoint
│   ├── delete/DeleteSavedQueryEndpoint
│   └── clone/CloneSavedQueryEndpoint
└── (existing) search/SearchTicketsEndpoint   # v1 term search — unchanged

dev.vepo.issues.home
└── savedqueries/ListHomeSavedQuerySectionsEndpoint   # GET …/home/saved-queries (single call)
```

**ANTLR build (`pom.xml`):**

- Dependency: `org.antlr:antlr4-runtime`
- Plugin: `antlr4-maven-plugin` — grammar `src/main/antlr4/TicketQuery.g4`; generated sources under `target/generated-sources/antlr4`
- Input/output: query language is **plain text** end-to-end (UI textarea → API body → DB `query_text` → ANTLR parse)

| Operation | Endpoint | Service | Repository |
|-----------|----------|---------|------------|
| Execute ad-hoc query | `SearchTicketsByQueryEndpoint` | `TicketQueryLanguageService` | `TicketRepository` (+ comment join/subquery) |
| CRUD saved query | `*SavedQueryEndpoint` | `SavedQueryService` | `SavedQueryRepository` |
| Clone | `CloneSavedQueryEndpoint` | `SavedQueryService` | `SavedQueryRepository` |
| Home sections | `ListHomeSavedQuerySectionsEndpoint` | `HomeService` (orchestrates) | `SavedQueryRepository` + `TicketQueryLanguageService` |

### Query language (Issues-native)

| Aspect | Decision |
|--------|----------|
| Input | **Plain text** query string — no JSON DSL; multiline text in UI and `TEXT` column |
| Parser | **ANTLR 4** grammar defines the Issues query language (**AQ4**) |
| Compatibility | **Not** Jira JQL — Issues field names and documented operator subset |
| Grammar | `field operator value` clauses; `AND` / `OR`; parentheses; optional `ORDER BY field ASC\|DESC` |
| Operators | `=`, `!=`, `IN (...)`, `NOT IN (...)`, `~` (contains), `IS EMPTY`, `IS NOT EMPTY`; date comparisons `>`, `<`, `>=`, `<=` on date fields |
| Functions | `currentUser()` / `me()` for assignee/author |
| Fields | All ticket fields + `comment` (matches comment body on ticket); see field reference in UI/docs |
| Project filter | `project = "Name"` or multiple projects via `IN` / repeated `OR` (**FQ5**) |
| Exclusions | `deleted = false` enforced server-side always |
| Errors | `400` + message for parse/unknown field/semantic errors |

### API surface

| Method | Path | Request / Response | Roles |
|--------|------|-------------------|-------|
| POST | `/api/tickets/search/query` | `SearchTicketsByQueryRequest { query: string }` → `List<TicketResponse>` | authenticated |
| GET | `/api/tickets/search` | v1 term search — unchanged | authenticated |
| GET | `/api/saved-queries` | → `List<SavedQueryResponse>` (current user's) | authenticated |
| POST | `/api/saved-queries` | `CreateSavedQueryRequest` → `SavedQueryResponse` | authenticated |
| GET | `/api/saved-queries/by-slug/{slug}` | → `SavedQueryResponse` + embedded or separate results call | authenticated (**FQ6**) |
| PUT | `/api/saved-queries/{id}` | `UpdateSavedQueryRequest` (name, query, showAtHome) | owner only |
| DELETE | `/api/saved-queries/{id}` | 204 | owner only |
| POST | `/api/saved-queries/{id}/clone` | → `SavedQueryResponse` (new slug, owner = caller) | authenticated non-owner (**FQ7**) |
| GET | `/api/home/saved-queries` | → `List<HomeSavedQuerySectionResponse>` — **one call**, all show-at-home sections + tickets | authenticated |

### Schema

```sql
CREATE TABLE tb_saved_queries (
    id           BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    slug         VARCHAR(36)  NOT NULL UNIQUE,
    name         VARCHAR(255) NOT NULL,
    query_text   TEXT         NOT NULL,
    show_at_home BOOLEAN      NOT NULL DEFAULT FALSE,
    owner_id     BIGINT       NOT NULL REFERENCES tb_users(id),
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);
CREATE INDEX idx_saved_queries_owner ON tb_saved_queries(owner_id);
```

Full-text indexes (**AQ1**, **FQ2**): PostgreSQL generated `tsvector` columns + GIN indexes on `tb_tickets` (title, description) and `tb_comments` (content) in `V1.0.0__Database_Creation.sql`. Text predicates (`~`, plain term paths) use `@@ to_tsquery` / `plainto_tsquery` via criteria or native fragments in `TicketQueryCriteriaBuilder`.

### Frontend

| Route | Component | Service |
|-------|-----------|---------|
| `/search/advanced` | `advanced-search` | `TicketService` / new `SavedQueryService` |
| `/search/queries` | `saved-query-list` | `SavedQueryService` |
| `/search/queries/new`, `…/:id/edit` | `saved-query-edit` | `SavedQueryService` |
| `/search/q/:slug` | `saved-query-view` | `SavedQueryService` |
| `/` home | extend `home` | `HomeService` — saved-query sections |

Run `npm run generate:api` after new endpoints.

### Tests

| Area | Tests |
|------|-------|
| Parser | `TicketQueryLanguageServiceTest` + grammar examples — operators, all fields, comment clause, ANTLR syntax errors |
| Query search | `SearchTicketsByQueryEndpointTest` |
| Saved query | `SavedQueryEndpointTest` — CRUD, owner-only edit/delete, clone non-owner |
| Home | `HomeSavedQuerySectionsEndpointTest` — showAtHome filter, owner scope |
| Angular | `saved-query-list`, `saved-query-edit`, `saved-query-view`, `advanced-search` specs |
| ArchUnit | `ArchitectureTest` for new Request/Response records |

## Changelog

### Query language, saved queries, and home sections — 2026-07-03

**Version:** 2  
**Status:** tasks-ready

**Description:** [GitHub issue #4](https://github.com/vepo/issues/issues/4) — Issues query language (plain text, ANTLR, JQL-inspired not compatible); saved queries with share link; list + edit + delete; clone for other users' queries; optional home sections.

**Development approval:** pending

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [home-screen](home-screen.md) | Home sections for owned queries with **show at home** checked; `GET /home/saved-queries` |
| [ticket-management](ticket-management.md) | Results link to ticket detail — unchanged |
| [ticket-management](ticket-management.md) comments | **comment** field in query language searches `tb_comments` |
| [kanban-board](kanban-board.md) | None |
| [project-administration](project-administration.md) | `project` field in queries |
| [phase-management](phase-management.md) | phase/version fields in queries |
| [workflow-configuration](workflow-configuration.md) | `status` field by workflow status name |
| [user-management](user-management.md) | assignee/author by email or name |
| [authentication](authentication.md) | All saved-query and share URLs require JWT (**FQ6**) |

#### Scope

##### S1 — Query language execution engine

| Aspect | Rule |
|--------|----------|
| Syntax | Issues-native; JQL-inspired; **not** Jira-compatible (**FQ3**) |
| Fields | All ticket fields + **comment** (**FQ4**) |
| Scope | Global results; optional `project` filter (**FQ5**) |
| Indexing | PostgreSQL `tsvector` + GIN (**AQ1**) |
| Parser | **ANTLR 4** — plain text query (**AQ4**) |
| API | `POST /api/tickets/search/query` (**AQ2**) |
| Exclusions | Soft-deleted tickets always excluded |

##### S2 — Saved query persistence and management

| Aspect | Rule |
|--------|----------|
| Entity | name, query text, owner, slug, **show_at_home**, timestamps |
| List | `/search/queries` — all queries owned by current user |
| Edit | `/search/queries/new`, `/search/queries/:id/edit` — **name**, query text, **show at home** checkbox (**FQ10**) |
| Delete | Owner only — from list and edit page |
| Share | `/search/q/:slug`; authenticated (**FQ6**) |
| Edit auth | Owner only on edit routes |
| Clone | **Only** when non-owner opens another user's query and chooses **Clonar** (**FQ7**) |
| Share URL | Stable slug; **Copiar link** on view page |

##### S3 — Show at home

| Aspect | Rule |
|--------|----------|
| Flag | `show_at_home` boolean; default **false**; set on edit page (**FQ10**) |
| Home UI | **One section per** flagged owned query (**FQ8**) |
| API | `GET /api/home/saved-queries` — single call (**AQ3**) |
| Refresh | Static snapshot per home visit (same as home activity) |

##### S4 — Search UI

| Aspect | Rule |
|--------|----------|
| `/search` | v1 term + status; link to advanced |
| `/search/advanced` | Ad-hoc query editor |
| Header | Simple term search retained (**FQ9**) |
| Nav | Entry to **Minhas consultas** from advanced search page |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | Flyway baseline: `tb_saved_queries`; `tsvector` + GIN on `tb_tickets` and `tb_comments` | ☐ |
| T2 | ANTLR 4: `TicketQuery.g4`, Maven plugin/runtime, generated lexer/parser compile in CI | ☐ |
| T3 | `TicketQueryCriteriaBuilder` + `TicketQueryLanguageService` — plain text → ANTLR → JPA criteria + tsvector | ☐ |
| T4 | `SearchTicketsByQueryEndpoint` + `SearchTicketsByQueryRequest` | ☐ |
| T5 | `SavedQuery` entity + `SavedQueryRepository` + `SavedQueryService` (CRUD, ownership, clone) | ☐ |
| T6 | Saved-query endpoints: create, list, find-by-slug, update, delete, clone | ☐ |
| T7 | `ListHomeSavedQuerySectionsEndpoint` + `HomeService` — one call, all show-at-home sections | ☐ |
| T8 | `dev-import.sql` — sample saved queries for local exploration | ☐ |
| T9 | Angular routes + `SavedQueryService` facade (after API codegen) | ☐ |
| T10 | `advanced-search` component — plain text query editor, execute, save | ☐ |
| T11 | `saved-query-list` component — list, open, edit, delete | ☐ |
| T12 | `saved-query-edit` component — name, query text, show-at-home, save, delete | ☐ |
| T13 | `saved-query-view` component — results, copy link, edit/clone by ownership | ☐ |
| T14 | Home component — saved-query sections from `GET /home/saved-queries` | ☐ |
| T15 | `/search` page — link to advanced + **Minhas consultas** | ☐ |
| T16 | Docs: `domain-specification.md`, `feature-catalog.md`, `README.md`, `ARCHITECTURE.md` §13 | ☐ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `TicketQueryLanguageServiceTest` | T2, T3 — ANTLR parse, all fields, comment, errors | ☐ |
| TC2 | `SearchTicketsByQueryEndpointTest` | T3, T4 — POST query, global scope, project filter | ☐ |
| TC3 | `SavedQueryEndpointTest` | T5, T6 — CRUD, owner-only edit/delete, clone non-owner | ☐ |
| TC4 | `HomeSavedQuerySectionsEndpointTest` | T7 — show-at-home filter, single response shape | ☐ |
| TC5 | `ArchitectureTest` | T4, T6 — new Request/Response records | ☐ |
| TC6 | `advanced-search.component.spec.ts` | T10 | ☐ |
| TC7 | `saved-query-list.component.spec.ts` | T11 | ☐ |
| TC8 | `saved-query-edit.component.spec.ts` | T12 | ☐ |
| TC9 | `saved-query-view.component.spec.ts` | T13 | ☐ |
| TC10 | `home.component.spec.ts` (extend) | T14 — saved-query sections | ☐ |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Query language documented; ANTLR rejects invalid plain text syntax | S1, **FQ3**, **FQ4**, **AQ4** | ☐ |
| FC2 | Query search all fields + comment; deleted excluded | S1, **FQ4** | ☐ |
| FC3 | PostgreSQL tsvector + GIN on ticket and comment text | S1, **FQ2**, **AQ1** | ☐ |
| FC4 | Advanced search page matches **Wireframe** | Wireframe | ☐ |
| FC5 | List page `/search/queries` with open, edit, delete | S2, Wireframe | ☐ |
| FC6 | Edit page: name, query, show-at-home, save, delete | S2, Wireframe, **FQ10** | ☐ |
| FC7 | Share URL `/search/q/:slug`; authenticated | S2, **FQ6** | ☐ |
| FC8 | Owner **Editar**; non-owner **Clonar** only (**FQ7**) | S2, Wireframe | ☐ |
| FC9 | Home sections for owned queries with show-at-home | S3, **FQ8**, **FQ10** | ☐ |
| FC10 | Header simple search unchanged | S4, **FQ9** | ☐ |
| FC11 | `domain-specification.md` updated | Docs | ☐ |
| FC12 | `feature-catalog.md` + `README.md` updated | Docs | ☐ |
| FC13 | `ARCHITECTURE.md` §13 updated | Architecture | ☐ |
| FC14 | Dev seed sample saved queries | S2 | ☐ |

## Changelog (continued)

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

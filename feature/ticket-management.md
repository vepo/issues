# Ticket management

**Feature version:** 5  
**Status:** in-progress
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Core ticket lifecycle: view and edit ticket fields (including optional **due date**), assign users, move status per workflow rules, soft-delete (admin/PM), add comments, audit history, subscribe/unsubscribe observers, unified activity feed, creation of a distinct ticket from selected values of an existing ticket (**Clone ticket**, v4), and tagging other users in comments to notify them directly (**Comment @mentions**, v5 planned).

## Wireframe

**Guide:** layout reference for UI implementation — update when fields or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-17 (Comment @mentions feature analysis started) |

### Screen: `/ticket/:ticketIdentifier`

| Region | Elements |
|--------|----------|
| Header | Identifier, title, status move, assignee, subscribe |
| Fields | Project, category, priority, description (rich text), **Data de vencimento** (optional), phase/versions when enabled |
| Actions | Save, delete (admin/PM), **Restaurar** when deleted (admin/PM only); read-only fields when deleted |
| Comments | Add comment form + thread |
| Atividade | Merged feed: comments + history (`.activity-feed`) |

```
┌────────────────────────────────────────────────────────┐
│  PROJ-42  Title  [excluído]        (controls disabled) │
├───────────────────────────────┬────────────────────────┤
│  Fields + description (read)  │  Comentários (hidden)  │
│  [ Restaurar ]  (admin/PM)    │                        │
├───────────────────────────────┴────────────────────────┤
│  Atividade (comments + history incl. DELETED/RESTORED) │
└────────────────────────────────────────────────────────┘
```

### Screen: `/ticket/:ticketIdentifier` — Clone action (v4 draft)

| Region | Elements |
|--------|----------|
| Header actions | **Clonar ticket** on an active, readable ticket |
| Result | Navigate to global create flow with source project selected by default and source-derived values pre-filled for review/edit (**FQ10**, **FQ12**) |

### Screen: create flow after clone (v4 draft)

```
┌────────────────────────────────────────────────────────┐
│  Novo ticket — valores copiados de PROJ-42             │
├────────────────────────────────────────────────────────┤
│  Projeto [ source project ▼ ]                           │
│  Title / description / category / priority / type      │
│  Custom fields compatible with selected target         │
│  (planning fields use blank/create defaults)            │
│  Avisos: source fields omitted from this target         │
│                                      [ Criar ticket ]   │
└────────────────────────────────────────────────────────┘
```

### Screen: `/ticket/:ticketIdentifier` — Comment @mentions (v5 draft)

| Region | Elements | Notes |
|--------|----------|-------|
| Comment editor | Typing `@` inside `app-rich-text-editor` opens an inline autocomplete of candidate users | Candidates = current **project members** (**FQ18**) |
| Autocomplete list | Filter as typed; keyboard (↑/↓/Enter) or mouse selection inserts `@username` as plain text | No auto-subscribe on selection (**FQ19**) |
| Comment content | Mention renders as plain `@username` text — no chip, link, or highlight | **FQ21** |

```
┌────────────────────────────────────────────────────────┐
│  Comentários                                            │
│  ┌──────────────────────────────────────────────────┐   │
│  │ Great work @an                                    │   │
│  │            ┌───────────────────┐                  │   │
│  │            │ @ana.silva        │  ← autocomplete   │   │
│  │            │ @andre.souza      │                  │   │
│  │            └───────────────────┘                  │   │
│  └──────────────────────────────────────────────────┘   │
│                                     [ Comentar ]         │
└────────────────────────────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `ticket`, `ticket.comments`, `ticket.history`, `ticket.business`, `customfield`; reactions in `notifications`, `mailer`; mention candidate lookup via `project` membership |
| Packages / files | Existing ticket operations plus planned `ticket.cloneprefill`; `CustomFieldService`; `ticket-view`; `create-ticket`; `ticket-form`; planned comment mention parsing/notification wiring (`Comment`/`CommentRequest`, `NotificationEvent`/`NotificationEventListener`); reuse `ListProjectMembersEndpoint` for autocomplete |
| API | Existing ticket API plus planned clone-prefill read operation; creation remains `POST /tickets`; comment creation (`POST /tickets/{id}/comments`) is unchanged — mentions are parsed server-side from `content` (**AQ25**) |
| UI | `/ticket/:ticketIdentifier` **Clonar ticket** → `/tickets/new` with target project selector and server-provided prefill; comment editor gains `@` autocomplete; [notifications](notifications.md) dropdown gains a mention entry type |
| Schema / seed | `tb_tickets`, `tb_comments`, `tb_ticket_history`, `tb_tickets_subscribers`; sample tickets in `dev-import.sql`; no schema change for mentions — reuses `tb_notifications` with new `type` (**AQ26**) |
| Tests | Existing ticket tests plus planned clone-prefill endpoint, create-ticket component, ticket-form, custom-field mapping, and ticket-view action specs; planned comment mention parsing/notification tests, autocomplete Angular specs |
| Docs | domain-spec (Ticket, Comment, History, Subscriber, Activity feed, planned **Mention**), feature-catalog (Ticket detail), README § Tickets & workflow |

### Risks

- Restore must re-include ticket in search/lists without breaking identifier uniqueness or history continuity.
- Clone must create a new identity and lifecycle without copying collaboration, audit, files, or relationships.
- Copied project-scoped values may be invalid if cross-project cloning is allowed.
- The existing ticket-change notification fan-out (`NotificationEventListener`) only reaches **ticket subscribers**; a mentioned user may not be subscribed and needs a separate delivery path (confirmed by **FQ19** — mentioning never subscribes).
- Autocomplete candidates must respect project membership/visibility — must not leak users outside the ticket's project (resolved by **FQ18** — reuse existing project-membership scope).
- Server-side `@username` parsing must not misfire inside emails/URLs embedded in comment text, and must match usernames **exactly** (no partial/substring matches) against the ticket's current project members.
- Comment author must never generate a self-notification if they mention themselves.

### Feature questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ9 | Should deleted tickets be restorable from the UI? | answered | **Yes** — admin/PM may restore soft-deleted tickets from ticket detail |

### Feature questions (due date — v2)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Field type and nullability? | answered | Optional `LocalDate dueDate` (`due_date DATE`) |
| FQ2 | Distinction from finish date? | answered | **Due date** = user-planned deadline; **`finished_at`** = workflow DONE completion |
| FQ3 | Where editable? | answered | Create ticket + ticket detail update |
| FQ4 | CSV import mapping? | answered | Out of scope for this gap closure |
| FQ5 | Kanban badge? | answered | Out of scope for this gap closure |
| FQ6 | Query language? | answered | `dueDate` / `due` with date comparators + IS EMPTY / IS NOT EMPTY |
| FQ7 | History? | answered | `FIELD_CHANGED` field `"dueDate"` |
| FQ8 | UI label? | answered | PT-BR **Data de vencimento** |

### Feature questions (Clone ticket — v4)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ10 | Clone only into the source project, or allow selecting another project? | answered | Allow selecting another **writable** project; source project is the initial target |
| FQ11 | Which optional planning fields copy: assignee, phase, observed/target versions, due date? | answered | Copy **none**: omit assignee, phase, observed/target versions, due date, and story points |
| FQ12 | Create immediately or open the normal create form pre-filled for review/edit? | answered | Open the normal create form pre-filled for review/edit |
| FQ13 | How should the title be initialized? | answered | Copy unchanged; user may edit before creation |
| FQ14 | Do clone values override the project ticket template? | answered | Clone values override target project template defaults |
| FQ15 | Copy in-scope custom-field values? | answered | Yes; map and validate against current target project/workflow scope |
| FQ16 | May admin/PM clone a soft-deleted ticket? | answered | No; active source tickets only |
| FQ17 | Record provenance linking clone to source? | answered | No dedicated provenance, link, or clone-specific history in v1; retain ordinary create history behavior |

### Feature questions (Comment @mentions — v5)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ18 | Who is eligible to be `@mentioned` on a ticket's comment — any project member, or only current subscribers/assignee? | answered | **Any project member** — autocomplete candidates reuse existing project membership (`ListProjectMembersEndpoint`) |
| FQ19 | Does mentioning a user auto-subscribe them to the ticket, or is it a one-off notification with no lasting subscription? | answered | **No** — one-off notification only; mentioning never changes the ticket's subscriber list |
| FQ20 | Should a mention also send an email (like the existing ticket-change email), or stay in-app/SSE only? | answered | **In-app / SSE only** — no email in v1 |
| FQ21 | How should a mention render in the comment thread — plain `@username` text, or a distinct clickable/highlighted mention style? | answered | **Plain `@username` text** — no special styling or click target in v1 |

## Changelog

### Initial implementation — baseline

**Version:** 1  
**Status:** done

**Description:** Full ticket CRUD (soft delete), workflow-validated moves, assignee updates, comments, structured history, subscribers, and merged Atividade feed on detail page.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Kanban board | Lists and moves tickets |
| Create ticket | Creates tickets via separate flow |
| Notifications | Fires on ticket changes for subscribers |
| Workflow configuration | Move validates against project workflow |
| Project administration | Tickets scoped to project |
| — | None identified beyond cross-context events |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Ticket detail matches **Wireframe** layout | Wireframe | ☑ |
| FC2 | Workflow-validated status moves | Summary | ☑ |
| FC3 | Comments and unified Atividade feed | Wireframe | ☑ |
| FC4 | Subscribe/unsubscribe observers | Summary | ☑ |
| FC5 | `domain-specification.md` — Ticket, Comment, History | Impact / Docs | ☑ |
| FC6 | `feature-catalog.md` — Ticket detail row | Impact / Docs | ☑ |

**Implementation notes:** `TicketHistoryService` logs CREATED, FIELD_CHANGED, STATUS_CHANGED, ASSIGNEE_CHANGED, SUBSCRIBED, UNSUBSCRIBED, DELETED; `ticket-view.component.ts` orchestrates detail UI.

### Ticket due date — 2026-07-03

**Version:** 2  
**Status:** done

**Development approval:** approved 2026-07-03 — tasks: T1–T12

**Description:** Close ARCHITECTURE §13 gap — optional planned **due date** on tickets, distinct from workflow **finish date**.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [create-ticket](create-ticket.md) | Optional due date on create form |
| [ticket-search](ticket-search.md) | Query language field `dueDate` / `due` |
| [kanban-board](kanban-board.md) | None (badge deferred) |
| [ticket-import](ticket-import.md) | None (CSV mapping deferred) |

## Architecture

| Layer | Change |
|-------|--------|
| Schema | `due_date DATE` on `tb_tickets` |
| Entity | `Ticket.dueDate` (`LocalDate`) |
| API | `CreateTicketRequest`, `UpdateTicketRequest`, `TicketResponse`, `TicketExpandedResponse` |
| Service | `TicketService` create/update + history |
| Query | `TicketQueryPredicateBuilder` — `dueDate` / `due` |
| UI | `ticket-form`, `ticket-view`; `query-language-reference.ts` |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | Changelog + FQ + Architecture + FC + test plan | ☑ |
| T2 | Flyway + JPA `dueDate` | ☑ |
| T3 | Extend Request/Response records | ☑ |
| T4 | `TicketService` persist + history | ☑ |
| T5 | Endpoint tests create/update | ☑ |
| T6 | Query language + tests | ☑ |
| T7 | `dev-import.sql` sample due dates | ☑ |
| T8 | Ticket detail UI | ☑ |
| T9 | Create ticket form UI | ☑ |
| T10 | OpenAPI codegen | ☑ |
| T11 | Query help reference + spec | ☑ |
| T12 | domain-spec, README, ARCHITECTURE §13 | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `CreateTicketEndpointTest` — due date on create | T4, T5 | ☑ |
| TC2 | `UpdateTicketEndpointTest` — due date update | T4, T5 | ☑ |
| TC3 | `TicketQueryLanguageServiceTest` — dueDate filter | T6 | ☑ |
| TC4 | `ArchitectureTest` | T3 | ☑ |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Ticket detail **Data de vencimento** matches **Wireframe** | Wireframe, FQ8 | ☑ |
| FC2 | Create ticket optional due date | FQ3 | ☑ |
| FC3 | History logs due date changes | FQ7 | ☑ |
| FC4 | Query language `dueDate` field | FQ6 | ☑ |
| FC5 | `domain-specification.md` — Due date term | Docs | ☑ |
| FC6 | ARCHITECTURE §13 gap closed | Docs | ☑ |

**Implementation notes:** `due_date` on `tb_tickets`; create/update API + UI; query language `dueDate`/`due`; history via `FIELD_CHANGED`. `mvn verify` + `npm run build` green (2026-07-03).

### Restore soft-deleted tickets — 2026-07-03

**Version:** 3  
**Status:** done

**Development approval:** approved 2026-07-03 — tasks: T2–T10

**Description:** Allow admin and project-manager to **restore** soft-deleted tickets from the UI; ticket reappears in lists and search; history logs restoration.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [ticket-search](ticket-search.md) | Restored tickets included in query results again |
| [kanban-board](kanban-board.md) | Restored tickets visible on board |
| [notifications](notifications.md) | **No** restore notification — history entry only (same as delete) |

## Architecture

**Scope:** changelog entry **Restore soft-deleted tickets — v3** only. Due-date architecture remains under v2 above.

| Area | Design |
|------|--------|
| Bounded contexts | `ticket`, `ticket.history` — no cross-context events |
| Packages / layers | `ticket.restore.RestoreTicketEndpoint` → `TicketService.restore` → `TicketRepository.restore`; find paths updated in `TicketService` + `ticket.find` |
| API | `POST /api/tickets/{id}/restore` — `@RolesAllowed(admin, project-manager)`; returns `TicketResponse` (200). `GET /api/tickets/{identifier}/expanded` returns deleted tickets for **admin/PM only**; regular `user` still gets 404 |
| Schema / seed | **No schema change** — reuse `tb_tickets.deleted` boolean |
| Response contract | Add `boolean deleted` to `TicketResponse` and `TicketExpandedResponse` so UI can render read-only deleted state |
| Service rules | `restore`: load ticket including deleted; reject if not deleted; set `deleted=false`; log `RESTORED` via existing `TicketHistoryService.logTicketRestored`. Mutations (`update`, `move`, `assign`, `delete`, comments, subscribe) reject deleted tickets with 400 |
| Frontend | `ticket-view`: when `deleted`, show badge, disable edit/move/comment/delete, show **Restaurar** for admin/PM; `ticket.service.restore(id)`; activity feed handles `RESTORED` action |
| Tests | `RestoreTicketEndpointTest`; extend find-expanded test for admin vs user on deleted ticket; `TicketHistoryServiceTest` or endpoint assertion for RESTORED entry; `ArchitectureTest` for new Response field |
| Docs | `feature-catalog.md` ticket detail row; domain-spec invariant already recorded |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | How can admin/PM open a deleted ticket for restore? | answered | **`GET …/expanded`** returns deleted tickets for **admin/PM**; regular users still 404 |
| AQ2 | Restore HTTP shape? | answered | **`POST /api/tickets/{id}/restore`** returning `TicketResponse` |
| AQ3 | Notification on restore? | answered | **No** — `RESTORED` history only (delete does not notify either) |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | Architecture + AQ + tasks + test plan on changelog v3 | ☑ |
| T2 | `TicketRepository.restore` + find-by-id/identifier including deleted | ☑ |
| T3 | `TicketService.restore` + reject mutations on deleted tickets + role-aware expanded find | ☑ |
| T4 | Add `deleted` to `TicketResponse` / `TicketExpandedResponse` | ☑ |
| T5 | `RestoreTicketEndpoint` — `POST /{id}/restore` | ☑ |
| T6 | `RestoreTicketEndpointTest` + find-expanded deleted visibility tests | ☑ |
| T7 | Ticket detail UI — deleted read-only state + **Restaurar** (admin/PM) | ☑ |
| T8 | Activity feed — `RESTORED` icon/summary | ☑ |
| T9 | OpenAPI codegen (`npm run generate:api`) | ☑ |
| T10 | `feature-catalog.md` + verify domain-spec invariant | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `RestoreTicketEndpointTest` — PM restores deleted ticket | T3, T5 | ☑ |
| TC2 | `RestoreTicketEndpointTest` — regular user forbidden | T5 | ☑ |
| TC3 | `RestoreTicketEndpointTest` — restore idempotent guard (not deleted → 400) | T3 | ☑ |
| TC4 | Find expanded by identifier — admin sees deleted; user gets 404 | T3 | ☑ |
| TC5 | `TicketHistoryServiceTest` or endpoint — RESTORED history entry | T3 | ☑ |
| TC6 | `ArchitectureTest` — Response records include `deleted` | T4 | ☑ |
| TC7 | `activity-feed.utils` spec — RESTORED summary | T8 | ☑ |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | **Restaurar** action on soft-deleted ticket detail | Wireframe, FQ9 | ☑ |
| FC2 | Restored ticket visible in lists and search | FQ9 | ☑ |
| FC3 | History logs restore event (`RESTORED`) | FQ9 | ☑ |
| FC4 | `domain-specification.md` — restore invariant | Docs | ☑ |
| FC5 | Deleted ticket detail read-only for all roles; restore admin/PM only | Wireframe, AQ1 | ☑ |
| FC6 | `feature-catalog.md` — restore step on ticket detail | Docs | ☑ |

**Implementation notes:** `POST /api/tickets/{id}/restore`; `deleted` on Response records; admin/PM expanded find includes soft-deleted tickets; ticket detail read-only + **Restaurar**; activity feed `RESTORED`. `mvn verify` + `npm run build` green (2026-07-03).

## Architecture — Clone ticket v4

| Area | Design |
|------|--------|
| Bounded contexts | **Ticket management** owns clone prefill and final create; uses **Custom fields** service for target-compatible values and **Project administration** access for readable source / writable target |
| Packages / layers | `ticket.cloneprefill.GetCloneTicketPrefillEndpoint` → `CloneTicketPrefillService` → `TicketRepository`, `ProjectAccessService`, `CustomFieldService`; existing `ticket.create.CreateTicketEndpoint` → `TicketService.create` remains the only create command |
| Prefill API | `GET /api/tickets/{sourceId}/clone-prefill?targetProjectId={id}` → `CloneTicketPrefillResponse`; authenticated roles; source must be active/readable and target writable |
| Writable targets API | `GET /api/projects/writable` → existing `ProjectResponse` list filtered by membership/ownership/admin (**AQ21**) |
| Final create API | Existing `POST /api/tickets` with `CreateTicketRequest`; generates identifier, author, start status, backlog rank, timestamps, and normal create history |
| Response contract | `CloneTicketPrefillResponse(sourceIdentifier, targetProjectId, title, description, categoryId, priority, ticketType, customFields, warnings)`; no planning/collaboration/relationship data |
| Custom fields | Match active source values to enabled target in-scope definitions by stable **key** and exact type; enum by stable option value; merge target template first, then clone values; omit incompatible values with warnings (**AQ19**) |
| Schema / seed | No schema or seed change |
| Frontend route | Ticket detail action navigates to `/tickets/new?cloneFrom={sourceId}&targetProjectId={sourceProjectId}`; create flow loads writable projects and target-aware prefill; changing target reloads prefill |
| Frontend state | Do not place ticket content in URL/router state. Apply a complete target template baseline, then overlay clone defaults to prevent stale values when target changes |
| History / notifications | No clone-specific history/provenance and no source notification. Existing create history behavior remains, including current initial custom-field `FIELD_CHANGED` entries |
| Tests | `CloneTicketPrefillEndpointTest`, `CreateTicketEndpointTest`, `ArchitectureTest`; Angular ticket-view, create-ticket, ticket-form/custom-field specs |

### Architecture questions (Clone ticket v4)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ18 | Use a target-aware backend prefill endpoint or derive all clone defaults in Angular? | answered | Target-aware backend prefill endpoint; final creation still uses existing `POST /tickets` |
| AQ19 | What happens to missing/type-incompatible/invalid target custom fields? | answered | Omit them and return visible warnings; do not block opening the form |
| AQ20 | Initial target project? | answered | Source project selected initially; user may choose another writable project |
| AQ21 | How does the UI discover valid cross-project targets? | answered | `GET /api/projects/writable` backed by project membership/ownership/admin scope |
| AQ22 | URL/state shape? | answered | `/tickets/new?cloneFrom={sourceId}&targetProjectId={sourceProjectId}`; fetch content from API |
| AQ23 | Custom-field compatibility identity? | answered | Stable field key + exact type; enum stable option value; never database id/label |
| AQ24 | History when copied custom values are submitted? | answered | Preserve normal create history (`CREATED` plus existing initial custom-field `FIELD_CHANGED` entries); no clone-specific event |

### Clone ticket — 2026-07-16

**Version:** 4  
**Status:** done

**Description:** Create a distinct ticket from selected values of an existing ticket. The clone receives a new identifier, author, start status, backlog rank, timestamps, and ordinary `CREATED` history.

**Scope invariant:** comments, prior history, subscribers, attachments, linked commits, and ticket links (including Epic children/parent) are never copied. The source remains unchanged.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Create ticket | Reuse the normal creation flow with server-provided source defaults |
| Ticket detail | New **Clonar ticket** action |
| Project administration | Writable-project list for cross-project target selection |
| Custom fields | Copy target-compatible values by stable key; warn on omissions |
| Backlog | Clone appends with a fresh backlog rank |
| Workflow | Clone starts at the project workflow start status |
| Links / Epics | No links, parent, or children copied |
| Attachments / comments / subscribers | Not copied |
| Notifications | No notification to source subscribers |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Clone has a new identifier, acting author, start status, backlog rank, and timestamps | Domain invariant | ☑ |
| FC2 | Source ticket remains unchanged | Summary | ☑ |
| FC3 | Copy title, description, category, priority, type, and target-compatible custom fields; no optional planning fields | FQ10–FQ15 | ☑ |
| FC4 | Comments, old history, subscribers, attachments, commits, and links are not copied | Scope invariant | ☑ |
| FC5 | Soft-deleted source behaviour matches **FQ16** | FQ16 | ☑ |
| FC6 | Provenance/history matches **FQ17** | FQ17 | ☑ |
| FC7 | Ticket detail and create flow match the final **Wireframe** | Wireframe, FQ12 | ☑ |
| FC8 | `domain-specification.md` defines Clone ticket and invariants | Impact / Docs | ☑ |
| FC9 | `feature-catalog.md` documents the clone click path | Impact / Docs | ☑ |
| FC10 | README Tickets & workflow mentions cloning | Impact / Docs | ☑ |
| FC11 | Cross-project target selector contains only writable projects | FQ10, Architecture | ☑ |
| FC12 | Omitted custom fields produce visible warnings | FQ15, AQ19 | ☑ |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | Add `GET /api/projects/writable` through Endpoint → Service → Repository/scope and endpoint tests | ☑ |
| T2 | Add `CloneTicketPrefillResponse` / warning records and `CloneTicketPrefillService` with source/target access, active-source checks, intrinsic field defaults, and cross-project custom-field mapping; unit tests | ☑ |
| T3 | Add `GetCloneTicketPrefillEndpoint` (`GET /tickets/{sourceId}/clone-prefill?targetProjectId=`) and endpoint tests for access, active source, response, omissions, and warnings | ☑ |
| T4 | Run backend API/architecture tests and regenerate the Angular OpenAPI client; add Ticket/Project service facades | ☑ |
| T5 | Add ticket-detail **Clonar ticket** action/navigation (active tickets only) and component spec | ☑ |
| T6 | Extend global create flow to read clone query context, load writable projects/prefill, default source target, reload on target change, display warnings, and submit through existing create API; component specs | ☑ |
| T7 | Make ticket form/custom-field defaults reset completely on target changes, then apply target template followed by clone override; component specs | ☑ |
| T8 | Update domain spec, feature catalog, README, ARCHITECTURE, feature index/backlog, and implementation notes; run final gates | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `ListWritableProjectsEndpointTest` — member/owner/admin scope excludes read-only projects | T1 | ☑ |
| TC2 | `CloneTicketPrefillServiceTest` — copies intrinsic fields; omits all planning/collaboration/relationship fields | T2 | ☑ |
| TC3 | `CloneTicketPrefillEndpointTest` — source read + active checks; target write; source target default contract | T2, T3 | ☑ |
| TC4 | `CloneTicketPrefillEndpointTest` — custom fields match key/type/enum value; incompatible values omitted with warnings | T2, T3 | ☑ |
| TC5 | `CreateTicketEndpointTest` — submitted clone defaults still produce fresh identifier/author/start status/backlog/history and do not mutate source | T3 | ☑ |
| TC6 | `ArchitectureTest` — new Request/Response records and one-operation endpoints | T1–T3 | ☑ |
| TC7 | `ticket-view.component.spec.ts` — clone action visibility and query navigation | T5 | ☑ |
| TC8 | `create-ticket.component.spec.ts` — source target, cross-project reload, template precedence, warning display, existing create submit | T6 | ☑ |
| TC9 | `ticket-form.component.spec.ts` / custom-field section specs — complete reset and template→clone overlay | T7 | ☑ |

**Development approval:** approved 2026-07-16 — tasks: T1, T2, T3, T4, T5, T6, T7, T8

**Implementation notes:** Added writable-project and target-aware clone-prefill APIs; target-compatible custom-field mapping with omission warnings; active-ticket clone navigation; clone-aware global create flow with target template precedence and complete default resets. Existing ticket creation remains the only write path and produces a fresh ticket without provenance. `mvn verify`, Angular build, and full Angular tests passed on 2026-07-16.

### Comment @mentions — 2026-07-17

**Version:** 5  
**Status:** in-progress

**Description:** Let comment authors tag other users with `@` in the rich-text comment editor; tagged users are notified directly, even when not already subscribed to the ticket.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [notifications](notifications.md) | New mention notification type; delivery path outside the existing subscriber-only fan-out (**FQ18–FQ19**) |
| Project administration | Autocomplete candidates drawn from existing project membership |
| Rich-text editor | Comment editor (`app-rich-text-editor`) gains an inline `@` autocomplete/mention affordance |

#### Feature checklist (phase 1 draft — refine after FQ18–FQ21 answers)

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Typing `@` in the comment editor offers a candidate list matching **Wireframe** | Wireframe | ☐ |
| FC2 | Mentioning a project member notifies them in-app/SSE even when not a ticket subscriber, without changing the subscriber list | FQ18–FQ19 | ☐ |
| FC3 | No email is sent for mentions in v1 | FQ20 | ☐ |
| FC4 | Mention renders as plain `@username` text in the posted comment | FQ21 | ☐ |
| FC5 | `domain-specification.md` defines **Mention** term and invariant | Impact / Docs | ☐ |
| FC6 | `feature-catalog.md` — ticket detail comment step mentions @mentions | Impact / Docs | ☐ |
| FC7 | README § Tickets & workflow mentions @mentions | Impact / Docs | ☐ |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | `TicketService.addComment` — parse `@username` tokens from the raw comment content (before HTML sanitization), resolve against the ticket's current project members, exclude the comment author, call `NotificationService.notifyMentions` | ☑ |
| T2 | `NotificationService.notifyMentions` — create one `Notification` (`type="comment-mention"`) per resolved mentioned user synchronously and push SSE via `NotificationChannelRegistry` | ☑ |
| T3 | `AddCommentEndpointTest` — mention notifies a non-subscriber project member; self-mention produces no notification; non-member/typo tokens and mid-word `@` (e.g. inside an email) are ignored | ☑ |
| T4 | `MentionParserTest` (unit) — extraction, dedup, punctuation boundary, email exclusion, blank/null | ☑ |
| T5 | `ticket-view` comment form — on `@` keystroke, filter the ticket's project members and insert `@username` as plain text on selection; added `username` to `ProjectMemberResponse` (was missing, required for the autocomplete) and regenerated the Angular API client | ☑ |
| T6 | `ticket-view.component.spec.ts` — autocomplete filters project members on `@`; selecting a candidate inserts plain-text mention; no trailing `@` closes the autocomplete | ☑ |
| T7 | Flip domain-spec invariant **81** from Planned to implemented; add @mentions to `feature-catalog.md` ticket-detail comment step and README § Tickets & workflow | ☐ |
| T8 | Run `mvn verify` + `npm run build` + Angular tests; recheck **Feature checklist** FC1–FC7 before `done` | ☐ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `AddCommentEndpointTest` — mentioned project member (not a ticket subscriber) receives a notification | T1, T3 | ☑ |
| TC2 | `AddCommentEndpointTest` — self-mention produces no notification | T1, T3 | ☑ |
| TC3 | `AddCommentEndpointTest` — non-member/typo `@token` and mid-word `@` (email-like text) are not treated as mentions | T1, T3 | ☑ |
| TC4 | `MentionParserTest` — extraction correctness (single/multiple/dedup/punctuation/email-exclusion/blank) | T2, T4 | ☑ |
| TC5 | `ticket-view.component.spec.ts` — typing `@` filters the project member list | T5, T6 | ☑ |
| TC6 | `ticket-view.component.spec.ts` — selecting a candidate inserts plain-text `@username` into the editor value | T5, T6 | ☑ |

**Development approval:** approved 2026-07-17 — tasks: T1, T2, T3, T4, T5, T6, T7, T8

**Implementation notes:** (in progress — TDD starting on T1.)

## Architecture — Comment @mentions v5

**Scope:** changelog entry **Comment @mentions — v5** only. Other Architecture sections above remain scoped to their own changelog entries.

| Area | Design |
|------|--------|
| Bounded contexts | **Ticket management** owns mention detection and comment persistence; **Notifications** owns delivery; candidate lookup reuses **Project administration** membership. No new cross-context repository access |
| Packages / layers | `ticket.comments.add.AddCommentEndpoint` → `TicketService.addComment` (extended) parses `@username` tokens out of the persisted `Comment.content` via a new `ticket.comments.MentionParser` utility, resolves them against `ProjectMemberRepository` for the ticket's project, then calls `NotificationService` synchronously to notify each resolved mentioned user |
| API | `POST /tickets/{id}/comments` — **`CommentRequest` unchanged** (**AQ25**); mentions are derived server-side from `content`. `CommentResponse` unchanged. No new endpoint; the comment-editor autocomplete reuses existing `GET /projects/{id}/members`, extended with `username` on `ProjectMemberResponse` (was missing — needed so the client can insert the exact token the server matches on) |
| Mention parsing | `MentionParser` regex over the **raw request content** (`CommentRequest.content`, before `HtmlSanitizer.sanitize` — the sanitizer HTML-entity-encodes `@` to `&#64;`, so parsing must happen first) matching `@` at a word boundary (`(?<!\w)@`, so it does not fire mid-token inside emails or URLs) followed by username characters `[A-Za-z0-9_-]+`; each candidate token is matched **exactly** against the ticket's current project member usernames — non-matching tokens (typos, non-members, partial words) are silently ignored, not errors |
| Schema / persistence | **No new table.** Reuse `tb_notifications` with a new `type = "comment-mention"` value (**AQ26**); `content` holds a short localized summary, `reffer` = the ticket. No column added to `tb_comments` |
| Cross-context integration | `TicketService.addComment` calls a new `NotificationService.notifyMentions(ticket, author, mentionedUsers, content)` **synchronously in the same transaction** (not the async `NotificationEvent`/`NotificationEventListener` CDI path used for ticket-move) — mention delivery is a direct, targeted call to specific users rather than a subscriber broadcast, and keeping it synchronous keeps it deterministically testable (`AddCommentEndpointTest` can assert the notification exists immediately after the `201`, no `Awaitility`/polling needed, matching how the rest of this test suite avoids asserting on `fireAsync` timing). `NotificationService` gains `NotificationChannelRegistry` and `Sse` (already CDI-injectable, same as `NotificationEventListener`) to push the SSE event inline |
| Frontend integration | `ticket-view` comment form: on `@` keystroke, filter the project's member list (fetched via `ProjectService.listMembers`) and insert `@username` as plain text on selection — purely a typing aid. No `mentionedUserIds` is submitted; the server is the sole source of truth for who got mentioned. No `app-rich-text-editor` plugin change — mention text is ordinary text, not a custom node |
| Tests | `AddCommentEndpointTest` — mention notifies non-project-subscriber member, self-mention produces no notification, `@nonmember` / `@typo` tokens are ignored, mid-word `@` (e.g. inside an email) is not treated as a mention; `NotificationServiceTest` or listener-level test for `comment-mention` fan-out; Angular `ticket-view.component.spec.ts` — autocomplete filters members and inserts plain-text mention |

### Architecture questions (Comment @mentions — v5)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ25 | Should the client submit resolved `mentionedUserIds` with the comment, or should the server parse `@username` tokens out of the comment text after the fact? | answered | **Server parses** `@username` tokens from `Comment.content` after persist; `CommentRequest` stays unchanged |
| AQ26 | Should mention notifications reuse `tb_notifications` with a new `type` value, or get a dedicated table/entity? | answered | **Reuse `tb_notifications`** with a new `type = "comment-mention"` value; no schema change |

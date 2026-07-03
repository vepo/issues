# Home screen

**Feature version:** 1  
**Status:** tasks-ready  
**Requested:** 2026-07-03  
**Source:** [GitHub issue #3](https://github.com/vepo/issues/issues/3) — *Criar página inicial que exiba os tickets atuais*

## Summary

Replace the current home page (`/`) — a project-picker grid — with a **personal work hub**: open tickets and recent activity scoped to the user's **assigned projects**. Introduces **project membership** (user ↔ project M:N): a user must be a member of a project to be eligible as a ticket assignee on that project.

Global shell (search, **Novo**, notifications, **Conta**) stays in `app.html` — not part of this feature body.

## Wireframe

**Guide:** layout reference for UI implementation — update when Scope, routes, or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)). Phase 4 Angular work must match this section unless revised here first.

| Field | Value |
|-------|-------|
| **Source** | [Excalidraw](https://excalidraw.com/#json=6adTLCItcTdo5lcRqsxnb,0FcpEQ91CMO62NRW6D-PQA) + ASCII below |
| **Last updated** | 2026-07-03 |

### Screen: `/` — personal work hub

| Region | Elements |
|--------|----------|
| Row 1 (2 cols) | **Tickets atuais** table \| **Tickets atribuídos** table |
| Row 2 (full) | **Atividade** feed (comments + status changes) |
| Columns | Identifier (link), title, project (link to hub), status, priority, updated |
| Empty states | Per-section guidance when no rows |

```
┌─────────────────────────────────────────────────────────┐
│  Header (existing): brand | search | Novo | … | Conta   │
├──────────────────────────┬──────────────────────────────┤
│  Tickets atuais          │  Tickets atribuídos          │
│  (open, scoped)          │  (assignee = me, open)       │
├──────────────────────────┴──────────────────────────────┤
│  Atividade (full width, static snapshot)                  │
└─────────────────────────────────────────────────────────┘
```

Mobile: stack sections vertically. **No** project grid on home (**Q5**).

### Screen: `/projects/:projectId` — project hub

| Region | Elements |
|--------|----------|
| Header | Project name, description |
| Actions | **Kanban**, **Painel**; **Editar** (owner PM/admin); **Alocação** (owner PM/admin) |

```
┌─────────────────────────────────────────────┐
│  Project Alpha                              │
│  Description text…                          │
│  [ Kanban ] [ Painel ] [ Editar ] [ Alocação ] │
└─────────────────────────────────────────────┘
```

### Screen: `/projects/:projectId/allocation`

| Region | Elements |
|--------|----------|
| Member table | User name, email; **Remover** |
| Guard UI | When remove blocked: list member's open assigned tickets (**Q13**) |
| Actions | Add member(s) |

### Screen: `/projects/:projectId/edit`

| Region | Elements |
|--------|----------|
| Form | Project fields; **owner** picker (admin + current owner per **Q17**) |

## Decisions (from open questions)

| Topic | Decision |
|-------|----------|
| **Tickets atuais** (**Q1**) | All **open** tickets in projects the user is a **member** of |
| **Tickets atribuídos** (**Q2**) | All open tickets where current user is **assignee** (within member projects); exclude finished/canceled |
| **Atividade** (**Q3**) | Latest **comments** and **status changes** on tickets in assigned projects |
| **Live updates** (**Q4**) | Load once per visit — no SSE on home |
| **Project grid** (**Q5**) | **Remove** from home |
| **Row limits** (**Q6**) | **No** cap — show all matching rows |
| **vs notifications** (**Q7**) | Notifications: SSE, mark-as-read. Home activity: static snapshot, always visible sections |
| **Finished/canceled** (**Q8**) | **Exclude** from all home lists and activity |
| **PM/admin scope** (**Q9**, refined **Q12**) | **Admin:** org-wide home. **Project owner** (see S1c): home + edit scoped to **owned** projects only — not org-wide. **`user` role:** member projects only |
| **Project owner** (**Q12**) | Exactly one **project owner** per project; must hold `project-manager` role; set at **project creation** |
| **Project hub** (**Q15**) | `/projects/:projectId` — view for **members** (all roles); links to Kanban and dashboard; edit/allocation only for owner PM or admin |
| **Member removal** (**Q13**) | Forbid removing a member with **open** (non-finished) assigned tickets until reassigned; allocation UI shows that user's open assigned tickets |
| **Search scope** (**Q16**) | **Unchanged** — global ticket search across all projects |
| **Owner transfer** (**Q17**, **Q18**) | **Admin** or **current project owner** may reassign owner on edit; new owner must have `project-manager` role; **need not** be an existing member (**Q18**) — added as member on transfer |
| **Header label** (**Q10**) | **No** change — **Novo** stays in header |
| **Menu Conta** | Out of scope — already in header |

### Open tickets (shared filter)

Exclude tickets in workflow **finish** statuses (`DONE` or `CANCELED` outcome) and soft-deleted tickets.

## Scope

### S1 — Project membership (new)

| Aspect | Rule |
|--------|------|
| Model | M:N **Project** ↔ **User** (`tb_project_members` or equivalent) |
| Cardinality | One user → many projects; one project → many members |
| **Project owner** | Exactly one per project (`owner_id`); must have `project-manager` role; set at **create**; transferable by **admin** or **current owner** (**Q17**) |
| Assignee rule | Ticket `assignee` must be a **member** of the ticket's project (create, update assignee, CSV import) |
| Owner transfer | New owner must have `project-manager` role; **not** required to be a member beforehand (**Q18**); **auto-added** as member when transfer completes |
| Home scope | **Admin:** all projects. **Project owner:** owned projects. **`user`:** member projects (**Q9** refined by **Q12**) |
| Edit / allocation | **Project owner** or **admin** only — PM cannot edit projects they do not own (**Q12**) |
| Membership admin | **Allocation** page (**Q11**); owner PM or admin |

### S1b — Project allocation page

Dedicated page under **Projects** to manage **project members** (not embedded in project edit form).

| Aspect | Rule |
|--------|--------|
| Route | `/projects/:projectId/allocation` |
| Roles | **Project owner** or **admin** only (**Q12**) |
| Actions | List members; add user(s); remove member |
| Remove guard | **Q13** — block removal if member has open assigned tickets; show list of those tickets in UI |
| Entry | Link from project hub and project list — **Alocação** |
| UI | Reuse list/table patterns from [ui-elements-gallery.md](../docs/ui-elements-gallery.md) |

Assignee pickers elsewhere (create ticket, ticket detail, import) list **members only**.

### S1c — Project hub page (**Q15**)

Replaces home project grid as the entry to Kanban and dashboard.

| Aspect | Rule |
|--------|--------|
| Route | `/projects/:projectId` — **view** hub (refactor; edit moves to `/projects/:projectId/edit` or read-only mode) |
| Access | **Project members** and **admin**; not org-wide for non-members (**Q12**, **Q15**) |
| Content | Project name, description; actions **Kanban**, **Painel** (dashboard/stats) |
| Edit | **Editar** → project edit — **project owner** or **admin** only |
| Allocation | **Alocação** link — owner PM or **admin** |

Home ticket tables: **project** column links to project hub (`/projects/:id`).

### S2 — Tickets atuais (home section)

Open tickets in the user's home scope (**Q9**, **Q12**): member projects (`user`), owned projects (project owner), all projects (`admin`).

| Column | Notes |
|--------|-------|
| Identifier | Link to `/ticket/:identifier` |
| Title | |
| Project | Name |
| Status | Current workflow status |
| Priority | |
| Updated | `updatedAt` |

Sort: `updatedAt` descending. Empty state with guidance when no open tickets.

### S3 — Tickets atribuídos (home section)

Open tickets where `assignee` = current user, within scope above.

Same columns as S2. Distinct section from S2 (personal responsibility vs project-wide open work).

### S4 — Atividade (home section)

Static feed of recent events across scoped tickets:

| Event | Source |
|-------|--------|
| Comments | `Comment` |
| Status changes | `TicketHistory` where `action = STATUS_CHANGED` |

Newest first; no row cap (**Q6**). Reuse `.activity-feed` patterns from ticket detail. Click row → ticket detail.

**API:** e.g. `GET /home/activity` (or under `/tickets/home/activity`).

### S5 — Home layout

See **Wireframe** § `/` — personal work hub. Section-level loading states.

### S6 — Project access enforcement

| Surface | Rule |
|---------|------|
| `GET /projects` | Member projects (`user`); owned projects (project owner); all (`admin`) |
| `GET /projects/{id}` (hub) | Members + admin (**Q15**) |
| `GET /projects/{id}/edit`, update, allocation | **Project owner** or admin only (**Q12**) |
| Kanban, dashboard, phases, versions | Members + admin (via hub links) |
| `GET /tickets/search` | **Unchanged** — any authenticated user (**Q16**) |
| Home APIs | Scope per **Q9** / **Q12** |

## Impact

Reviewed after **Q1–Q16** answers. Scope: home hub + **project membership** + **project owner** + **project hub page** + allocation guards.

### Bounded contexts

| Context | Effect |
|---------|--------|
| `project` | M:N membership; **`owner_id`** on project; hub page; allocation; owner-scoped edit; filtered list |
| `ticket` | Home APIs; assignee ∈ member; open-ticket filter; member open-tickets query for allocation (**Q13**) |
| `user` | Membership target; assignee picker data source |
| `workflow` | Join finish statuses to exclude done/canceled from home queries |
| `dashboards` | Project dashboard behind membership gate |
| `phase` | Phase/version routes behind membership gate |
| `notifications` | **No change** — home activity is static (**Q4**, **Q7**) |

### API (new / changed)

| Endpoint | Purpose |
|----------|---------|
| `GET /projects/{id}/members` | List project members (allocation page) |
| `POST /projects/{id}/members` | Add member(s) |
| `DELETE /projects/{id}/members/{userId}` | Remove member — blocked if open assignee tickets (**Q13**) |
| `GET /projects/{id}/members/{userId}/open-tickets` | Open assigned tickets for member (allocation UI) (**Q13**) |
| `POST /projects` | Set **owner** at create (creator or explicit); owner + creator as members (**Q14**) |
| `PUT /projects/{id}` | **Project owner** or admin; may update fields including **owner** transfer (**Q17**) |
| `GET /home/tickets/current` | Tickets atuais |
| `GET /home/tickets/assigned` | Tickets atribuídos |
| `GET /home/activity` | Comments + status changes |
| `GET /projects` | Scope: member / owned / all by role |
| `GET /projects/{id}` | Hub — members + admin |
| Kanban, dashboard, phases, tickets under project | Members + admin |
| `GET /tickets/search` | **No change** (**Q16**) |

### UI (new / changed)

| Route / component | Effect |
|-------------------|--------|
| `/` | Personal hub; no project grid (**Q5**) |
| `/projects/:projectId` | **Project hub** — Kanban + Painel links; member view (**Q15**) |
| `/projects/:projectId/edit` | Project edit — owner PM or admin; **owner** picker for admin and current owner (**Q17**) |
| `/projects/:projectId/allocation` | Member allocation (**Q11**, **Q13** UI) |
| Home ticket tables | Project column → project hub |
| `app.routes.ts` | Hub vs edit routes; allocation; home resolver |

### Schema and seed

| Artifact | Effect |
|----------|--------|
| `V1.0.0__Database_Creation.sql` | `tb_project_members`; `tb_projects.owner_id` FK → `tb_users` |
| `dev-import.sql` | Owners per project; memberships; backfill assignees; owner as member (**Q14**) |

### Tests

| Area | Tests |
|------|-------|
| Membership API | `ProjectMemberEndpointTest` (TC1) |
| Assignee rule | Create, assign, import rejection (TC2) |
| Project list filter | `ListProjectsEndpointTest` (TC3) |
| Project access guard | `FindProjectByIdEndpointTest`, `UpdateProjectEndpointTest` | TC4 |
| Home APIs | `HomeTicketsEndpointTest`, `HomeActivityEndpointTest` | TC5, TC6 |
| Angular | `home`, `project-allocation`, project hub specs | TC7–TC9 |
| Contracts | `ArchitectureTest` | TC10 |

### Docs

| Doc | Update |
|-----|--------|
| `domain-specification.md` | Project member, allocation, home terms, invariants 24–25 (done) |
| `feature-catalog.md` | Home happy path; allocation route; project list steps |
| `README.md` | Features: home hub, project allocation |
| `ui-elements-gallery.md` | Home sections; allocation table if new pattern |
| `project-administration.md` | Cross-impact note (allocation page) |

### Impact review log

| Answer | Impact delta |
|--------|----------------|
| **Q1** | Introduced **project membership** — schema, APIs, domain invariants; home scope = member projects |
| **Q2** | Full lists, no pagination — performance risk elevated; no "Ver todos" links |
| **Q3** | Activity = comments + `STATUS_CHANGED` only; aggregated cross-ticket query |
| **Q4** | No SSE on home; no `notifications` changes |
| **Q5** | Remove project grid — navigation gap for Kanban (**Q15**); update feature-catalog Home row |
| **Q6** | Confirms unbounded queries — aligns with **Q2** performance risk |
| **Q7** | Home activity distinct from notification dropdown (static vs SSE/read) |
| **Q8** | All home queries join workflow finish statuses; exclude done/canceled |
| **Q9** | Role-split scope: `user` vs admin/PM; applies to home + project list; project URL access **Q12** |
| **Q10** | not valid — header out of scope |
| **Q11** | Dedicated allocation route/page; tasks T11, TC8 |
| **Q12** | **Supersedes Q9 for PM** — project **owner** model; `owner_id`; edit/allocation owner-only; no PM org-wide |
| **Q13** | Removal guard + open-tickets list API/UI on allocation |
| **Q14** | Creator + owner auto-member; owner eligible as assignee |
| **Q15** | **Project hub** page; refactor `/projects/:id` view vs edit |
| **Q16** | Search unchanged — drop search filter task |
| **Q17** | **Owner transfer** — admin + current owner; `ownerId` on update; edit UI owner picker |
| **Q18** | New owner need not be member beforehand; auto-add on transfer |

### Risks

- **Breaking change** — `owner_id` + memberships; seed backfill; route split for project edit.
- **Q9 / Q12 alignment** — docs and code must use **owner-scoped PM**, not platform-wide PM on home.
- **Performance** (**Q2**, **Q6**) — unbounded home lists and activity.
- **Member removal UX** (**Q13**) — PM must reassign tickets before removal.
- **Route refactor** (**Q15**) — `/projects/:id` becomes hub; edit URL changes (bookmark break).

### Open questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| Q1 | What defines **Tickets atuais**? | answered | Open tickets in projects the user is a **member** of |
| Q2 | Capped lists or all? | answered | **All** open tickets; exclude finished/canceled |
| Q3 | **Atividade** scope and event types? | answered | Tickets in **assigned projects**; **comments** + **status changes** only |
| Q4 | Real-time home activity? | answered | **No** — load once per visit |
| Q5 | Project grid on home? | answered | **Remove** |
| Q6 | Row limits? | answered | **No** limits |
| Q7 | Home vs notifications? | answered | Notifications: SSE + read state. Home: static, always shown |
| Q8 | Include finished/canceled? | answered | **No** |
| Q9 | PM/admin org-wide home? | answered | **Revised by Q12:** **admin** org-wide; **project owner** sees owned projects; **`user`** sees member projects |
| Q10 | Rename **Novo** → **Novo ticket**? | not valid | Header unchanged; out of scope |
| Q11 | Where is project membership managed in UI? | answered | Dedicated **project allocation** page (`/projects/:projectId/allocation`) |
| Q12 | Org-wide PM on direct project URLs? | answered | **No.** One **project owner** (PM role) per project, set at create. PM edits/updates/allocation **only owned** projects. Admin retains full access |
| Q13 | Remove member who is assignee on open tickets? | answered | **Forbid** until tickets reassigned. Allocation UI lists member's **open assigned tickets** |
| Q14 | Creator auto-added as member? | answered | **Yes.** Owner is also a member and may be ticket **assignee** |
| Q15 | Navigation to Kanban without home grid? | answered | **Project hub** at `/projects/:projectId` — all **members**; Kanban + stats links; edit for owner PM/admin |
| Q16 | Filter ticket search by membership? | answered | **No** — global search unchanged |
| Q17 | Can **admin** or owner **transfer project owner** after creation? | answered | **Yes** — **admin** and **current project owner** may reassign owner on edit; new owner must have `project-manager` role |
| Q18 | Must the **new owner** already be a project **member** before transfer? | answered | **No** — not required beforehand; system **adds** new owner as member on transfer |

**Gate:** all open questions **answered** or **not valid** — ready for phase 3 task approval.

## Changelog

### Personal work hub + project membership — 2026-07-03

**Version:** 1  
**Status:** tasks-ready

**Description:** Issue #3 — home shows tickets atuais, tickets atribuídos, and atividade; add project membership and enforce assignee ∈ members.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [project-administration](project-administration.md) | Owner on create + **transfer** on edit (**Q17**); hub vs edit routes; allocation |
| [ticket-management](ticket-management.md) | Assignee ∈ member; allocation open-tickets helper |
| [create-ticket](create-ticket.md) | Assignee picker → members; project list scoped |
| [ticket-import](ticket-import.md) | Assignee ∈ member validation |
| [kanban-board](kanban-board.md) | Entry via project hub; member access |
| [project-dashboard](project-dashboard.md) | Entry via project hub; member access |
| [phase-management](phase-management.md) | Member access on project routes |
| [ticket-search](ticket-search.md) | **None** (**Q16**) |
| [notifications](notifications.md) | **None** — home not SSE (**Q7**) |
| [user-management](user-management.md) | Users are allocation targets; no user CRUD change |
| [ui-design-system](ui-design-system.md) | Reuse `.activity-feed`, `.data-table`, `.page`; document home + allocation |
| [authentication](authentication.md) | **None** — uses existing JWT roles for scope split (**Q9**) |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | M:N project membership + `owner_id` on project | S1 | ☐ |
| FC2 | Assignee must be project member (create, assign, import) | S1 | ☐ |
| FC3 | Allocation page matches **Wireframe**; remove guard (**Q13**) | S1b, Wireframe | ☐ |
| FC4 | Project hub matches **Wireframe** (**Q15**) | S1c, Wireframe | ☐ |
| FC5 | Home hub matches **Wireframe** — no project grid (**Q5**) | S5, Wireframe | ☐ |
| FC6 | Tickets atuais — open tickets in scoped projects (**Q1**) | S2 | ☐ |
| FC7 | Tickets atribuídos — open assignee tickets (**Q2**) | S3 | ☐ |
| FC8 | Atividade — comments + status changes, static (**Q3**, **Q4**) | S4 | ☐ |
| FC9 | Access rules per S6 (**Q9**, **Q12**, **Q16**) | S6 | ☐ |
| FC10 | Owner transfer on edit (**Q17**, **Q18**) | S1, Wireframe | ☐ |
| FC11 | `domain-specification.md` — membership, home terms | Impact / Docs | ☐ |
| FC12 | `feature-catalog.md` — home, hub, allocation paths | Impact / Docs | ☐ |
| FC13 | `README.md` — home hub, allocation | Impact / Docs | ☐ |
| FC14 | `dev-import.sql` — owners, memberships (**Q14**) | T14 | ☐ |

#### Tasks (phase 2)

| ID | Task | Done |
|----|------|------|
| T1 | Flyway: `tb_project_members` + `tb_projects.owner_id` + JPA (`ProjectMember`, `Project.owner`) | ☐ |
| T2 | `ProjectMemberRepository` — members by project, projects by user, membership checks, open tickets by assignee | ☐ |
| T3 | `ProjectService` — owner on create; **owner transfer** (**Q17**); members; owner/admin update guard | ☐ |
| T4 | Member endpoints + open-assigned-tickets for allocation (**Q13**) + tests | ☐ |
| T5 | Enforce assignee ∈ members (create, assign, import) + tests | ☐ |
| T6 | Filter `listProjects` — member / owned / admin scopes (**Q9**, **Q12**) | ☐ |
| T6b | Access rules: hub (member+admin); edit/allocation (owner+admin); kanban/dashboard (member+admin) | ☐ |
| T7 | `GET` home tickets atuais + test | ☐ |
| T8 | `GET` home tickets atribuídos + test | ☐ |
| T9 | `GET` home activity + test | ☐ |
| T10 | Redesign `home.component`; project column → hub; `home.service` + resolver | ☐ |
| T11 | **Allocation** page — members, remove guard, open-tickets list (**Q13**) | ☐ |
| T12 | **Project hub** + **edit** (owner picker for admin/current owner per **Q17**); split routes (**Q15**) | ☐ |
| T13 | Assignee pickers → project members only | ☐ |
| T14 | `dev-import.sql` — owners, memberships, backfill (**Q14**) | ☐ |
| T15 | Docs: domain-spec, feature-catalog, README, ui-elements-gallery | ☐ |

#### Test coverage (phase 2)

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `ProjectMemberEndpointTest` (+ open-tickets list) | T4 | ☐ |
| TC2 | Assignee ∉ member rejected (create, assign, import) | T5 | ☐ |
| TC3 | `ListProjectsEndpointTest` — member / owned / admin | T6 | ☐ |
| TC4 | `UpdateProjectEndpointTest` — owner transfer (admin + owner); hub access | T3, T6b, T12 | ☐ |
| TC5 | `HomeTicketsEndpointTest` | T7, T8 | ☐ |
| TC6 | `HomeActivityEndpointTest` | T9 | ☐ |
| TC7 | `home.component.spec.ts` | T10 | ☐ |
| TC8 | `project-allocation.component.spec.ts` | T11 | ☐ |
| TC9 | `project-hub.component.spec.ts` (or projects view) | T12 | ☐ |
| TC10 | `ArchitectureTest` | New Request/Response records | ☐ |

**Development approval:** pending

**Implementation notes:** (pending)

# Phase and version management

**Feature version:** 4  
**Status:** planned  
**Requested:** 2026-07-03  
**Tracking:** [GitHub issue #6 — Gerenciamento de Sprints](https://github.com/vepo/issues/issues/6)

## Summary

Introduce **phase** and **version** planning for projects: time-boxed **phases** with lifecycle (planned → active → completed), a single plain-text **objective** and optional **deliverables** per phase, a **deliverable version** per phase, ticket fields for **phase assignment**, **observed version**, **target version**, and **finish date**, workflow extensions (**phase start status**, **finish statuses**), and a **version changelog** — a release-oriented view listing every ticket associated with a version. Vocabulary must stay **methodology-neutral** (no exclusive Scrum terms such as *sprint*, *backlog*, *velocity*).

This capability extends Issues beyond workflow status (Kanban columns) into release-oriented planning without prescribing Agile/Scrum process.

## Scope

### In scope (issue #6 + extensions)

| Capability | Description |
|------------|-------------|
| Create phases | Project-scoped phases with name, optional date range, **objective** (plain text), **deliverables**, deliverable version |
| Active phase | At most one **active** phase per project; activating a new phase **completes** the previous active phase |
| Phase lifecycle | **Planned** → **Active** → **Completed** (start = activate, finish = complete) |
| Phase edit after close | Completed phases remain **editable** (name, objective, deliverables, deliverable version, dates) |
| Assign ticket to phase | Optional FK on ticket; unassigned = not in any phase |
| Phase start status | Optional on **Workflow** (like `start`); on **activate**, move assigned tickets when a valid transition exists; **skip** others (phase still activates) |
| Finish statuses | **Workflow** defines terminal statuses as **done** or **canceled**; reaching one sets ticket **finish date** |
| Versions | Project-scoped catalog; labels validated as **SemVer** |
| Version changelog | Derived ticket list per version; **canceled** tickets excluded; sorted by **finish date** |
| Deliverable version | Each phase references one version as its intended delivery target |
| Ticket observed version | Optional FK — version where the change was observed/released |
| Ticket target version | Optional FK — version where the change is intended to land |
| Ticket finish date | Set on **done** finish status; **cleared** when ticket leaves a done finish status |
| Phase on ticket create | **No default**; optional combobox to assign a **planned** or **active** phase |
| Project planning settings | Template **objective** and **deliverables** on project; copied into each new phase |

### Out of scope (initial delivery — defer unless decided otherwise)

- Cross-project version catalogs or shared release trains
- Burndown, velocity, story points, capacity planning
- CSV import mapping for phase/version (follow-up on `ticket-import`)
- New notification types or email templates for phase events
- Dashboard widgets for phase/version metrics (slice 4+)
- Gantt or timeline views
- Full i18n (PT-BR labels only for now)

## Proposed ubiquitous language (draft for domain spec)

Terms below are **candidates** for `docs/domain-specification.md`. Final labels require domain-spec update before code.

| Term | Meaning | Avoid |
|------|---------|-------|
| **Phase** | Time-boxed planning period within a project | Sprint, iteration (Scrum-specific) |
| **Phase status** | `PLANNED`, `ACTIVE`, `COMPLETED` | Sprint started/closed jargon |
| **Active phase** | The single phase currently in progress for a project | Current sprint |
| **Activate phase** | Transition `PLANNED` → `ACTIVE`; **completes** any other active phase in the same project | Start sprint |
| **Complete phase** | Transition `ACTIVE` → `COMPLETED` | Finish sprint, close sprint |
| **Objective** | Single plain-text statement of the phase's main achievement (copied from project template on create) | Sprint goal, OKR list |
| **Deliverable** | Outcome statement attached to a phase (checklist item or text; copied from project template) | Increment |
| **Version** | SemVer release label scoped to a project (e.g. `1.2.0`) | Build number |
| **Version changelog** | Release notes view for a version: associated tickets in grouped sections, sorted by finish date | Release notes |
| **Changelog association** | Why a ticket appears: `TARGET`, `OBSERVED`, or `PHASE_DELIVERABLE` | — |
| **Deliverable version** | Version associated with a phase as its delivery target | Sprint goal version |
| **Observed version** | Version on a ticket indicating where the work was observed or shipped | Fix version |
| **Target version** | Version on a ticket indicating intended delivery | Affects version |
| **Start status** | Required initial workflow status for every new ticket | — |
| **Phase start status** | Optional workflow status; on phase activate, tickets with a valid transition move here; others unchanged | — |
| **Finish status** | Workflow status marked terminal: outcome **done** or **canceled** | Closed status |
| **Finish date** | Timestamp when ticket reaches a **done** finish status; cleared when leaving done | Resolved date |
| **Assignable phase** | Phase eligible for ticket assignment: `PLANNED` or `ACTIVE` (not `COMPLETED`) | — |
| **Unplanned ticket** | Ticket with no phase assignment | Backlog item |

### PT-BR UI labels (until i18n)

| Term | Label |
|------|-------|
| Phase | Fase |
| Objective | Objetivo |
| Deliverable | Entregável |
| Version | Versão |
| Observed version | Versão observada |
| Target version | Versão alvo |
| Finish date | Data de conclusão |
| Version changelog | Registro de alterações da versão |

### Proposed invariants (draft)

1. **One active phase per project** — activating phase B **completes** the previously active phase; never two `ACTIVE` phases in the same project.
2. **Phase and ticket same project** — a ticket's phase must belong to the ticket's project.
3. **Versions project-scoped** — observed/target/deliverable version FKs must reference versions of the ticket's project.
4. **Version label SemVer** — version labels must match SemVer (e.g. `1.0.0`, `2.1.3-beta.1`); unique per project.
5. **Phases editable after completed** — completed phases may be updated (objective, deliverables, deliverable version, dates, name).
6. **Phase start status optional** — if workflow defines `phase_start_id`, activating a phase moves each assigned ticket to that status **only when a valid transition exists**; tickets without a valid transition **keep their current status**; activation is **never blocked** by failed status moves.
7. **Finish statuses on workflow** — each workflow maintains a list of finish statuses, each tagged `DONE` or `CANCELED`.
8. **Finish date on done** — moving to a `DONE` finish status sets `finished_at`; moving **out of** a `DONE` finish status **clears** `finished_at`. `CANCELED` does not set finish date.
9. **Changelog excludes canceled** — tickets currently in a `CANCELED` finish status are excluded; **reappear** when moved out of canceled.
10. **History** — phase, observed version, target version, and finish date changes logged via `TicketHistoryService` (`FIELD_CHANGED` or dedicated actions).
11. **Version changelog membership** — a non-canceled ticket appears when **any** of: `target_version_id`, `observed_version_id`, or phase with matching `deliverable_version_id`. One row per ticket with all applicable association tags.
12. **Version changelog is derived** — no separate persistence; recomputed from tickets, phases, and workflow finish outcomes.
13. **Changelog sort** — tickets sorted by **finish date** ascending (nulls last).
14. **Ticket create — no default phase** — new tickets have `phase_id = null`; create form offers optional combobox of **planned** and **active** phases for the selected project.
15. **Roles** — phase/version CRUD: `PROJECT_MANAGER` (and `ADMIN`); ticket fields: same as ticket update; version changelog: read-only for authenticated users.

### Open questions

Reference by **Q*n*** in tasks and changelog. Status: `open` | `answered` | `not valid`.

| # | Question | Status | Answer |
|---|----------|--------|--------|
| Q1 | Does “objects” in the original request mean a separate entity? | answered | **Objective** — one plain-text field per phase describing the main achievement |
| Q2 | Can a project have multiple active phases at once? | answered | **Forbidden** — activating a new phase **completes** the previously active phase |
| Q3 | How does phase start status work on activate? | answered | Optional on **Workflow**; on activate, move tickets **when a valid transition exists**; **skip** others; phase always activates |
| Q4 | Can completed phases be edited? | answered | **Yes** — completed phases remain editable (name, objective, deliverables, deliverable version, dates) |
| Q5 | What format should version labels use? | answered | **SemVer** with unique-per-project constraint |
| Q6 | How should objectives and deliverables be stored in the database? | answered | `objective` as **TEXT** on `tb_phases` (and template on `tb_projects`); **deliverables** as normalized rows (`tb_phase_deliverables`, `tb_project_phase_deliverable_templates`) |
| Q7 | Should project planning templates apply to new phases? | answered | **Objective** + **deliverables** template on project; **copied** into each new phase (editable per phase) |
| Q8 | Should new tickets default to the active phase? | answered | **No default**; optional combobox on create listing **planned** and **active** phases; default selection empty |
| Q9 | What UI language should labels use? | answered | **PT-BR** for now; full i18n later |
| Q10 | What package name should own phase/version logic? | answered | **`phase`** (`dev.vepo.issues.phase.*`) |
| Q11 | How should the version changelog group tickets? | answered | **Grouped sections** (Planned / Shipped / Via phase) with association tags |
| Q12 | How should changelog sort and finish statuses work? | answered | Sort by **finish date**; workflow **finish statuses** (`DONE` / `CANCELED`); **done** sets finish date; **canceled** excluded from changelog |
| Q13 | What happens when a canceled ticket is reopened? | answered | **Reappears** on version changelog when moved out of canceled finish status |
| Q14 | Should finish date clear when leaving a done status? | answered | **Yes** — leaving a done finish status clears `finished_at` |
| Q15 | Should phase activation be blocked if some tickets cannot transition? | answered | **Activate anyway** — skip status move for tickets without valid transition to phase start status |

## Impact

| Area | Effect |
|------|--------|
| **Bounded contexts** | New **`phase`** context; extends **`workflow`** (phase start + finish statuses); extends **`ticket`** (FKs, finish date, move on phase activate); extends **`project`** (planning templates) |
| **Packages / files** | New `dev.vepo.issues.phase.*`; changes to `workflow.Workflow`, `ticket.Ticket`, `ticket.TicketService`, `ticket.move`, `project.Project` |
| **API** | `/projects/{projectId}/phases`, `/projects/{projectId}/versions`, version changelog; workflow create/update extended; ticket update extended |
| **UI** | Phase/version admin, version changelog, workflow editor (phase start + finish statuses), ticket detail fields |
| **Schema / seed** | `tb_phases`, `tb_versions`, deliverable tables, workflow columns/tables, ticket columns; `dev-import.sql` samples |
| **Tests** | `PhaseServiceTest`, `VersionServiceTest`, `WorkflowServiceTest` extensions, `MoveTicketEndpointTest`, `*EndpointTest` |
| **Docs** | `domain-specification.md`, `feature-catalog.md`, `README.md`, `ARCHITECTURE.md` |

### Bounded context placement

```
phase     →  may depend on: infra, project, workflow (validation only via services)
ticket    →  may depend on: phase (PhaseService / VersionService)
workflow  →  may depend on: infra only (phase start + finish status config)
project   →  may depend on: phase (template defaults)
```

**Decision:** package name **`phase`** (not `planning`).

### Schema (proposed)

```sql
-- Versions (project-scoped, SemVer label)
CREATE TABLE tb_versions (
    id          BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    project_id  BIGINT NOT NULL REFERENCES tb_projects,
    label       VARCHAR(64) NOT NULL,
    description TEXT,
    CONSTRAINT tb_versions_project_label_UK UNIQUE (project_id, label)
);

-- Phases
CREATE TABLE tb_phases (
    id                      BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    project_id              BIGINT NOT NULL REFERENCES tb_projects,
    name                    VARCHAR(128) NOT NULL,
    objective               TEXT,
    status                  VARCHAR(16) NOT NULL,  -- PLANNED | ACTIVE | COMPLETED
    start_date              DATE,
    end_date                DATE,
    deliverable_version_id  BIGINT REFERENCES tb_versions,
    created_at              TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    completed_at            TIMESTAMP(6) WITH TIME ZONE
);

-- Deliverables (objective is a column on tb_phases)
CREATE TABLE tb_phase_deliverables (
    id         BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    phase_id   BIGINT NOT NULL REFERENCES tb_phases,
    sort_order INT NOT NULL DEFAULT 0,
    text       TEXT NOT NULL
);

-- Project template — copied on phase create
ALTER TABLE tb_projects ADD COLUMN phase_template_objective TEXT;

CREATE TABLE tb_project_phase_deliverable_templates (
    id         BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES tb_projects,
    sort_order INT NOT NULL DEFAULT 0,
    text       TEXT NOT NULL
);

-- Workflow extensions (see Workflow.java — today only start_id)
ALTER TABLE tb_workflows ADD COLUMN phase_start_id BIGINT REFERENCES tb_workflow_status;

CREATE TABLE tb_workflow_finish_statuses (
    workflow_id BIGINT NOT NULL REFERENCES tb_workflows,
    status_id   BIGINT NOT NULL REFERENCES tb_workflow_status,
    outcome     VARCHAR(16) NOT NULL,  -- DONE | CANCELED
    PRIMARY KEY (workflow_id, status_id)
);

-- Ticket extensions
ALTER TABLE tb_tickets ADD COLUMN phase_id BIGINT REFERENCES tb_phases;
ALTER TABLE tb_tickets ADD COLUMN observed_version_id BIGINT REFERENCES tb_versions;
ALTER TABLE tb_tickets ADD COLUMN target_version_id BIGINT REFERENCES tb_versions;
ALTER TABLE tb_tickets ADD COLUMN finished_at TIMESTAMP(6) WITH TIME ZONE;
```

### API (proposed)

| Operation | Method / path | Role |
|-----------|---------------|------|
| List versions | `GET /projects/{projectId}/versions` | authenticated |
| Create version | `POST /projects/{projectId}/versions` | project-manager |
| Update version | `POST /projects/{projectId}/versions/{versionId}` | project-manager |
| Find version | `GET /projects/{projectId}/versions/{versionId}` | authenticated |
| Version changelog | `GET /projects/{projectId}/versions/{versionId}/changelog` | authenticated |
| List phases | `GET /projects/{projectId}/phases` | authenticated |
| Find phase | `GET /projects/{projectId}/phases/{phaseId}` | authenticated |
| Create phase | `POST /projects/{projectId}/phases` | project-manager |
| Update phase | `POST /projects/{projectId}/phases/{phaseId}` | project-manager (including completed) |
| Activate phase | `POST /projects/{projectId}/phases/{phaseId}/activate` | project-manager |
| Complete phase | `POST /projects/{projectId}/phases/{phaseId}/complete` | project-manager |
| Active phase | `GET /projects/{projectId}/phases/active` | authenticated |
| Update workflow (extended) | existing workflow endpoints — add `phaseStartStatusId`, `finishStatuses[]` | project-manager |
| Update ticket (extended) | existing `POST /tickets/{id}` — `phaseId`, `observedVersionId`, `targetVersionId` | ticket editor |
| Create ticket (extended) | existing create — optional `phaseId`; combobox lists **planned** + **active** phases | authenticated |
| Move ticket (extended) | existing move — sets/clears `finished_at` on done finish status transitions | authenticated |

One endpoint class per row per [issues-layered-architecture.mdc](../.cursor/rules/issues-layered-architecture.mdc).

| Route | Feature | Roles |
|-------|---------|-------|
| `/project/:projectId/phases` | Phase list (status badges, active highlight) | authenticated |
| `/project/:projectId/phases/new` | Create phase (objective, deliverables, deliverable version) | project-manager |
| `/project/:projectId/phases/:phaseId` | Phase detail / edit / activate / complete (editable when completed) | project-manager |
| `/project/:projectId/versions` | Version catalog list | authenticated |
| `/project/:projectId/versions/new` | Create version (SemVer) | project-manager |
| `/project/:projectId/versions/:versionId` | Version detail + **changelog** (grouped sections, sorted by finish date) | authenticated |
| `/tickets/new`, `/project/:projectId/tickets/new` | Optional **phase** combobox (planned + active phases) | authenticated |
| `/ticket/:ticketIdentifier` | Phase, versions, finish date | authenticated |
| `/workflows/:workflowId` | Phase start status + finish statuses (done/canceled) | project-manager |
| `/project/:projectId/kanban` | Optional filter: active phase / all / unplanned | authenticated |

### Cross-feature impact

| Feature / area | Impact |
|----------------|--------|
| **Workflow editor** | Phase start status selector; finish status list with done/canceled outcome |
| **Move ticket** | Sets/clears `finished_at` on done finish status transitions; changelog updates |
| **Activate phase** | Best-effort move to phase start status; skips tickets without valid transition |
| **Kanban board** | Optional phase filter; phase badge on cards |
| **Ticket detail** | New fields + history entries; finish date display |
| **Version changelog** | Excludes canceled (reincludes on reopen); grouped sections; sort by finish date |
| **Create ticket** | No default phase; optional combobox for planned/active phases |
| **CSV import** | No change in initial slices |
| **Project edit** | Phase template section (objective + deliverables) |

## Delivery slices (for planning phase)

| Slice | Deliverable | Depends on |
|-------|-------------|------------|
| **1 — Workflow finish + ticket finish date** | Finish statuses on workflow; `finished_at` on move; workflow editor UI | Domain spec |
| **2 — Versions + changelog** | Version CRUD (SemVer); observed/target on ticket; changelog (grouped, finish-date sort, exclude canceled) | Slice 1 |
| **3 — Phase lifecycle** | Phase CRUD; activate (close previous + optional status move); complete; deliverable version; ticket phase assignment | Slice 2 |
| **4 — Templates & board** | Project phase template; create-ticket phase combobox; Kanban phase filter | Slice 3 |

## Version changelog — response shape (proposed)

```java
public record VersionChangelogEntry(
    long ticketId,
    String identifier,
    String title,
    String statusName,
    String priority,
    LocalDateTime finishedAt,
    Set<ChangelogAssociation> associations  // TARGET, OBSERVED, PHASE_DELIVERABLE
) {}

public record VersionChangelogSection(
    String name,  // e.g. "Planejado", "Entregue", "Via fase"
    List<VersionChangelogEntry> tickets
) {}

public record VersionChangelogResponse(
    long versionId,
    String label,
    String description,
    List<VersionChangelogSection> sections
) {}
```

Query (conceptual): union of non-deleted tickets where association matches, **excluding** tickets whose current status is a workflow finish status with outcome `CANCELED`. Order by `finished_at ASC NULLS LAST` within each section.

## Changelog

### Phase and version management (initial) — 2026-07-03

**Version:** 1  
**Status:** planned

**Description:** Full capability per issue #6 and extended requirements: phases with lifecycle, versions, version changelog, deliverable version per phase, ticket observed/target version, objective and deliverables. Methodology-neutral naming throughout.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Ticket detail | New optional fields; history entries |
| Kanban | Optional phase filter and card badge (slice 4) |
| Project admin | New phase/version routes; phase template |
| CSV import | None in initial slices |
| Notifications | None identified |
| Workflow | None identified (superseded by decisions entry below) |

**Implementation notes:** _(pending — fill after each slice)_

### Version changelog — 2026-07-03

**Version:** 2  
**Status:** planned

**Description:** Each version exposes a changelog: associated tickets in grouped sections, sorted by finish date, excluding canceled.

**Implementation notes:** _(pending)_

### Product decisions — 2026-07-03

**Version:** 3  
**Status:** planned

**Description:** Resolved **Q1**–**Q12**: single objective text; one active phase; phase start status; phases editable after close; SemVer; `phase` package; PT-BR UI; workflow finish statuses with ticket finish date; canceled excluded from changelog.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Workflow | New `phase_start_id` and `tb_workflow_finish_statuses` |
| Move ticket | Sets `finished_at`; affects changelog eligibility |
| Activate phase | May bulk-move tickets to phase start status |
| Version changelog | Sort by finish date; exclude canceled |

**Implementation notes:** _(pending)_

### Final product decisions — 2026-07-03

**Version:** 4  
**Status:** planned

**Description:** Resolved **Q8**, **Q13**–**Q15**: ticket create has no default phase (optional combobox); reopen canceled → back on changelog; clear finish date when leaving done; phase activate proceeds when some tickets cannot transition.

**Implementation notes:** _(pending)_

---

## Next steps

1. **Update `docs/domain-specification.md`** — Ubiquitous Language, workflow extensions, `phase` bounded context.
2. **Technical planning** — slice 1 task breakdown (workflow finish statuses first).
3. **TDD** — failing tests per slice; no production code before domain spec is updated.

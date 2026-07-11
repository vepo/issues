# Custom fields

**Feature version:** 2  
**Status:** done  
**Requested:** 2026-07-10

## Summary

Projects and workflows define **custom fields** — named, typed attributes beyond built-in ticket fields. Types: **String** (per-field max ≤ platform cap **255**), **Text** (same storage/editor model as **Description**, max **1200**), **Integer** (optional min/max), **Boolean** (checkbox; optional may be null), **Enum** (**single-select**).

**Both** project and workflow own definitions; a ticket sees their **union**. **Keys** must be unique across that union for a project (**FQ18**). Values are edited on **create ticket** and **ticket detail** (not Kanban). Fields may be **required** (always on create/update); workflow fields may also be **status-required** (enforced on move into listed statuses, and on create if start status is listed). Globally required workflow fields imply all statuses (**FQ19**).

**Ticket template** may default in-scope custom fields; stale defaults drop when workflow changes. **CSV import** maps by **key**. **Query language** uses `cf.<key>` with type-appropriate operators. History logs `FIELD_CHANGED` with the field **key**. No notification/email for custom field changes in v1.

## Wireframe

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-11 |

### Screen: Project edit — custom fields + template defaults (`/projects/:projectId/edit`)

| Region | Elements | Notes |
|--------|----------|-------|
| Section | **Campos personalizados (projeto)** | Project owner / admin (**FQ13**) |
| List | Key, label, type, required, enabled, actions | Soft-disable; hard delete blocked if values (**FQ14**) |
| Template | Built-in + defaults for **in-scope** custom fields only (**FQ21**) | |

```
┌─────────────────────────────────────────────────────────┐
│  Editar projeto                                          │
├─────────────────────────────────────────────────────────┤
│  … name / workflow / prefix …                           │
│  ☐ Usar template de ticket                              │
│  … built-in template …                                  │
│  Template — campos personalizados                       │
│  │ sprint [ 1 ]  environment [ Homolog ▼ ]              │
│  Campos personalizados (projeto)                        │
│  │ key │ label │ tipo │ obr. │ [Editar] [Desativar]     │
│  [ Adicionar campo ]                                    │
│                         [Cancelar]  [Salvar]            │
└─────────────────────────────────────────────────────────┘
```

### Screen: Workflow edit — custom fields (`/workflows/:workflowId`)

| Region | Elements | Notes |
|--------|----------|-------|
| Section | **Campos personalizados (processo)** | PM / admin |
| List | key, label, type, required, status-required, enabled | |

```
┌─────────────────────────────────────────────────────────┐
│  Editar processo                                        │
├─────────────────────────────────────────────────────────┤
│  … statuses / transitions / WIP …                       │
│  Campos personalizados (processo)                       │
│  │ key │ label │ tipo │ obr. │ status obr. │ [Editar]   │
│  [ Adicionar campo ]                                    │
└─────────────────────────────────────────────────────────┘
```

### Screen: Workflow create — custom fields (disabled) (`/workflows/new`)

| Region | Elements | Notes |
|--------|----------|-------|
| Section | **Campos personalizados (processo)** | Always visible on create (**FQ25**) |
| Actions | **Adicionar campo** disabled | Nested API needs workflow id |
| Hint | `.form-hint` explaining save-first | Do not hide the section |

```
┌─────────────────────────────────────────────────────────┐
│  Novo processo                                          │
├─────────────────────────────────────────────────────────┤
│  … statuses / transitions / WIP …                       │
│  Campos personalizados (processo)                       │
│  [ Adicionar campo ]  (disabled)                        │
│  Salve o processo antes de adicionar campos             │
│  personalizados.                                        │
│                         [Cancelar]  [Salvar]            │
└─────────────────────────────────────────────────────────┘
```

### Screen: Add / edit custom field (dialog)

| Region | Elements | Notes |
|--------|----------|-------|
| Common | **Key** (immutable after create), **label**, type, required, enabled | Key: stable machine id (**FQ12**) |
| Workflow | **Obrigatório nos status** multi-select | Status names from workflow |
| String | Max length (1–255) | |
| Text | (no extra; max 1200 like Description) | Same textarea/editor as Description |
| Integer | Min, max optional | |
| Boolean | — | Checkbox; optional → null |
| Enum | Ordered options; remove blocked if in use | Single-select |

### Screen: Create ticket / Ticket detail

Dynamic **Campos personalizados** after built-ins; template pre-fill on create; orphaned values (former workflow) read-only on detail until cleared (**FQ15**).

### Screen: CSV import — column mapping

Built-in columns + custom field **keys** for target project scope (**FQ22**). Global import: map keys; row invalid if project lacks that key.

### Screen: Search

Query help documents `cf.<key>` predicates (**FQ23**). No Kanban card display (**FQ20**).

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | New **Custom fields** context `customfield`; `project`/`workflow` nest definition endpoints; `ticket` (values, move, import, query, history) depends on `customfield` |
| Packages / files | `dev.vepo.issues.customfield.*`; nested `project.customfield.*`, `workflow.customfield.*`; ticket create/update/move/import/query changes; Angular project/workflow/ticket/import/search |
| API | Nested definition CRUD; in-scope list; extend ticket + template + import mapping + query |
| UI | Project/workflow field admin; create/detail dynamic forms; import mapping; search help |
| Schema / seed | New tables in `V1.0.0`; widen `tb_ticket_history.field`; `dev-import.sql` samples |
| Tests | Endpoint + query + import + history + Angular specs; `ArchitectureTest` for new Request/Response |
| Docs | domain-spec, feature-catalog, ARCHITECTURE §13, README |

### Risks

- Key collision when changing project workflow (**FQ18** / **FQ15**) — reject workflow change on collision; orphan values for removed workflow fields.
- Import/query expand test matrix.
- History `field` column length vs custom field keys — widen to 64 (**AQ3**).

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Ownership? | answered | **Both** project and workflow |
| FQ2 | Required? | answered | **Yes**; workflow fields may be **status-required** |
| FQ3 | Edit surfaces? | answered | **Create + ticket detail** |
| FQ4 | Enum options lifecycle? | answered | Add/reorder/rename OK; **block remove if in use** |
| FQ5 | Template defaults? | answered | **Yes** |
| FQ6 | CSV import? | answered | **Yes** |
| FQ7 | Query language? | answered | **Yes** |
| FQ8 | TEXT editor? | answered | **Same as Description** (shared UI/storage model) |
| FQ9 | STRING platform cap? | answered | Per-field max + platform hard cap **255** |
| FQ10 | Boolean UI / null? | answered | **Checkbox**; optional may be **null** |
| FQ11 | Enum multi-select? | answered | **Single-select only** in v1 |
| FQ12 | Key vs label? | answered | **Yes** — stable **key** + display **label**; key immutable after create |
| FQ13 | Roles? | answered | Project fields: **project owner or admin**. Workflow fields: **PM or admin**. Values: anyone who may create/update the ticket |
| FQ14 | Delete definition with values? | answered | **Block hard delete** if any ticket has a value; allow **soft-disable** (`enabled=false`, hidden from forms, values retained) |
| FQ15 | Project changes workflow? | answered | Orphan former workflow values: **keep, read-only** until cleared. Reject workflow change if project field **keys** collide with new workflow fields. Drop stale template custom defaults (**FQ21**) |
| FQ16 | History? | answered | **Yes** — `FIELD_CHANGED` with `field` = custom field **key** |
| FQ17 | Notify/email? | answered | **No** in v1 |
| FQ18 | Combine / collision? | answered | **Union**; **reject duplicate keys** across project ∪ workflow for a project |
| FQ19 | Required semantics? | answered | (a) globally required → enforce on **create + update**; (b) status-required → enforce on **move into** that status; (c) **yes** on create if start status is listed; (d) globally required workflow field **implies all statuses** |
| FQ20 | Kanban cards? | answered | **No** in v1 |
| FQ21 | Template scope? | answered | Defaults only for **in-scope** fields; **drop stale** when workflow changes |
| FQ22 | Import identity? | answered | Map by custom field **key**; global import validates key exists on row’s project scope |
| FQ23 | Query syntax? | answered | `cf.<key>` with type-appropriate operators (`=`, `!=`, `~`, comparisons, `IS EMPTY`, `IN` as applicable) |
| FQ24 | TEXT max size? | answered | **Same as Description** — max **1200** characters |
| FQ25 | Custom fields on **new** workflow? | answered | **Show** the section; keep **Adicionar campo** disabled; explain that the process must be **saved** first (definitions require a workflow id) |

**Gate:** all blocking **FQ*n*** answered.

## Architecture

| Area | Design |
|------|--------|
| Bounded contexts | **Custom fields** (`customfield`) owns definitions, enum options, status-required links, values, validation — depends on **platform only** (FKs by id). `project` / `workflow` expose nested HTTP and call `CustomFieldService`; `ticket` calls it on create/update/move/import/query. Dependency: `project`→`customfield`, `workflow`→`customfield`, `ticket`→`customfield` |

| Packages / layers | See below |
| API | Nested definition CRUD; `GET …/custom-fields/in-scope`; extend ticket/template/import/query payloads |
| Schema / seed | Tables below; amend `V1.0.0`; sample fields in `dev-import.sql` |
| Cross-context | No CDI events for v1 (no notify). Ticket history via existing `TicketHistoryService` |
| Frontend | Field admin on project/workflow forms; shared dynamic field renderer; import mapping keys; search help for `cf.*` |
| Tests | `*CustomField*EndpointTest`, ticket create/update/move tests, import + `TicketQueryLanguageServiceTest`, Angular specs |

### Packages / layers

| Layer | Type | Responsibility |
|-------|------|----------------|
| Entity | `customfield.CustomField`, `EnumOption`, `CustomFieldValue`, template default entity | Persistence model |
| Enum | `CustomFieldType` (`STRING`,`TEXT`,`INTEGER`,`BOOLEAN`,`ENUM`) | |
| Repository | `CustomFieldRepository`, `CustomFieldValueRepository` | Queries; in-use checks |
| Service | `CustomFieldService` | Definition CRUD, key collision, enable/disable, value validate/apply, in-scope resolution, status-required checks, template defaults |
| Endpoint | `project.customfield.*`, `workflow.customfield.*` | One HTTP op per class |
| Ticket | Extend `TicketService` create/update/move | Delegate values + required checks |
| Import | Extend mapping + executor | Keys → values |
| Query | `TicketQuery.g4` + `TicketQueryPredicateBuilder` | `cf.<key>` |

### Schema (amend `V1.0.0`)

```
tb_custom_fields (
  id, key VARCHAR(32) NOT NULL, label VARCHAR(128) NOT NULL,
  type VARCHAR(16) NOT NULL, required BOOLEAN NOT NULL DEFAULT FALSE,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  project_id NULL, workflow_id NULL,  -- exactly one set
  string_max_length INT NULL,
  integer_min INT NULL, integer_max INT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  UNIQUE (project_id, key) WHERE project_id IS NOT NULL,
  UNIQUE (workflow_id, key) WHERE workflow_id IS NOT NULL,
  CHECK (project_id IS NOT NULL) <> (workflow_id IS NOT NULL)
)
tb_custom_field_enum_options (id, custom_field_id, value VARCHAR(128), label VARCHAR(128), sort_order)
tb_custom_field_status_required (custom_field_id, status_id)  -- workflow fields only; FK workflow status
tb_ticket_custom_field_values (
  ticket_id, custom_field_id,
  string_value VARCHAR(255) NULL,
  text_value TEXT NULL,
  integer_value INT NULL,
  boolean_value BOOLEAN NULL,  -- null = unset
  enum_option_id NULL,
  PRIMARY KEY (ticket_id, custom_field_id)
)
tb_project_ticket_template_custom_values (
  project_id, custom_field_id, … same typed columns as values …
)
```

Also: `tb_ticket_history.field` → `VARCHAR(64)`. Import: JSON column `custom_field_column_mapping` on `tb_ticket_imports` (`Map<key, headerName>`); row storage for custom values (JSON on `tb_ticket_import_rows` or child table).

### API surface

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| `GET` | `/projects/{id}/custom-fields` | viewable project | Project-owned definitions |
| `GET` | `/projects/{id}/custom-fields/in-scope` | viewable | Project ∪ workflow enabled fields |
| `POST` | `/projects/{id}/custom-fields` | owner/admin | Create; reject key collision with workflow |
| `PUT` | `/projects/{id}/custom-fields/{fieldId}` | owner/admin | Update label/config/required/enabled; not key/type |
| `DELETE` | `/projects/{id}/custom-fields/{fieldId}` | owner/admin | 400 if any values exist |
| `GET/POST/PUT/DELETE` | `/workflows/{id}/custom-fields[/{fieldId}]` | PM/admin | Same; status-required on create/update |
| — | Extend `CreateProjectRequest` / `ProjectResponse` ticket template | | `customFieldDefaults: [{key, value}]` |
| — | Extend `CreateTicketRequest`, `UpdateTicketRequest`, ticket responses | | `customFields: [{key, value}]` |
| — | `MoveTicketEndpoint` unchanged path | | Service validates status-required |
| — | Import mapping request | | `customFieldColumns: { key: header }` |
| — | Query | | `cf.sprint = 12` |

**Records:** `CustomFieldRequest`, `CustomFieldResponse`, `CustomFieldValueRequest`, `CustomFieldValueResponse`, `EnumOptionRequest`/`Response`, etc. (no VO/DTO suffix).

### Validation rules (service)

1. In-scope = enabled project fields ∪ enabled workflow fields for ticket’s project.
2. Key uniqueness across that union (**FQ18**); enforce on definition create and on project workflow change.
3. Type checks; STRING length ≤ min(configured, 255); TEXT ≤ 1200; INTEGER min/max; ENUM ∈ options; BOOLEAN null only if not required.
4. Globally required: non-null valid value on create/update.
5. Status-required: on move to status S, and on create if start ∈ required statuses; globally required workflow field ⇒ all statuses.
6. Orphan values (field not in scope / disabled): retained; not shown as editable; clear via explicit clear/update when product allows removing orphan — detail shows read-only (**FQ15**).
7. Enum option delete: block if referenced by any value.
8. Field hard delete: block if any `tb_ticket_custom_field_values` row (including soft-deleted tickets).

### Frontend

| Area | Approach |
|------|----------|
| Shared | `CustomFieldFormSectionComponent` — renders controls by type (checkbox, number, text, textarea, select) |
| Project / workflow | Definition list + dialog; workflow status multi-select |
| Create / detail | Load in-scope; bind `customFields` map by key |
| Import | Extra mapping rows for keys from in-scope (project-scoped) or documented keys (global) |
| Search | Document `cf.<key>` in query help |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Shared `customfield` package vs split under project/workflow/ticket? | answered | **Shared `customfield`** package; nested HTTP under project/workflow paths |
| AQ2 | Value storage: typed columns vs JSON? | answered | **Typed columns** on `tb_ticket_custom_field_values` (+ template table) |
| AQ3 | History `field` VARCHAR(32) too short? | answered | Widen to **VARCHAR(64)**; store custom field **key** |
| AQ4 | Status-required: store status id or name? | answered | Store **status_id** (FK); API accepts status **name** like WIP limits and resolves |
| AQ5 | Definitions embedded in project/workflow update vs separate endpoints? | answered | **Separate nested CRUD** endpoints (clearer auth and validation); template defaults remain on project create/update |
| AQ6 | Query `cf.key` with dot — grammar change? | answered | Extend grammar (or lexer) to allow `cf.` + IDENTIFIER; predicate builder resolves key and joins values |
| AQ7 | Type change after create? | answered | **Forbidden** — type and key immutable; create new field instead |

**Gate:** blocking **AQ*n*** answered. Changelog status → `architecture-ready` then task-broken to `tasks-ready`.

## Changelog

### Show disabled custom fields on new workflow — 2026-07-11

**Version:** 2  
**Status:** done

**Description:** On **Novo processo**, keep the **Campos personalizados (processo)** section visible but disabled, with a clear hint that fields can only be added after the workflow is saved (API is nested under `/workflows/{id}/custom-fields`).

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Workflow create (`/workflows/new`) | Section always shown; add disabled + hint (**FQ25**) |
| Workflow edit | Unchanged (section already interactive) |
| Project create/edit | None (projects already exist before edit UI) |
| API / schema | None |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC20 | Create workflow form shows **Campos personalizados (processo)** | FQ25, Wireframe | ☑ |
| FC21 | **Adicionar campo** is disabled when there is no workflow id | FQ25 | ☑ |
| FC22 | Hint explains save-first (PT-BR) | FQ25, Wireframe | ☑ |
| FC23 | Edit workflow still allows add/edit as today | Regression | ☑ |
| FC24 | Wireframe + feature-catalog note updated | Docs | ☑ |

#### Architecture (this entry)

| Area | Design |
|------|--------|
| Bounded contexts | UI only — `workflow` form + shared `custom-field-admin`; no backend change |
| Packages / layers | `workflow-form.component.html` always renders `app-custom-field-admin`; pass `ownerId` null on create |
| API / schema | None — definitions remain nested under persisted workflow id |
| Frontend | `CustomFieldAdminComponent`: when `ownerId` is null, show save-first `.form-hint`, keep add button disabled, skip list load |
| Tests | Angular spec: create mode shows disabled add + hint; edit mode unchanged |

No blocking **AQ*n*** for this entry (UI-only; nested CRUD already decided in **AQ5**).

#### Tasks

| ID | Task | Done |
|----|------|------|
| T16 | Always render `app-custom-field-admin` on workflow form (create + edit); pass `null` `ownerId` on create | ☑ |
| T17 | `CustomFieldAdminComponent`: when `ownerId` is null, show PT-BR save-first hint; keep **Adicionar campo** disabled | ☑ |
| T18 | Angular unit test for create (disabled + hint) and edit still interactive | ☑ |
| T19 | Update feature-catalog create-workflow steps; confirm Wireframe above | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC10 | Angular: workflow form / custom-field-admin — create shows disabled add + hint | T16, T17 | ☑ |
| TC11 | Angular: with `ownerId` set, add remains enabled (smoke) | T17 | ☑ |

**Development approval:** approved 2026-07-11 — tasks: T16, T17, T18, T19

**Implementation notes:** Shipped 2026-07-11.

- Workflow form always renders `app-custom-field-admin`; create passes `ownerId=null`.
- Save-first `.form-hint`: *Salve o processo antes de adicionar campos personalizados.*
- Tests: `custom-field-admin.component.spec.ts` (2/2); `npm run build` green.
- Docs: feature-catalog create-workflow steps.

**Version:** 1  
**Status:** done

**Description:** Typed custom fields on projects and workflows; values on tickets; required + status-required; template defaults; CSV import by key; query `cf.<key>`; history; soft-disable / block delete-if-in-use.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Project administration | Definition CRUD + template custom defaults |
| Workflow configuration | Definition CRUD + status-required |
| Create ticket | Dynamic fields + template pre-fill + required |
| Ticket management | Detail edit; move status-required; history; orphan read-only |
| Ticket import | Mapping + validation by key |
| Ticket search | `cf.<key>` predicates |
| Kanban board | **None** (FQ20) |
| Notifications / email | **None** (FQ17) |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Project can define custom fields (all five types) | S1 | ☑ |
| FC2 | Workflow can define custom fields + status-required | S2, FQ2 | ☑ |
| FC3 | String max ≤ configured and ≤ 255 | FQ9 | ☑ |
| FC4 | Text uses Description editor/storage model, max 1200 | FQ8, FQ24 | ☑ |
| FC5 | Integer min/max enforced | S3 | ☑ |
| FC6 | Enum single-select; option remove blocked if in use | FQ4, FQ11 | ☑ |
| FC7 | Create + detail persist in-scope values; union by key | FQ3, FQ18 | ☑ |
| FC8 | Required on create/update; status-required on move/create-as-start | FQ19 | ☑ |
| FC9 | `FIELD_CHANGED` history with field key | FQ16 | ☑ |
| FC10 | Template defaults for in-scope fields; stale dropped on workflow change | FQ5, FQ21 | ☑ |
| FC11 | CSV import maps by key | FQ6, FQ22 | ☑ |
| FC12 | Query supports `cf.<key>` | FQ7, FQ23 | ☑ |
| FC13 | Soft-disable; hard delete blocked if values | FQ14 | ☑ |
| FC14 | Orphan workflow values read-only after workflow change | FQ15 | ☑ |
| FC15 | Duplicate keys rejected across project ∪ workflow | FQ18 | ☑ |
| FC16 | Roles: owner/admin project defs; PM/admin workflow; ticket editors for values | FQ13 | ☑ |
| FC17 | No Kanban card custom fields; no notify/email for CF changes | FQ17, FQ20 | ☑ |
| FC18 | UI matches **Wireframe** | Wireframe | ☑ |
| FC19 | domain-spec / feature-catalog / README / ARCHITECTURE updated | Docs | ☑ |

#### Tasks (phase 3)

| ID | Task | Done |
|----|------|------|
| T1 | Schema: custom field tables + history `field` widen + import JSON columns in `V1.0.0`; entities + repositories | ☑ |
| T2 | `CustomFieldService` — definition CRUD, key collision, enable/disable, enum option guards | ☑ |
| T3 | Project nested endpoints: list / in-scope / create / update / delete + tests | ☑ |
| T4 | Workflow nested endpoints: list / create / update / delete (status-required) + tests | ☑ |
| T5 | Ticket values on create/update/detail responses; required validation; history `FIELD_CHANGED` + tests | ☑ |
| T6 | `moveTicket` status-required validation + tests | ☑ |
| T7 | Project template custom defaults (API + apply on create) + workflow-change collision/orphan/stale-default handling + tests | ☑ |
| T8 | CSV import mapping + execution for custom field keys + tests | ☑ |
| T9 | Query language `cf.<key>` grammar + predicate builder + tests | ☑ |
| T10 | `dev-import.sql` sample custom fields/values | ☑ |
| T11 | Angular: definition admin on project + workflow forms | ☑ |
| T12 | Angular: shared field renderer on create ticket + ticket detail | ☑ |
| T13 | Angular: import mapping UI for custom field keys | ☑ |
| T14 | Angular: search help for `cf.*`; regenerate API client | ☑ |
| T15 | Docs: domain-spec finalize, feature-catalog, README, ARCHITECTURE §13 | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `CreateProjectCustomFieldEndpointTest` / update / delete (collision, delete-if-in-use, disable) | T2, T3 | ☑ |
| TC2 | `CreateWorkflowCustomFieldEndpointTest` (status-required) | T2, T4 | ☑ |
| TC3 | `CreateTicketEndpointTest` / update — custom values, required, template defaults | T5, T7 | ☑ |
| TC4 | `MoveTicketEndpointTest` — status-required reject/accept | T6 | ☑ |
| TC5 | `UpdateProjectEndpointTest` — workflow change collision; template custom defaults | T7 | ☑ |
| TC6 | CSV import mapping/execute tests with custom field keys | T8 | ☑ |
| TC7 | `TicketQueryLanguageServiceTest` — `cf.<key>` predicates | T9 | ☑ |
| TC8 | `ArchitectureTest` still green for new Request/Response records | T3–T5 | ☑ |
| TC9 | Angular specs: field admin, ticket form section, import mapping | T11–T13 | ☑ |

**Development approval:** approved 2026-07-10 — tasks: T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15

**Implementation notes:** Shipped 2026-07-10.

- **Backend:** `customfield.CustomFieldService` (+ entities/repos); nested `project.customfield.*` / `workflow.customfield.*` endpoints; ticket create/update/move + template defaults / workflow-change collision & stale-default drop in `ProjectService`; CSV `customFieldColumns`; query grammar `cf.<key>`.
- **Frontend:** `custom-field-admin` / `custom-field-dialog` on project + workflow forms; `custom-field-form-section` on create + detail (orphan read-only); import mapping; `query-language-reference` `cf.<chave>`; API codegen.
- **Seed:** `dev-import.sql` sample project/workflow fields and ticket values.
- **Tests run / present:** `CreateProjectCustomFieldEndpointTest`, `CreateWorkflowCustomFieldEndpointTest`, `CreateTicketEndpointTest` (CF), `MoveTicketEndpointTest` (status-required), `UpdateProjectEndpointTest` (workflow-change collision + template CF defaults), `ImportTicketsEndpointTest` (customFieldColumns), `TicketQueryLanguageServiceTest` (`cf.*`), Angular `custom-field-form-section.component.spec.ts`, `ticket-import-wizard.component.spec.ts` (CF mapping). `mvn verify` green.
- **Docs:** domain-spec UL/invariants, feature-catalog routes, README Features, ARCHITECTURE §5/§7/§13, backlog `done`.

## Scope (product)

| ID | Scope |
|----|--------|
| S1 | Project custom field definitions |
| S2 | Workflow custom field definitions + status-required |
| S3 | Types: String, Text, Integer, Boolean, Enum (single) |
| S4 | Values on create + detail |
| S5 | Template defaults |
| S6 | CSV import by key |
| S7 | Query `cf.<key>` |
| S8 | Out of v1: Kanban cards, notify/email |

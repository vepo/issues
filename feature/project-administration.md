# Project administration

**Feature version:** 2  
**Status:** planned  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Project managers create and edit projects: name, prefix, required description, assigned workflow, and optional **ticket template** (default title, description, category, priority for new tickets). Projects scope all tickets and Kanban boards.

## Wireframe

**Guide:** layout reference for UI implementation — update when project form or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-03 |

### Screen: `/projects`

| Region | Elements |
|--------|----------|
| List | `.data-table`: name, prefix, workflow; row → edit |
| Actions | **Novo projeto** |

### Screen: `/projects/new` and `/projects/:projectId` (edit)

| Region | Elements |
|--------|----------|
| Form | Name, prefix (read-only when project has tickets), description, workflow, ticket template section |
| Template | Checkbox **Usar template de ticket**; default field values |

```
┌─────────────────────────────────────────────┐
│  Projetos                    [ Novo ]       │
├─────────────────────────────────────────────┤
│  Nome │ Prefixo │ Processo │ [Editar]       │
└─────────────────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `project`, `workflow` |
| Packages / files | `project.create`, `project.update`, `project.list`, `project.find`, `project.workflow`, `project.status` |
| API | `GET/POST /projects`, `GET /projects/{id}`, `GET /projects/{id}/workflow`, `GET /projects/{id}/statuses` |
| UI | `/projects`, `/projects/new`, `/projects/:projectId`; `projects-view`, `project-edit` components |
| Schema / seed | `tb_projects` (including template columns); dev projects in `dev-import.sql` |
| Tests | `CreateProjectEndpointTest`, `UpdateProjectEndpointTest`, `ListProjectsEndpointTest`, `FindProjectByIdEndpointTest`, `FindProjectWorkflowEndpointTest`, `ListProjectStatusesEndpointTest` |
| Docs | domain-spec (Project, Ticket template), feature-catalog (Project list/create/edit), README § Projects & administration |

### Risks

- Prefix immutability must be enforced in API when `tb_tickets` rows exist for the project.

### Feature questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Should project prefix be immutable after tickets exist? | answered | **Yes** — prefix cannot change once the project has tickets |
| FQ2 | Will projects ever support multiple workflows? | not valid | One workflow per project is the current invariant |

## Changelog

### Initial implementation — baseline

**Version:** 1  
**Status:** done

**Description:** Project CRUD for project-manager role; workflow assignment; optional ticket template with **Usar template de ticket** checkbox.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Create ticket | Template pre-fill on project-scoped create |
| Kanban board | Project context for board |
| Ticket import | Global import maps project column |
| Workflow configuration | Project references workflow |
| — | None identified |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Project list matches **Wireframe** | Wireframe | ☑ |
| FC2 | Create/edit form matches **Wireframe** | Wireframe | ☑ |
| FC3 | Optional ticket template on project | Summary | ☑ |
| FC4 | `feature-catalog.md` — Project rows | Impact / Docs | ☑ |

**Implementation notes:** `project-edit.component.ts`; template fields embedded on `Project` entity.

### Immutable project prefix — 2026-07-03

**Version:** 2  
**Status:** planned

**Description:** Reject prefix changes on update when the project has one or more tickets; UI shows prefix read-only in that case.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [ticket-management](ticket-management.md) | Identifiers remain stable |
| [create-ticket](create-ticket.md) | Identifier generation unchanged |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Prefix read-only on edit when tickets exist | Wireframe, FQ1 | ☐ |
| FC2 | API rejects prefix change when tickets exist | FQ1 | ☐ |
| FC3 | `domain-specification.md` — prefix immutability | Docs | ☐ |

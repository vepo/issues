# Workflow configuration

**Feature version:** 1  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Project managers and admins define workflows: name, start status, status list, and allowed transitions. Workflows are assigned to projects and govern ticket moves on Kanban and API. Edit flow allows name, start status, and transition changes; status names are fixed after create (partial update — see ARCHITECTURE §13).

## Wireframe

**Guide:** layout reference for UI implementation — update when workflow editor or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-03 |

### Screen: `/workflows`

| Region | Elements |
|--------|----------|
| List | Workflow name, start status; **Editar** per row |
| Actions | **Novo processo** |

### Screen: `/workflows/new` and `/workflows/:workflowId`

| Region | Elements |
|--------|----------|
| Form | Name, start status |
| Status table | `.inline-table` — status names (fixed on edit); optional **WIP** limit per status ([kanban-board](kanban-board.md) v2) |
| Transitions table | From → To allowed moves |
| Extensions | Phase start status; finish statuses (done/canceled) when enabled |

```
┌─────────────────────────────────────────────┐
│  Processos                   [ Novo ]       │
├─────────────────────────────────────────────┤
│  Nome │ Status inicial │ [Editar]           │
└─────────────────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `workflow` |
| Packages / files | `workflow.create`, `workflow.update`, `workflow.list`, `workflow.find`, `workflow.status.list` |
| API | `GET/POST /workflows`, `GET /workflows/{id}`, `POST /workflows/{id}`, `GET /status` |
| UI | `/workflows`, `/workflows/new`, `/workflows/:workflowId`; `workflows-view`, `workflow-create`, `workflow-edit`, `workflow-form` components |
| Schema / seed | `tb_workflows`, `tb_workflow_status`, `tb_workflow_statuses`, `tb_workflow_transitions`; dev workflows in `dev-import.sql` |
| Tests | `CreateWorkflowEndpointTest`, `UpdateWorkflowEndpointTest`, `ListWorkflowsEndpointTest`, `ListStatusesEndpointTest` |
| Docs | domain-spec (Workflow, Status, Transition), feature-catalog (Workflow rows), README § Tickets & workflow |

### Risks

- Status list not editable after workflow creation (ARCHITECTURE §13).

### Open questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| Q1 | Should workflow statuses be editable after create? | open | |
| Q2 | How should orphan transitions be handled if statuses are removed? | open | |

## Changelog

### Initial implementation — baseline

**Version:** 1  
**Status:** done

**Description:** Workflow builder UI with status table and transitions table on create; edit updates name, start status, and transitions.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Project administration | Each project references one workflow |
| Kanban board | Columns derived from workflow statuses; WIP limits configured here ([kanban-board](kanban-board.md) v2) |
| Ticket management | Move validates transitions |
| Create ticket | Initial status from workflow start status |
| Ticket import | Status column must match workflow |
| — | None identified |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Workflow list matches **Wireframe** | Wireframe | ☑ |
| FC2 | Create/edit form with status + transition tables | Wireframe | ☑ |
| FC3 | Start status and transitions govern ticket moves | Summary | ☑ |
| FC4 | `feature-catalog.md` — Workflow rows | Impact / Docs | ☑ |

**Implementation notes:** `workflow-form.component.ts`; `UpdateWorkflowEndpoint` partial update scope documented in ARCHITECTURE §13.

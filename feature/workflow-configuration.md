# Workflow configuration

**Feature version:** 1  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Project managers and admins define workflows: name, start status, status list, and allowed transitions. Workflows are assigned to projects and govern ticket moves on Kanban and API. Edit flow allows name, start status, and transition changes; status names are fixed after create (partial update — see ARCHITECTURE §13).

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
| Kanban board | Columns derived from workflow statuses |
| Ticket management | Move validates transitions |
| Create ticket | Initial status from workflow start status |
| Ticket import | Status column must match workflow |
| — | None identified |

**Implementation notes:** `workflow-form.component.ts`; `UpdateWorkflowEndpoint` partial update scope documented in ARCHITECTURE §13.

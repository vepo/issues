# Ticket import (CSV)

**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Bulk-create tickets from CSV files via a multi-step wizard: upload, column mapping, preview/validation, and execute. Supports **project-scoped** import (fixed project from Kanban) and **global** import (project resolved per row from a mapped column).

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `ticket`, `ticket.csvimport`, `project`, `workflow`, `categories`, `user` |
| Packages / files | `ticket.csvimport.*` (upload, mapping, preview, correct, execute for project and global variants); `TicketImportService`, `CsvImportParser`, `TicketImportRowExecutor` |
| API | `POST /projects/{id}/tickets/import/upload`, mapping, preview, correct, execute; `POST /tickets/import/*` global equivalents |
| UI | `/project/:projectId/tickets/import`, `/tickets/import`; `ticket-import-wizard` component |
| Schema / seed | `tb_ticket_imports`, `tb_ticket_import_rows` |
| Tests | `TicketImportServiceTest`, `ImportTicketsEndpointTest`, `GlobalTicketImportEndpointTest`, `PreviewTicketImportEndpointTest` |
| Docs | domain-spec (CSV import, Column mapping, Import batch), feature-catalog (Import rows), README § Tickets & workflow |

### Risks and open questions

- Large CSV files may need streaming/chunking for production scale.
- Row correction UI for validation errors; partial import success handling.

## Changelog

### Initial implementation — baseline

**Status:** done

**Description:** OpenCSV parsing, persisted import batches, column-mapping wizard, preview with validation, execute creates tickets per valid row.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Create ticket | Same field validation rules applied to imported rows |
| Kanban board | Project-scoped import entry (Importar CSV) |
| Project administration | Global import maps project column |
| Workflow configuration | Status column must match workflow statuses |
| — | None identified |

**Implementation notes:** `ticket-import-wizard.component.ts`; separate endpoint classes per HTTP operation under `ticket.csvimport.{upload,mapping,preview,correct,execute}`.

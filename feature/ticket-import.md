# Ticket import (CSV)

**Feature version:** 2  
**Status:** tasks-ready  
**Requested:** retrospective baseline (documented 2026-07-03); open questions answered 2026-07-11

## Summary

Bulk-create tickets from CSV files via a multi-step wizard: upload, column mapping, preview/validation with **row correction**, and execute. Supports **project-scoped** import (fixed project from Kanban) and **global** import (project resolved per row from a mapped column).

**Upload:** **chunked** transfer (**FQ1**, **AQ1**) with caps **5 MB** total / **1 MB** per chunk / **500** rows (**FQ3**). **Partial success:** valid rows execute; invalid rows stay correctable in preview (**FQ2**).

## Wireframe

**Guide:** layout reference for UI implementation — update when wizard steps or **FQ*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-11 |

### Wizard: `/project/:projectId/tickets/import` and `/tickets/import`

| Step | Elements | Notes |
|------|----------|-------|
| 1 Upload | File picker; **chunk progress** (e.g. 2/5); error if over **5 MB** / **500** rows; **Próximo** after complete | **FQ1**, **FQ3** — parts ≤ **1 MB** |
| 2 Mapping | Column → field mapping; project column (global only) | |
| 3 Preview | Valid/invalid rows table; **inline row correction** (project, status, assignee, …) | **FQ2** — fix invalid rows without re-upload |
| 4 Execute | Import summary (created count + remaining invalid); link to Kanban or list | Partial: valid rows created; siblings not rolled back |

```
 Upload (chunked) → Mapeamento → Pré-visualização (correção) → Importar
┌─────────────────────────────────────────────┐
│  Importar CSV — passo 1/4                   │
│  arquivo.csv — enviando 2/5 partes…         │
│  [ Voltar ]                                 │
└─────────────────────────────────────────────┘
┌─────────────────────────────────────────────┐
│  Importar CSV — passo 3/4                   │
│  Linha 12  ✗ projeto inválido                │
│    Projeto: [ Acme ▾ ]  ← correct in place  │
│  Linha 14  ✓                                │
│  [ Voltar ]              [ Importar válidas ]│
└─────────────────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `ticket` / `ticket.csvimport` owns import; may use `project`, `workflow`, `categories`, `user` for validation |
| Packages / files | New `ticket.csvimport.upload.chunk` (or extend upload package): init / part / complete endpoints; `TicketImportService` staging + concat + existing parse; Angular wizard chunked `File` slice upload |
| API | Replace single-shot primary path with init → part(s) → complete (project + global). Keep or deprecate legacy `POST …/upload` octet-stream. Existing mapping / preview / correct / execute unchanged |
| UI | Upload step: slice file into ≤1 MB parts, show progress, call chunk APIs; mapping+ onwards unchanged (**FQ2**) |
| Schema / seed | Extend `tb_ticket_imports` for staging: e.g. `upload_status`, `expected_bytes` / `received_bytes`, `chunk_count`, optional blob/temp path or `tb_ticket_import_chunks` |
| Tests | Chunk init/part/complete endpoint tests (oversize, too many rows, missing part); Angular upload progress / failure; regression on correct + execute |
| Docs | domain-spec limits; feature-catalog click path notes chunk progress; ARCHITECTURE §13 |

### Risks

- Staging blobs must be cleaned on abandon / TTL — incomplete uploads.
- Concurrent parts for same `importId` need ordered append or indexed chunks (**AQ1**).

### Feature questions (FQ*n*)

Product, scope, UX, and domain decisions. Legacy **Q*n*** renumbered to **FQ*n***. Status: `open` | `answered` | `not valid`.

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | What is the maximum CSV file size and should uploads stream/chunk? | answered | **Chunk** — upload via chunks (not single-shot only). |
| FQ2 | How should partial import success and row correction UX behave? | answered | **Row correction UX** — preview corrects in place; execute creates valid rows without rolling back siblings. |
| FQ3 | Limits with chunked upload: max total bytes, max chunk size, max rows? | answered | Keep today’s caps: **5 MB** max total, **500** max rows; **1 MB** max chunk size. |

**Gate:** all blocking FQs answered.

## Architecture

| Area | Design |
|------|--------|
| Bounded contexts | `ticket.csvimport` owns chunked upload + existing import lifecycle |
| Packages / layers | One `*Endpoint` per HTTP op under `ticket.csvimport.upload` (or `.chunk`): Init / UploadPart / Complete. `TicketImportService` orchestrates staging, concat, then existing `CsvImportParser` + row persist. Repositories only for persistence |
| API | See below |
| Schema / seed | Staging on `tb_ticket_imports` + optional `tb_ticket_import_chunks (import_id, part_index, bytes)` OR single BYTEA assembled server-side; status e.g. `UPLOADING` → existing post-parse status. Amend `V1.0.0` only |
| Cross-context | Unchanged — author from security context; project membership checks as today |
| Frontend | Wizard step 1: `file.slice` into ≤1 MB parts; init → sequential or parallel parts with index → complete; then existing mapping flow |
| Tests | Endpoint tests for limits (**FQ3**); missing/duplicate part; happy path parse after complete; Angular chunk progress |

### API surface (v2 chunked upload)

Project-scoped (global mirrors under `/tickets/import/…`):

| Method | Path | Auth | Body / notes |
|--------|------|------|--------------|
| `POST` | `/projects/{id}/tickets/import/upload/init` | user / admin / PM | `InitTicketImportUploadRequest` (fileName, totalBytes, chunkCount) → `TicketImportUploadResponse` (importId) |
| `PUT` | `/projects/{id}/tickets/import/{importId}/upload/parts/{partIndex}` | same | octet-stream ≤ 1 MB; `Content-Length` checked |
| `POST` | `/projects/{id}/tickets/import/{importId}/upload/complete` | same | Concat + parse; same outcome as today’s upload response (headers, truncated, …) |

Reject when `totalBytes > 5 MB`, any part `> 1 MB`, or after parse `rowCount > 500` (truncate flag / error — match current parser behaviour).

Legacy single `POST …/upload` may remain as thin wrapper that init+one-part+complete for small files, or be removed in the same changelog — prefer **wrapper** for backward compatibility with generated clients.

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Chunk protocol for CSV upload? | answered | **(A)** resumable **init / part / complete** + server concat then parse (not B single-body-only). |

**Gate:** blocking AQs answered → task break complete below.

## Changelog

### Chunked CSV upload — 2026-07-11

**Version:** 2  
**Status:** tasks-ready

**Description:** Replace single-shot primary upload with **init / part / complete** chunked upload (**FQ1**, **AQ1**); enforce **5 MB** / **1 MB** / **500** rows (**FQ3**); keep **FQ2** row-correction + partial execute.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Ticket import wizard | Upload step: chunk progress + multi-request upload |
| OpenAPI / Angular codegen | New init/part/complete operations |
| Create ticket | Unchanged validation on execute |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Init / part / complete APIs per **Architecture** (**AQ1**) | Architecture, **AQ1** | ☐ |
| FC2 | Enforce 5 MB total, 1 MB chunk, 500 rows (**FQ3**) | **FQ3** | ☐ |
| FC3 | Wizard upload shows chunk progress; rejects oversize | Wireframe, **FQ1** | ☐ |
| FC4 | Row correction + partial execute unchanged (**FQ2**) | **FQ2**, Wireframe | ☐ |
| FC5 | Schema staging in `V1.0.0`; domain-spec + feature-catalog + ARCHITECTURE updated | Impact / Docs | ☐ |
| FC6 | Legacy single upload still works via wrapper or documented removal | Architecture | ☐ |

#### Tasks

| ID | Deliverable |
|----|-------------|
| T1 | Schema: staging fields and/or `tb_ticket_import_chunks` in `V1.0.0`; entity/repository support |
| T2 | `TicketImportService` init / accept part / complete (concat + existing parse); enforce **FQ3** limits |
| T3 | Project-scoped endpoints: init, upload part, complete (`*Endpoint` per op) |
| T4 | Global-scoped endpoints: same three operations |
| T5 | Legacy `POST …/upload` wrapper (init + single part + complete) or remove + regenerate clients |
| T6 | Angular wizard: slice file, call chunk APIs, progress UI per wireframe |
| T7 | Docs: domain-spec limits, feature-catalog, ARCHITECTURE §13 |

#### Test coverage

| ID | Coverage |
|----|----------|
| TC1 | Init rejects totalBytes > 5 MB; part rejects > 1 MB (**FQ3**) — with T2/T3 |
| TC2 | Complete with all parts → parse + headers like today — with T2/T3 |
| TC3 | Missing part / wrong import state → 400 — with T2/T3 |
| TC4 | Global chunk upload happy path — with T4 |
| TC5 | Legacy wrapper (if kept) still imports small CSV — with T5 |
| TC6 | Angular: chunked upload progress / oversize handling — with T6 |
| TC7 | Regression: correct row + execute partial still green — with T2 |

**Development approval:** — (awaiting explicit task IDs)

### Initial implementation — baseline

**Version:** 1  
**Status:** done

**Description:** OpenCSV parsing, persisted import batches, column-mapping wizard, preview with validation and **row correction** (**FQ2**), execute creates tickets per valid row.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Create ticket | Same field validation rules applied to imported rows |
| Kanban board | Project-scoped import entry (Importar CSV) |
| Project administration | Global import maps project column |
| Workflow configuration | Status column must match workflow statuses |
| — | None identified |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Import wizard matches **Wireframe** steps | Wireframe | ☑ |
| FC2 | Project-scoped and global import routes | Summary | ☑ |
| FC3 | Preview shows validation before execute | Wireframe | ☑ |
| FC4 | Inline row correction for invalid rows | **FQ2** | ☑ |
| FC5 | `feature-catalog.md` — Import rows | Impact / Docs | ☑ |

**Implementation notes:** `ticket-import-wizard.component.ts`; separate endpoint classes per HTTP operation under `ticket.csvimport.{upload,mapping,preview,correct,execute}`. Correction endpoints + UI for project/status/assignee. Upload remains single-shot until Version 2.

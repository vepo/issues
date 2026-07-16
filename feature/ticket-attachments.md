# Ticket attachments

**Feature version:** 1  
**Status:** done  
**Requested:** 2026-07-16

## Summary

Allow authenticated users to **upload**, **list**, **download**, and **delete** **Attachments** on a **Ticket**. Specs, screenshots, and dumps are weak as comments alone; attachments bind files to the ticket with metadata, history audit, and the same **project security level** access rules as other ticket reads/writes.

**v1 goal:** ticket-scoped attachments on ticket detail — multipart upload, metadata list, binary download, hard delete; history events; UI panel on `/ticket/:ticketIdentifier`.

**Out of scope for v1:** comment-scoped attachments (**FQ7**), inline image preview/lightbox (**FQ9**), virus scanning, object-storage backends (**FQ1**), attachment search/query predicates, bulk download ZIP, drag-drop into description editor, subscriber notify/email on attach (**FQ6**).

Related but distinct: [ticket-import.md](ticket-import.md) (CSV batch upload), [git-integration.md](git-integration.md) (linked commits), [rich-text-editor.md](rich-text-editor.md) (HTML description — not file storage).

## Decisions

| ID | Decision | Source |
|----|----------|--------|
| D1 | Bytes on **local filesystem**; DB stores path + metadata (not BYTEA / S3) | **FQ1** |
| D2 | Max **10 MB** per file; allow-list: images (`png`/`jpeg`/`gif`/`webp`), `pdf`, `txt`/`md`/`csv`, Office (`doc`/`docx`/`xls`/`xlsx`/`odt`/`ods`), `zip`; block executables/scripts | **FQ2**, **FQ3** |
| D3 | Any user who may **write** the ticket may delete (not uploader-only) | **FQ4** |
| D4 | Soft-deleted ticket: **no** upload/delete; list+download only for callers who can view the deleted ticket (admin/PM) | **FQ5** |
| D5 | **No** subscriber SSE/email on add/remove in v1 | **FQ6** |
| D6 | Attachments are **ticket-only** (not on comments) | **FQ7** |
| D7 | Max **20** files and **50 MB** total per ticket | **FQ8** |
| D8 | UI: metadata list + download only (no inline preview) | **FQ9** |
| D9 | History `ATTACHMENT_ADDED` / `ATTACHMENT_REMOVED` in activity feed | **FQ10** |
| D10 | JWT + PAT / project SA; history `via_agent` when token auth | **FQ11** |
| D11 | Upload wire format is **multipart/form-data** only | **AQ1** |
| D12 | On-disk path `{storage-dir}/tickets/{ticketId}/{uuid}` | **AQ2** |
| D13 | Delete DB row then best-effort file delete | **AQ3** |
| D14 | Validate extension allow-list **and** MIME family match | **AQ4** |
| D15 | Config `issues.attachments.storage-dir` (default `./data/attachments`; `%test` → `target/test-attachments`) | **AQ5** |
| D16 | History filename in old/new value; `referenceId` = attachment id | **AQ6** |
| D17 | Dedicated `AttachmentService` in `ticket.attachments` | **AQ7** |
| D18 | Sanitize download filename; RFC 5987 `filename*` when needed | **AQ8** |

## Wireframe

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-16 (AQ1–AQ8 accepted; tasks-ready) |

### Screen: `/ticket/:ticketIdentifier` — Anexos (extends ticket detail)

| Region | Elements | Notes |
|--------|----------|-------|
| Header / fields | Existing ticket chrome | [ticket-management](ticket-management.md) |
| **Anexos** | Section title; file list (original filename, human size, uploader name, uploaded-at); **Baixar**; **Excluir** (write + not deleted); empty state | Gallery: section like **Vínculos**; `.btn` / `.btn-secondary`; confirm before **Excluir** |
| Upload | File picker + **Anexar**; client-side size hint; server errors as toast | Hidden/disabled when soft-deleted or no write (**FQ5**) |
| Activity | History rows for add/remove with filename | **FQ10**; labels i18n |

```
┌─────────────────────────────────────────────────────────────┐
│  ISS-003 · Fix login redirect                               │
├─────────────────────────────────────────────────────────────┤
│  (fields, vínculos, subtarefas…)                            │
├─────────────────────────────────────────────────────────────┤
│  Anexos                                                     │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  spec.pdf   1.2 MB   Alice · 16 jul 2026            │   │
│  │             [ Baixar ]  [ Excluir ]                 │   │
│  │  screenshot.png  240 KB  Bob · 15 jul 2026          │   │
│  │             [ Baixar ]  [ Excluir ]                 │   │
│  └─────────────────────────────────────────────────────┘   │
│  [ Escolher arquivo ]  [ Anexar ]                           │
│                                                             │
│  Comentários / Atividade (Anexo adicionado / removido)      │
└─────────────────────────────────────────────────────────────┘
```

### Non-UI surfaces

| Surface | Layout | Notes |
|---------|--------|-------|
| REST multipart upload / list / download / delete | N/A | OpenAPI + Angular client (**AQ1**) |
| Agent / PAT / SA | N/A | Same endpoints; `via_agent` (**FQ11**) |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | **Ticket management** — `ticket.attachments`; history; project access (security level + membership for writes) |
| Packages / files | `Attachment` entity/repo; `AttachmentService`; endpoints upload/list/download/delete; `TicketHistoryAction` + service methods; activity-feed labels; `ticket-view` Anexos; Flyway `tb_ticket_attachments`; `issues.attachments.storage-dir` config; i18n |
| API | `GET/POST /tickets/{id}/attachments`; `GET /tickets/{id}/attachments/{attachmentId}` (bytes); `DELETE …/{attachmentId}`; `AttachmentResponse` metadata |
| UI | Ticket detail **Anexos**; confirm delete; no preview (**FQ9**) |
| Schema / seed | `tb_ticket_attachments` in `V1.0.0`; optional `dev-import` sample file under storage dir |
| Tests | Endpoint auth/limits/soft-delete/history; filesystem cleanup; Angular Anexos + history labels; ArchUnit |
| Docs | domain-spec, feature-catalog, README, ARCHITECTURE §13 |

### Risks

- Disk growth — enforce **FQ2**/**FQ8** caps; orphan files if DB delete fails after IO (delete metadata first or transactional cleanup strategy — **AQ3**).
- Extension spoofing — allow-list extension **and** declared MIME family (**AQ4**).
- Security level bypass on download URL — always `requireRead` on ticket’s project.
- Path traversal — store opaque UUID filenames only; never use client path segments.

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Where are file bytes stored in v1? | answered | **B** — local filesystem under configured data dir; path in DB |
| FQ2 | Max size per file? | answered | **10 MB** |
| FQ3 | Allowed content types / extensions? | answered | Images `png`/`jpeg`/`gif`/`webp`; `pdf`; `txt`/`md`/`csv`; Office `doc`/`docx`/`xls`/`xlsx`/`odt`/`ods`; `zip`; block executables/scripts |
| FQ4 | Who may delete? | answered | Any user who may **write** the ticket |
| FQ5 | Soft-deleted ticket behaviour? | answered | No upload/delete; list+download only for callers who can view the deleted ticket (admin/PM) |
| FQ6 | Notify subscribers? | answered | **Never in v1** |
| FQ7 | Comment attachments? | answered | **Ticket only** |
| FQ8 | Per-ticket caps? | answered | **20 files** and **50 MB** total |
| FQ9 | Inline preview? | answered | **List + download only** |
| FQ10 | Ticket history? | answered | **Yes** — `ATTACHMENT_ADDED` / `ATTACHMENT_REMOVED` |
| FQ11 | PAT / SA? | answered | **Yes** — same write rules; `via_agent` on history when token auth |

## Architecture

| Area | Design |
|------|--------|
| Bounded contexts | **Ticket management** owns attachments; depends on identity + project access; no new context |
| Packages / layers | `ticket.attachments` — `Attachment`, `AttachmentRepository`, `AttachmentService`; endpoints: `attachments.upload.UploadTicketAttachmentEndpoint`, `attachments.list.ListTicketAttachmentsEndpoint`, `attachments.download.DownloadTicketAttachmentEndpoint`, `attachments.delete.DeleteTicketAttachmentEndpoint` |
| API | See below |
| Schema | `tb_ticket_attachments` — see below |
| Storage | Files under `issues.attachments.storage-dir` at `{base}/tickets/{ticketId}/{uuid}` (**AQ2**); DB holds relative/storage key + metadata |
| Cross-context | `TicketHistoryService` for add/remove; **no** notification events (**FQ6**); access via `ProjectAccessService.requireRead` / `requireView` (write = membership) |
| Frontend | `ticket-view` Anexos section; `TicketService` or thin attachment facade; regenerate OpenAPI client |
| Tests | `*Attachment*EndpointTest`; history display; Angular specs; `%test` storage dir |

### API map

| Method | Path | Auth | Behaviour |
|--------|------|------|-----------|
| `GET` | `/tickets/{id}/attachments` | read ticket | List `AttachmentResponse` (metadata only) |
| `POST` | `/tickets/{id}/attachments` | write ticket | Multipart file upload → `201` + `AttachmentResponse` |
| `GET` | `/tickets/{id}/attachments/{attachmentId}` | read ticket | Binary stream; `Content-Type` stored MIME; `Content-Disposition: attachment` |
| `DELETE` | `/tickets/{id}/attachments/{attachmentId}` | write ticket | Hard-delete row + file; `204` |

`AttachmentResponse`: `id`, `originalFilename`, `contentType`, `sizeBytes`, `uploadedBy` (user summary), `uploadedAt`.

Roles: same as comments — `@RolesAllowed` USER / ADMIN / PROJECT_MANAGER; service enforces project read/write + soft-delete rules.

### Schema (`V1.0.0`)

```sql
CREATE TABLE tb_ticket_attachments (
  id BIGSERIAL PRIMARY KEY,
  ticket_id BIGINT NOT NULL REFERENCES tb_tickets(id),
  original_filename VARCHAR(255) NOT NULL,
  content_type VARCHAR(127) NOT NULL,
  size_bytes BIGINT NOT NULL,
  storage_key VARCHAR(512) NOT NULL,
  uploaded_by BIGINT NOT NULL REFERENCES tb_users(id),
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ticket_attachments_ticket ON tb_ticket_attachments(ticket_id);
```

### Layer flow

```
UploadTicketAttachmentEndpoint
  → AttachmentService.upload(ticketId, file, username)
      → require writable non-deleted ticket
      → validate size / extension / MIME / caps
      → write file → persist Attachment → history ATTACHMENT_ADDED
```

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Upload wire format: multipart vs octet-stream + filename header? | answered | **multipart** only in v1 |
| AQ2 | On-disk path layout? | answered | `{storage-dir}/tickets/{ticketId}/{uuid}` — opaque uuid; original name only in DB |
| AQ3 | Delete order / orphan cleanup? | answered | Delete DB row then best-effort file delete; log failures; no soft-delete of attachments |
| AQ4 | Type validation? | answered | Extension allow-list **and** declared MIME must map to that family; reject mismatch |
| AQ5 | Config property and defaults? | answered | `issues.attachments.storage-dir` — default `./data/attachments`; `%test` → `target/test-attachments` |
| AQ6 | History payload shape? | answered | `newValue`/`oldValue` = original filename; `referenceId` = attachment id |
| AQ7 | Service placement? | answered | Dedicated **`AttachmentService`** in `ticket.attachments` |
| AQ8 | Content-Disposition filename? | answered | Strip path separators / control chars; RFC 5987 `filename*` when non-ASCII |

## Changelog

### Ticket attachments v1 — 2026-07-16

**Version:** 1  
**Status:** done

**Description:** Upload, list, download, and delete files on a ticket; history audit; ticket-detail **Anexos** UI; access aligned with project security level.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Ticket management / detail | **Anexos** region; activity history actions |
| Project visibility | List/download = ticket read; upload/delete = write (membership) |
| Notifications / email | **None** in v1 (**FQ6**) |
| Agentic / API tokens | Upload/delete allowed (**FQ11**); `via_agent` |
| Ticket import | Unrelated — keep separate paths/UI |
| i18n | Anexos / Attachments chrome + history labels |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Write-capable users can upload within 10 MB, allow-list types, 20 files / 50 MB caps | FQ2–FQ4, FQ8, D2, D7 | ☑ |
| FC2 | Read-capable users can list and download | D4 / security level | ☑ |
| FC3 | Soft-deleted: no upload/delete; list+download for admin/PM viewers only | FQ5, D4 | ☑ |
| FC4 | Any ticket writer may hard-delete attachment (bytes + metadata) | FQ4, D3, AQ3 | ☑ |
| FC5 | Activity/history shows ATTACHMENT_ADDED / REMOVED with filename | FQ10, D9, AQ6, Wireframe | ☑ |
| FC6 | Ticket detail **Anexos** matches **Wireframe** (list, Baixar, Excluir, Anexar; no preview) | Wireframe, FQ9 | ☑ |
| FC7 | No subscriber notify/email on attach/remove | FQ6, D5 | ☑ |
| FC8 | No comment-scoped attachments | FQ7, D6 | ☑ |
| FC9 | Files under `issues.attachments.storage-dir` at `{dir}/tickets/{ticketId}/{uuid}`; path in DB | FQ1, AQ2, AQ5 | ☑ |
| FC10 | Multipart upload only; extension + MIME validation | AQ1, AQ4 | ☑ |
| FC11 | Download uses sanitized `Content-Disposition` (+ `filename*` when needed) | AQ8 | ☑ |
| FC12 | `domain-specification.md` — Attachment terms + invariants | Docs | ☑ |
| FC13 | `feature-catalog.md` — ticket detail Anexos steps | Docs | ☑ |
| FC14 | README Features — attachments bullet | Docs | ☑ |
| FC15 | ARCHITECTURE API map — attachment endpoints | Docs | ☑ |
| FC16 | i18n `pt`/`en` for Anexos chrome and history labels | i18n | ☑ |
| FC17 | PAT/SA upload/delete with `via_agent` history | FQ11, D10 | ☑ |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | Flyway `tb_ticket_attachments` + JPA `Attachment` entity; config `issues.attachments.storage-dir` (+ `%test`) | ☑ |
| T2 | `AttachmentRepository` (by ticket, by id+ticket, count/sum size) | ☑ |
| T3 | `TicketHistoryAction` ATTACHMENT_ADDED/REMOVED + `TicketHistoryService` helpers (**AQ6**) | ☑ |
| T4 | `AttachmentService` — validate, store/read/delete files, access + soft-delete rules, caps (**AQ2–AQ5**, **AQ7**) | ☑ |
| T5 | Endpoints: upload (multipart), list, download, delete — OpenAPI operationIds | ☑ |
| T6 | Backend endpoint tests: upload/list/download/delete, auth, soft-delete, size/MIME/caps, history, `via_agent` | ☑ |
| T7 | Angular: regenerate API client; ticket/attachment service facades | ☑ |
| T8 | `ticket-view` **Anexos** UI per Wireframe (list, Anexar, Baixar, Excluir confirm; no preview) | ☑ |
| T9 | Activity/history labels for ATTACHMENT_* + i18n `pt`/`en` | ☑ |
| T10 | Angular specs for Anexos + history labels | ☑ |
| T11 | Docs: feature-catalog, README, ARCHITECTURE §13; `.gitignore` `data/attachments/` (and confirm domain-spec) | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | Upload succeeds for writer; returns metadata; file on disk | T4, T5, T6 | ☑ |
| TC2 | Reject oversize, disallowed type, MIME/extension mismatch, over caps | T4, T6 | ☑ |
| TC3 | List/download require read; forbidden without project read | T4, T5, T6 | ☑ |
| TC4 | Soft-deleted: reject upload/delete; admin/PM list+download ok | T4, T6 | ☑ |
| TC5 | Delete removes row + file; any writer may delete | T4, T5, T6 | ☑ |
| TC6 | History ATTACHMENT_ADDED / REMOVED with filename (+ `via_agent` when token) | T3, T6 | ☑ |
| TC7 | Angular Anexos render/upload/download/delete; history labels | T8, T9, T10 | ☑ |

**Development approval:** approved 2026-07-16 — tasks: T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11

**Implementation notes:**

- Backend: `ticket.attachments` package; `tb_ticket_attachments`; filesystem under `issues.attachments.storage-dir`; multipart upload/list/download/delete; history `ATTACHMENT_*`; `TicketAttachmentEndpointTest` + ArchUnit FileUpload allow-list.
- Frontend: ticket detail **Anexos**; `TicketService` facades; history `$localize` labels; Angular specs.
- Docs: domain-spec, feature-catalog, README, ARCHITECTURE; `.gitignore` `/data/attachments/`.
- Gates: `mvn verify` pass; `npm run build` pass; ticket-view specs pass.

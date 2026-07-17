# Ticket export

**Feature version:** 1
**Status:** done  
**Requested:** 2026-07-17

## Summary

Allow authenticated users to download all tickets matching the current **simple search**, **advanced query**, or **saved query** as CSV or JSON. Exports are visibility-safe, deterministic, stable for automation, capped at 10,000 tickets, and include reporting fields plus typed custom-field values. Comments, history, links, subscribers, commits, attachment metadata, and soft-deleted tickets are excluded.

## Scope

| ID | Capability |
|----|------------|
| S1 | Export all matches from `/search`, `/search/advanced`, and `/search/q/:slug` |
| S2 | Offer CSV and JSON from a compact result-level export menu |
| S3 | Include stable reporting fields, display-name companions, and custom fields |
| S4 | Use stable English keys/codes and ISO-8601 timestamps; never localize file values |
| S5 | Stream synchronously with a hard cap of 10,000 matching tickets |
| S6 | Apply the caller's readable-project scope to every source |
| S7 | Exclude soft-deleted tickets and related-resource collections |
| S8 | Preserve explicit advanced-query ordering; otherwise order by identifier ascending |

## Wireframe

| Field | Value |
|-------|-------|
| **Source** | ASCII below; extends existing ticket-search screens |
| **Last updated** | 2026-07-17 |

### Screen: `/search`

| Region | Elements | Notes |
|--------|----------|-------|
| Header actions | Existing **Busca avançada**, **Minhas consultas**; new **Exportar** menu | Menu items **CSV** and **JSON** |
| Results | Existing cards | Export uses current term and optional status filter; all matches, not DOM rows |
| Feedback | Export action shows loading; inline/banner error on failure | Action disabled while request runs |

```text
┌──────────────────────────────────────────────────────────┐
│ Resultados  [Busca avançada] [Minhas consultas] [Exportar ▾] │
│                                      ├ CSV              │
│ [ termo………… ] [status chips]         └ JSON             │
├──────────────────────────────────────────────────────────┤
│ Existing result cards                                   │
└──────────────────────────────────────────────────────────┘
```

### Screen: `/search/advanced`

| Region | Elements | Notes |
|--------|----------|-------|
| Query editor | Existing query input and **Executar** | |
| Result actions | **Exportar** menu beside successful-result controls | Hidden/disabled until a valid query has executed |
| Feedback | Loading and error feedback | Export re-executes the submitted query server-side |

```text
┌──────────────────────────────────────────────────────────┐
│ Busca avançada                                           │
│ [ query text.......................................... ] │
│ [Executar] [Salvar consulta]              [Exportar ▾]   │
├──────────────────────────────────────────────────────────┤
│ Existing result table/detail                            │
└──────────────────────────────────────────────────────────┘
```

### Screen: `/search/q/:slug`

| Region | Elements | Notes |
|--------|----------|-------|
| Header actions | Existing **Copiar link**, **Editar/Clonar**; new **Exportar** menu | Any authenticated reader of the shared query may export readable results |
| Results | Existing saved-query result cards | Server resolves the saved query by slug |
| Feedback | Loading and error feedback | |

```text
┌──────────────────────────────────────────────────────────┐
│ Query name  [Copiar link] [Editar/Clonar] [Exportar ▾]   │
├──────────────────────────────────────────────────────────┤
│ Existing result cards                                   │
└──────────────────────────────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `ticket.export` owns export; depends on ticket search, project read access, and custom-field read services |
| Packages / files | New `ticket.export` endpoint/service/repository/writers; visibility predicate in `ticket.search.query`; Angular export facade and three search components |
| API | `POST /api/tickets/export` with `ExportTicketsRequest`; streamed CSV/JSON attachment response |
| UI | No new route; export menu on simple, advanced, and saved-query result screens |
| Schema / seed | None; generation is synchronous and not persisted |
| Security | Authenticated roles only; all sources constrained by `ProjectAccessService.readableProjectIds`; fix advanced/saved-query visibility gap |
| Tests | Export endpoint/service/writer tests, query visibility regression, three Angular component specs, catalog completeness |
| Docs | Domain specification, feature catalog, UI gallery, README, ARCHITECTURE, backlog |

### Risks

- Advanced and saved-query execution currently omit readable-project filtering; export must not amplify that security gap.
- A union of heterogeneous custom-field keys can create wide CSV files; columns remain stable and sorted.
- CSV values can trigger spreadsheet formulas; dangerous leading characters must be neutralized.
- Rich-text descriptions/custom-field text must be flattened deterministically without leaking markup.
- A 10,000-ticket export can expose N+1 loading; custom fields and display names require bounded/batched reads.

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Which result sources are exportable? | answered | Simple search, ad-hoc advanced query, and saved-query results |
| FQ2 | Which formats ship in v1? | answered | CSV and JSON |
| FQ3 | What ticket content is included? | answered | Reporting built-in fields plus custom fields; exclude comments/history/links/subscribers/commits/attachments |
| FQ4 | How are values localized? | answered | Stable English keys/codes, ISO-8601 timestamps, and display-name companion fields; not localized |
| FQ5 | How are large exports delivered? | answered | Immediate streamed download capped at 10,000 tickets |
| FQ6 | Who may export? | answered | Authenticated `user`, `project-manager`, and `admin`; only tickets readable under project security |
| FQ7 | Are deleted tickets included? | answered | No; soft-deleted tickets are excluded for every role |
| FQ8 | How are custom fields represented? | answered | CSV union columns `custom.<key>` sorted by key; JSON typed `customFields` object keyed by stable key |
| FQ9 | How is rich text represented? | answered | Deterministic plain text in both formats |
| FQ10 | What ordering is used? | answered | Preserve explicit query `ORDER BY`; otherwise identifier ascending |
| FQ11 | Where is the action shown? | answered | Compact CSV/JSON menu on the three result screens; no dedicated route |
| FQ12 | Is anonymous public-project export supported? | answered | No; export follows authenticated global-search scope |

**Gate:** all blocking FQs answered.

## Architecture

| Area | Design |
|------|--------|
| Bounded contexts | Ticket management owns export. Project administration supplies readable project IDs. Custom fields supply batched typed values. No cross-context repository access from the endpoint |
| Packages / layers | `ticket.export.ExportTicketsEndpoint → TicketExportService → TicketExportRepository`; `TicketCsvWriter` / `TicketJsonWriter` are service collaborators; advanced source delegates parsing to `TicketQueryLanguageService` |
| API | One `POST /api/tickets/export`; `ExportTicketsRequest` selects format and exactly one source; class `@DenyAll`, method roles `USER`, `PROJECT_MANAGER`, `ADMIN`; returns streamed attachment |
| Search security | `TicketQueryLanguageService` / `TicketQueryRepository` receive readable project IDs and add a mandatory project predicate for advanced and saved queries, including their existing non-export callers |
| Export model | Internal `TicketExportRow` projection contains stable built-ins, display names, plain-text rich fields, and typed custom-field map; not a reuse of `TicketResponse` |
| CSV | OpenCSV writer, RFC 4180 comma-separated UTF-8, stable headers, quoted newlines, empty string for null, sorted `custom.<key>` columns, formula neutralization |
| JSON | Versioned `TicketExportDocument` envelope: `schemaVersion`, `generatedAt`, `source`, `count`, `tickets`; typed values and custom-field object |
| Streaming / limits | Resolve and authorize before writing; query at most 10,001 to detect overflow; reject >10,000 rather than silently truncate; attachment filename `tickets-YYYY-MM-DD.{csv,json}` |
| Schema / seed | None |
| Frontend | `TicketExportService` posts source criteria with `responseType: blob`, reads `Content-Disposition`, downloads and revokes object URL; menus use Transloco `ticketExport.*` keys |
| Tests | Endpoint/service/repository/writer tests, query visibility regression, ArchitectureTest, Angular service/component specs, runtime catalog completeness |

### Request contract

`ExportTicketsRequest` fields:

| Field | Values / rule |
|-------|---------------|
| `format` | `CSV` or `JSON` |
| `source` | `SIMPLE_SEARCH`, `ADVANCED_QUERY`, or `SAVED_QUERY` |
| `term`, `statusId` | Allowed only for `SIMPLE_SEARCH` |
| `query` | Required only for `ADVANCED_QUERY` |
| `savedQuerySlug` | Required only for `SAVED_QUERY` |

The service rejects mixed or incomplete source fields with `400` and more than 10,000 matches with `413`. A saved-query export resolves current stored query text by slug and does not trust browser result rows.

### Stable reporting fields

`identifier`, `title`, `description`, `projectKey`, `projectName`, `statusCode`, `statusName`, `categoryId`, `categoryName`, `priority`, `type`, `authorEmail`, `authorName`, `assigneeEmail`, `assigneeName`, `phaseId`, `phaseName`, `observedVersionId`, `observedVersionName`, `targetVersionId`, `targetVersionName`, `storyPoints`, `dueDate`, `createdAt`, `updatedAt`, followed by sorted custom-field keys.

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | One endpoint or one endpoint per source/format? | answered | One export operation with validated source/format discriminators |
| AQ2 | Client-side or server-side serialization? | answered | Server-side streaming so authorization, limits, escaping, and schema stay authoritative |
| AQ3 | Persist export jobs/files? | answered | No; synchronous generation with a 10,000-ticket cap |
| AQ4 | How is advanced-query visibility enforced? | answered | Mandatory readable-project predicate in the shared query repository path, not export-only post-filtering |
| AQ5 | Dedicated export projection or existing ticket response? | answered | Dedicated `TicketExportRow`, avoiding response mismatch and per-ticket custom-field loading |
| AQ6 | How are custom fields loaded? | answered | Batched read by ticket IDs through the owning custom-field service; no N+1 endpoint mapping |
| AQ7 | How is JSON versioned? | answered | `TicketExportDocument` envelope with schema version `1` |
| AQ8 | How are filenames supplied? | answered | Server `Content-Disposition` with ASCII and RFC 5987 UTF-8 filename forms; client honors it |

**Gate:** all blocking AQs answered.

## Changelog

### Export search results as CSV or JSON — 2026-07-17

**Version:** 1
**Status:** done

**Description:** Add visibility-safe, synchronous CSV/JSON downloads for simple, advanced, and saved-query ticket results.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Ticket search | Adds result export actions; fixes advanced/saved query project-visibility enforcement |
| Custom fields | Reads typed values in batches and exposes stable keys |
| Project visibility | Export and all query-language results obey readable-project scope |
| UI i18n | Export controls use reactive PT/EN catalogs; file contract remains locale-neutral |
| Ticket import | No protocol reuse or schema impact; stable CSV vocabulary may support future round-trip work but import compatibility is not promised |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Simple, advanced, and saved-query screens expose a CSV/JSON export menu matching the wireframes | S1–S2, FQ1, FQ11 | ☑ |
| FC2 | CSV and JSON contain the documented stable reporting fields and custom fields | S3–S4, FQ2–FQ4, FQ8 | ☑ |
| FC3 | Export excludes soft-deleted tickets and related-resource collections | S7, FQ3, FQ7 | ☑ |
| FC4 | Every source includes only projects readable by the authenticated caller | S6, FQ6, FQ12, AQ4 | ☑ |
| FC5 | Explicit query ordering is preserved; other exports sort by identifier | S8, FQ10 | ☑ |
| FC6 | More than 10,000 matches is rejected without creating a partial file | S5, FQ5, AQ3 | ☑ |
| FC7 | CSV follows RFC 4180 and neutralizes spreadsheet formulas | Risks, Architecture | ☑ |
| FC8 | JSON uses schema version 1 and typed custom-field values | AQ7, Architecture | ☑ |
| FC9 | UI shows loading/errors and reacts immediately to PT/EN changes without route changes | Wireframe, UI i18n impact | ☑ |
| FC10 | Query-language visibility gap is fixed for export and existing advanced/saved-query callers | Impact, AQ4 | ☑ |
| FC11 | Domain spec, feature catalog, UI gallery, README, ARCHITECTURE, feature index, and backlog are current | Impact / Docs | ☑ |
| FC12 | Final backend/frontend gates, lints, and dev smoke pass before status changes to `done` | T10, TC10 | ☑ |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | Add query-language visibility regression tests and enforce readable-project predicates in shared advanced/saved-query execution | ☑ |
| T2 | Add export request/source/format domain types, `TicketExportRow`, source validation, deterministic ordering, 10,001 overflow detection, and batched custom-field projection | ☑ |
| T3 | Implement and test RFC 4180 `TicketCsvWriter`, stable/dynamic headers, rich-text flattening, and formula neutralization | ☑ |
| T4 | Implement and test schema-v1 `TicketJsonWriter` / `TicketExportDocument` with typed values | ☑ |
| T5 | Implement `ExportTicketsEndpoint` streaming, roles, content types, content disposition, filenames, empty results, and endpoint tests | ☑ |
| T6 | Add Angular `TicketExportService` blob download, server filename parsing, object-URL cleanup, loading/error contract, and service specs | ☑ |
| T7 | Add export menu and tests to simple search per wireframe | ☑ |
| T8 | Add export menus and tests to advanced search and saved-query view per wireframes | ☑ |
| T9 | Add PT/EN `ticketExport.*` catalog keys, completeness coverage, and update user-facing/domain/architecture docs | ☑ |
| T10 | Recheck FC1–FC12; run affected backend/frontend tests, ArchitectureTest, catalog checks, `mvn verify`, Angular build/tests, lints, and dev smoke | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `SearchTicketsByQueryEndpointTest` / saved-query tests — private project exclusion and readable project inclusion | T1 | ☑ |
| TC2 | `TicketExportServiceTest` — source validation, visibility IDs, ordering, empty results, overflow, batched custom fields | T2 | ☑ |
| TC3 | `TicketCsvWriterTest` — RFC 4180, dynamic columns, nulls/newlines, plain text, formula safety | T3 | ☑ |
| TC4 | `TicketJsonWriterTest` — schema envelope, typed fields/custom fields, stable ISO values | T4 | ☑ |
| TC5 | `TicketExportEndpointTest` — roles, CSV/JSON content, headers, filename, invalid requests, 10,000 cap | T5 | ☑ |
| TC6 | `ArchitectureTest` — endpoint/request contract remains compliant | T5 | ☑ |
| TC7 | `ticket-export.service.spec.ts` — payload, blob filename/download, URL cleanup, errors | T6 | ☑ |
| TC8 | Search component specs — source mapping, menu states, loading/errors, PT/EN rerender | T7–T9 | ☑ |
| TC9 | PT/EN `ticketExport.*` key parity plus catalog-backed component rerender coverage | T9 | ☑ |
| TC10 | Full backend/frontend gates and dev smoke | T10 | ☑ |

**Development approval:** approved 2026-07-17 — tasks: T1, T2, T3, T4, T5, T6, T7, T8, T9, T10

**Implementation notes:**

- T1–T5 evidence: shared query execution now applies readable-project IDs; export source validation, non-deleted filtering, deterministic ordering, 10,001-row overflow detection, batched typed custom fields, CSV/JSON writers, and the secured streamed endpoint are covered by the query, service, writer, endpoint, and architecture tests listed in TC1–TC6.
- T6–T8 evidence: `TicketExportService` posts the stable source discriminator contract as a blob request, honors server filenames, cleans temporary browser resources, and is used by the simple, advanced, and saved-query result menus with duplicate-submit prevention, error feedback, and PT/EN rerender specs.
- T9 evidence: both runtime catalogs contain matching `ticketExport.action`, `ticketExport.errorLimit`, and `ticketExport.errorGeneral` keys. Domain vocabulary/invariants, catalog click paths, export-menu gallery guidance, README capability, architecture package/service/API/route maps, feature index, and backlog status are synchronized.
- T10 evidence: FC1–FC12 were rechecked against implementation and docs; `mvn verify`, `npm run build`, all Angular tests, runtime catalog/canonical-routing checks, and changed-file lints pass. `mvn quarkus:dev` served `/`, `/search`, and `/search/advanced` with HTTP 200; unauthenticated `POST /api/tickets/export` returned 401; OpenAPI exposed `/api/tickets/export`.

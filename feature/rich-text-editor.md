# Rich text editor

**Feature version:** 1  
**Status:** done  
**Requested:** 2026-07-11 (backlog Order 2)

## Summary

Reuse the existing **`app-rich-text-editor`** (already used for ticket **comments**) as the shared editor for:

- Ticket **Description** (create + detail edit)
- **Text** custom fields (create + detail)
- **Project description** and **ticket template description** (**FQ2**)

Read surfaces render HTML (Angular sanitization, same pattern as comments). Length limits that already exist (ticket Description / Text / template description **1200**) are enforced on **plain text**, not HTML markup (**FQ3**). No data migration (**FQ7**). Phase-template objective/deliverables stay plain textarea (**FQ8**).

## Wireframe

**Guide:** update when **FQ*n*** change which surfaces adopt the editor.

| Field | Value |
|-------|-------|
| **Source** | ASCII below + gallery §3.6 |
| **Last updated** | 2026-07-11 |

### Control: `app-rich-text-editor` (shared)

| Region | Elements | Notes |
|--------|----------|-------|
| Toolbar | Bold, italic, underline, list, link, clear | Existing |
| Body | contenteditable | HTML emit; plain-text length for limits (**FQ3**) |
| Disabled | Greyed / non-editable | Orphan CF / deleted ticket |
| Form binding | `ControlValueAccessor` | Reactive forms (**AQ1**) |

```
┌─────────────────────────────────────────┐
│ [B] [I] [U] [• list] [link] [clear]     │
├─────────────────────────────────────────┤
│  contenteditable…                       │
└─────────────────────────────────────────┘
```

### Screen: create ticket

| Region | Elements |
|--------|----------|
| Descrição | `app-rich-text-editor` |
| Text CF | Same editor in `CustomFieldFormSection` |

### Screen: ticket detail

| Region | Elements |
|--------|----------|
| Edit Descrição / Text CF | Rich text editor |
| View Descrição / Text CF | Sanitized HTML (**FQ4**, **FQ5**) |
| Comments | Unchanged |

### Screen: project create / edit

| Region | Elements |
|--------|----------|
| Project description | Rich text editor (**FQ2**) |
| Ticket template description | Rich text editor when template enabled (**FQ2**) |
| Phase template objective / deliverables | **Textarea** unchanged (**FQ8**) |

### Screen: project list / hub (read)

| Region | Elements |
|--------|----------|
| Project description | Sanitized HTML render (**FQ4**) |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | Frontend + light `ticket` / `customfield` / `project` validation (**FQ3**) |
| Packages / files | `rich-text-editor` (CVA, plain-text length); shared HTML display helper/pipe; `ticket-form`; `ticket-view`; `custom-field-form-section`; `project-edit`; project list/hub templates |
| API | Keep storing HTML strings; replace/augment `@Size(max=1200)` / Text / template checks with **plain-text** length (**FQ3**, **AQ2**) |
| UI | Editors on listed surfaces; HTML read mode for Description, Text CF, project description |
| Schema / seed | None |
| Tests | Angular specs (CVA, surfaces); Java tests for plain-text length validator; template description / CF Text length |
| Docs | gallery §3.6; feature-catalog; domain-spec Description/Text/Project description; README; ARCHITECTURE if needed |

### Risks

- HTML in search/`tsvector` and CSV (**FQ6**) — accepted as-is.
- XSS — rely on Angular `[innerHTML]` sanitization like comments (**FQ5**); no `bypassSecurityTrustHtml`.
- Existing plain text loads into editor without migration (**FQ7**).
- Project description currently unbounded `@NotBlank` — do **not** invent a new 1200 max; only switch editor + HTML display (**AQ3**).

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Which surfaces get the shared editor in v1? | answered | Ticket **Description** (create + edit) + **Text** custom fields |
| FQ2 | Include project / template description fields? | answered | **Yes** — project description + ticket template description |
| FQ3 | Max length: HTML vs plain text? Keep 1200? | answered | Keep **1200** on **plain text** (client + server) where that limit already exists |
| FQ4 | Read mode? | answered | Render **HTML** (same as comments) |
| FQ5 | Sanitize? | answered | **Yes** — Angular default sanitization on display; write path matches comments (no extra library) |
| FQ6 | Import / search? | answered | Store HTML as-is; import plain stays plain until edited; search indexes stored string |
| FQ7 | Migration of existing plain text? | answered | **No migration** — load as-is; HTML only after next save |
| FQ8 | Phase template objective / deliverables? (opened by **FQ2**) | answered | **No** — remain textarea (deliverables are line-based) |

## Architecture

| Area | Design |
|------|--------|
| Bounded contexts | UI in Angular; validation helpers in `ticket` / `customfield` / `project` / shared `infra` |
| Packages / layers | No new REST endpoints |
| API | Unchanged paths; validation semantics for length |
| Schema | None |
| Frontend | CVA editor; shared safe HTML display; wire forms |
| Tests | Unit + component specs; validator tests |

### Packages / layers

| Layer | Responsibility |
|-------|----------------|
| Angular `RichTextEditorComponent` | Implement `ControlValueAccessor`; expose plain-text length for validators; keep toolbar behaviour |
| Angular display | Bind read-only HTML via `[innerHTML]` (Angular sanitizes) — **no** bypass |
| Angular forms | Replace textareas on ticket-form, ticket-view description/Text CF, project-edit description + templateDescription |
| Java | `PlainTextLength` + `@PlainTextSize` used by ticket Description, Text CF, project template description |

### Validation (**FQ3**)

| Field | Rule |
|-------|------|
| Ticket Description | min 5 / max 1200 **plain text** (`@PlainTextSize`) |
| Text CF | max 1200 **plain text** |
| Ticket template description | 5–1200 **plain text** when present |
| Project description | Required non-blank (existing); no new max |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Form integration: CVA vs only `value`/`valueChange`? | answered | **ControlValueAccessor** so reactive forms work cleanly |
| AQ2 | Where does server plain-text length live? | answered | Shared `infra.PlainTextLength` + `@PlainTextSize` |
| AQ3 | New max length for project description? | answered | **No** — only editor + HTML display |
| AQ4 | Display helper? | answered | `[innerHTML]` with Angular sanitization (comments pattern) |

## Changelog

### Shared rich text for Description, Text CF, and project/template — 2026-07-11

**Version:** 1  
**Status:** done

**Description:** Adopt `app-rich-text-editor` for ticket Description, Text custom fields, project description, and ticket template description; HTML read mode; plain-text 1200 where limits already exist.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [create-ticket](create-ticket.md) | Descrição rich text |
| [ticket-management](ticket-management.md) | Detail edit/view Description |
| [custom-fields](custom-fields.md) | Text type shared editor |
| [project-administration](project-administration.md) | Project + template description editors; hub/list HTML display |
| Ticket comments | Unchanged |
| Ticket search / import | HTML as-is (**FQ6**) |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Ticket Description + Text CF use `app-rich-text-editor` | FQ1, Wireframe | ☑ |
| FC2 | Project description + ticket template description use editor | FQ2, Wireframe | ☑ |
| FC3 | Phase template fields remain textarea | FQ8 | ☑ |
| FC4 | Read mode shows sanitized HTML for Description, Text CF, project description | FQ4, FQ5 | ☑ |
| FC5 | Plain-text 1200 (and template 5–1200) enforced client + server | FQ3, AQ2 | ☑ |
| FC6 | Existing plain-text values load without migration | FQ7 | ☑ |
| FC7 | Import/search store/index as-is | FQ6 | ☑ |
| FC8 | Gallery §3.6 lists all adopting surfaces | Docs | ☑ |
| FC9 | feature-catalog + domain-spec + README updated | Docs | ☑ |
| FC10 | Wireframe screens match live UI | Wireframe | ☑ |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | Java `PlainTextLength` (or equivalent) + use for ticket Description validation (create/update) | ☑ |
| T2 | Apply plain-text max to Text CF + ticket template description checks | ☑ |
| T3 | Backend tests for plain-text length (HTML under limit, plain over limit fails) | ☑ |
| T4 | `RichTextEditorComponent` → `ControlValueAccessor` + plain-text length helper for validators | ☑ |
| T5 | Angular: ticket-form Description + Text CF (`CustomFieldFormSection`) | ☑ |
| T6 | Angular: ticket-view Description + Text CF edit/view (HTML read) | ☑ |
| T7 | Angular: project-edit description + templateDescription; list/hub HTML for project description | ☑ |
| T8 | Angular specs (CVA; at least one form surface; badge/read HTML smoke) | ☑ |
| T9 | Docs: gallery, feature-catalog, domain-spec, README; cross-link create-ticket / custom-fields | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | Ticket create/update — HTML with plain text ≤1200 OK; plain >1200 rejected | T1, T3 | ☑ |
| TC2 | Text CF / template description plain-text max | T2, T3 | ☑ |
| TC3 | Rich-text editor CVA writes value on input | T4, T8 | ☑ |
| TC4 | Ticket form / CF section uses editor (not textarea) | T5, T8 | ☑ |
| TC5 | Project edit description uses editor; phase fields still textarea | T7, T8 | ☑ |

**Development approval:** approved 2026-07-11 — tasks: T1, T2, T3, T4, T5, T6, T7, T8, T9

**Implementation notes:** `PlainTextLength` + `@PlainTextSize` on ticket Description; TEXT CF and template description use plain-text length. Angular CVA editor on create/edit Description, Text CF, project + template description; HTML read via `[innerHTML]`. `mvn verify` + `npm run build` green (2026-07-11).

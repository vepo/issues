# Git integration

**Feature version:** 1  
**Status:** done  
**Requested:** 2026-07-11

## Summary

Associate a **git repository** with a **project** and surface **commits** that reference ticket identifiers on the ticket **activity / history** feed.

Developers often mention keys such as `ISS-003` in commit messages. Issues already generates unique `{prefix}-{seq}` identifiers; this feature closes the loop so linked commits appear on the ticket without manual comments.

**v1 goal:** one project-level repository association + forge push webhook **and** authenticated inbound API ingest of commits that mention in-project ticket ids → immutable **linked commits** on the activity feed (SHA, message, author, deep link when possible).

**Out of scope for v1:** pull/merge request linking, branch status on Kanban, auto-transition on commit/merge (**FQ9**), server-side clone/poll of remotes, multi-repo mapping, blame/diff UI, subscriber notify on commit link (**FQ6**). Outbound **webhooks** (Issues → external systems) stay a separate backlog item.

Related but distinct: [agentic-integration.md](agentic-integration.md) (agents read/update tickets via PAT/MCP) — agents/CI authenticate **inbound commit API** with PAT or project SA; commit→activity is this capability.

## Decisions

| ID | Decision | Source |
|----|----------|--------|
| D1 | Ticket linking uses project **prefix** + numeric seq in commit subject/body (`\b{PREFIX}-\d+\b`, case-insensitive prefix) | **FQ7**, **AQ5**; identifier invariant |
| D2 | Auto-move / auto-close from commit or merge is **out of v1** | **FQ9** `not valid` |
| D3 | Outbound Issues webhooks remain a **separate** backlog idea | Backlog; agentic FQ7 |
| D4 | Repo URL on the project is useful for **commit deep links** even when ingest is push-based | Exploration |
| D5 | Ingest = forge **webhook (A)** + authenticated **inbound API (B)**; no poll/clone | **FQ1** |
| D6 | Provider-agnostic remote URL; deep links best-effort from payload URL or host templates | **FQ2**, **AQ4** |
| D7 | **One** git repository per project in v1 | **FQ3** |
| D8 | Persist **`tb_ticket_commits`**; project into activity feed (not a `TicketHistoryAction` alone) | **FQ4** |
| D9 | Match committer email to Issues user when possible; else show author from payload (`matched_user` nullable) | **FQ5** |
| D10 | **No** notify subscribers on commit link in v1 | **FQ6** |
| D11 | Soft-deleted tickets excluded from linking; multiple ids → one linked commit per ticket | **FQ7** |
| D12 | Git association configurable by **owner / admin** only | **FQ8** |
| D13 | Webhook: per-project HMAC secret; API ingest: **PAT** or **project SA** Bearer | **FQ10** |

## Wireframe

**Guide:** layout reference for UI implementation — update when Scope or **FQ*n*** decisions change.

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-11 (FQ/AQ accepted) |

### Screen: `/projects/:projectId/edit` — Repositório Git (extends project edit)

| Region | Elements | Notes |
|--------|----------|-------|
| Header / existing form | Unchanged project fields | [project-administration](project-administration.md) |
| **Repositório Git** | Remote URL; optional provider hint (GitHub / GitLab / Gitea / Outro); default branch (optional) | **FQ8** owner/admin only |
| Setup help | Both webhook **and** CI/API steps (**FQ1** A+B) | Payload URL + secret; PAT/SA note for API |
| Actions | Save with project; **Regenerar segredo**; **Copiar** secret / webhook URL | Secret shown once / confirm regenerate |

```
┌─────────────────────────────────────────────────────────────┐
│  Editar projeto                                              │
├─────────────────────────────────────────────────────────────┤
│  (existing fields…)                                         │
├─────────────────────────────────────────────────────────────┤
│  Repositório Git                                            │
│  URL          [ https://github.com/org/repo              ]  │
│  Provedor     [ GitHub ▼ ]                                  │
│  Branch       [ main                                     ]  │
│                                                             │
│  Conectar commits                                           │
│  1. Cole a URL do repositório e salve                       │
│  2. Webhook (push): configure no forge com:                 │
│     Payload URL  https://…/api/projects/…/git/webhook       │
│     Secret       ••••••••  [ Copiar ]  [ Regenerar ]        │
│  3. API (CI / agente): POST commits com Bearer PAT ou SA    │
│     POST /api/projects/{id}/git/commits                     │
│                                                             │
│  [ Salvar ]                                                 │
└─────────────────────────────────────────────────────────────┘
```

### Screen: `/ticket/:ticketIdentifier` — activity feed (extends ticket detail)

| Region | Elements | Notes |
|--------|----------|-------|
| Activity / history item | Commit icon; short SHA; message excerpt; author (matched user name or payload author); timestamp; external link | From **linked commit** (**FQ4**); merged in `buildActivityFeed` |
| Empty / no commits | No special empty state beyond existing feed | |

```
┌─────────────────────────────────────────────────────────────┐
│  ISS-003 · Fix login redirect                               │
├─────────────────────────────────────────────────────────────┤
│  [ Histórico ]  [ Comentários ]                             │
│                                                             │
│  ● Commit a1b2c3d                                           │
│    fix(auth): redirect after login (ISS-003)                │
│    Alice <alice@…>  ·  11 jul 2026  ·  [ Abrir no GitHub ]  │
│                                                             │
│  ● Status  To Do → In Progress                              │
│    Bob  ·  10 jul 2026                                      │
└─────────────────────────────────────────────────────────────┘
```

### Non-UI surfaces

| Surface | Layout | Notes |
|---------|--------|-------|
| Forge webhook | N/A | `POST …/git/webhook` + HMAC (**AQ3**) |
| Inbound commit API | N/A | `POST …/git/commits` + Bearer PAT/SA (**FQ10**) |
| Commit deep-link builder | N/A | Payload URL preferred; else remote + SHA (**AQ4**, **D4**) |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | New **`git`** context (`dev.vepo.issues.git`); **`project`** (association fields/UI); **`ticket`** activity merge; identity for PAT/SA on API ingest (agentic) |
| Packages / files | `git.*` entities/repos/services/endpoints; project edit Angular; `activity-feed.utils` merge commits; Flyway baseline |
| API | `GET/PUT` project git association; regenerate secret; webhook; inbound commits; ticket responses/expanded include linked commits (or list on ticket) |
| UI | Project edit **Repositório Git**; ticket activity commit rows |
| Schema / seed | `tb_project_git_repositories` (1:1 project); `tb_ticket_commits`; `dev-import.sql` sample association + optional commits |
| Tests | Webhook HMAC + idempotency; API auth; identifier parse; soft-delete skip; activity rendering; ArchUnit `git` boundaries |
| Docs | domain-spec (git terms + context); feature-catalog; README; ARCHITECTURE §13; bounded-contexts rule |

### Risks

- Webhook retries / CI re-runs without **idempotency** `(ticket_id, sha)` spam the feed — unique constraint required.
- Private remotes: paste URL does not grant read access — poll/clone remains out of scope.
- Prefix parse must resolve to an **existing non-deleted** ticket in the **same project** as the association.
- Agentic PAT/SA must exist (or land first) for API ingest path — webhook-only still works without agentic.
- Confusing this with outbound **webhooks** backlog — copy must say **commit ingest**.

### Feature questions (FQ*n*)

Product, scope, UX, and domain decisions. Status: `open` | `answered` | `not valid`.

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | How do commits enter Issues in v1? **(A)** forge push webhook, **(B)** authenticated inbound API, **(C)** server poll/clone, or **A+B**? | answered | **A+B** — forge push webhook + authenticated inbound API; **no** poll/clone |
| FQ2 | Which **providers** in v1? | answered | **Provider-agnostic** URL + generic ingest; deep links best-effort by detected host / templates |
| FQ3 | **One** git repository per project in v1, or **multiple** remotes? | answered | **One** per project in v1 |
| FQ4 | Persist as **`TicketHistoryAction`**, dedicated commit entity, or both? | answered | **Dedicated `tb_ticket_commits`** projected into the activity feed |
| FQ5 | History actor when committer email does not match an Issues user? | answered | **Match email** when possible; else **nullable matched user** on the linked commit — show author name/email from payload (no system bot user; history `user_id` unchanged) |
| FQ6 | Notify ticket subscribers on commit link? | answered | **Never in v1** |
| FQ7 | Matching rules: subject vs body; multiple ids; soft-deleted? | answered | **Subject + body**; **multiple** ticket ids → one linked commit per ticket; **exclude soft-deleted** |
| FQ8 | Who may configure the association? | answered | **Project owner / admin** only (same as project edit) |
| FQ9 | Auto-transition when commit/merge mentions a ticket? | not valid | Out of v1 (**D2**) |
| FQ10 | Auth for inbound API ingest? | answered | **Bearer PAT or project SA** (agentic); forge webhook uses **per-project HMAC secret** (not PAT) |

**Gate:** blocking **FQ*n*** resolved → phase 2 complete with Architecture below.

## Architecture

| Area | Design |
|------|--------|
| Bounded contexts | **`git`** owns association, ingest, linked commits; may depend on platform, identity, project, ticket. **`project`** exposes association on edit. **`ticket`** activity UI merges linked commits. No `git` → `notifications` in v1 (**FQ6**). |
| Packages / layers | `git` → Endpoint → `GitCommitService` / `ProjectGitRepositoryService` → repositories. Identifier parse util in `git`. Do **not** put forge logic in `TicketHistoryService` (**AQ6**). |
| API | See API surface below |
| Schema / seed | `tb_project_git_repositories`, `tb_ticket_commits`; amend `V1.0.0` only |
| Cross-context | `GitCommitService` loads tickets by identifier within project; matches `User` by email; persists linked commits. **No** CDI notify events (**AQ7**). |
| Frontend | Project edit section; extend `buildActivityFeed` with commit items; codegen after OpenAPI |
| Tests | Endpoint tests (webhook, API, idempotency, parse); Angular project-edit + activity specs; ArchUnit package |

### API surface

| Method | Path | Auth | Request / response |
|--------|------|------|--------------------|
| `GET` | `/api/projects/{projectId}/git` | owner/admin (edit) or member read if useful | `ProjectGitRepositoryResponse` (URL, provider, defaultBranch, webhookUrl, hasSecret — never raw secret after create) |
| `PUT` | `/api/projects/{projectId}/git` | owner/admin | `ProjectGitRepositoryRequest` → response; creates secret on first save if missing |
| `POST` | `/api/projects/{projectId}/git/regenerate-secret` | owner/admin | Returns new secret **once** in response |
| `POST` | `/api/projects/{projectId}/git/webhook` | HMAC (forge signature); no JWT | Raw/provider payload → 204 / 200; link commits |
| `POST` | `/api/projects/{projectId}/git/commits` | Bearer PAT or SA with project access | `IngestCommitsRequest` → `IngestCommitsResponse` (linked count / skipped) |
| (existing) | Ticket get / expanded | unchanged | Include `linkedCommits` (or dedicated list) for activity merge |

**`IngestCommitsRequest`** (sketch): list of `{ sha, message, authorName, authorEmail, committedAt, commitUrl? }`.

**Webhook:** verify HMAC with stored secret; normalize provider payload to the same commit shape; ignore pushes that do not match associated remote when detectable.

### Schema

**`tb_project_git_repositories`**

| Column | Notes |
|--------|-------|
| `id` | PK |
| `project_id` | UNIQUE NOT NULL FK → `tb_projects` (**FQ3** one row) |
| `remote_url` | NOT NULL |
| `provider` | optional enum/string hint (GITHUB, GITLAB, GITEA, OTHER) |
| `default_branch` | optional |
| `webhook_secret_hash` | store hashed secret; plaintext only in regenerate response |
| `created_at` / `updated_at` | |

**`tb_ticket_commits`**

| Column | Notes |
|--------|-------|
| `id` | PK |
| `ticket_id` | FK → `tb_tickets` |
| `project_id` | FK (denorm for queries) |
| `sha` | NOT NULL |
| `message` | NOT NULL |
| `author_name` / `author_email` | from payload |
| `matched_user_id` | NULL FK → `tb_users` (**FQ5**) |
| `committed_at` | |
| `commit_url` | nullable deep link |
| `created_at` | ingest time |
| UNIQUE | `(ticket_id, sha)` idempotency |

### Ingest algorithm

1. Authenticate (HMAC or Bearer).
2. Normalize commits; for each message, find `\b{prefix}-\d+\b` case-insensitive (**AQ5**).
3. Resolve tickets in **this project**, **not soft-deleted** (**FQ7**).
4. Upsert linked commit per (ticket, sha); skip duplicates.
5. Set `matched_user_id` if email matches a user; always keep author fields.
6. Deep link: use `commitUrl` if present; else template from `remote_url` + provider + sha (**AQ4**).

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Package placement? | answered | **`dev.vepo.issues.git`** |
| AQ2 | Association storage? | answered | **`tb_project_git_repositories`** with unique `project_id` (one row / project) |
| AQ3 | Webhook verification / path? | answered | **HMAC** per association; `POST /api/projects/{projectId}/git/webhook` |
| AQ4 | Deep-link strategy? | answered | **Prefer full URL from payload**; else build from stored remote + SHA via provider templates |
| AQ5 | Identifier parse? | answered | Shared util in `git`; **case-insensitive** prefix match; resolve to ticket by project |
| AQ6 | Who owns persistence? | answered | **`GitCommitService`** owns ingest + `tb_ticket_commits`; does not extend forge logic into `TicketHistoryService` |
| AQ7 | CDI events for notifications? | answered | **No** in v1 (**FQ6**) |

**Gate:** blocking AQs answered → task break.

## Scope (v1)

| ID | Scope item | Notes |
|----|------------|-------|
| S1 | Associate one git remote with project (URL, provider, optional default branch) | **FQ2**, **FQ3**, **FQ8** |
| S2 | Forge webhook + inbound API ingest | **FQ1**, **FQ10** |
| S3 | Parse ticket identifiers from subject+body; resolve non-deleted tickets in project | **FQ7**, **D1**, **AQ5** |
| S4 | Idempotent linked commits `(ticket_id, sha)` | **FQ4**, **FQ5** |
| S5 | Ticket activity UI shows commit entries with deep link when possible | Wireframe, **D4** |
| S6 | Project edit UI — Repositório Git + webhook + API setup help | Wireframe |
| S7 | Domain + feature-catalog + README + ARCHITECTURE updates | Docs |

**Explicitly deferred:** PR/MR sync; Kanban branch badges; auto-transition; remote poll/clone; multi-repo; outbound Issues→world webhooks; notify on link.

## Changelog

### Git repository association and commit history — 2026-07-11

**Version:** 1  
**Status:** done  
**Change name:** Project git association + linked commits on activity  

**Description:** Allow a project to associate one git repository and record commits that reference ticket identifiers as linked commits on the ticket activity feed, via forge webhook and authenticated inbound API.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Project administration | Edit form gains **Repositório Git** section |
| Ticket management / activity | Linked commits merged into activity feed |
| Notifications / email | **None** in v1 (**FQ6**) |
| Agentic integration | PAT/SA authenticate `POST …/git/commits`; feature remains independent (webhook works without agentic) |
| Webhooks (outbound backlog) | Unchanged; inbound forge webhook is this feature |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Project owner/admin can associate one git remote (URL, provider, optional branch) | S1, FQ3, FQ8, Wireframe | ☑ |
| FC2 | Commits enter via forge webhook **and** authenticated inbound API | S2, FQ1 | ☑ |
| FC3 | Subject+body `{prefix}-{seq}` links to non-deleted tickets in that project (multi-id OK) | S3, FQ7, D1 | ☑ |
| FC4 | Same `(ticket_id, sha)` is not duplicated on retry | S4 | ☑ |
| FC5 | Activity feed shows linked commit (SHA, message, author, timestamp, link when available) | S5, Wireframe, FQ4 | ☑ |
| FC6 | Project edit UI matches Wireframe (webhook + API setup, copy/regenerate secret) | S6, Wireframe | ☑ |
| FC7 | Matched user when email matches; else author from payload with null matched user | FQ5 | ☑ |
| FC8 | No subscriber notification on commit link | FQ6 | ☑ |
| FC9 | No auto-transition from commit/merge | FQ9, D2 | ☑ |
| FC10 | Webhook HMAC secret; API ingest PAT/SA | FQ10, AQ3 | ☑ |
| FC11 | Package `dev.vepo.issues.git`; schema tables per Architecture | AQ1, AQ2, Architecture | ☑ |
| FC12 | `domain-specification.md` — git context + terms | Docs | ☑ |
| FC13 | `feature-catalog.md` — project edit git + ticket activity commit row | Docs | ☑ |
| FC14 | README Features — git / commit linking bullet | Docs | ☑ |
| FC15 | All FQs/AQs answered or not valid | FQ/AQ tables | ☑ |

#### Tasks

| ID | Deliverable | Done |
|----|-------------|------|
| T1 | Flyway baseline: `tb_project_git_repositories`, `tb_ticket_commits` (+ unique `(ticket_id, sha)`); JPA entities + repositories | ☑ |
| T2 | `ProjectGitRepositoryService` + `GET/PUT …/git` + `POST …/git/regenerate-secret`; secret hashing; owner/admin auth | ☑ |
| T3 | Identifier parse util + `GitCommitService` link algorithm (match email, idempotency, soft-delete skip, deep-link) | ☑ |
| T4 | `POST …/git/webhook` (HMAC verify, normalize payload) + endpoint tests | ☑ |
| T5 | `POST …/git/commits` (PAT/SA Bearer) + endpoint tests; graceful if agentic not yet shipped (document dependency) | ☑ |
| T6 | Expose linked commits on ticket get/expanded (or list endpoint); activity feed merge on Angular | ☑ |
| T7 | Angular project edit **Repositório Git** (wireframe: URL, provider, branch, webhook URL, copy/regenerate, API help) + specs | ☑ |
| T8 | Angular ticket activity commit row (icon, SHA, message, author, link) + specs | ☑ |
| T9 | `dev-import.sql` sample git association (+ optional linked commits) | ☑ |
| T10 | Docs: domain-spec (if needed at ship), feature-catalog, README, ARCHITECTURE §13; ArchUnit `git` package rule if added | ☑ |

#### Test coverage

| ID | Covers | Tasks | Done |
|----|--------|-------|------|
| TC1 | Association CRUD + regenerate secret + auth | T2 | ☑ |
| TC2 | Parse subject+body; multi-ticket; soft-deleted skipped; case-insensitive prefix | T3 | ☑ |
| TC3 | Webhook HMAC accept/reject; idempotent re-delivery | T4 | ☑ |
| TC4 | Inbound API auth + ingest + idempotency | T5 | ☑ |
| TC5 | Ticket response includes linked commits | T6 | ☑ |
| TC6 | Angular project edit git section | T7 | ☑ |
| TC7 | Angular activity commit row / feed merge | T6, T8 | ☑ |
| TC8 | Doc review checklist | T10 | ☑ |

**Development approval:** approved 2026-07-16 — tasks: T1, T2, T3, T4, T5, T6, T7, T8, T9, T10

**Implementation notes (2026-07-16):**

- Package `dev.vepo.issues.git`; tables `tb_project_git_repositories` + `tb_ticket_commits`
- APIs: GET/PUT `/git`, regenerate-secret, webhook (HMAC via InputStream), ingest commits
- `TicketExpandedResponse.linkedCommits`; Angular project edit Repositório Git; history tab commits
- Seed sample association + ISS-003 commit
- Webhook secret stored recoverable (plaintext pre-prod) for HMAC
- `mvn verify` + Angular specs green

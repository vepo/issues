# Git integration

**Feature version:** 1  
**Status:** planned  
**Requested:** 2026-07-11

## Summary

Associate a **git repository** with a **project** and surface **commits** that reference ticket identifiers on the ticket **activity / history** feed.

Developers often mention keys such as `ISS-003` in commit messages. Issues already generates unique `{prefix}-{seq}` identifiers; this feature closes the loop so linked commits appear on the ticket without manual comments.

**v1 goal:** project-level repository association + ingest of commits that mention in-project ticket ids → immutable history/activity entries (SHA, message, author, deep link when possible).

**Out of scope for v1 (provisional):** pull/merge request linking, branch status on Kanban, auto-transition on commit/merge, server-side clone/poll of remotes, multi-repo monorepo mapping, blame/diff UI. Outbound **webhooks** (Issues → external systems) stay a separate backlog item; this feature may still **receive** forge push webhooks or an inbound API.

Related but distinct: [agentic-integration.md](agentic-integration.md) (agents read/update tickets via PAT/MCP) — agents may *post* commits via ingest, but commit→history is this capability.

## Decisions

| ID | Decision | Source |
|----|----------|--------|
| D1 | Ticket linking uses project **prefix** + numeric seq in commit subject/body (`\b{PREFIX}-\d+\b`) | Exploration; identifier invariant |
| D2 | Auto-move / auto-close from commit or merge is **out of v1** | Exploration lean |
| D3 | Outbound Issues webhooks remain a **separate** backlog idea | Backlog #14; agentic FQ7 |
| D4 | Repo URL on the project is useful for **commit deep links** even when ingest is push-based | Exploration |

_Blocking product choices remain in **FQ*n*** below — D1–D4 are non-blocking lean defaults until overridden._

## Wireframe

**Guide:** layout reference for UI implementation — update when Scope or **FQ*n*** decisions change.

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-11 |

### Screen: `/projects/:projectId/edit` — Repositório Git (extends project edit)

| Region | Elements | Notes |
|--------|----------|-------|
| Header / existing form | Unchanged project fields | [project-administration](project-administration.md) |
| **Repositório Git** | Remote URL; optional provider (GitHub / GitLab / Gitea / Outro); default branch (optional) | Owner or admin only — same as project edit |
| Setup help | Short steps for webhook **or** CI/API ingest (depends on **FQ1**) | Copy webhook URL + secret when **FQ1** = webhook |
| Actions | Save with project; **Regenerar segredo** if webhook secret used | Secret shown once / confirm regenerate |

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
│  2. (webhook) Configure push no forge com:                  │
│     Payload URL  https://…/api/projects/…/git/webhook       │
│     Secret       ••••••••  [ Copiar ]  [ Regenerar ]        │
│     — ou —                                                  │
│  2. (API) No CI, POST commits autenticado (PAT / secret)    │
│                                                             │
│  [ Salvar ]                                                 │
└─────────────────────────────────────────────────────────────┘
```

### Screen: `/ticket/:ticketIdentifier` — activity feed (extends ticket detail)

| Region | Elements | Notes |
|--------|----------|-------|
| Activity / history item | Commit icon; short SHA; message excerpt; author; timestamp; external link | New history action or projected commit item (**FQ4**) |
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
| Ingest API and/or forge webhook | N/A | **FQ1** |
| Commit deep-link builder | N/A | Uses stored remote URL + SHA (**D4**) |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | New **`git` / SCM** context (ingest, association); **`project`** (config UI/API); **`ticket` / `ticket.history`** (display + persistence of linked events) |
| Packages / files | Project edit Angular; ticket activity feed utils; `TicketHistoryAction` and/or commit entity; ingest endpoints; Flyway baseline |
| API | Project git association CRUD (or fields on update project); ingest webhook and/or `POST` commits; history/expanded ticket responses include commit events |
| UI | Project edit **Repositório Git**; ticket activity commit rows (**Wireframe**) |
| Schema / seed | Association columns or `tb_project_git_repositories`; optional commit table; history action / nullable user (**FQ5**); `dev-import.sql` sample association |
| Tests | Ingest idempotency; identifier parse; project auth; activity rendering; ArchUnit package boundaries |
| Docs | domain-spec (Git repository association, Linked commit, history action); feature-catalog (project edit + ticket activity); README Features; ARCHITECTURE §13 |

### Risks

- **`user_id` NOT NULL** on `tb_ticket_history` — commit author may not be an Issues user (**FQ5**).
- Webhook retries / CI re-runs without **idempotency** (SHA + ticket) spam the feed.
- Private remotes: “paste URL” does not grant read access — poll/clone (**FQ1** option C) is high cost.
- Prefix collision across forges is mitigated by **globally unique** ticket identifiers today, but parse still must resolve to an existing ticket.
- Notification noise if every linked commit notifies subscribers (**FQ6**).
- Confusing this with outbound **webhooks** backlog or agentic MCP — copy and docs must distinguish **commit ingest**.

### Feature questions (FQ*n*)

Product, scope, UX, and domain decisions. Status: `open` | `answered` | `not valid`.

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | How do commits enter Issues in v1? **(A)** forge push webhook (GitHub/GitLab/…), **(B)** authenticated inbound API (CI / hook / agent posts payload), **(C)** server poll/clone remote, or **A+B**? | open | |
| FQ2 | Which **providers** in v1 — GitHub only, GitHub+GitLab, or provider-agnostic URL + generic ingest (deep links best-effort)? | open | |
| FQ3 | **One** git repository per project in v1, or allow **multiple** remotes? | open | |
| FQ4 | Persist as new **`TicketHistoryAction`** (e.g. `COMMIT_LINKED`) on existing history, a **dedicated commit entity** (`tb_ticket_commits`) projected into the activity feed, or both? | open | |
| FQ5 | Who is the **history actor** when the committer email does not match an Issues user — match email when possible, else **system/bot user**, else **nullable user** on SCM events? | open | |
| FQ6 | Should linking a commit **notify** ticket subscribers (in-app / email)? Default **off**, **on**, or never in v1? | open | |
| FQ7 | Matching rules: scan **subject only** vs **subject + body**; support **multiple** ticket ids in one commit (one history row per ticket)? Include **soft-deleted** tickets? | open | |
| FQ8 | Who may configure the association — **project owner / admin** only (same as project edit), or any **project member**? | open | Lean: owner/admin only |
| FQ9 | Should v1 include **auto-transition** when a commit/merge mentions a ticket (e.g. move to Done)? | open | Lean **no** (**D2**) — confirm `not valid` or defer |
| FQ10 | Auth for inbound API ingest (**FQ1** B): reuse **personal API tokens** (agentic), **per-project webhook secret**, or both? | open | Opened by **FQ1** when B or A+B |

**Gate:** phase 2 blocked while blocking **FQ*n*** remain `open` (**FQ1–FQ7**, **FQ9–FQ10**; **FQ8** lean owner/admin unless challenged).

## Architecture

_Pending phase 2 — after blocking **FQ*n*** are answered._

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Package placement — `dev.vepo.issues.git` vs nest under `project` / `ticket`? | open | |
| AQ2 | Association storage — columns on `tb_projects` vs `tb_project_git_repositories`? | open | |
| AQ3 | Webhook verification — HMAC secret per project; endpoint path shape? | open | Depends on **FQ1** |
| AQ4 | Deep-link URL templates per provider vs store full commit URL from payload? | open | |
| AQ5 | Identifier parse — shared utility; case sensitivity of prefix? | open | |
| AQ6 | Extend `TicketHistoryService` vs `GitCommitService` owning persistence and calling history? | open | Depends on **FQ4** |
| AQ7 | Should ingest fire CDI events for notifications (if **FQ6** on)? | open | |

## Scope (v1 — provisional, refine after FQs)

| ID | Scope item | Notes |
|----|------------|-------|
| S1 | Associate git remote with project (URL, provider, optional default branch) | **FQ2**, **FQ3**, **FQ8** |
| S2 | Ingest path for commits (webhook and/or API) | **FQ1**, **FQ10** |
| S3 | Parse ticket identifiers from commit message; resolve tickets in that project | **FQ7**, **D1** |
| S4 | Idempotent link of commit → ticket history/activity | **FQ4**, **FQ5** |
| S5 | Ticket activity UI shows commit entries with deep link when possible | **Wireframe**, **D4** |
| S6 | Project edit UI — Repositório Git + setup help | **Wireframe** |
| S7 | Domain + feature-catalog + README updates | Docs |

**Explicitly deferred:** PR/MR sync; Kanban branch badges; auto-transition (**FQ9** lean); remote poll/clone unless **FQ1**=C; multi-repo unless **FQ3** allows; outbound Issues→world webhooks.

## Changelog

### Git repository association and commit history — 2026-07-11

**Version:** 1  
**Status:** planned

**Description:** Allow a project to associate a git repository and record commits that reference ticket identifiers on the ticket activity/history feed.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Project administration | Edit form gains **Repositório Git** section |
| Ticket management / activity | New commit events in history/activity feed |
| Notifications / email | Only if **FQ6** enables notify-on-link |
| Agentic integration | May authenticate ingest (**FQ10**); does not replace this feature |
| Webhooks (outbound backlog) | Unchanged — separate idea; inbound forge webhook is optional here (**FQ1**) |
| Home activity | Optional later inclusion of commit events — not required for v1 unless scoped |

#### Feature checklist (phase 1 — build; update through phase 5; recheck before done)

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Project owner/admin can associate a git remote URL (and provider) with a project | S1, FQ8, Wireframe | ☐ |
| FC2 | Commits enter Issues via the chosen ingest path(s) for that association | S2, FQ1 | ☐ |
| FC3 | Commits mentioning `{prefix}-{seq}` link to the matching non-excluded ticket(s) | S3, FQ7, D1 | ☐ |
| FC4 | Same commit SHA + ticket is not duplicated on retry | S4 | ☐ |
| FC5 | Ticket activity/history shows linked commit (SHA, message, author, timestamp, link when available) | S5, Wireframe, FQ4 | ☐ |
| FC6 | Project edit UI matches **Wireframe** Repositório Git region (incl. setup help for chosen ingest) | S6, Wireframe | ☐ |
| FC7 | History actor behaviour matches **FQ5** for unknown committers | FQ5 | ☐ |
| FC8 | Subscriber notification behaviour matches **FQ6** | FQ6 | ☐ |
| FC9 | Auto-transition from commit/merge is not in v1 unless FQ9 answered otherwise | FQ9, D2 | ☐ |
| FC10 | `domain-specification.md` — Git repository association, Linked commit, history action/terms | Impact / Docs | ☐ |
| FC11 | `feature-catalog.md` — project edit git section + ticket activity commit row | Impact / Docs | ☐ |
| FC12 | README Features — git / commit linking bullet | Impact / Docs | ☐ |

#### Tasks (phase 3 — required before approval)

_Pending phase 2 architecture and phase 3 task break._

#### Test coverage (phase 3 — required before done)

_Pending phase 3._

**Development approval:** pending

**Implementation notes:** (fill after done)

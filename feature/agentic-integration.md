# Agentic Development integration

**Feature version:** 1  
**Status:** tasks-ready  
**Requested:** 2026-07-11

## Summary

Enable **coding agents** (Cursor, Claude Code, CI bots, scripts) to **read and update Issues** while implementing features — without using a human password or browser session.

**UX principle (FQ9):** agent configuration must be as **user-friendly as possible** — guided in-app setup, copy-ready snippets, sensible defaults, minimal manual env editing.

v1 delivers:

1. **Personal API tokens (PAT)** and **project service accounts** + tokens (**FQ1**).
2. **Full update** permissions matching the authenticated principal (**FQ2**).
3. Actor attribution: **Agente em nome de &lt;nome&gt;** (**FQ3**); for SA tokens, **&lt;nome&gt;** = service account display name (**FQ13**).
4. **Separate Java Quarkus MCP project** (Quarkiverse); Issues may later become a **multi-module** Maven reactor including that module (**FQ4**, **AQ7**).
5. **Composite ticket-context** API (**FQ5**).
6. **Guided Agent setup** using **configurable public URLs** via `application.properties` (**FQ10**, **FQ11**, **FQ14**).
7. Backup skill/docs in-repo (**FQ8**).

**Out of scope for v1:** webhooks (**FQ7**), Issues→Cursor SDK spawn, Python/Node MCP, admin UI for public URL.

## Decisions

| ID | Decision | Source |
|----|----------|--------|
| D1 | Guided configuration over power-user-only docs | FQ9 |
| D2 | Account settings: PAT + **Conectar agente** | FQ9, FQ10 |
| D3 | Secret once + copy MCP config from server (public URLs) | FQ6, FQ10, FQ11 |
| D4 | No required token expiry on primary path | FQ6 |
| D5 | MCP is **separate Quarkus project**; path to **multi-module** monorepo | FQ4, AQ7 |
| D6 | Full update as principal | FQ2 |
| D7 | Both PAT and project service accounts | FQ1 |
| D8 | UI: **Agente em nome de {displayName}**; persist `via_agent` | FQ3, AQ8 |
| D9 | Ticket context composite endpoint | FQ5 |
| D10 | Webhooks separate backlog | FQ7 |
| D11 | Service accounts at `/projects/:projectId/service-accounts` | FQ12 |
| D12 | SA “on behalf of” name = **service account display name** | FQ13 |
| D13 | Public base URLs: **`application.properties` only** in v1 | FQ14 |
| D14 | SA permissions = **project member–aligned** powers on that project | AQ9 |

## Wireframe

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-11 |

### Screen: `/account/settings` — PAT + Agent setup

| Region | Elements | Notes |
|--------|----------|-------|
| **Conectar agente** | IDE preset; **Gerar token e configuração**; copy snippet | **FQ9**, **FQ10** |
| Config preview | MCP JSON: MCP public URL + Issues API URL + token | From server config (**FQ11**, **FQ14**) |
| Tokens de API | List + **Revogar**; **Criar token** | |
| Secret reveal | Once | **FQ6** |

```
┌─────────────────────────────────────────────────────────────┐
│  Conta → Conectar agente + Tokens de API                    │
│  Cole em Cursor MCP: url=<mcp-public>/mcp  Authorization=…  │
└─────────────────────────────────────────────────────────────┘
```

### Screen: `/projects/:projectId/service-accounts` (**FQ12**)

| Region | Elements | Notes |
|--------|----------|-------|
| Nav | From project admin / hub | PM + admin |
| List | Name, created, last used, **Gerar token**, **Desativar** | |
| Create | Nome (display name); **Criar** | **FQ13** |

```
┌─────────────────────────────────────────────────────────────┐
│  Projeto → Contas de serviço                                │
│  │ bot-ci     …   [ Gerar token ] [ Desativar ] │           │
│  [ Nova conta de serviço ]                                  │
└─────────────────────────────────────────────────────────────┘
```

### Ticket detail — attribution (**FQ3**)

```
│  Agente em nome de Maria Silva · há 2 min   ← PAT            │
│  Agente em nome de bot-ci · há 1 min        ← SA (**FQ13**)  │
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `auth.apitoken`; `project.serviceaccount`; `ticket` context + `via_agent`; separate MCP Maven project/module |
| Packages / files | Issues app: token/SA/context/setup endpoints + Angular; **new** Quarkus MCP project calling Issues REST |
| API | PAT/SA CRUD; Bearer; `GET /tickets/{id}/context`; `GET /agent/setup-config`; MCP HTTP on MCP service |
| UI | Account setup; `/projects/:id/service-accounts`; history/comment labels |
| Schema / seed | Token + SA tables; `via_agent` on history/comments; `issues.public-base-url`, `issues.mcp-public-base-url` |
| Build | Prepare multi-module reactor (or sibling repo now, merge as module later) — **AQ7** |
| Tests | Endpoint + auth + attribution + Angular; MCP module tests against Issues API |
| Docs | domain-spec; feature-catalog; README; ARCHITECTURE (multi-module note) |

### Risks

- Separate MCP deploy doubles ops (URL + health); mitigate with clear props + setup snippet.
- Multi-module migration must not break single-module CI mid-flight — introduce MCP as sibling/module with its own `pom` first.
- SA member-aligned powers must still enforce project membership / ticket visibility rules.

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Auth model? | answered | **Both** PAT and project service accounts |
| FQ2 | Mutations? | answered | **Full update** as principal |
| FQ3 | Actor label? | answered | **Agente em nome de &lt;nome&gt;** |
| FQ4 | MCP ship? | answered | **Java Quarkus**; no Python |
| FQ5 | Ticket context? | answered | **Yes** |
| FQ6 | Secret / expiry? | answered | Once; no required expiry |
| FQ7 | Webhooks? | answered | Separate backlog |
| FQ8 | Skill? | answered | In-app primary; in-repo backup |
| FQ9 | Config friendliness? | answered | Maximize |
| FQ10 | Paste-ready config UI? | answered | **Yes** |
| FQ11 | Base URL? | answered | Configurable public URL |
| FQ12 | SA management UI? | answered | **`/projects/:projectId/service-accounts`** |
| FQ13 | SA “on behalf of” name? | answered | **Service account display name** |
| FQ14 | Who edits public URL? | answered | **`application.properties` only** in v1 (no admin UI) |

**Gate:** all blocking **FQ*n*** answered.

## Architecture

| Area | Design |
|------|--------|
| Bounded contexts | Identity + project own tokens/SA; ticket owns context/attribution; MCP is **external client** of `/api` |
| Packages / layers | Issues: Endpoint → Service → Repository; MCP project: `@Tool` → REST client → Issues `/api` |
| API | See below |
| Schema | See below |
| Cross-context | MCP never touches EntityManager; only HTTP + Bearer token from IDE |
| Frontend | Account + project SA + attribution labels |
| Build | Separate Maven project now; **Issues can migrate to multi-module** reactor including `issues-mcp` (**AQ7**) |
| Tests | Issues `@QuarkusTest`; MCP module unit/integration with WireMock or Testcontainers Issues |

### Packages / layers (Issues app)

| Layer | Type | Responsibility |
|-------|------|----------------|
| Endpoint | `auth.apitoken.*` | PAT create/list/revoke |
| Endpoint | `project.serviceaccount.*` | SA + SA tokens |
| Endpoint | `ticket.context.GetTicketContextEndpoint` | Composite context |
| Endpoint | `agent.setup.GetAgentSetupConfigEndpoint` | Snippet JSON (public URLs) |
| Service | `ApiTokenService`, `ServiceAccountService` | Hash/verify; last-used; revoke |
| Auth | Unified Bearer (JWT \| `iss_pat_` \| `iss_sat_`) | **AQ2** |
| History/comment | Set `via_agent=true` when SecurityIdentity is API-token | **AQ8** |

### Separate MCP project (**AQ7**)

| Item | Design |
|------|--------|
| Artifact | e.g. `issues-mcp` Quarkus app (`quarkus-mcp-server-http`) |
| Location | Sibling directory / own repo **or** future reactor module under Issues multi-module |
| Config | `issues.api-base-url` + token from MCP client Authorization (forward to Issues) |
| Tools | `search_tickets`, `get_ticket_context`, `update_ticket`, `move_ticket`, `add_comment`, `list_projects` |
| Auth | Client → MCP with PAT/SA; MCP → Issues with same Bearer |
| Snippet | Points at **`issues.mcp-public-base-url`**, not necessarily Issues UI origin |

### API surface

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| `POST/GET/DELETE` | `/api/account/api-tokens` | user JWT | PAT |
| `CRUD` | `/api/projects/{id}/service-accounts` (+ `…/tokens`) | PM/Admin | SA |
| `GET` | `/api/tickets/{id}/context` | JWT or API token | Detail + transitions + in-scope CFs |
| `GET` | `/api/agent/setup-config?preset=` | JWT | MCP JSON using public URLs |
| MCP HTTP | `{mcp-public}/mcp` | API token | Separate process |

### Schema

- `tb_api_tokens` — user_id, name, token_hash, token_prefix, created_at, last_used_at, revoked_at  
- `tb_service_accounts` — project_id, name (display), active, created_at  
- `tb_service_account_tokens` — service_account_id, name, token_hash, token_prefix, created_at, last_used_at, revoked_at  
- `tb_ticket_history.via_agent BOOLEAN NOT NULL DEFAULT FALSE`  
- `tb_comments.via_agent BOOLEAN NOT NULL DEFAULT FALSE` (or equivalent comment table)  
- Config keys: `issues.public-base-url`, `issues.mcp-public-base-url`

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Token storage? | answered | **Hash only** |
| AQ2 | Bearer JWT + API token? | answered | **Unified Bearer filter** |
| AQ3 | PAT package? | answered | `auth.apitoken.*` |
| AQ4 | MCP runtime? | answered | **Java / Quarkus MCP** |
| AQ5 | Token prefixes? | answered | `iss_pat_…` / `iss_sat_…` |
| AQ6 | Snippet generation? | answered | **Server-assisted** setup-config |
| AQ7 | MCP process boundary? | answered | **Separated project**; Issues **may migrate to multi-module** and absorb `issues-mcp` as a module |
| AQ8 | Persist attribution? | answered | **`via_agent` boolean** on history + comments |
| AQ9 | SA permissions? | answered | **Project member–aligned** powers on that project only |

**Gate:** all blocking **AQ*n*** answered.

## Scope (v1)

| ID | Scope item |
|----|------------|
| S1 | Personal API tokens |
| S2 | Project service accounts + tokens (`/projects/:id/service-accounts`) |
| S3 | Bearer auth PAT/SA |
| S4 | `via_agent` + **Agente em nome de …** UI |
| S5 | Guided Agent setup + setup-config |
| S6 | `issues.public-base-url` + `issues.mcp-public-base-url` props |
| S7 | `GET /tickets/{id}/context` |
| S8 | Separate `issues-mcp` Quarkus project + tools |
| S9 | Docs + backup skill; multi-module migration note in ARCHITECTURE |

## Changelog

### Agentic Development integration (tokens + MCP + skill) — 2026-07-11

**Version:** 1  
**Status:** tasks-ready

**Description:** PAT + project service accounts, separate Quarkus MCP project (multi-module-ready), ticket-context API, guided setup with properties-based public URLs, and persisted agent attribution.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Account settings | Conectar agente + PAT |
| Project administration | Service accounts route |
| Authentication | Bearer JWT or API token |
| Ticket management | Context endpoint; `via_agent` display |
| Build / platform | Path to multi-module; separate MCP deployable |
| Webhooks | Separate backlog |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | User create/list/revoke PATs; secret once | S1, FQ6 | ☐ |
| FC2 | SA CRUD + tokens at `/projects/:id/service-accounts` | S2, FQ12 | ☐ |
| FC3 | `/api` accepts PAT/SA Bearer; full principal powers | S3, FQ2, AQ9 | ☐ |
| FC4 | History/comments persist `via_agent`; UI **Agente em nome de …** (user or SA name) | S4, FQ3, FQ13, AQ8 | ☐ |
| FC5 | Conectar agente + setup-config uses public URL props | S5, S6, FQ10, FQ14 | ☐ |
| FC6 | `GET /tickets/{id}/context` composite | S7, FQ5 | ☐ |
| FC7 | Separate Java MCP project with tools over REST | S8, AQ7, FQ4 | ☐ |
| FC8 | No Python MCP | FQ4 | ☐ |
| FC9 | Wireframes matched (account + SA + attribution) | Wireframe | ☐ |
| FC10 | domain-spec, feature-catalog, README, ARCHITECTURE multi-module note | S9 | ☐ |
| FC11 | Webhooks not in this changelog | FQ7 | ☐ |

#### Tasks (phase 3)

| ID | Task | Done |
|----|------|------|
| T1 | Flyway baseline: `tb_api_tokens`, `tb_service_accounts`, `tb_service_account_tokens`, `via_agent` columns | ☐ |
| T2 | Domain entities + repositories for PAT and SA (+ tokens) | ☐ |
| T3 | `ApiTokenService` / `ServiceAccountService` (hash, create, revoke, last-used) | ☐ |
| T4 | Unified Bearer auth (JWT \| `iss_pat_` \| `iss_sat_`) + tests | ☐ |
| T5 | PAT endpoints `auth.apitoken.*` + endpoint tests | ☐ |
| T6 | SA endpoints `project.serviceaccount.*` + endpoint tests (member-aligned authz) | ☐ |
| T7 | Set `via_agent` on history/comment writes; expose in responses; Angular labels | ☐ |
| T8 | `GetTicketContextEndpoint` + tests | ☐ |
| T9 | Public URL props + `GetAgentSetupConfigEndpoint` (presets) + tests | ☐ |
| T10 | Angular `/account/settings` — Conectar agente + PAT list/create/revoke | ☐ |
| T11 | Angular `/projects/:projectId/service-accounts` page + nav entry | ☐ |
| T12 | Create separate `issues-mcp` Quarkus project (HTTP MCP tools → Issues REST) | ☐ |
| T13 | Wire MCP config into setup-config snippet; local/dev run docs | ☐ |
| T14 | Docs: domain-spec, feature-catalog, README, ARCHITECTURE (multi-module path), backup skill | ☐ |
| T15 | Dev seed sample PAT/SA (optional) + `mvn verify` / MCP module build | ☐ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | PAT create/list/revoke + secret-once; auth with `iss_pat_` | T4, T5 | ☐ |
| TC2 | SA create/token/revoke; auth with `iss_sat_`; project scope | T4, T6 | ☐ |
| TC3 | Reject revoked/invalid tokens | T4 | ☐ |
| TC4 | Ticket update/move/comment via token sets `via_agent`; response/UI label | T7 | ☐ |
| TC5 | `GET /tickets/{id}/context` shape (detail + transitions + CFs) | T8 | ☐ |
| TC6 | `GET /agent/setup-config` uses configured public URLs | T9 | ☐ |
| TC7 | Angular account setup / PAT specs | T10 | ☐ |
| TC8 | Angular service-accounts specs | T11 | ☐ |
| TC9 | MCP tool smoke (get_context / comment) against API | T12, T13 | ☐ |
| TC10 | `ArchitectureTest` / OpenAPI ids for new endpoints | T5, T6, T8, T9 | ☐ |

**Development approval:** pending — approve task IDs to start phase 5 (e.g. “Approve T1–T15” or a subset).

**Implementation notes:** (fill after done)

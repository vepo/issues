# Notifications

**Feature version:** 3  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03); v3 polish 2026-07-11

## Summary

In-app notifications for ticket subscribers: persisted alerts, mark-as-read, and real-time delivery via Server-Sent Events (SSE). The dropdown loads history via a **paginated list API** with **infinite scroll**; SSE delivers **live** events only. After a network drop the client **auto-reconnects** SSE using a fresh access token (refresh if needed).

**v3:** accurate **unread count** badge from the server (fixes pagination under-count) and **Marcar todas como lidas** for all unread notifications of the current user.

## Wireframe

**Guide:** layout reference for UI implementation — update when notification UX or **FQ*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below (global shell widget) |
| **Last updated** | 2026-07-11 |

### Widget: global header (no dedicated route)

| Region | Elements | Notes |
|--------|----------|-------|
| Badge | Unread count on bell icon | Server **unread count** (**FQ1**); hide when 0; display `99+` when unread > 99 (**FQ4**) |
| Dropdown header | Label **Notificações** + **Marcar todas como lidas** | Mark-all only when unread > 0 (**FQ3**); secondary text button; stops menu close on click |
| Dropdown list | Notification list | Newest first; click marks read + navigates to ticket |
| Scroll | Infinite scroll | Near bottom → fetch next page; show subtle loading; stop when `hasMore` false |
| Empty | Short empty state | When first page empty |

```
Header:  …  [🔔 3]  …          (or [🔔 99+] when unread > 99)
         ┌─────────────────────────────────┐
         │ Notificações  [Marcar todas…]   │  ← when unread > 0
         │ Ticket PROJ-1 updated           │
         │ Comment on PROJ-2               │
         │ …                               │
         │ (scroll → load more)            │
         └─────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `notifications` |
| Packages / files | `NotificationService`, `NotificationRepository`; `notifications.unread.UnreadNotificationCountEndpoint`; `notifications.readall.MarkAllNotificationsReadEndpoint`; Angular `notification` + service |
| API | `GET /notifications/unread-count`; `POST /notifications/read-all`; list / SSE / single read unchanged |
| UI | Badge from server unread (+ `99+`); dropdown header **Marcar todas como lidas**; after mark-all reload page 0 + unread (**FQ5**) |
| Schema / seed | None — filter existing `tb_notifications.read` |
| Tests | Unread + mark-all endpoint tests; Angular badge / mark-all / display-cap specs |
| Docs | domain-spec terms + invariant; feature-catalog; README; ui-elements-gallery §8.2; ARCHITECTURE API rows |

### Risks

- Mark-all must filter by authenticated username (never global update).
- Live SSE may arrive during/after mark-all — client reloads page 0 + unread after success (**FQ5**); new live events still merge and bump unread.
- Path order: `unread-count` and `read-all` must not collide with `{id}/read` (literal paths vs path param).

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Should the bell badge show **total unread** from the server (not only loaded pages)? | answered | **Yes** — dedicated unread count from server |
| FQ2 | Does **Marcar todas como lidas** mark **all** unread for the user, or only items currently loaded in the dropdown? | answered | **All** unread for the authenticated user |
| FQ3 | Where is the mark-all control? | answered | Dropdown header: **Marcar todas como lidas**, only when unread > 0 |
| FQ4 | Cap badge display (e.g. `99+`) when unread is large? | answered | **Yes** — show `99+` when unread > 99 |
| FQ5 | After mark-all, should the client reload page 0 and unread count, or only flip local `read` flags? | answered | **Reload** page 0 and unread count |

## Architecture

**Guide:** technical design for changelog v3.

| Area | Design |
|------|--------|
| Bounded contexts | `notifications` owns unread count + mark-all |
| Packages / layers | Endpoints → `NotificationService` → `NotificationRepository` |
| API | See API surface below |
| Schema | No Flyway change; JPQL bulk update `read = true` where unread for user (**AQ2**) |
| Cross-context | Unchanged CDI ticket events → persist + SSE |
| Frontend | Fetch unread on init + after reconnect; badge display cap; mark-all → POST then reload page 0 + unread |
| Tests | Endpoint tests; Angular specs for badge text and mark-all flow |

### Packages / layers

| Layer | Type | Responsibility |
|-------|------|----------------|
| Endpoint | `notifications.unread.UnreadNotificationCountEndpoint` | `GET /notifications/unread-count` |
| Endpoint | `notifications.readall.MarkAllNotificationsReadEndpoint` | `POST /notifications/read-all` |
| Service | `NotificationService.countUnread` / `markAllAsRead` | Username-scoped; return response records |
| Repository | `countUnreadByUsername` / `markAllReadByUsername` | Count + bulk update |

### API surface

| Method | Path | Auth | Success | Notes |
|--------|------|------|---------|-------|
| `GET` | `/api/notifications/unread-count` | authenticated roles | `200` `UnreadNotificationCountResponse` | **FQ1** |
| `POST` | `/api/notifications/read-all` | authenticated roles | `200` `MarkAllNotificationsReadResponse` | No body; **FQ2** |
| existing | list / register / `{id}/read` | — | — | Unchanged |

```text
record UnreadNotificationCountResponse(long unread)

record MarkAllNotificationsReadResponse(long updated, long unread)
```

- `operationId`: `getUnreadNotificationCount`, `markAllNotificationsRead`
- Tag: `Notification`
- Roles: same as list (`USER`, `ADMIN`, `PROJECT_MANAGER`)
- `MarkAllNotificationsReadResponse.unread` is always `0` after success (**AQ3**)
- Register literal paths before/alongside `{id}` so JAX-RS does not treat `unread-count` / `read-all` as ids (**AQ1**)

### Client behaviour

1. On init (and after SSE reconnect): `GET unread-count` + existing page-0 list load.
2. Badge text: `unread` if 1–99; `99+` if > 99; hidden if 0 (**FQ4**).
3. On live SSE unread event: increment local unread (or re-fetch count — prefer increment for new unread; re-fetch on reconnect).
4. On single mark-as-read success: decrement unread (min 0) and merge event.
5. On mark-all: `POST read-all` → reload page 0 + unread count (**FQ5**); hide mark-all when unread is 0.

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Unread count via dedicated endpoint vs field on page response? | answered | **Dedicated** `GET /notifications/unread-count` — badge needs count without depending on list payload |
| AQ2 | Mark-all persistence: load entities vs bulk JPQL update? | answered | **Bulk JPQL** `UPDATE … SET read = true WHERE receive.username = :username AND read = false` |
| AQ3 | Mark-all response shape? | answered | `MarkAllNotificationsReadResponse(updated, unread)` with `unread = 0` |

## Changelog

### Mark all as read and accurate unread badge — 2026-07-11

**Version:** 3  
**Status:** done

**Description:** Expose an accurate unread count for the header badge (fix pagination under-count) and a **Marcar todas como lidas** action that marks **all** unread notifications for the current user.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Authentication | Same authenticated roles as existing notification endpoints |
| — | Ticket event → notification create / SSE live push unchanged |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Bell badge shows server unread total; `99+` when > 99; hidden at 0 | FQ1, FQ4, Wireframe | ☑ |
| FC2 | Dropdown header shows **Marcar todas como lidas** only when unread > 0 | FQ2, FQ3, Wireframe | ☑ |
| FC3 | Mark-all updates only the authenticated user's unread notifications | Risks, FQ2, AQ2 | ☑ |
| FC4 | After mark-all, client reloads page 0 and unread count | FQ5 | ☑ |
| FC5 | Dropdown + badge match **Wireframe** | Wireframe | ☑ |
| FC6 | `domain-specification.md` — Unread count + Mark all as read + invariants 63–64 | Impact / Docs | ☑ |
| FC7 | `feature-catalog.md` + gallery §8.2 updated | Impact / Docs | ☑ |
| FC8 | README notifications bullet mentions mark-all / accurate badge | Impact / Docs | ☑ |
| FC9 | ARCHITECTURE API map includes unread-count and read-all | Architecture | ☑ |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | `NotificationRepository.countUnreadByUsername` + `markAllReadByUsername` (bulk JPQL) | ☑ |
| T2 | `UnreadNotificationCountResponse` + `MarkAllNotificationsReadResponse`; service methods | ☑ |
| T3 | `UnreadNotificationCountEndpoint` `GET /notifications/unread-count` | ☑ |
| T4 | `MarkAllNotificationsReadEndpoint` `POST /notifications/read-all` | ☑ |
| T5 | Endpoint tests — unread count; mark-all updates all for user; empty/idempotent; auth 401 | ☑ |
| T6 | Angular facade — unread count + mark-all API | ☑ |
| T7 | Angular UI — server badge + `99+`; header mark-all; reload page 0 + unread after mark-all; refresh unread on init/reconnect | ☑ |
| T8 | Angular specs — badge display; mark-all calls API and reloads | ☑ |
| T9 | OpenAPI codegen + docs (domain-spec, feature-catalog, gallery §8.2, README, ARCHITECTURE) | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | Unread count endpoint — correct total; 0 when none; 401 unauthenticated | T1–T3, T5 | ☑ |
| TC2 | Mark-all — marks all unread for user; `updated` count; `unread` 0; idempotent second call | T1, T2, T4, T5 | ☑ |
| TC3 | Mark-all does not affect another user's notifications | T4, T5 | ☑ |
| TC4 | Angular — badge shows count / `99+` / hidden at 0 | T7, T8 | ☑ |
| TC5 | Angular — mark-all triggers POST then reloads list + unread | T6–T8 | ☑ |

**Development approval:** approved 2026-07-11 — tasks: T1, T2, T3, T4, T5, T6, T7, T8, T9

**Implementation notes:** `GET /notifications/unread-count`; `POST /notifications/read-all` (bulk JPQL by username). Angular badge from server unread with `99+` cap; dropdown **Marcar todas como lidas** reloads page 0 + unread. `mvn verify` + `npm run build` + notification Angular specs green (2026-07-11).

### Initial implementation — baseline

**Version:** 1  
**Status:** done

**Description:** CDI events on ticket changes create notifications for subscribers; SSE register endpoint pushes to connected clients; mark-as-read updates persistence.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Ticket management | Subscribe/unsubscribe controls recipients |
| Email delivery | Parallel email path for same events |
| Authentication | SSE requires authenticated session |
| — | None identified |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Notification badge and dropdown match **Wireframe** | Wireframe | ☑ |
| FC2 | SSE register on authenticated session | Summary | ☑ |
| FC3 | Mark-as-read updates persistence | Summary | ☑ |
| FC4 | `feature-catalog.md` — Notifications row | Impact / Docs | ☑ |

**Implementation notes:** `notification.component.ts`; `NotificationEventListener` bridges ticket events to SSE and persistence.

### SSE reconnect and infinite scroll — 2026-07-03

**Version:** 2  
**Status:** done

**Description:** Auto-reconnect SSE on disconnect with token refresh; paginated `GET /notifications` with infinite scroll in the dropdown; SSE no longer dumps full history on register.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [authentication](authentication.md) | Reconnect uses access + refresh token (**AQ4**) |
| — | Ticket event → notification create unchanged |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | SSE reconnects after network drop | FQ1, Wireframe | ☑ |
| FC2 | Reconnect refreshes access token when needed | AQ4 | ☑ |
| FC3 | Dropdown loads first page via list API | FQ2, AQ3 | ☑ |
| FC4 | Scrolling near bottom loads next page until `hasMore` false | Wireframe, FQ2 | ☑ |
| FC5 | List ordered newest-first with stable tie-break | Architecture | ☑ |
| FC6 | SSE register does not dump historical notifications | AQ3 | ☑ |
| FC7 | After reconnect, page 0 reloaded | AQ5 | ☑ |
| FC8 | `feature-catalog.md` + ARCHITECTURE list API row | Docs | ☑ |
| FC9 | Domain invariant 42 remains accurate | Docs | ☑ |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | `NotificationRepository` paginated query + count (ordered) | ☑ |
| T2 | `NotificationPageResponse` + `NotificationService.list` | ☑ |
| T3 | `ListNotificationsEndpoint` `GET /notifications` | ☑ |
| T4 | `RegisterNotificationsEndpoint` — live events only (remove history dump) | ☑ |
| T5 | `ListNotificationsEndpointTest` (+ register no longer floods history if covered) | ☑ |
| T6 | `sse.client` / `NotificationService` — reconnect with token refresh | ☑ |
| T7 | Angular: list API facade + infinite scroll + merge SSE by id | ☑ |
| T8 | Reload page 0 after SSE reconnect | ☑ |
| T9 | Angular specs (scroll/load more; hide recovery paths N/A — reconnect/list) | ☑ |
| T10 | OpenAPI codegen + docs (feature-catalog, ARCHITECTURE, README if needed) | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `ListNotificationsEndpointTest` — page 0 size, ordering, hasMore | T1–T3, T5 | ☑ |
| TC2 | `ListNotificationsEndpointTest` — empty user → empty page | T5 | ☑ |
| TC3 | Register SSE does not require/assert full history dump (or unit assert service path) | T4 | ☑ |
| TC4 | Angular notification spec — loads page; requests next page on scroll | T7, T9 | ☑ |
| TC5 | SSE reconnect path uses refreshed token (unit/spec on client or service) | T6, T8 | ☑ |

**Development approval:** approved 2026-07-10 — tasks: T1, T2, T3, T4, T5, T6, T7, T8, T9, T10

**Implementation notes:** `GET /notifications` paginated; SSE live-only; Angular infinite scroll + reconnect reloads page 0; token refresh on SSE 401. Index `idx_notifications_receive_created`. `mvn verify` + Angular notification/sse specs green (2026-07-10).

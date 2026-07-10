# Notifications

**Feature version:** 2  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

In-app notifications for ticket subscribers: persisted alerts, mark-as-read, and real-time delivery via Server-Sent Events (SSE). The dropdown loads history via a **paginated list API** with **infinite scroll**; SSE delivers **live** events only. After a network drop the client **auto-reconnects** SSE using a fresh access token (refresh if needed).

## Wireframe

**Guide:** layout reference for UI implementation — update when notification UX or **FQ*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below (global shell widget) |
| **Last updated** | 2026-07-10 |

### Widget: global header (no dedicated route)

| Region | Elements | Notes |
|--------|----------|-------|
| Badge | Unread count on bell icon | From loaded items + live SSE; unread = `!read` |
| Dropdown | Notification list | Newest first; click marks read + navigates to ticket |
| Scroll | Infinite scroll | Near bottom → fetch next page; show subtle loading; stop when `hasMore` false |
| Empty | Short empty state | When first page empty |

```
Header:  …  [🔔 3]  …
         ┌─────────────────────────────┐
         │ Ticket PROJ-1 updated       │
         │ Comment on PROJ-2           │
         │ …                           │
         │ (scroll → load more)        │
         └─────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `notifications` |
| Packages / files | `notifications.list`, `NotificationService`, `NotificationRepository`, `register` (SSE live-only), Angular `notification` + `sse.client` |
| API | **`GET /notifications?page=&size=`** (new); existing `GET /notifications/register` (SSE, no history dump); `POST /notifications/{id}/read` |
| UI | Dropdown: initial page load + infinite scroll; SSE reconnect with token refresh |
| Schema / seed | No Flyway change — index optional on `(receive_id, created_at)` if missing |
| Tests | `ListNotificationsEndpointTest`; SSE/register regression; Angular notification + sse specs |
| Docs | domain-spec invariant 42 (already); feature-catalog; ARCHITECTURE API row; README if needed |

### Risks

- Removing SSE history dump changes reconnect behaviour — client must load list via REST after connect/reconnect (**AQ3**).
- Stale JWT on long-lived SSE — reconnect must refresh access token (**AQ4**).

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | How should SSE reconnect after network drop? | answered | **Yes** — client auto-reconnects SSE after network drop |
| FQ2 | What retention or pagination strategy applies at high notification volume? | answered | **Infinite scroll** — paginated fetch as user scrolls the notification list |

## Architecture

**Guide:** technical design for changelog v2.

| Area | Design |
|------|--------|
| Bounded contexts | `notifications` owns list + SSE + mark-as-read |
| Packages / layers | `ListNotificationsEndpoint` → `NotificationService.list` → `NotificationRepository.findPage` |
| API | `GET /api/notifications?page=0&size=20` → `NotificationPageResponse`; SSE register = live events only |
| Schema | No new tables; query `ORDER BY created_at DESC, id DESC`; ensure index if useful |
| Cross-context | Unchanged CDI ticket events → persist + push to open sinks |
| Frontend | Open menu / init: fetch page 0; scroll → next pages; SSE prepend/merge by id; reconnect via `sse.client` + `AuthService.refreshToken` on auth failure |
| Tests | Endpoint pagination; Angular scroll/load; reconnect uses current/refreshed token |

### Packages / layers

| Layer | Type | Responsibility |
|-------|------|----------------|
| Endpoint | `notifications.list.ListNotificationsEndpoint` | `GET /notifications` — `page` (default 0), `size` (default 20, max 50) |
| Endpoint | `notifications.register.RegisterNotificationsEndpoint` | SSE register; **do not** dump history (**AQ3**) |
| Service | `NotificationService.list(username, page, size)` | Clamp size; return page response |
| Repository | `NotificationRepository.findPage` / `countByUsername` | Ordered page + total |

### API surface

| Method | Path | Auth | Success | Notes |
|--------|------|------|---------|-------|
| `GET` | `/api/notifications` | authenticated roles | `200` `NotificationPageResponse` | Query: `page` ≥ 0, `size` 1–50 |
| `GET` | `/api/notifications/register` | authenticated | SSE stream | Live `UserNotificationEvent` only |
| `POST` | `/api/notifications/{id}/read` | authenticated | `UserNotificationEvent` | Unchanged |

`NotificationPageResponse`:

```text
record NotificationPageResponse(
  List<UserNotificationEvent> items,
  long total,
  int page,
  int size,
  boolean hasMore
)
```

`operationId`: `listNotifications`. Tag: `Notification`.

Ordering: `created_at DESC`, then `id DESC` (stable).

### SSE + reconnect

1. Client opens SSE with current Bearer access token.
2. On stream end / network error: wait ~3s (existing), ensure valid token (`refreshToken` if needed), reconnect.
3. After reconnect: optionally refresh page 0 of list to catch missed events (or rely on live stream only for new events after reconnect — **AQ5**: reload first page).
4. Server: one sink per username (existing map); closed sinks dropped on next push.

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Page size? | answered | Default **20**; query `size` max **50** |
| AQ2 | Offset vs cursor? | answered | **Offset** `page` + `size` — sufficient for notification volumes |
| AQ3 | SSE register still dumps full history? | answered | **No** — history via list API only; SSE = live pushes |
| AQ4 | Token on reconnect? | answered | Reconnect with **current** access token; on 401 refresh via `POST /auth/refresh` then retry |
| AQ5 | After SSE reconnect, reload list? | answered | **Yes** — reload page 0 and reset scroll paging so missed events while disconnected appear |

## Changelog

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

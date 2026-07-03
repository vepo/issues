# Notifications

**Feature version:** 2  
**Status:** planned  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

In-app notifications for ticket subscribers: persisted alerts, mark-as-read, and real-time delivery via Server-Sent Events (SSE). On login the client registers an SSE channel; ticket changes push updates to the notification badge and list.

## Wireframe

**Guide:** layout reference for UI implementation — update when notification UX or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below (global shell widget) |
| **Last updated** | 2026-07-03 |

### Widget: global header (no dedicated route)

| Region | Elements |
|--------|----------|
| Badge | Unread count on bell icon |
| Dropdown | Notification list with **infinite scroll**; click marks read; link to ticket |

```
Header:  …  [🔔 3]  …
         ┌─────────────────────────┐
         │ Ticket PROJ-1 updated   │
         │ Comment on PROJ-2       │
         └─────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `notifications`; listens to `ticket` CDI events |
| Packages / files | `notifications.register`, `notifications.read`, `NotificationService`, `NotificationEventListener`, `NotificationChannelRegistry`, `UserNotificationEvent` |
| API | `GET /notifications/register` (SSE), `POST /notifications/{id}/read`, **`GET /notifications?page=`** (paginated list for infinite scroll) |
| UI | Global `notification` component (badge, dropdown); background SSE on authenticated session |
| Schema / seed | `tb_notifications` |
| Tests | `RegisterNotificationsEndpointTest`, `UpdateNotificationReadEndpointTest` |
| Docs | domain-spec (Notification, Notification channel), feature-catalog (Notifications SSE row), README § Notifications & email |

### Feature questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | How should SSE reconnect after network drop? | answered | **Yes** — client auto-reconnects SSE after network drop |
| FQ2 | What retention or pagination strategy applies at high notification volume? | answered | **Infinite scroll** — paginated fetch as user scrolls the notification list |

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
**Status:** planned

**Description:** Auto-reconnect SSE on disconnect; paginated notification list with infinite scroll in dropdown.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [authentication](authentication.md) | Reconnect reuses valid session/refresh token |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | SSE reconnects after network drop | FQ1 | ☐ |
| FC2 | Notification dropdown loads more on scroll | Wireframe, FQ2 | ☐ |
| FC3 | Paginated list API with stable ordering | FQ2 | ☐ |

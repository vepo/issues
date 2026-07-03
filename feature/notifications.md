# Notifications

**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

In-app notifications for ticket subscribers: persisted alerts, mark-as-read, and real-time delivery via Server-Sent Events (SSE). On login the client registers an SSE channel; ticket changes push updates to the notification badge and list.

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `notifications`; listens to `ticket` CDI events |
| Packages / files | `notifications.register`, `notifications.read`, `NotificationService`, `NotificationEventListener`, `NotificationChannelRegistry`, `UserNotificationEvent` |
| API | `GET /notifications/register` (SSE), `POST /notifications/{id}/read` |
| UI | Global `notification` component (badge, dropdown); background SSE on authenticated session |
| Schema / seed | `tb_notifications` |
| Tests | `RegisterNotificationsEndpointTest`, `UpdateNotificationReadEndpointTest` |
| Docs | domain-spec (Notification, Notification channel), feature-catalog (Notifications SSE row), README § Notifications & email |

### Risks and open questions

- SSE connection recovery on network drop — client reconnect behaviour.
- Notification volume per user at scale.

## Changelog

### Initial implementation — baseline

**Status:** done

**Description:** CDI events on ticket changes create notifications for subscribers; SSE register endpoint pushes to connected clients; mark-as-read updates persistence.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Ticket management | Subscribe/unsubscribe controls recipients |
| Email delivery | Parallel email path for same events |
| Authentication | SSE requires authenticated session |
| — | None identified |

**Implementation notes:** `notification.component.ts`; `NotificationEventListener` bridges ticket events to SSE and persistence.

# Due-date reminders

**Feature version:** 1  
**Status:** done  
**Requested:** 2026-07-17

## Summary

Tickets already carry an optional **due date** ([ticket-management.md](ticket-management.md) v2), but nothing nudges anyone as it approaches or passes. This feature adds a scheduled background check that notifies the relevant user(s) — in-app and/or email — when a ticket's due date is near or overdue, since (unlike every other notification in this system) a reminder is triggered by time passing, not by a user action.

## Wireframe

**Guide:** layout reference for UI implementation — update whenever Scope, routes, or **FQ*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | N/A — no dedicated UI page; surfaces through the existing notification dropdown only (**FQ3**: in-app only, no email) |
| **Last updated** | 2026-07-17 (FQ1–FQ4 accepted) |

### Widget: global header notification dropdown (existing, no new route)

| Region | Elements | Notes |
|--------|----------|-------|
| Dropdown list | New reminder entry, same list/format as other notification types | Reuses existing `.notification-item` — no new UI component; two distinct message variants: "due tomorrow" and "overdue" (**FQ1**) |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `ticket` (due date source), `notifications` (in-app delivery) — no `mailer` involvement (**FQ3**: in-app only); no new bounded context expected |
| Packages / files | New scheduled trigger (class/package TBD — phase 2); reuses `TicketRepository`; delivery reuses the non-subscriber-targeted notification pattern built for **Comment @mentions** ([ticket-management.md](ticket-management.md) v5, `NotificationService.notifyMentions`) rather than the subscriber-only `NotificationEvent` path, since the recipient is the **assignee** (**FQ2**) |
| API | No new endpoint expected — a background job drives the existing notification path |
| UI | None beyond the existing notification dropdown surfacing a new entry type; no dedicated route |
| Schema / seed | No table changes anticipated for the notification itself (reuses `tb_notifications` with a new `type`); a de-dup marker to avoid re-sending the same reminder is needed — exact shape is **AQ**, phase 2 |
| Tests | New scheduled-job / eligibility-query tests, notification creation test |
| Docs | domain-spec (new term + invariant), README § Notifications & email |

### Risks

- Requires a **new Quarkus dependency** (`quarkus-scheduler`) — this is the first scheduled/cron job in the codebase; cadence (`%dev` vs `%prod`) and `@Scheduled` wiring are architecture decisions for phase 2.
- Must not re-notify the same ticket on every scheduler tick — needs a de-dup marker (e.g. one row per ticket per firing point) so a nightly tick doesn't re-fire the "1 day before" or "overdue" reminder it already sent.
- Must exclude tickets that are finished (`finishedAt` set), canceled (`canceledAt` set), or soft-deleted (`deleted`) from eligibility — a reminder on a closed ticket is noise.
- Due date is optional; tickets without one are never eligible.
- Unassigned tickets are skipped entirely (**FQ2**) — no fallback recipient.
- If a ticket's due date or assignee changes after a reminder already fired for that firing point, the de-dup design must decide whether that resets eligibility (**AQ**, phase 2).

### Feature questions (Due-date reminders — v1)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | When should a reminder fire relative to the due date — a fixed number of days before, exactly on the due date, once overdue, or a combination? | answered | **Two firing points**: 1 day before the due date, and again once the ticket becomes overdue |
| FQ2 | Who should receive the reminder — the ticket assignee, all subscribers, or both? And if unassigned, skip or fall back to subscribers? | answered | **Assignee only**; unassigned tickets are skipped — no fallback |
| FQ3 | Delivery channel — in-app only, email only, or both? | answered | **In-app only** — no email in v1 |
| FQ4 | Should reminders repeat/escalate while a ticket stays overdue, or fire once per due date? | answered | **Fire once per firing point** (once for the "1 day before" check, once for "just became overdue") — never repeats beyond that |

**Gate:** phase 2 (architecture design) requires no blocking **FQ*n*** — resolve or mark `not valid` first.

## Changelog

### Due-date reminders — 2026-07-17

**Version:** 1  
**Status:** done

**Description:** Scheduled background check that notifies the relevant user(s) when a ticket's due date is near or has passed.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [ticket-management](ticket-management.md) | Reads existing `dueDate`, `finishedAt`, `canceledAt`, `assignee` fields — no changes to ticket CRUD |
| [notifications](notifications.md) | New `type="due-date-reminder"` notification, delivered directly to the assignee (non-subscriber-targeted, same pattern as Comment @mentions) |
| [email-delivery](email-delivery.md) | None — in-app only in v1 (**FQ3**) |

#### Feature checklist (phase 1 draft — refine after FQ1–FQ4 answers)

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Reminder fires 1 day before the due date and again once overdue, and only then | FQ1 | ☑ |
| FC2 | Reminder reaches only the ticket assignee; unassigned tickets are skipped | FQ2 | ☑ |
| FC3 | Reminder is in-app only — no email is sent | FQ3 | ☑ |
| FC4 | Each firing point (before / overdue) notifies at most once — no repeats | FQ4 | ☑ |
| FC5 | Finished, canceled, and soft-deleted tickets are never reminded | Risks | ☑ |
| FC6 | The same due-date reminder is never sent twice for the same ticket occurrence | Risks | ☑ |
| FC7 | `domain-specification.md` defines the reminder term and invariant | Impact / Docs | ☑ |
| FC8 | README § Notifications & email mentions due-date reminders | Impact / Docs | ☑ |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | Add `quarkus-scheduler` extension to `pom.xml` | ☑ |
| T2 | Schema — `due_soon_reminder_sent_at` / `overdue_reminder_sent_at` (nullable `TIMESTAMP`) on `tb_tickets` in `V1.0.0__Database_Creation.sql`; add fields + getters/setters to `Ticket` entity | ☑ |
| T3 | `TicketRepository` — eligibility queries for "due tomorrow" and "overdue" ticket sets | ☑ |
| T4 | `NotificationService.notifyDueDateReminder(ticket, assignee)` — synchronous `Notification` (`type="due-date-reminder"`) + SSE, mirroring `notifyMentions` | ☑ |
| T5 | `ticket.reminders.DueDateReminderService.checkAndNotify()` — query eligible tickets, call `notifyDueDateReminder`, set the fired marker | ☑ |
| T6 | `ticket.reminders.DueDateReminderScheduler` — `@Scheduled(cron = "{app.due-date-reminder.cron}")`, default daily 06:00, calling `checkAndNotify()` | ☑ |
| T7 | `TicketService.update` / assignee-change path — clear both reminder markers when `dueDate` or `assignee` changes | ☑ |
| T8 | `DueDateReminderServiceTest` — due-tomorrow/overdue eligibility, excludes finished/canceled/deleted/unassigned, no double-fire, marker reset on due-date/assignee change | ☑ |
| T9 | domain-spec term + invariant; README § Notifications & email line | ☑ |
| T10 | Run `mvn verify`; recheck **Feature checklist** FC1–FC8 before `done` | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `DueDateReminderServiceTest` — ticket due tomorrow with assignee fires the reminder | T3, T4, T5 | ☐ |
| TC2 | `DueDateReminderServiceTest` — overdue ticket with assignee fires the reminder | T3, T4, T5 | ☐ |
| TC3 | `DueDateReminderServiceTest` — finished, canceled, soft-deleted, and unassigned tickets are excluded | T3, T5 | ☐ |
| TC4 | `DueDateReminderServiceTest` — a second scheduler run does not re-fire an already-sent reminder | T5 | ☐ |
| TC5 | `DueDateReminderServiceTest` or `UpdateTicketEndpointTest` — changing due date or assignee resets the markers | T7 | ☐ |
| TC6 | `ArchitectureTest` | T4 | ☐ |

**Development approval:** approved 2026-07-17 — tasks: T1, T2, T3, T4, T5, T6, T7, T8, T9, T10

**Implementation notes:** T1–T10 complete. Fixed a test bug found during T10 verification: `DueDateReminderServiceTest` marker-reset cases called `.put(...)` against endpoints that are actually `@POST /api/tickets/{id}` and `@PATCH /api/tickets/{id}/assignee` — corrected to the real HTTP methods. `mvn verify` green.

## Architecture

| Area | Design |
|------|--------|
| Bounded contexts | **Ticket management** owns eligibility and the de-dup markers (new columns on `tb_tickets`); **Notifications** owns delivery. No new bounded context |
| Packages / layers | New `ticket.reminders` package: `DueDateReminderScheduler` (`@Scheduled`) → `DueDateReminderService.checkAndNotify()` → `TicketRepository` (new eligibility queries) → `NotificationService` (new `notifyDueDateReminder(ticket, assignee)` method, mirroring `notifyMentions` — direct, targeted, synchronous, bypassing the subscriber-only `NotificationEvent` path) |
| Scheduling | New dependency `quarkus-scheduler`. `@Scheduled(cron = "{app.due-date-reminder.cron}")`, default **once daily** (**AQ1**) |
| Eligibility query | Ticket is reminder-eligible when: `dueDate` is not null, `deleted = false`, `finishedAt` is null, `canceledAt` is null, `assignee` is not null, and the relevant marker (`dueSoonReminderSentAt` / `overdueReminderSentAt`) is null. "Due tomorrow" fires when `dueDate = today + 1`; "overdue" fires when `dueDate < today` |
| Schema | Two new nullable columns on `tb_tickets`: `due_soon_reminder_sent_at TIMESTAMP`, `overdue_reminder_sent_at TIMESTAMP` — amend `V1.0.0__Database_Creation.sql` directly (not in production yet — [issues-flyway.mdc](../.cursor/rules/issues-flyway.mdc)) |
| Marker reset | `TicketService.update`/`assign` clears both marker columns whenever `dueDate` or `assignee` changes, so a new deadline or new owner gets its own reminder cycle (**AQ2**) |
| Persistence | Reuse `tb_notifications` with `type = "due-date-reminder"`; content is a short localized summary ("vence amanhã" / "venceu") |
| Tests | `DueDateReminderServiceTest` — eligibility filtering (due-tomorrow, overdue, excludes finished/canceled/deleted/unassigned, respects markers, doesn't double-fire), marker reset on due-date/assignee change; `NotificationServiceTest` for `notifyDueDateReminder` |

### Architecture questions (Due-date reminders — v1)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | How often should the scheduler check for eligible tickets? | answered | **Once daily**, early morning (e.g. 06:00 server time) — due dates are day-granularity, more frequent checks gain nothing |
| AQ2 | Should changing a ticket's due date or assignee reset its reminder markers (allowing a fresh reminder cycle), or leave them as-is? | answered | **Yes** — `TicketService.update`/`assign` clears both markers whenever `dueDate` or `assignee` changes |

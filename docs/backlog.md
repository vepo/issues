# Product backlog

Ordered ideas for Issues that are **not yet** in active development (or are queued). Agents and humans keep this table current and **reorderable** by the **Order** column (lower = higher priority).

**Canonical file:** [docs/backlog.md](backlog.md)  
**Rule:** [.cursor/rules/backlog-management.mdc](../.cursor/rules/backlog-management.mdc)  
**Active work:** [feature/](../feature/) changelog entries — promote a backlog row into a feature doc when starting analysis.

## How to use

| Action | How |
|--------|-----|
| Reorder | Change **Order** integers (1, 2, 3…); keep unique contiguous ranks for `idea` / `ready` rows |
| Add | Append a row; assign next Order or insert and renumber |
| Start work | Set Status `promoted`; create/extend `feature/<slug>.md`; link in **Feature doc** |
| Drop | Status `wont` or `done` (if shipped without a prior feature doc); do not delete history rows |

### Status values

| Status | Meaning |
|--------|---------|
| `idea` | Suggested; not analyzed |
| `ready` | Scoped enough to start feature analysis when picked |
| `promoted` | Has an active `feature/*.md` changelog (see Feature doc) |
| `done` | Shipped; keep briefly then archive or leave for history |
| `wont` | Rejected or deferred indefinitely |

## Backlog (ordered)

| Order | Status | Idea | Why / notes | Suggested slug | Feature doc |
|------:|--------|------|-------------|----------------|-------------|
| 1 | promoted | **Custom fields** for tickets (project + workflow defs, values, import, query) | Core extensibility; tasks-ready awaiting approval | `custom-fields` | [custom-fields.md](../feature/custom-fields.md) |
| 2 | promoted | **Notifications** — SSE reconnect, infinite scroll, page API | Reliability gap; in progress | `notifications` | [notifications.md](../feature/notifications.md) |
| 3 | idea | **Editable workflow statuses after create** | Known gap — status list fixed after create | `workflow-configuration` | [workflow-configuration.md](../feature/workflow-configuration.md) (extend) |
| 4 | idea | **Shared rich-text editor** for Description and Text custom fields | Description is textarea today; CF Text should share one editor | `rich-text-editor` | — |
| 5 | idea | **Ticket attachments** (upload files on ticket) | Common tracker need; comments alone are weak for specs | `ticket-attachments` | — |
| 6 | idea | **Related / linked tickets** (blocks, relates-to, duplicates) | Cross-ticket planning without phases alone | `ticket-links` | — |
| 7 | idea | **Clone ticket** | Fast create from existing work item | `ticket-management` | [ticket-management.md](../feature/ticket-management.md) (extend) |
| 8 | idea | **CSV / query export** of tickets | Symmetric to import; reporting | `ticket-export` | — |
| 9 | idea | **@mentions in comments** + notify mentioned users | Collaboration; ties to notifications | `ticket-mentions` | — |
| 10 | idea | **Due-date reminders** (email and/or in-app) | Due date exists; no nudge yet | `due-date-reminders` | — |
| 11 | idea | **Bulk ticket operations** (assign, move, set fields) | Power-user / triage | `bulk-ticket-ops` | — |
| 12 | idea | **Custom fields on Kanban cards** | Deferred from CF v1 (**FQ20**) | `custom-fields` | [custom-fields.md](../feature/custom-fields.md) (later changelog) |
| 13 | idea | **Enum multi-select** custom fields | Deferred from CF v1 (**FQ11**) | `custom-fields` | [custom-fields.md](../feature/custom-fields.md) (later changelog) |
| 14 | idea | **Notify / email on custom field change** | Deferred from CF v1 (**FQ17**) | `custom-fields` | [custom-fields.md](../feature/custom-fields.md) (later changelog) |
| 15 | idea | **Board / dashboard filters by custom fields** | Analytics after CF ships | `kanban-board` / `project-dashboard` | — |
| 16 | idea | **Webhooks** (ticket created/moved/updated) | External integrations | `webhooks` | — |
| 17 | idea | **Personal API tokens** | Scripting without user password | `api-tokens` | — |
| 18 | idea | **Project archive** (hide from menus, read-only) | Soft end-of-life without delete | `project-administration` | — |
| 19 | idea | **Time estimate / spent** on tickets | Lightweight planning metric | `time-tracking` | — |
| 20 | idea | **UI i18n** (EN + keep PT-BR) | Broader audience; labels mixed today | `i18n` | — |
| 21 | idea | **Saved Kanban view preferences** (swimlane, phase filter) | Swimlane not persisted today | `kanban-board` | — |
| 22 | idea | **Admin audit log** (config changes) | Complement ticket history | `audit-log` | — |
| 23 | idea | **Recurring tickets** | Ops / checklist cadence | `recurring-tickets` | — |
| 24 | idea | **SLA / age policies** (warn when stuck in status) | Ops maturity | `sla-policies` | — |

## Suggested next picks (after current promoted work)

1. Finish **custom-fields** (T1–T15) and **notifications** v2.  
2. **Workflow status edit** — unblocks real process evolution.  
3. **Rich-text editor** — unlocks Description + Text CF quality.  
4. **Attachments** or **ticket links** — highest user-visible tracker gaps.

## Changelog (backlog maintenance)

| Date | Change |
|------|--------|
| 2026-07-10 | Initial backlog + ordering; seeded from ARCHITECTURE §13, custom-fields deferred FQs, and product gaps |

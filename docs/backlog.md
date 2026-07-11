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
| 1 | promoted | **Agentic Development integration** — PAT + SA + separate Quarkus MCP | Tasks-ready; await task approval | `agentic-integration` | [agentic-integration.md](../feature/agentic-integration.md) |
| 2 | promoted | **Git integration** — project repo + commits on ticket history | Tasks-ready T1–T10; await approval | `git-integration` | [git-integration.md](../feature/git-integration.md) |
| 3 | promoted | **CSV import chunked upload** | Tasks-ready T1–T7; await approval | `ticket-import` | [ticket-import.md](../feature/ticket-import.md) |
| 4 | promoted | **UI i18n** (EN + keep PT-BR) | Tasks-ready; FQ/AQ answered | `i18n` | [i18n.md](../feature/i18n.md) |
| 5 | idea | **Ticket attachments** (upload files on ticket) | Common tracker need; comments alone are weak for specs | `ticket-attachments` | — |
| 6 | idea | **Clone ticket** | Fast create from existing work item | `ticket-management` | [ticket-management.md](../feature/ticket-management.md) (extend) |
| 7 | idea | **CSV / JSON export** of tickets | Symmetric to import; reporting | `ticket-export` | — |
| 8 | idea | **@mentions in comments** + notify mentioned users | Collaboration; ties to notifications | `ticket-mentions` | — |
| 9 | idea | **Due-date reminders** (email and/or in-app) | Due date exists; no nudge yet | `due-date-reminders` | — |
| 10 | idea | **Bulk ticket operations** (assign, move, set fields) | Power-user / triage | `bulk-ticket-ops` | — |
| 11 | idea | **Custom fields on Kanban cards** | Deferred from CF v1 (**FQ20**) | `custom-fields` | [custom-fields.md](../feature/custom-fields.md) (later changelog) |
| 12 | idea | **Enum multi-select** custom fields | Deferred from CF v1 (**FQ11**) | `custom-fields` | [custom-fields.md](../feature/custom-fields.md) (later changelog) |
| 13 | idea | **Notify / email on custom field change** | Deferred from CF v1 (**FQ17**) | `custom-fields` | [custom-fields.md](../feature/custom-fields.md) (later changelog) |
| 14 | idea | **Board / dashboard filters by custom fields** | Analytics after CF ships | `kanban-board` / `project-dashboard` | — |
| 15 | idea | **Webhooks** (ticket created/moved/updated) | External integrations; confirmed **separate** from agentic v1 (**FQ7**) | `webhooks` | — |
| 16 | idea | **Project archive** (hide from menus, read-only) | Soft end-of-life without delete | `project-administration` | — |
| 17 | idea | **Time estimate / spent** on tickets | Hours spent etc.; story points shipped with burndown | `time-tracking` | — |
| 18 | idea | **Saved Kanban view preferences** (swimlane, phase filter) | Swimlane not persisted today | `kanban-board` | — |
| 19 | idea | **Admin audit log** (config changes) | Complement ticket history | `audit-log` | — |
| 20 | idea | **Recurring tickets** | Ops / checklist cadence | `recurring-tickets` | — |
| 21 | idea | **SLA / age policies** (warn when stuck in status) | Ops maturity | `sla-policies` | — |
| — | done | **Editable workflow statuses after create** | Shipped 2026-07-11 — [workflow-configuration.md](../feature/workflow-configuration.md) v2 | `workflow-configuration` | [workflow-configuration.md](../feature/workflow-configuration.md) |
| — | done | **Ticket links, epics & subtasks** (typed links + hierarchy) | Peer links + parent/child; not Phase/Category | `ticket-links` | [ticket-links.md](../feature/ticket-links.md) |
| — | done | **Burndown** — points remaining vs ideal over phase | Shipped 2026-07-11 — [burndown.md](../feature/burndown.md) v1 | `burndown` | [burndown.md](../feature/burndown.md) |
| — | done | **Ticket backlog** — ranked list + reorder | Shipped 2026-07-11 — [ticket-backlog.md](../feature/ticket-backlog.md) v1 | `ticket-backlog` | [ticket-backlog.md](../feature/ticket-backlog.md) |
| — | promoted | **Personal API tokens** | Subsumed by agentic-integration v1 | `agentic-integration` | [agentic-integration.md](../feature/agentic-integration.md) |
| — | done | **Shared rich-text editor** for Description, Text CF, project/template description | Shipped 2026-07-11 — [rich-text-editor.md](../feature/rich-text-editor.md) v1 | `rich-text-editor` | [rich-text-editor.md](../feature/rich-text-editor.md) |
| — | done | **Notifications** — mark all as read + accurate unread badge | Shipped 2026-07-11 — [notifications.md](../feature/notifications.md) v3 | `notifications` | [notifications.md](../feature/notifications.md) |
| — | done | **Notifications** — SSE reconnect, infinite scroll, page API | Shipped 2026-07-10 — [notifications.md](../feature/notifications.md) v2 | `notifications` | [notifications.md](../feature/notifications.md) |
| — | done | **Custom fields** for tickets (project + workflow defs, values, import, query) | Shipped 2026-07-10 — [custom-fields.md](../feature/custom-fields.md) v1 | `custom-fields` | [custom-fields.md](../feature/custom-fields.md) |
| — | done | **Custom fields** — disabled CF section on new workflow | Shipped 2026-07-11 — [custom-fields.md](../feature/custom-fields.md) v2 | `custom-fields` | [custom-fields.md](../feature/custom-fields.md) |

## Suggested next picks (after current promoted work)

1. **Approve agentic-integration** tasks → implement ([agentic-integration.md](../feature/agentic-integration.md)).  
2. **Approve git-integration** T1–T10 → implement ([git-integration.md](../feature/git-integration.md)).  
3. **Approve ticket-import** chunked upload T1–T7 → implement ([ticket-import.md](../feature/ticket-import.md) v2).  
4. **Approve i18n** tasks → implement ([i18n.md](../feature/i18n.md)).  
5. **Attachments** — highest remaining user-visible tracker gap among ideas.

## Changelog (backlog maintenance)

| Date | Change |
|------|--------|
| 2026-07-10 | Initial backlog + ordering; seeded from ARCHITECTURE §13, custom-fields deferred FQs, and product gaps |
| 2026-07-10 | Custom fields v1 → `done`; renumbered active ranks |
| 2026-07-11 | Notifications v2 → `done` (was stale `promoted`); renumbered active ranks |
| 2026-07-11 | Notifications v3 (mark-all + unread badge) → `promoted`; renumbered |
| 2026-07-11 | Notifications v3 → `done`; renumbered active ranks |
| 2026-07-11 | Rich-text editor → `promoted`; linked [rich-text-editor.md](../feature/rich-text-editor.md) |
| 2026-07-11 | Agentic Development integration → `promoted`; personal API tokens subsumed; linked [agentic-integration.md](../feature/agentic-integration.md) |
| 2026-07-11 | Rich-text editor v1 → `done`; renumbered active ranks |
| 2026-07-11 | Agentic FQs answered; status architecture-ready; webhooks confirmed separate |
| 2026-07-11 | Agentic AQ7 separate MCP project + defaults; status `tasks-ready` |
| 2026-07-11 | Git integration → `promoted`; linked [git-integration.md](../feature/git-integration.md); renumbered active ranks |
| 2026-07-11 | Ticket links / epics / subtasks → `promoted`; linked [ticket-links.md](../feature/ticket-links.md) |
| 2026-07-11 | Ticket backlog (ranked list + reorder) → `promoted`; linked [ticket-backlog.md](../feature/ticket-backlog.md); renumbered active ranks |
| 2026-07-11 | Burndown → `promoted`; exploration in [burndown.md](../feature/burndown.md); renumbered active ranks |
| 2026-07-11 | Burndown FQ1–3,7–8 answered: points-only, dedicated Kanban-peer route, disable+tooltip without dates, canceled=burned |
| 2026-07-11 | Burndown FQ11–12 + defaults for FQ4–6,10,13–14; architecture + tasks T1–T10 → `tasks-ready` |
| 2026-07-11 | UI i18n → `promoted`; linked [i18n.md](../feature/i18n.md); FQ1–10 open |
| 2026-07-11 | Ticket backlog v1 → `done`; renumbered active ranks |
| 2026-07-11 | Git integration FQ/AQ accepted; architecture + tasks T1–T10 → `tasks-ready` |
| 2026-07-11 | Workflow editable statuses → `promoted`; FQ1 yes, FQ2 remap-to-suggested status; FQ3–8 opened |
| 2026-07-11 | Workflow FQ3–8 + AQ1–3 accepted; architecture + tasks T1–T7 → `tasks-ready` |
| 2026-07-11 | Burndown v1 → `done`; renumbered active ranks |
| 2026-07-11 | CSV import chunked upload → `promoted` ([ticket-import.md](../feature/ticket-import.md) v2 `tasks-ready`); i18n notes → FQ/AQ answered / tasks-ready; workflow → in-progress; custom-fields v2 → `done`; renumbered active ranks 1–22 |
| 2026-07-11 | Workflow editable statuses v2 → `done`; renumbered active ranks |

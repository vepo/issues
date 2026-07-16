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
| 1 | promoted | **UI i18n** (EN + keep PT-BR) | Tasks-ready; FQ/AQ answered | `i18n` | [i18n.md](../feature/i18n.md) |
| 2 | idea | **Ticket attachments** (upload files on ticket) | Common tracker need; comments alone are weak for specs | `ticket-attachments` | — |
| 3 | idea | **Clone ticket** | Fast create from existing work item | `ticket-management` | [ticket-management.md](../feature/ticket-management.md) (extend) |
| 4 | idea | **CSV / JSON export** of tickets | Symmetric to import; reporting | `ticket-export` | — |
| 5 | idea | **@mentions in comments** + notify mentioned users | Collaboration; ties to notifications | `ticket-mentions` | — |
| 6 | idea | **Due-date reminders** (email and/or in-app) | Due date exists; no nudge yet | `due-date-reminders` | — |
| 7 | idea | **Bulk ticket operations** (assign, move, set fields) | Power-user / triage | `bulk-ticket-ops` | — |
| 8 | idea | **Custom fields on Kanban cards** | Deferred from CF v1 (**FQ20**) | `custom-fields` | [custom-fields.md](../feature/custom-fields.md) (later changelog) |
| 9 | idea | **Enum multi-select** custom fields | Deferred from CF v1 (**FQ11**) | `custom-fields` | [custom-fields.md](../feature/custom-fields.md) (later changelog) |
| 10 | idea | **Notify / email on custom field change** | Deferred from CF v1 (**FQ17**) | `custom-fields` | [custom-fields.md](../feature/custom-fields.md) (later changelog) |
| 11 | idea | **Board / dashboard filters by custom fields** | After dashboard hardening | `kanban-board` / `project-dashboard` | — |
| 12 | idea | **Dashboard widgets** — WIP, throughput, assignee, due-date, points | Suggested in [project-dashboard.md](../feature/project-dashboard.md) audit; after v3 | `project-dashboard` | [project-dashboard.md](../feature/project-dashboard.md) |
| 13 | idea | **Webhooks** (ticket created/moved/updated) | External integrations; confirmed **separate** from agentic v1 (**FQ7**) | `webhooks` | — |
| 14 | idea | **Project archive** (hide from menus, read-only) | Soft end-of-life without delete | `project-administration` | — |
| 15 | idea | **Time estimate / spent** on tickets | Hours spent etc.; story points shipped with burndown | `time-tracking` | — |
| 16 | idea | **Saved Kanban view preferences** (swimlane, phase filter) | Swimlane not persisted today | `kanban-board` | — |
| 17 | idea | **Admin audit log** (config changes) | Complement ticket history | `audit-log` | — |
| 18 | idea | **Recurring tickets** | Ops / checklist cadence | `recurring-tickets` | — |
| 19 | idea | **SLA / age policies** (warn when stuck in status) | Ops maturity | `sla-policies` | — |
| — | done | **CSV import chunked upload** | Shipped 2026-07-16 — [ticket-import.md](../feature/ticket-import.md) v2 | `ticket-import` | [ticket-import.md](../feature/ticket-import.md) |
| — | done | **Git integration** — project repo + commits on ticket history | Shipped 2026-07-16 — [git-integration.md](../feature/git-integration.md) v1 | `git-integration` | [git-integration.md](../feature/git-integration.md) |
| — | done | **Catalog compliance fixes** — SPA guards, admin project update, shell menus | Shipped 2026-07-16 — multi-feature | `catalog-compliance` | (multi) |
| — | done | **Project dashboard hardening** — enum path, membership, UX | Shipped 2026-07-16 — [project-dashboard.md](../feature/project-dashboard.md) v3 | `project-dashboard` | [project-dashboard.md](../feature/project-dashboard.md) |
| — | done | **Project visibility** — Private / Internal / Public security levels (SEC1) | Shipped 2026-07-16 — [project-visibility.md](../feature/project-visibility.md) v1 | `project-visibility` | [project-visibility.md](../feature/project-visibility.md) |
| — | done | **Agentic Development integration** — PAT + SA + separate Quarkus MCP | Shipped 2026-07-11 — [agentic-integration.md](../feature/agentic-integration.md) v1 | `agentic-integration` | [agentic-integration.md](../feature/agentic-integration.md) |
| — | done | **Editable workflow statuses after create** | Shipped 2026-07-11 — [workflow-configuration.md](../feature/workflow-configuration.md) v2 | `workflow-configuration` | [workflow-configuration.md](../feature/workflow-configuration.md) |
| — | done | **Ticket links, epics & subtasks** (typed links + hierarchy) | Peer links + parent/child; not Phase/Category | `ticket-links` | [ticket-links.md](../feature/ticket-links.md) |
| — | done | **Burndown** — points remaining vs ideal over phase | Shipped 2026-07-11 — [burndown.md](../feature/burndown.md) v1 | `burndown` | [burndown.md](../feature/burndown.md) |
| — | done | **Ticket backlog** — ranked list + reorder | Shipped 2026-07-11 — [ticket-backlog.md](../feature/ticket-backlog.md) v1 | `ticket-backlog` | [ticket-backlog.md](../feature/ticket-backlog.md) |
| — | done | **Personal API tokens** | Subsumed and shipped under agentic-integration (account PAT UI + Bearer) | `agentic-integration` | [agentic-integration.md](../feature/agentic-integration.md) |
| — | done | **Shared rich-text editor** for Description, Text CF, project/template description | Shipped 2026-07-11 — [rich-text-editor.md](../feature/rich-text-editor.md) v1 | `rich-text-editor` | [rich-text-editor.md](../feature/rich-text-editor.md) |
| — | done | **Notifications** — mark all as read + accurate unread badge | Shipped 2026-07-11 — [notifications.md](../feature/notifications.md) v3 | `notifications` | [notifications.md](../feature/notifications.md) |
| — | done | **Notifications** — SSE reconnect, infinite scroll, page API | Shipped 2026-07-10 — [notifications.md](../feature/notifications.md) v2 | `notifications` | [notifications.md](../feature/notifications.md) |
| — | done | **Custom fields** for tickets (project + workflow defs, values, import, query) | Shipped 2026-07-10 — [custom-fields.md](../feature/custom-fields.md) v1 | `custom-fields` | [custom-fields.md](../feature/custom-fields.md) |
| — | done | **Custom fields** — disabled CF section on new workflow | Shipped 2026-07-11 — [custom-fields.md](../feature/custom-fields.md) v2 | `custom-fields` | [custom-fields.md](../feature/custom-fields.md) |

## Suggested next picks (after current promoted work)

1. **Approve i18n** tasks → implement ([i18n.md](../feature/i18n.md)).  
2. **Attachments** — highest remaining user-visible tracker gap among ideas.

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
| 2026-07-11 | Project dashboard hardening → `promoted` (v3 planned from code audit); new widgets idea; renumbered active ranks |
| 2026-07-11 | Project dashboard FQ5–8 / AQ1–3 accepted; architecture + tasks T1–T9 → `tasks-ready` |
| 2026-07-11 | Feature-catalog review → sync personas/README/catalog; promote **catalog-compliance**; agentic backlog note → `in-progress` |
| 2026-07-11 | Agentic T14 docs + [issues-agent](../.cursor/skills/issues-agent/) skill; personal API tokens backlog → `done` (subsumed); agentic remains `promoted` until T15/`done` |
| 2026-07-11 | **Project visibility** (SEC1 security levels) → `promoted` Order 1; linked [project-visibility.md](../feature/project-visibility.md); renumbered active ranks |
| 2026-07-11 | Agentic Development integration v1 → `done`; renumbered active ranks |
| 2026-07-11 | Project visibility FQ1–FQ9 answered (default **Internal**); AQ1–AQ5 opened |
| 2026-07-16 | Project visibility AQ1–AQ5 accepted; architecture + tasks T1–T11 → `tasks-ready` |
| 2026-07-16 | Project visibility v1 → `done`; renumbered active ranks |
| 2026-07-16 | Project dashboard hardening v3 → `done`; renumbered active ranks |
| 2026-07-16 | Catalog compliance → `done` (user guards, admin update, shell menus, seed); renumbered active ranks |
| 2026-07-16 | Git integration v1 → `done`; renumbered active ranks |
| 2026-07-16 | CSV import chunked upload v2 → `done`; renumbered active ranks |

# Project dashboard

**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Per-project analytics page with configurable widget layout: pie charts (tickets by day, status, priority), recent tickets table, and performance KPIs. Default widgets shown on first visit; users customize via **Editar layout**.

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `dashboards`, `ticket`, `project`, `workflow` |
| Packages / files | `dashboards.pie.LoadPieDashboardEndpoint`, `dashboards.table.LoadTableDashboardEndpoint`, `dashboards.kpi.LoadKpiDashboardEndpoint` |
| API | `GET /projects/{id}/dashboard/pie/{type}`, `/dashboard/table/{type}`, `/dashboard/kpi/{type}` |
| UI | `/project/:projectId/dashboard`; `dashboard` component (Chart.js/ng2-charts) |
| Schema / seed | Reads `tb_tickets`, workflow statuses; no dedicated dashboard tables |
| Tests | `LoadPieDashboardEndpointTest`, `LoadTableDashboardEndpointTest`, `LoadKpiDashboardEndpointTest` |
| Docs | domain-spec (Dashboard, Dashboard widget types), feature-catalog (Project dashboard), README § Views & analytics |

### Risks and open questions

- Widget layout persistence mechanism (local vs server) — verify implementation if extending.
- Chart performance on large ticket volumes.

## Changelog

### Initial implementation — baseline

**Status:** done

**Description:** Dashboard with tickets-by-day, tickets-by-status, tickets-by-priority pie charts, recent-tickets table, and performance-kpi widget; editable layout.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Kanban board | Shared project navigation |
| Ticket management | Recent tickets link to detail |
| Project administration | Project must exist |
| — | None identified |

**Implementation notes:** `dashboard.component.ts`; `DashboardType` enum drives widget types; Chart.js visualizations.

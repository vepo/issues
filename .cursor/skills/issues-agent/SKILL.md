---
name: issues-agent
description: >-
  Use Issues via MCP or REST with personal/service-account API tokens.
  Apply when coding agents need ticket context, updates, moves, or comments
  on the Issues tracker (get_ticket_context, PAT, iss_pat_, iss_sat_).
---

# Issues agent (MCP)

Backup skill for coding agents. Prefer in-app **Conectar agente** at `/account/settings` for paste-ready config.

## Auth

| Token | Prefix | Scope |
|-------|--------|-------|
| Personal API token (PAT) | `iss_pat_…` | Acts as the user |
| Service account token | `iss_sat_…` | Member-aligned powers on one project |

Send `Authorization: Bearer <token>` to Issues `/api` and to the MCP server (MCP forwards the same header).

Create PAT: Account → **Tokens de API** / **Conectar agente**.  
Create SA: `/projects/:projectId/service-accounts` (PM/admin).

## MCP tools (`issues-mcp`)

Separate Quarkus app — see [`issues-mcp/README.md`](../../../issues-mcp/README.md). Default local URL: `http://localhost:8082/mcp`.

| Tool | Purpose |
|------|---------|
| `list_projects` | List viewable projects |
| `search_tickets` | Search tickets |
| `get_ticket_context` | Detail + transitions + in-scope custom fields |
| `update_ticket` | Update fields |
| `move_ticket` | Workflow transition |
| `add_comment` | Add comment |

Prefer **`get_ticket_context`** before mutating a ticket.

## Cursor snippet

```json
{
  "mcpServers": {
    "issues": {
      "url": "http://localhost:8082/mcp",
      "headers": {
        "Authorization": "Bearer <YOUR_API_TOKEN>"
      }
    }
  }
}
```

Or call `GET /api/agent/setup-config?preset=cursor` while logged in (uses `issues.mcp-public-base-url`).

## Attribution

Mutations via API token set `via_agent`; ticket **Atividade** shows **Agente em nome de &lt;nome&gt;** (user or service-account display name).

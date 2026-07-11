# issues-mcp

Separate Quarkus MCP HTTP server that exposes coding-agent tools over the Issues REST API. Bearer tokens from the MCP client (`Authorization` header) are forwarded to Issues.

This module is **standalone** (own `pom.xml`). It is not yet part of the Issues Maven reactor; it can be absorbed as a multi-module reactor member later.

## Prerequisites

- Java 21
- Issues API running locally (default `http://localhost:8080/api`)
- A personal API token (`iss_pat_…`) or service-account token (`iss_sat_…`)

## Run locally

Terminal 1 — Issues:

```bash
cd /home/vepo/source/issues
mvn quarkus:dev
```

Terminal 2 — MCP:

```bash
cd /home/vepo/source/issues/issues-mcp
mvn quarkus:dev
```

MCP listens on **port 8082**. Quarkiverse MCP HTTP endpoint:

| Transport | Path |
|-----------|------|
| Streamable HTTP (default) | `http://localhost:8082/mcp` |
| Legacy SSE | `http://localhost:8082/mcp/sse` |

## Cursor MCP snippet

Matches Issues `GET /api/agent/setup-config?preset=cursor` (`issues.mcp-public-base-url` + `/mcp`):

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

## Configuration

| Property | Default | Meaning |
|----------|---------|---------|
| `issues.api-base-url` | `http://localhost:8080/api` | Issues REST base (includes `/api`) |
| `quarkus.http.port` | `8082` | MCP server port |
| `quarkus.mcp.server.http.root-path` | `/mcp` | MCP HTTP root path |

Override example:

```bash
mvn quarkus:dev -Dissues.api-base-url=http://localhost:8080/api -Dquarkus.http.port=8082
```

## Tools

| Tool | Issues API |
|------|------------|
| `search_tickets` | `GET /tickets/search` |
| `get_ticket_context` | `GET /tickets/{id}/context` |
| `update_ticket` | `POST /tickets/{id}` |
| `move_ticket` | `POST /tickets/{id}/move` |
| `add_comment` | `POST /tickets/{id}/comments` |
| `list_projects` | `GET /projects` |

## Build / test

```bash
mvn -f issues-mcp/pom.xml verify
```

## Related

- Feature: [../feature/agentic-integration.md](../feature/agentic-integration.md)
- Issues setup-config: `GET /api/agent/setup-config?preset=cursor`

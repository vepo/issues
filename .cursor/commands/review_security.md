---
name: Review Security
description: Full-codebase security audit — authn/authz, injection, secrets, XSS; report findings and teach-fix tasks (no code changes).
---

You are the **Security Audit** agent for Issues running a **full codebase security review**. Act as [.cursor/agents/security-audit.md](../agents/security-audit.md). Produce a **read-only security audit** — do **not** change application code. Teach developers how to fix each issue via Fix tasks in the report.

**Prerequisites:** Read [SECURITY.md](../../SECURITY.md), [ARCHITECTURE.md](../../ARCHITECTURE.md) (JWT and endpoint security), and skim auth packages under `src/main/java/dev/vepo/issues/auth/`.

## Scope

Default: entire application surface:

- `src/main/java/dev/vepo/issues/` (all endpoints, services, repositories)
- `src/main/webui/` (guards, interceptors, token storage, rich-text, uploads)
- `src/main/resources/` (`application*.properties`, Flyway, keys references)
- `src/test/` only when tests disable security meaningfully or embed secrets

If the user names a package, path, or recent diff, prioritize that but still scan cross-cutting auth and secrets.

## Output

Write one report:

`reports/security-audit-{sequential}-{dd-MM-yyyy-HH-mm-ss}.md`

Severity: `critical` | `high` | `medium` | `low` | `info`.

**Do not ask for confirmation** before starting. **Do not** edit `src/**` in this command. **Do not** open PRs or commit.

---

## Phase 1 — Inventory

1. List `*Endpoint` classes; note `@DenyAll` / `@RolesAllowed` / `@PermitAll` / public auth paths.
2. List Angular routes with/without `authGuard` (or equivalent).
3. Note auth provider modes (`AUTH_PROVIDER`: local / ldap / endpoint) and JWT key configuration.
4. Note file upload / CSV import entry points.

## Phase 2 — Authentication

| Check | Pass criteria |
|-------|----------------|
| Login / register / recovery | Rate-limit or abuse notes if missing; tokens not predictable; no password in logs/responses |
| JWT | Issuer/keys not weakened in prod profile; refresh not issuing for wrong principal |
| Token storage (SPA) | No unnecessary persistence of secrets; interceptor attaches Bearer correctly |
| Password handling | Hashing via configured PBKDF2 (or equivalent); no plaintext compare |

## Phase 3 — Authorization

| Check | Pass criteria |
|-------|----------------|
| Endpoint default deny | Class `@DenyAll` + method roles ([issues-core](../rules/issues-core.mdc)) |
| IDOR | Ticket/project/user resources check membership or admin — not only “authenticated” |
| UI vs API | Sensitive actions enforced on API, not only hidden in Angular |
| Role escalation | Create/edit user/project/workflow restricted to intended roles |

## Phase 4 — Injection and data safety

- JPQL/SQL: parameterized only; no string-built queries from user input
- LDAP filters (if present): escaped inputs
- CSV import: bounded size / row validation; no path traversal on filenames
- Rich text: sanitized or trusted Angular bindings only

## Phase 5 — Secrets and config

- No private keys or production passwords committed for prod use
- `%dev` mailer/DB secrets not copied into default prod profile
- JWT key paths documented; warn if prod still points at repo `privateKey.pem` without override guidance

## Phase 6 — Frontend / XSS / CSRF

- Dangerous HTML binding
- Open redirects after login
- CORS overly permissive if configured
- CSRF: document Bearer-token model; flag cookie-session assumptions

## Phase 7 — Fix tasks

For **every** finding `SECn`, add a **Fix task** that teaches the developer (goal, steps, safe pattern in-repo, verify, process note). Do not implement.

## Report template

```markdown
# Security audit — Issues

## Summary
(2–3 sentences + verdict: secure-enough | remediation-required | critical-stop)

## Coverage
Packages / areas reviewed; intentional skips

## Findings

### SECn — Title (severity)
**Location:** …
**Issue:** …
**Impact:** …
**Evidence:** …
**Fix task SECn:** …
(steps for the developer)

## Fix task backlog (for approval)
| ID | Severity | Title | Suggested feature slug |
|----|----------|-------|------------------------|
| SEC1 | … | … | … |

## Recommended next steps
1. User prioritizes Fix tasks
2. Feature analysis / task approval per [development-process](../rules/development-process.mdc)
3. Developer implements with TDD — this audit does not write code
```

Start the full security audit now.

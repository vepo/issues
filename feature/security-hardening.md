# Security hardening

**Feature version:** 1  
**Status:** done  
**Requested:** 2026-07-11

## Summary

Remediate [security-audit](../reports/security-audit-1-11-07-2026-16-38-26.md) findings **SEC2–SEC15**. **SEC1** (ticket project membership / project visibility) is out of scope — tracked in [project-visibility.md](project-visibility.md).

## Wireframe

| Field | Value |
|-------|-------|
| **Source** | N/A — primarily backend/config; SPA route guards only |
| **Last updated** | 2026-07-11 |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `auth`, `notifications`, `ticket` import, `dashboards`, `phase`, `project`, `user` (SPA) |
| Schema | Password hash format; refresh/reset token hashes |
| Config | JWT prod keys, Swagger, mailer env, password.default scoped |
| Docs | SECURITY.md, README |

## Architecture

| Area | Design |
|------|--------|
| AuthZ | `ProjectAccessService.requireView` / `requireManage` on dashboard, phase, version, import |
| Password | Per-user salt embedded in stored hash; `MessageDigest.isEqual` |
| HTML | Server-side sanitizer before persist |
| Rate limit | In-app bucket on auth endpoints → 429 |
| Tokens | Hash refresh/reset at rest like API tokens |

## Changelog

### Security audit remediations SEC2–SEC15 — 2026-07-11

**Version:** 1  
**Status:** done

**Description:** Close audit findings SEC2–SEC15 (exclude SEC1).

**Development approval:** approved 2026-07-11 — tasks: T1–T14 (plan confirmation)

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Prod cannot use repo JWT PEMs silently | SEC2 | ☑ |
| FC2 | Per-user password salt + constant-time compare | SEC3, SEC9 | ☑ |
| FC3 | Rich text sanitized on write | SEC4 | ☑ |
| FC4 | Notification mark-read owner-only | SEC5 | ☑ |
| FC5 | Import membership + author binding | SEC6 | ☑ |
| FC6 | Dashboard/phase/version requireView/Manage | SEC7 | ☑ |
| FC7 | Auth rate limiting → 429 | SEC8 | ☑ |
| FC8 | LDAP exact group match | SEC10 | ☑ |
| FC9 | Swagger off in prod | SEC11 | ☑ |
| FC10 | `/users*` admin roleGuard | SEC12 | ☑ |
| FC11 | Mailer secrets via env | SEC13 | ☑ |
| FC12 | Refresh/reset tokens hashed | SEC14 | ☑ |
| FC13 | Project create/update allow ADMIN | SEC15 | ☑ |

#### Tasks

| ID | Task | SEC | Done |
|----|------|-----|------|
| T1 | Dev-only JWT PEMs; prod requires external keys | SEC2 | ☑ |
| T2 | Per-user salt + constant-time compare; scope password.default | SEC3+9 | ☑ |
| T3 | Server HTML sanitizer on rich-text writes | SEC4 | ☑ |
| T4 | Notification mark-read ownership | SEC5 | ☑ |
| T5 | CSV import requireView + author binding | SEC6 | ☑ |
| T6 | Dashboard/phase/version requireView/Manage | SEC7 | ☑ |
| T7 | Auth endpoint rate limiting | SEC8 | ☑ |
| T8 | LDAP exact group match | SEC10 | ☑ |
| T9 | Swagger only %dev/%test | SEC11 | ☑ |
| T10 | roleGuard admin on /users* | SEC12 | ☑ |
| T11 | Mailtrap secrets → env | SEC13 | ☑ |
| T12 | Hash refresh/reset tokens | SEC14 | ☑ |
| T13 | ADMIN on project create/update RolesAllowed | SEC15 | ☑ |
| T14 | SECURITY.md + verify gates | Docs | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | Notification owner mark-read | T4 | ☑ |
| TC2 | Import foreign id / non-member | T5 | ☑ |
| TC3 | Dashboard/phase foreign project 403 | T6 | ☑ |
| TC4 | Password salt uniqueness + matches | T2 | ☑ |
| TC5 | HTML sanitizer strips script | T3 | ☑ |
| TC6 | Auth rate limit 429 | T7 | ☑ |
| TC7 | LDAP substring no escalate | T8 | ☑ |
| TC8 | ArchitectureTest / endpoint tests | T13 | ☑ |

**Implementation notes:**
- **T4 / SEC5:** `findByIdAndUsername`; non-owner → 404.
- **T5 / SEC6:** project upload `requireView`; import ops author-bound.
- **T6 / SEC7:** dashboard/phase/version `requireView` / `requireManage`.
- **T2 / SEC3+9:** `v1$iterations$saltB64$hashB64`; legacy rehash on login.
- **T8 / SEC10:** LDAP exact group match.
- **T12 / SEC14:** refresh/reset SHA-256 at rest.
- **T3 / SEC4:** OWASP Java HTML Sanitizer on ticket/comment/project/TEXT CF writes.
- **T1 / SEC2:** `ProdJwtKeyGuard` fails `%prod` on repo PEMs.
- **T7 / SEC8:** `AuthRateLimitFilter` → 429; disabled in `%test` by default.
- **T9–T11 / T13:** Swagger profile-gated; mailer env; users `roleGuard(['admin'])`; project create/update `@RolesAllowed` includes ADMIN.
- **T14:** SECURITY.md operational table; README JWT note.

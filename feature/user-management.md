# User management

**Feature version:** 3  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Administrators list, create, and edit users: name, email, and combinable roles (`user`, `admin`, `project-manager`). Public **self-registration** creates a user with role `user` only. Admins may **soft-delete** users when they are not assignees on blocking tickets. Local passwords must satisfy the **password policy** (8–64 chars, upper, lower, digit).

**v3 (catalog compliance):** Angular `/users*` routes must use `roleGuard(['admin'])` so URL access matches catalog Roles and the admin-only menu ([feature-catalog review](../reports/feature-catalog-review-1-11-07-2026-16-27-54.md)).

## Wireframe

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-10 |

### Screen: `/users`

| Region | Elements |
|--------|----------|
| List | `.data-table`: name, email, roles; search/filter chips |
| Actions | **Novo usuário**; row **Editar**; row **Excluir** + confirm |

### Screen: `/login/register`

| Region | Elements |
|--------|----------|
| Form | Username, name, email, password, confirm password; policy hint |
| Actions | **Criar conta**; link to **Entrar** |

## Impact

| Area | Effect |
|------|--------|
| API | `POST /auth/register`; `DELETE /users/{id}` |
| UI | `/login/register`; `/users` Excluir |
| Docs | domain-spec **49**; feature-catalog; README; ARCHITECTURE |

### Feature questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Self-registration? | answered | Yes |
| FQ2 | Delete while assignee on open tickets? | answered | No — block |
| FQ3 | Password policy? | answered | **B** — 8–64 + upper + lower + digit |
| FQ4 | Should `/users*` SPA routes use `roleGuard(['admin'])`? | answered | **Yes** — match catalog Roles; close authenticated URL bypass (menu already admin-only) |

## Architecture

**v3 — Harden user admin SPA guards**

| Area | Design |
|------|--------|
| Bounded contexts | `user` UI only — mutating APIs already `@RolesAllowed(ADMIN)` |
| Packages / layers | Angular `app.routes.ts` only; no Java change required for guards |
| API | Unchanged |
| Frontend | `/users`, `/users/new`, `/users/:userId` — `canActivate: [authGuard, roleGuard(['admin'])]` (same pattern as `/categories`) |
| Tests | Route guard unit/spec or component navigation test; optional e2e note |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Also restrict list GET APIs to ADMIN? | answered | **No for v3** — out of scope; SPA guard + mutating ADMIN is enough; revisit if list leaks sensitive fields |

## Changelog

### Harden user admin SPA role guards — 2026-07-11

**Version:** 3  
**Status:** done

**Description:** Add `roleGuard(['admin'])` on `/users`, `/users/new`, `/users/:userId` so non-admins cannot open admin UI by URL. Source: [feature-catalog-review](../reports/feature-catalog-review-1-11-07-2026-16-27-54.md) major finding.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Authentication | Reuses existing `roleGuard` |
| Feature catalog | Remove “menu-only until roleGuard” note when done |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | `/users*` routes require admin roleGuard | FQ4, catalog | ☑ |
| FC2 | Non-admin redirected/blocked when opening `/users` by URL | Review finding | ☑ |
| FC3 | Admin can still list/create/edit | Regression | ☑ |
| FC4 | `feature-catalog.md` User rows drop interim menu-only note | Docs | ☑ |
| FC5 | Angular route/guard test | Tests | ☑ |

#### Tasks

| ID | Deliverable | Done |
|----|-------------|------|
| T1 | Add `roleGuard(['admin'])` to `/users`, `/users/new`, `/users/:userId` in `app.routes.ts` | ☑ |
| T2 | Spec or guard test: non-admin blocked; admin allowed | ☑ |
| T3 | Update `feature-catalog.md` User list/create/edit Steps (remove interim note) | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | Non-admin cannot activate `/users` routes | T1, T2 | ☑ |
| TC2 | Admin can activate `/users` routes | T1, T2 | ☑ |

**Development approval:** approved 2026-07-16 — tasks: T1, T2, T3

**Implementation notes (2026-07-16):** `roleGuard(['admin'])` already on routes; added `role.guard.spec.ts`; catalog interim notes removed.

**Version:** 1  
**Status:** done

### Self-registration and user removal guard — 2026-07-03

**Version:** 2  
**Status:** done

**Development approval:** approved 2026-07-10 — tasks: T1, T2, T3, T4, T5, T6

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Self-registration API + `/login/register` UI | FQ1 | ☑ |
| FC2 | Registered users get role `user` only | AQ3 | ☑ |
| FC3 | Password policy **FQ3** B on register / change / reset | FQ3 | ☑ |
| FC4 | Soft-delete succeeds when no blocking assignments | FQ2 | ☑ |
| FC5 | Delete rejected when assignee on blocking tickets | FQ2 | ☑ |
| FC6 | UI Excluir + confirm; error toast | Wireframe | ☑ |
| FC7 | domain-spec **49** + feature-catalog + README | Docs | ☑ |
| FC8 | Endpoint + Angular tests | Tests | ☑ |

#### Tasks

| ID | Deliverable | Done |
|----|-------------|------|
| T1 | `@StrongPassword` on change/reset/register | ☑ |
| T2 | `POST /auth/register` | ☑ |
| T3 | Soft-delete + assignee guard | ☑ |
| T4 | Endpoint tests | ☑ |
| T5 | Angular register + Excluir + validators | ☑ |
| T6 | Docs | ☑ |

**Implementation notes:** `StrongPassword` composite constraint; `RegisterUserEndpoint`; `DeleteUserEndpoint` + `countBlockingAssignedTickets`. `mvn verify` + Angular specs green.

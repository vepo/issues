# User management

**Feature version:** 2  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Administrators list, create, and edit users: name, email, and combinable roles (`user`, `admin`, `project-manager`). Public **self-registration** creates a user with role `user` only. Admins may **soft-delete** users when they are not assignees on blocking tickets. Local passwords must satisfy the **password policy** (8–64 chars, upper, lower, digit).

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

## Changelog

### Initial implementation — baseline

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

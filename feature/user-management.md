# User management

**Feature version:** 1  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Administrators list, create, and edit users: name, email, password, and combinable roles (`user`, `admin`, `project-manager`). Supports assignee pickers and access control across the application.

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `user` (Identity & access) |
| Packages / files | `user.create`, `user.update`, `user.find`, `user.search` |
| API | `POST /users`, `POST /users/{id}`, `GET /users/{id}`, `GET /users/search` |
| UI | `/users`, `/users/new`, `/users/:userId`; `users-view`, `users-edit` components |
| Schema / seed | `tb_users`; dev personas in `dev-import.sql` |
| Tests | `CreateUserEndpointTest`, `UpdateUserEndpointTest`, `FindUserByIdEndpointTest`, `SearchUsersEndpointTest` |
| Docs | domain-spec (User, Role), feature-catalog (User list/create/edit), README § Projects & administration |

### Open questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| Q1 | Should self-registration be supported? | open | |
| Q2 | What password policy should apply beyond the dev default? | open | |

## Changelog

### Initial implementation — baseline

**Version:** 1  
**Status:** done

**Description:** Admin-only user CRUD with multi-role assignment and user search for assignee/autocomplete use cases.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Authentication | Users authenticate with created credentials |
| Ticket management | Assignee references users |
| All role-gated routes | Roles assigned here control access |
| — | None identified |

**Implementation notes:** `users-view.component.ts`, `users-edit.component.ts`; `@RolesAllowed("admin")` on mutating endpoints.

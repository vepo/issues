# Account settings

**Feature version:** 1  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Authenticated users view their profile (name, email, roles) and change password while logged in. Complements password recovery for users who know their current password.

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `auth` |
| Packages / files | `auth.me.MeEndpoint`, `auth.changepassword.ChangePasswordEndpoint` |
| API | `GET /auth/me`, `POST /auth/change-password` |
| UI | `/account/settings`; `account-settings` component |
| Schema / seed | `tb_users` (password hash update) |
| Tests | `MeEndpointTest`, `ChangePasswordEndpointTest` |
| Docs | feature-catalog (Account settings row), README § Authentication |

### Open questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| Q1 | Should users edit name and email in account settings (self-service)? | open | |

## Changelog

### Initial implementation — baseline

**Version:** 1  
**Status:** done

**Description:** Account settings page showing profile from `GET /auth/me`; change password form requiring current password.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Authentication | Reuses JWT session and `MeEndpoint` |
| User management | Admin edits other users; account settings is self-service only |
| — | None identified |

**Implementation notes:** `account-settings.component.ts`; `ChangePasswordEndpoint` validates current password before update.

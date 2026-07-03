# Authentication

**Feature version:** 1  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

JWT-based login and password recovery for Issues. Users authenticate with email and password, receive a Bearer token, and use it for all protected API calls and Angular routes. Unauthenticated users can request a password reset email and complete reset via a tokenized link.

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `auth` (Identity & access), `mailer` (recovery email) |
| Packages / files | `auth.login`, `auth.me`, `auth.recovery`, `auth.changepassword`; `PasswordResetToken`; `MailerService` |
| API | `POST /auth/login`, `GET /auth/me`, `POST /auth/recovery`, `POST /auth/recovery/confirm` |
| UI | `/login`, `/login/reset-password`, `/login/reset-password/:token`; `login`, `password-reset-request`, `password-reset` components; `auth.service`, `auth.guard`, `auth.interceptor` |
| Schema / seed | `tb_users`, `tb_password_reset_tokens`; dev personas in `dev-import.sql` |
| Tests | `LoginEndpointTest`, `MeEndpointTest`, `ResetPasswordEndpointTest`, `ConfirmPasswordResetEndpointTest` |
| Docs | domain-spec (Session, Password recovery), feature-catalog (Login, Password reset rows), README § Authentication |

### Open questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| Q1 | How should JWT key rotation be handled in production? | open | |
| Q2 | Should the app support token refresh, or remain single long-lived JWT? | open | |

## Changelog

### Initial implementation — baseline

**Version:** 1  
**Status:** done

**Description:** Login with email/password returning JWT; password recovery request and confirm flows; `GET /auth/me` for current user profile.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| All authenticated routes | Require valid Bearer token |
| Account settings | Uses `GET /auth/me` and `POST /auth/change-password` |
| Email delivery | Sends password reset link |
| — | None identified beyond auth gate |

**Implementation notes:** `auth.login.LoginEndpoint`, `auth.recovery.*`, `auth.me.MeEndpoint`; Angular `auth.service.ts` stores token; SmallRye JWT RS256 per `application.properties`.

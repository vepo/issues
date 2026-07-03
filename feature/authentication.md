# Authentication

**Feature version:** 2  
**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

JWT-based login and password recovery for Issues. Users authenticate with email and password, receive a Bearer token, and use it for all protected API calls and Angular routes. Unauthenticated users can request a password reset email and complete reset via a tokenized link.

## Wireframe

**Guide:** layout reference for UI implementation — update when routes or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-03 |

### Screen: `/login`

| Region | Elements |
|--------|----------|
| Center card | Email, password fields; **Entrar** primary button |
| Footer link | **Esqueci minha senha** → `/login/reset-password` |

```
┌──────────────────────────────────────┐
│              [Logo / Issues]         │
│  ┌────────────────────────────────┐  │
│  │ Email                          │  │
│  │ Senha                          │  │
│  │ [ Entrar ]                     │  │
│  │ Esqueci minha senha            │  │
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
```

### Screen: `/login/reset-password`

| Region | Elements |
|--------|----------|
| Center card | Email field; submit; link back to login |

### Screen: `/login/reset-password/:token`

| Region | Elements |
|--------|----------|
| Center card | New password + confirm; submit → redirect login |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `auth` (Identity & access), `mailer` (recovery email) |
| Packages / files | `auth.login`, `auth.me`, `auth.recovery`, `auth.changepassword`, **`auth.refresh`**; `PasswordResetToken`; `MailerService` |
| API | `POST /auth/login`, `GET /auth/me`, `POST /auth/recovery`, `POST /auth/recovery/confirm`, **`POST /auth/refresh`** |
| UI | `/login`, `/login/reset-password`, `/login/reset-password/:token`; `login`, `password-reset-request`, `password-reset` components; `auth.service`, `auth.guard`, `auth.interceptor` — **refresh token** handling in interceptor |
| Schema / seed | `tb_users`, `tb_password_reset_tokens`; dev personas in `dev-import.sql` |
| Tests | `LoginEndpointTest`, `MeEndpointTest`, `ResetPasswordEndpointTest`, `ConfirmPasswordResetEndpointTest`, **`RefreshTokenEndpointTest`** |
| Docs | domain-spec (Session, Password recovery), feature-catalog (Login, Password reset rows), README § Authentication |

### Risks

- Production JWT key rotation requires coordinated key material and refresh-token invalidation strategy.

### Feature questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | How should JWT key rotation be handled in production? | answered | **Yes** — support key rotation in production (multiple valid signing keys during rollover) |
| FQ2 | Should the app support token refresh, or remain single long-lived JWT? | answered | **Yes** — add refresh-token flow; short-lived access JWT + refresh token |

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

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Login form matches **Wireframe** `/login` | Wireframe | ☑ |
| FC2 | Password reset request matches **Wireframe** | Wireframe | ☑ |
| FC3 | Password reset confirm matches **Wireframe** | Wireframe | ☑ |
| FC4 | `feature-catalog.md` — Login and reset rows | Impact / Docs | ☑ |
| FC5 | JWT returned on successful login | Summary | ☑ |

**Implementation notes:** `auth.login.LoginEndpoint`, `auth.recovery.*`, `auth.me.MeEndpoint`; Angular `auth.service.ts` stores token; SmallRye JWT RS256 per `application.properties`.

### JWT refresh and key rotation — 2026-07-03

**Version:** 2  
**Status:** done

**Development approval:** approved 2026-07-03 — tasks: T2–T10

**Description:** Short-lived access JWT with refresh token; production JWT signing key rotation with overlapping valid keys.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| All authenticated routes | Interceptor refreshes access token before expiry |
| Account settings | Session continuity via refresh |
| — | None identified beyond auth layer |

## Architecture

| Area | Design |
|------|--------|
| Schema | `tb_refresh_tokens` — opaque token, user FK, expiry, revoked flag |
| Access JWT | 15 min TTL via `auth.access-token-minutes`; issued by `JwtTokenIssuer` |
| Refresh token | 30 days via `auth.refresh-token-days`; rotated on each `POST /auth/refresh` |
| API | `LoginResponse { token, refreshToken, expiresIn }`; `POST /auth/refresh` with `RefreshTokenRequest` |
| Invalidation | Revoke all refresh tokens on password change and password reset confirm |
| Key rotation | Document JWKS/multi-key verify via `mp.jwt.verify.publickey.location` in prod |
| Frontend | `auth.service` stores both tokens; interceptor retries once on 401 via refresh |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Refresh token storage? | answered | Server-side `tb_refresh_tokens` (opaque UUID), rotated on refresh |
| AQ2 | Access token TTL? | answered | **15 minutes** default (`auth.access-token-minutes`) |
| AQ3 | Key rotation mechanism? | answered | SmallRye JWT verify against JWKS or multi-key location — documented in README + `application.properties` |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T1 | Architecture + AQ + tasks + test plan | ☑ |
| T2 | Flyway `tb_refresh_tokens` + `RefreshToken` entity + repository | ☑ |
| T3 | `JwtTokenIssuer` + config properties | ☑ |
| T4 | Extend `LoginResponse`; login issues access + refresh tokens | ☑ |
| T5 | `RefreshTokenEndpoint` + `AuthenticationService.refresh` | ☑ |
| T6 | Revoke refresh tokens on password change / reset confirm | ☑ |
| T7 | `RefreshTokenEndpointTest` + update `LoginEndpointTest` | ☑ |
| T8 | Angular `auth.service` + interceptor 401 refresh retry | ☑ |
| T9 | OpenAPI codegen | ☑ |
| T10 | README + domain-spec + feature doc | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `RefreshTokenEndpointTest` — rotate refresh token | T5 | ☑ |
| TC2 | `RefreshTokenEndpointTest` — old refresh token rejected after rotation | T5 | ☑ |
| TC3 | `RefreshTokenEndpointTest` — refreshed token authorizes `/auth/me` | T5 | ☑ |
| TC4 | `LoginEndpointTest` — login returns refreshToken + expiresIn | T4 | ☑ |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | `POST /auth/refresh` issues new access token | FQ2 | ☑ |
| FC2 | Angular interceptor refreshes on 401/expiry | FQ2 | ☑ |
| FC3 | Production key rotation documented and configurable | FQ1 | ☑ |
| FC4 | `domain-specification.md` — Session / refresh terms | Docs | ☑ |

**Implementation notes:** Access JWT 15 min; refresh token 30 days with DB rotation; interceptor 401 retry. `mvn verify` + `npm run build` green (2026-07-03).

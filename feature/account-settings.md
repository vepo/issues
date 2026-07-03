# Account settings

**Feature version:** 2  
**Status:** planned  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Authenticated users view their profile (name, email, roles) and change password while logged in. Complements password recovery for users who know their current password.

## Wireframe

**Guide:** layout reference for UI implementation — update when routes or **Q*n*** decisions change ([development-process.mdc](../.cursor/rules/development-process.mdc)).

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-03 |

### Screen: `/account/settings`

| Region | Elements |
|--------|----------|
| Profile panel | Name, email (editable), roles (read-only) |
| Password panel | Senha atual, Nova senha, Confirmar; **Salvar** |

```
┌─────────────────────────────────────────────┐
│  Conta                                      │
├─────────────────────────────────────────────┤
│  Perfil                                     │
│  Nome [……]  Email [……]    Papéis: … (read-only)           │
├─────────────────────────────────────────────┤
│  Alterar senha                              │
│  Senha atual [________]                     │
│  Nova senha  [________]                     │
│  Confirmar   [________]                     │
│  [ Salvar ]                                 │
└─────────────────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `auth` |
| Packages / files | `auth.me.MeEndpoint`, `auth.changepassword.ChangePasswordEndpoint`, **`auth.updateprofile.UpdateProfileEndpoint`** |
| API | `GET /auth/me`, `POST /auth/change-password`, **`POST /auth/profile`** |
| UI | `/account/settings`; `account-settings` component |
| Schema / seed | `tb_users` (password hash update) |
| Tests | `MeEndpointTest`, `ChangePasswordEndpointTest`, **`UpdateProfileEndpointTest`** |
| Docs | feature-catalog (Account settings row), README § Authentication |

### Feature questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Should users edit name and email in account settings (self-service)? | answered | **Yes** — editable name and email on `/account/settings` |

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

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Settings page matches **Wireframe** | Wireframe | ☑ |
| FC2 | Profile loaded from `GET /auth/me` | Summary | ☑ |
| FC3 | Change password requires current password | Summary | ☑ |
| FC4 | `feature-catalog.md` — Account settings row | Impact / Docs | ☑ |

**Implementation notes:** `account-settings.component.ts`; `ChangePasswordEndpoint` validates current password before update.

### Self-service profile edit — 2026-07-03

**Version:** 2  
**Status:** planned

**Description:** Authenticated users update their own name and email on account settings; roles remain read-only.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| User management | Admin still edits other users; account settings is self-service only |
| Authentication | Profile changes reflected on next `GET /auth/me` |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Profile form matches **Wireframe** (editable name/email) | Wireframe, FQ1 | ☐ |
| FC2 | `POST /auth/profile` updates own user only | FQ1 | ☐ |
| FC3 | Email uniqueness validated | FQ1 | ☐ |

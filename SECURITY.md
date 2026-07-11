# Security Policy

## Supported Versions

This project is currently in active development and does not have any official release versions yet. All security updates will be applied to the main branch.

| Development Status | Supported          |
| ------------------ | ------------------ |
| main branch        | :white_check_mark: |

## Reporting a Vulnerability

If you discover a security vulnerability in this project, please report it by opening a new issue in our [GitHub Issues](https://github.com/vepo/issues/issues) page.

### What to include in your report:
- A clear description of the vulnerability
- Steps to reproduce the issue
- Any potential impact of the vulnerability
- Suggestions for mitigation (if you have any)

### Our response process:
1. We will acknowledge receipt of your report within 3 business days
2. We will investigate the issue and determine its severity
3. We will work on a fix and keep you updated on our progress
4. Once resolved, we will document the vulnerability and solution in the issue thread

### Important notes:
- Please do not disclose the vulnerability publicly before we've had a chance to address it
- While we don't currently offer bug bounties, we greatly appreciate all security reports
- Our response time may vary depending on the complexity of the issue and maintainer availability

We value the security of our project and appreciate your help in keeping it safe for all users.

## Operational requirements (pre-production → shared/prod)

Remediations from [security-audit SEC2–SEC15](reports/security-audit-1-11-07-2026-16-38-26.md) (excluding SEC1 / project visibility):

| Topic | Requirement |
|-------|-------------|
| **JWT keys (SEC2)** | Bundled `privateKey.pem` / `publicKey.pem` are **development and test only**. `%prod` startup fails if those paths remain. Set `SMALLRYE_JWT_SIGN_KEY_LOCATION` and `MP_JWT_VERIFY_PUBLICKEY_LOCATION` (or JWKS) to environment-specific material. **Never** copy repo PEMs to shared/staging/prod. |
| **Passwords (SEC3/9)** | Per-user salt (`v1$…` format); constant-time digest compare. Do not use `%dev` `password.default` outside local/test. |
| **Rich text (SEC4)** | Server sanitizes HTML on write; SPA still stores JWTs in `localStorage` (XSS → token theft residual — prioritize sanitizer + future httpOnly work). |
| **Auth rate limit (SEC8)** | App-layer limits on login/register/recovery/refresh (`auth.rate-limit.*`). Also rate-limit at the reverse proxy. |
| **Swagger (SEC11)** | Disabled for `%prod` by default; enable only for internal staging if needed. |
| **Mailer (SEC13)** | `%dev` Mailtrap credentials via `MAILTRAP_USERNAME` / `MAILTRAP_PASSWORD` env (default mock). Rotate any credentials that were previously committed. |
| **SEC1** | Ticket/project membership isolation tracked separately in `feature/project-visibility.md`. |

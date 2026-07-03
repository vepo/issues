# Email delivery

**Status:** done  
**Requested:** retrospective baseline (documented 2026-07-03)

## Summary

Transactional email via Quarkus Mailer and Qute templates: password reset links and ticket-change notifications to subscribers. Complements in-app notifications; no dedicated UI beyond flows that trigger email.

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `mailer`; triggered from `auth`, `ticket` events |
| Packages / files | `mailer.MailerService`; Qute templates under `src/main/resources/templates/mailer/` |
| API | No dedicated mailer endpoints; invoked from `POST /auth/recovery` and ticket change handlers |
| UI | Password reset request page triggers email; ticket changes trigger background send |
| Schema / seed | `tb_password_reset_tokens` for recovery links |
| Tests | Covered indirectly via `ResetPasswordEndpointTest`, `ConfirmPasswordResetEndpointTest`; mailer mocked in tests |
| Docs | domain-spec (Password recovery, Ticket change email), README § Notifications & email |

### Risks and open questions

- Dev mailer config in `application.properties` must not leak to production.
- Email deliverability, rate limits, and template localization not addressed.

## Changelog

### Initial implementation — baseline

**Status:** done

**Description:** `MailerService` sends password reset email with token link and `notifyTicketChange.html` for subscriber ticket updates.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Authentication | Recovery flow depends on email delivery |
| Notifications | Parallel channel to SSE for same ticket events |
| Ticket management | Ticket changes trigger subscriber emails |
| — | None identified |

**Implementation notes:** `MailerService.java`; Qute templates `resetPassword.html`, `notifyTicketChange.html`; `%dev` SMTP settings in `application.properties`.

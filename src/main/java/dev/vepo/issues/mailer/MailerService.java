package dev.vepo.issues.mailer;

import java.util.HashSet;
import java.util.Objects;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.notifications.NotificationEvent;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.user.PasswordResetToken;
import dev.vepo.issues.user.User;
import io.quarkus.mailer.MailTemplate.MailTemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

@ApplicationScoped
public class MailerService {

    private static final Logger logger = LoggerFactory.getLogger(MailerService.class);
    private final TicketRepository ticketRepository;
    private final String baseUrl;

    record resetPasswordEmail(String baseUrl, User user, PasswordResetToken token) implements MailTemplateInstance {}

    record notifyTicketChange(String baseUrl, NotificationEvent event, Ticket ticket) implements MailTemplateInstance {}

    @Inject
    public MailerService(TicketRepository ticketRepository,
                         @ConfigProperty(name = "baseUrl") String baseUrl) {
        this.ticketRepository = ticketRepository;
        this.baseUrl = baseUrl;
    }

    public void listenNotifications(@ObservesAsync NotificationEvent notification) {
        logger.info("Processing CDI Event! event={}", notification);
        this.ticketRepository.findById(notification.ticketId())
                             .ifPresent(ticket -> new notifyTicketChange(baseUrl,
                                                                         notification,
                                                                         ticket).to(allTicket(ticket))
                                                                                .subject("[%s] Updated!".formatted(ticket.getIdentifier()))
                                                                                .send()
                                                                                .subscribe()
                                                                                .with(success -> logger.info("Ticket notification email sent for {}",
                                                                                                             ticket.getIdentifier()),
                                                                                      failure -> logger.error("Failed to send ticket notification email for {}",
                                                                                                              ticket.getIdentifier(),
                                                                                                              failure)));
    }

    private String[] allTicket(Ticket ticket) {
        var allEnvolved = new HashSet<String>();
        allEnvolved.addAll(ticket.getSubscribers().stream().map(User::getEmail).toList());
        allEnvolved.add(ticket.getAuthor().getEmail());
        if (Objects.nonNull(ticket.getAssignee())) {
            allEnvolved.add(ticket.getAssignee().getEmail());
        }
        return allEnvolved.toArray(String[]::new);
    }

    public void sendResetPassword(User user, PasswordResetToken resetToken) {
        logger.info("Sending reset password: user={}", user);
        new resetPasswordEmail(baseUrl,
                               user,
                               resetToken).to(user.getEmail())
                                          .subject("[ATENÇÃO] Alterar senha!")
                                          .send()
                                          .subscribe()
                                          .with(success -> logger.info("Password reset email sent to {}", user.getEmail()),
                                                failure -> logger.error("Failed to send password reset email to {}", user.getEmail(),
                                                                        failure));
    }
}

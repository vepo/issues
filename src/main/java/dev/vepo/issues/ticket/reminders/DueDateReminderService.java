package dev.vepo.issues.ticket.reminders;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import dev.vepo.issues.notifications.NotificationService;
import dev.vepo.issues.ticket.TicketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DueDateReminderService {

    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    @Inject
    public DueDateReminderService(TicketRepository ticketRepository, NotificationService notificationService, Clock clock) {
        this.ticketRepository = ticketRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Transactional
    public void checkAndNotify() {
        var today = LocalDate.now(clock);
        notifyDueSoon(today);
        notifyOverdue(today);
    }

    private void notifyDueSoon(LocalDate today) {
        ticketRepository.findDueSoonReminderEligible(today.plusDays(1))
                        .forEach(ticket -> {
                            notificationService.notifyDueDateReminder(ticket,
                                                                      ticket.getAssignee(),
                                                                      "Ticket %s vence amanhã".formatted(ticket.getIdentifier()));
                            ticket.setDueSoonReminderSentAt(LocalDateTime.now(clock));
                        });
    }

    private void notifyOverdue(LocalDate today) {
        ticketRepository.findOverdueReminderEligible(today)
                        .forEach(ticket -> {
                            notificationService.notifyDueDateReminder(ticket,
                                                                      ticket.getAssignee(),
                                                                      "Ticket %s venceu".formatted(ticket.getIdentifier()));
                            ticket.setOverdueReminderSentAt(LocalDateTime.now(clock));
                        });
    }
}

package dev.vepo.issues.ticket.reminders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DueDateReminderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DueDateReminderScheduler.class);

    private final DueDateReminderService reminderService;

    @Inject
    public DueDateReminderScheduler(DueDateReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(cron = "{issues.due-date-reminder.cron}")
    public void run() {
        logger.info("Checking due-date reminders");
        reminderService.checkAndNotify();
    }
}

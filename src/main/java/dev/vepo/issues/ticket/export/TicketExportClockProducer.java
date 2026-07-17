package dev.vepo.issues.ticket.export;

import java.time.Clock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@ApplicationScoped
class TicketExportClockProducer {

    @Produces
    @Singleton
    Clock systemClock() {
        return Clock.systemUTC();
    }
}

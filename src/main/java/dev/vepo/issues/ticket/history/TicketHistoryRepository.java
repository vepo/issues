package dev.vepo.issues.ticket.history;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class TicketHistoryRepository {
    @PersistenceContext
    private EntityManager em;

    public TicketHistory save(TicketHistory history) {
        em.persist(history);
        return history;
    }
}
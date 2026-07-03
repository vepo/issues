package dev.vepo.issues.ticket.history;

import java.time.Instant;
import java.util.List;

import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

    public List<TicketHistory> findByTicketId(long ticketId) {
        return em.createQuery("FROM TicketHistory h WHERE h.ticket.id = :id ORDER BY h.timestamp DESC, h.id DESC",
                              TicketHistory.class)
                 .setParameter("id", ticketId)
                 .getResultList();
    }
}

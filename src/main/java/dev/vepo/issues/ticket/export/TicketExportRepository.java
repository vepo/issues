package dev.vepo.issues.ticket.export;

import java.util.List;
import java.util.Set;

import dev.vepo.issues.ticket.Ticket;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class TicketExportRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Ticket> search(SimpleTicketExportCriteria criteria,
                               Set<Long> readableProjectIds,
                               int limit,
                               boolean includeDeleted) {
        var query = em.createQuery("""
                                   SELECT DISTINCT t FROM Ticket t
                                   WHERE t.project.id IN :projectIds
                                     AND (:includeDeleted = true OR t.deleted = false)
                                     AND (:statusId IS NULL OR t.status.id = :statusId)
                                     AND (:term IS NULL
                                          OR LOWER(t.title) LIKE :term
                                          OR LOWER(t.description) LIKE :term
                                          OR LOWER(t.identifier) LIKE :term)
                                   """, Ticket.class);
        var term = criteria.term() == null || criteria.term().isBlank()
                                                                        ? null
                                                                        : "%%%s%%".formatted(criteria.term().trim().toLowerCase());
        return query.setParameter("projectIds", readableProjectIds)
                    .setParameter("includeDeleted", includeDeleted)
                    .setParameter("statusId", criteria.statusId())
                    .setParameter("term", term)
                    .setMaxResults(limit)
                    .getResultList();
    }
}

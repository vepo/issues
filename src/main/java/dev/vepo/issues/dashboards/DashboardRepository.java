package dev.vepo.issues.dashboards;

import java.util.List;

import dev.vepo.issues.ticket.Ticket;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

@ApplicationScoped
public class DashboardRepository {

    public static final int RECENT_TICKETS_LIMIT = 20;

    @PersistenceContext
    private EntityManager em;

    public List<Tuple> countTicketsByStatus(long projectId) {
        return em.createQuery("""
                              SELECT t.status.name AS label, COUNT(t) AS total
                              FROM Ticket t
                              WHERE t.project.id = :projectId AND t.deleted = false
                              GROUP BY t.status.name
                              ORDER BY t.status.name
                              """, Tuple.class)
                 .setParameter("projectId", projectId)
                 .getResultList();
    }

    public List<Tuple> countTicketsByPriority(long projectId) {
        return em.createQuery("""
                              SELECT t.priority AS label, COUNT(t) AS total
                              FROM Ticket t
                              WHERE t.project.id = :projectId AND t.deleted = false
                              GROUP BY t.priority
                              ORDER BY t.priority
                              """, Tuple.class)
                 .setParameter("projectId", projectId)
                 .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> countTicketsByCreatedDay(long projectId) {
        return em.createNativeQuery("""
                                    SELECT TO_CHAR(created_at AT TIME ZONE 'UTC', 'DD/MM/YYYY') AS label,
                                           COUNT(*) AS total
                                    FROM tb_tickets
                                    WHERE project_id = :projectId AND deleted = false
                                    GROUP BY label
                                    ORDER BY MIN(created_at)
                                    """)
                 .setParameter("projectId", projectId)
                 .getResultList();
    }

    public List<Ticket> findRecentTickets(long projectId) {
        return em.createQuery("""
                              FROM Ticket t
                              WHERE t.project.id = :projectId AND t.deleted = false
                              ORDER BY t.updatedAt DESC
                              """, Ticket.class)
                 .setParameter("projectId", projectId)
                 .setMaxResults(RECENT_TICKETS_LIMIT)
                 .getResultList();
    }

    public long countTickets(long projectId) {
        return em.createQuery("""
                              SELECT COUNT(t)
                              FROM Ticket t
                              WHERE t.project.id = :projectId AND t.deleted = false
                              """, Long.class)
                 .setParameter("projectId", projectId)
                 .getSingleResult();
    }
}

package dev.vepo.issues.ticket.csvimport;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TicketImportRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<TicketImport> findById(long id) {
        return em.createQuery("FROM TicketImport WHERE id = :id", TicketImport.class)
                 .setParameter("id", id)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<TicketImport> findByIdAndProjectIdWithRows(long id, long projectId) {
        return em.createQuery("""
                              SELECT DISTINCT i FROM TicketImport i
                              LEFT JOIN FETCH i.rows
                              WHERE i.id = :id AND i.project.id = :projectId
                              """, TicketImport.class)
                 .setParameter("id", id)
                 .setParameter("projectId", projectId)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<TicketImport> findGlobalById(long id) {
        return em.createQuery("FROM TicketImport WHERE id = :id AND project IS NULL", TicketImport.class)
                 .setParameter("id", id)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<TicketImport> findGlobalByIdAndAuthorId(long id, long authorId) {
        return em.createQuery("""
                              FROM TicketImport
                              WHERE id = :id AND project IS NULL AND author.id = :authorId
                              """, TicketImport.class)
                 .setParameter("id", id)
                 .setParameter("authorId", authorId)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<TicketImport> findByIdAndProjectId(long id, long projectId) {
        return em.createQuery("FROM TicketImport WHERE id = :id AND project.id = :projectId", TicketImport.class)
                 .setParameter("id", id)
                 .setParameter("projectId", projectId)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<TicketImport> findByIdAndProjectIdAndAuthorId(long id, long projectId, long authorId) {
        return em.createQuery("""
                              FROM TicketImport
                              WHERE id = :id AND project.id = :projectId AND author.id = :authorId
                              """, TicketImport.class)
                 .setParameter("id", id)
                 .setParameter("projectId", projectId)
                 .setParameter("authorId", authorId)
                 .getResultStream()
                 .findFirst();
    }

    @Transactional
    public TicketImport save(TicketImport ticketImport) {
        em.persist(ticketImport);
        return ticketImport;
    }

    @Transactional
    public TicketImport merge(TicketImport ticketImport) {
        return em.merge(ticketImport);
    }
}

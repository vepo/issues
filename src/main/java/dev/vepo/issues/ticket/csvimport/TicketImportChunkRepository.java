package dev.vepo.issues.ticket.csvimport;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TicketImportChunkRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<TicketImportChunk> findByImportIdAndPartIndex(long importId, int partIndex) {
        return Optional.ofNullable(em.find(TicketImportChunk.class, new TicketImportChunkId(importId, partIndex)));
    }

    public List<TicketImportChunk> findByImportIdOrderByPartIndex(long importId) {
        return em.createQuery("""
                              FROM TicketImportChunk
                              WHERE importId = :importId
                              ORDER BY partIndex
                              """, TicketImportChunk.class)
                 .setParameter("importId", importId)
                 .getResultList();
    }

    public long countByImportId(long importId) {
        return em.createQuery("SELECT COUNT(c) FROM TicketImportChunk c WHERE c.importId = :importId", Long.class)
                 .setParameter("importId", importId)
                 .getSingleResult();
    }

    @Transactional
    public TicketImportChunk save(TicketImportChunk chunk) {
        em.persist(chunk);
        return chunk;
    }

    @Transactional
    public void deleteByImportId(long importId) {
        em.createQuery("DELETE FROM TicketImportChunk WHERE importId = :importId")
          .setParameter("importId", importId)
          .executeUpdate();
    }
}

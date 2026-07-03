package dev.vepo.issues.phase;

import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PhaseRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<Phase> findByIdAndProjectId(long phaseId, long projectId) {
        return em.createQuery("""
                              FROM Phase p
                              LEFT JOIN FETCH p.deliverables
                              WHERE p.id = :phaseId AND p.project.id = :projectId
                              """, Phase.class)
                 .setParameter("phaseId", phaseId)
                 .setParameter("projectId", projectId)
                 .getResultStream()
                 .findFirst();
    }

    public Stream<Phase> findByProjectId(long projectId) {
        return em.createQuery("""
                              FROM Phase p
                              LEFT JOIN FETCH p.deliverables
                              WHERE p.project.id = :projectId
                              ORDER BY p.createdAt DESC
                              """, Phase.class)
                 .setParameter("projectId", projectId)
                 .getResultStream();
    }

    public Optional<Phase> findActiveByProjectId(long projectId) {
        return em.createQuery("""
                              FROM Phase p
                              LEFT JOIN FETCH p.deliverables
                              WHERE p.project.id = :projectId AND p.status = :status
                              """, Phase.class)
                 .setParameter("projectId", projectId)
                 .setParameter("status", PhaseStatus.ACTIVE)
                 .getResultStream()
                 .findFirst();
    }

    @Transactional
    public Phase save(Phase phase) {
        em.persist(phase);
        return phase;
    }
}

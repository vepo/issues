package dev.vepo.issues.phase;

import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class VersionRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<Version> findByIdAndProjectId(long versionId, long projectId) {
        return em.createQuery("""
                              FROM Version v
                              WHERE v.id = :versionId AND v.project.id = :projectId
                              """, Version.class)
                 .setParameter("versionId", versionId)
                 .setParameter("projectId", projectId)
                 .getResultStream()
                 .findFirst();
    }

    public Stream<Version> findByProjectId(long projectId) {
        return em.createQuery("""
                              FROM Version v
                              WHERE v.project.id = :projectId
                              ORDER BY v.label
                              """, Version.class)
                 .setParameter("projectId", projectId)
                 .getResultStream();
    }

    public boolean existsByProjectIdAndLabel(long projectId, String label) {
        return em.createQuery("""
                              SELECT COUNT(v) FROM Version v
                              WHERE v.project.id = :projectId AND v.label = :label
                              """, Long.class)
                 .setParameter("projectId", projectId)
                 .setParameter("label", label)
                 .getSingleResult() > 0;
    }

    public boolean existsByProjectIdAndLabelExcludingId(long projectId, String label, long versionId) {
        return em.createQuery("""
                              SELECT COUNT(v) FROM Version v
                              WHERE v.project.id = :projectId AND v.label = :label AND v.id <> :versionId
                              """, Long.class)
                 .setParameter("projectId", projectId)
                 .setParameter("label", label)
                 .setParameter("versionId", versionId)
                 .getSingleResult() > 0;
    }

    @Transactional
    public Version save(Version version) {
        em.persist(version);
        return version;
    }
}

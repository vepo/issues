package dev.vepo.issues.dashboards;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class DashboardLayoutRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<DashboardLayout> findByUserIdAndProjectId(long userId, long projectId) {
        return em.createQuery("""
                              FROM DashboardLayout
                              WHERE user.id = :userId AND project.id = :projectId
                              """, DashboardLayout.class)
                 .setParameter("userId", userId)
                 .setParameter("projectId", projectId)
                 .getResultStream()
                 .findFirst();
    }

    public DashboardLayout save(DashboardLayout layout) {
        if (layout.getId() == null) {
            em.persist(layout);
            return layout;
        }
        return em.merge(layout);
    }
}

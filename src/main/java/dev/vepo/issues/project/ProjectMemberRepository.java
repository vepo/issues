package dev.vepo.issues.project;

import java.util.List;
import java.util.stream.Stream;

import dev.vepo.issues.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ProjectMemberRepository {

    @PersistenceContext
    private EntityManager em;

    public boolean isMember(long projectId, long userId) {
        return (long) em.createQuery("""
                                     SELECT COUNT(m) FROM ProjectMember m
                                     WHERE m.project.id = :projectId AND m.user.id = :userId
                                     """, Long.class)
                        .setParameter("projectId", projectId)
                        .setParameter("userId", userId)
                        .getSingleResult() > 0;
    }

    public Stream<Long> findProjectIdsForMember(long userId) {
        return em.createQuery("""
                              SELECT m.project.id FROM ProjectMember m
                              WHERE m.user.id = :userId
                              ORDER BY m.project.name
                              """, Long.class)
                 .setParameter("userId", userId)
                 .getResultStream();
    }

    public List<User> findMembersByProjectId(long projectId) {
        return em.createQuery("""
                              SELECT m.user FROM ProjectMember m
                              WHERE m.project.id = :projectId
                              ORDER BY m.user.name
                              """, User.class)
                 .setParameter("projectId", projectId)
                 .getResultList();
    }

    @Transactional
    public void addMember(Project project, User user) {
        em.persist(new ProjectMember(project, user));
    }

    @Transactional
    public boolean removeMember(long projectId, long userId) {
        return em.createQuery("""
                              DELETE FROM ProjectMember m
                              WHERE m.project.id = :projectId AND m.user.id = :userId
                              """)
                 .setParameter("projectId", projectId)
                 .setParameter("userId", userId)
                 .executeUpdate() > 0;
    }
}

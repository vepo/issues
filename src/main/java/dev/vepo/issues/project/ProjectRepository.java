package dev.vepo.issues.project;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class ProjectRepository {
    @PersistenceContext
    private EntityManager em;

    public Optional<Project> findById(long id) {
        return em.createQuery("FROM Project where id = :id", Project.class)
                 .setParameter("id", id)
                 .getResultStream()
                 .findFirst();
    }

    public Stream<Project> findAll() {
        return em.createQuery("FROM Project ORDER BY name", Project.class)
                 .getResultStream();
    }

    public List<Long> findAllIds() {
        return em.createQuery("SELECT p.id FROM Project p", Long.class)
                 .getResultList();
    }

    public Stream<Long> findOwnedProjectIds(long userId) {
        return em.createQuery("SELECT p.id FROM Project p WHERE p.owner.id = :userId", Long.class)
                 .setParameter("userId", userId)
                 .getResultStream();
    }

    public Stream<Project> findOwnedByUserId(long userId) {
        return em.createQuery("FROM Project p WHERE p.owner.id = :userId ORDER BY p.name", Project.class)
                 .setParameter("userId", userId)
                 .getResultStream();
    }

    public Stream<Project> findByMemberUserId(long userId) {
        return em.createQuery("""
                              SELECT p FROM Project p
                              JOIN ProjectMember m ON m.project = p
                              WHERE m.user.id = :userId
                              ORDER BY p.name
                              """, Project.class)
                 .setParameter("userId", userId)
                 .getResultStream();
    }

    public Project save(Project project) {
        em.persist(project);
        return project;
    }

    public Optional<Project> findByName(String name) {
        return em.createQuery("FROM Project where name = :name", Project.class)
                 .setParameter("name", name)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<Project> findByNameIgnoreCase(String name) {
        return em.createQuery("FROM Project WHERE lower(name) = lower(:name)", Project.class)
                 .setParameter("name", name)
                 .getResultStream()
                 .findFirst();
    }
}

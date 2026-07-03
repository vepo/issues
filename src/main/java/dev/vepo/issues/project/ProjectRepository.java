package dev.vepo.issues.project;

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
        return em.createQuery("FROM Project", Project.class)
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

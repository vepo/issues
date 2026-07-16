package dev.vepo.issues.git;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ProjectGitRepositoryRepository {

    private final EntityManager em;

    @Inject
    public ProjectGitRepositoryRepository(EntityManager em) {
        this.em = em;
    }

    public Optional<ProjectGitRepository> findByProjectId(long projectId) {
        return em.createQuery("FROM ProjectGitRepository g WHERE g.project.id = :projectId", ProjectGitRepository.class)
                 .setParameter("projectId", projectId)
                 .getResultStream()
                 .findFirst();
    }

    @Transactional
    public ProjectGitRepository save(ProjectGitRepository repository) {
        if (repository.getId() == null) {
            em.persist(repository);
            return repository;
        }
        return em.merge(repository);
    }
}

package dev.vepo.issues.workflow;

import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class WorkflowRepository {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowRepository.class);

    @PersistenceContext
    private EntityManager em;

    public Optional<Workflow> findById(long id) {
        return em.createQuery("FROM Workflow WHERE id = :id", Workflow.class)
                 .setParameter("id", id)
                 .getResultStream()
                 .findFirst();
    }

    public Stream<Workflow> findAll() {
        return em.createQuery("FROM Workflow", Workflow.class)
                 .getResultStream();
    }

    public Stream<WorkflowStatus> findAllStatus() {
        return em.createQuery("FROM WorkflowStatus", WorkflowStatus.class)
                 .getResultStream();
    }

    public Optional<WorkflowStatus> findStatusByName(String name) {
        return em.createQuery("FROM WorkflowStatus WHERE name = :name", WorkflowStatus.class)
                 .setParameter("name", name)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<Workflow> findByName(String name) {
        return em.createQuery("FROM Workflow WHERE name = :name", Workflow.class)
                 .setParameter("name", name)
                 .getResultStream()
                 .findFirst();
    }

    public WorkflowStatus save(WorkflowStatus status) {
        em.persist(status);
        return status;
    }

    public Workflow save(Workflow workflow) {
        logger.info("Saving workflow! workflow={}", workflow);
        em.persist(workflow);
        return workflow;
    }

    public Optional<FinishOutcome> findFinishOutcome(long workflowId, long statusId) {
        return em.createQuery("""
                              SELECT fs.outcome FROM WorkflowFinishStatus fs
                              WHERE fs.id.workflowId = :workflowId AND fs.id.statusId = :statusId
                              """, FinishOutcome.class)
                 .setParameter("workflowId", workflowId)
                 .setParameter("statusId", statusId)
                 .getResultStream()
                 .findFirst();
    }
}

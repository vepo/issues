package dev.vepo.issues.customfield;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CustomFieldRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<CustomField> findById(long id) {
        return em.createQuery("""
                              FROM CustomField f
                              LEFT JOIN FETCH f.enumOptions
                              LEFT JOIN FETCH f.statusRequired
                              WHERE f.id = :id
                              """, CustomField.class)
                 .setParameter("id", id)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<CustomField> findByIdAndProjectId(long id, long projectId) {
        return em.createQuery("""
                              FROM CustomField f
                              LEFT JOIN FETCH f.enumOptions
                              WHERE f.id = :id AND f.projectId = :projectId
                              """, CustomField.class)
                 .setParameter("id", id)
                 .setParameter("projectId", projectId)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<CustomField> findByIdAndWorkflowId(long id, long workflowId) {
        return em.createQuery("""
                              FROM CustomField f
                              LEFT JOIN FETCH f.enumOptions
                              LEFT JOIN FETCH f.statusRequired
                              WHERE f.id = :id AND f.workflowId = :workflowId
                              """, CustomField.class)
                 .setParameter("id", id)
                 .setParameter("workflowId", workflowId)
                 .getResultStream()
                 .findFirst();
    }

    public List<CustomField> listByProjectId(long projectId) {
        return em.createQuery("""
                              SELECT DISTINCT f FROM CustomField f
                              LEFT JOIN FETCH f.enumOptions
                              WHERE f.projectId = :projectId
                              ORDER BY f.sortOrder, f.id
                              """, CustomField.class)
                 .setParameter("projectId", projectId)
                 .getResultList();
    }

    public List<CustomField> listByWorkflowId(long workflowId) {
        return em.createQuery("""
                              SELECT DISTINCT f FROM CustomField f
                              LEFT JOIN FETCH f.enumOptions
                              LEFT JOIN FETCH f.statusRequired
                              WHERE f.workflowId = :workflowId
                              ORDER BY f.sortOrder, f.id
                              """, CustomField.class)
                 .setParameter("workflowId", workflowId)
                 .getResultList();
    }

    public List<CustomField> listEnabledInScope(long projectId, long workflowId) {
        return em.createQuery("""
                              SELECT DISTINCT f FROM CustomField f
                              LEFT JOIN FETCH f.enumOptions
                              LEFT JOIN FETCH f.statusRequired
                              WHERE f.enabled = TRUE
                                AND (f.projectId = :projectId OR f.workflowId = :workflowId)
                              ORDER BY f.sortOrder, f.id
                              """, CustomField.class)
                 .setParameter("projectId", projectId)
                 .setParameter("workflowId", workflowId)
                 .getResultList();
    }

    public boolean existsKeyOnProject(long projectId, String key) {
        return em.createQuery("""
                              SELECT COUNT(f) FROM CustomField f
                              WHERE f.projectId = :projectId AND f.key = :key
                              """, Long.class)
                 .setParameter("projectId", projectId)
                 .setParameter("key", key)
                 .getSingleResult() > 0;
    }

    public boolean existsKeyOnWorkflow(long workflowId, String key) {
        return em.createQuery("""
                              SELECT COUNT(f) FROM CustomField f
                              WHERE f.workflowId = :workflowId AND f.key = :key
                              """, Long.class)
                 .setParameter("workflowId", workflowId)
                 .setParameter("key", key)
                 .getSingleResult() > 0;
    }

    public boolean existsKeyCollisionForProject(long projectId, long workflowId, String key) {
        return em.createQuery("""
                              SELECT COUNT(f) FROM CustomField f
                              WHERE f.key = :key
                                AND (f.projectId = :projectId OR f.workflowId = :workflowId)
                              """, Long.class)
                 .setParameter("key", key)
                 .setParameter("projectId", projectId)
                 .setParameter("workflowId", workflowId)
                 .getSingleResult() > 0;
    }

    public Stream<String> findCollidingKeys(long projectId, long workflowId) {
        return em.createQuery("""
                              SELECT f.key FROM CustomField f
                              WHERE f.projectId = :projectId
                                AND f.key IN (
                                  SELECT wf.key FROM CustomField wf WHERE wf.workflowId = :workflowId
                                )
                              """, String.class)
                 .setParameter("projectId", projectId)
                 .setParameter("workflowId", workflowId)
                 .getResultStream();
    }

    public boolean existsKeyOnProjectsUsingWorkflow(long workflowId, String key) {
        return em.createQuery("""
                              SELECT COUNT(f) FROM CustomField f
                              WHERE f.key = :key
                                AND f.projectId IN (
                                  SELECT p.id FROM Project p WHERE p.workflow.id = :workflowId
                                )
                              """, Long.class)
                 .setParameter("key", key)
                 .setParameter("workflowId", workflowId)
                 .getSingleResult() > 0;
    }

    @Transactional
    public CustomField save(CustomField field) {
        if (field.getId() == null) {
            em.persist(field);
            return field;
        }
        return em.merge(field);
    }

    @Transactional
    public void delete(CustomField field) {
        em.remove(em.contains(field) ? field : em.merge(field));
    }

    public long countValuesByFieldId(long fieldId) {
        return em.createQuery("""
                              SELECT COUNT(v) FROM TicketCustomFieldValue v
                              WHERE v.customField.id = :fieldId
                              """, Long.class)
                 .setParameter("fieldId", fieldId)
                 .getSingleResult();
    }

    public long countValuesByEnumOptionId(long optionId) {
        return em.createQuery("""
                              SELECT COUNT(v) FROM TicketCustomFieldValue v
                              WHERE v.enumOption.id = :optionId
                              """, Long.class)
                 .setParameter("optionId", optionId)
                 .getSingleResult();
    }

    public Optional<CustomFieldEnumOption> findEnumOptionById(long optionId) {
        return Optional.ofNullable(em.find(CustomFieldEnumOption.class, optionId));
    }
}

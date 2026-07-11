package dev.vepo.issues.customfield;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CustomFieldValueRepository {

    @PersistenceContext
    private EntityManager em;

    public List<TicketCustomFieldValue> listByTicketId(long ticketId) {
        return em.createQuery("""
                              FROM TicketCustomFieldValue v
                              JOIN FETCH v.customField f
                              LEFT JOIN FETCH f.enumOptions
                              LEFT JOIN FETCH v.enumOption
                              WHERE v.id.ticketId = :ticketId
                              """, TicketCustomFieldValue.class)
                 .setParameter("ticketId", ticketId)
                 .getResultList();
    }

    public Optional<TicketCustomFieldValue> find(long ticketId, long fieldId) {
        return Optional.ofNullable(em.find(TicketCustomFieldValue.class,
                                           new TicketCustomFieldValueId(ticketId, fieldId)));
    }

    @Transactional
    public TicketCustomFieldValue save(TicketCustomFieldValue value) {
        var existing = em.find(TicketCustomFieldValue.class, value.getId());
        if (existing == null) {
            em.persist(value);
            return value;
        }
        return em.merge(value);
    }

    @Transactional
    public void delete(TicketCustomFieldValue value) {
        em.remove(em.contains(value) ? value : em.merge(value));
    }

    public List<ProjectTicketTemplateCustomValue> listTemplateValuesByProjectId(long projectId) {
        return em.createQuery("""
                              FROM ProjectTicketTemplateCustomValue v
                              JOIN FETCH v.customField f
                              LEFT JOIN FETCH v.enumOption
                              WHERE v.id.projectId = :projectId
                              """, ProjectTicketTemplateCustomValue.class)
                 .setParameter("projectId", projectId)
                 .getResultList();
    }

    @Transactional
    public ProjectTicketTemplateCustomValue saveTemplateValue(ProjectTicketTemplateCustomValue value) {
        var existing = em.find(ProjectTicketTemplateCustomValue.class, value.getId());
        if (existing == null) {
            em.persist(value);
            return value;
        }
        return em.merge(value);
    }

    @Transactional
    public void deleteTemplateValue(ProjectTicketTemplateCustomValue value) {
        em.remove(em.contains(value) ? value : em.merge(value));
    }

    @Transactional
    public void deleteTemplateValuesNotInScope(long projectId, List<Long> inScopeFieldIds) {
        if (inScopeFieldIds.isEmpty()) {
            em.createQuery("""
                           DELETE FROM ProjectTicketTemplateCustomValue v
                           WHERE v.id.projectId = :projectId
                           """)
              .setParameter("projectId", projectId)
              .executeUpdate();
            return;
        }
        em.createQuery("""
                       DELETE FROM ProjectTicketTemplateCustomValue v
                       WHERE v.id.projectId = :projectId
                         AND v.id.customFieldId NOT IN :fieldIds
                       """)
          .setParameter("projectId", projectId)
          .setParameter("fieldIds", inScopeFieldIds)
          .executeUpdate();
    }
}

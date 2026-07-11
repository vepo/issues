package dev.vepo.issues.ticket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.phase.ChangelogAssociation;
import dev.vepo.issues.ticket.comments.Comment;
import dev.vepo.issues.ticket.history.TicketHistory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;

@ApplicationScoped
public class TicketRepository {
    private static final Logger logger = LoggerFactory.getLogger(TicketRepository.class);

    @PersistenceContext
    private EntityManager em;

    public Stream<Ticket> search(String[] terms, long statusId) {
        Objects.requireNonNull(terms, "terms cannot be null!");

        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(Ticket.class);
        var ticket = cq.from(Ticket.class);

        var predicates = new ArrayList<Predicate>();

        predicates.add(cb.isFalse(ticket.get("deleted")));

        if (terms.length > 0) {
            var termPredicates = new ArrayList<Predicate>();
            for (String term : terms) {
                var pattern = "%%%s%%".formatted(term).toLowerCase();
                termPredicates.add(cb.or(cb.like(cb.lower(ticket.get("title")), pattern),
                                         cb.like(cb.lower(ticket.get("description")), pattern)));
            }
            predicates.add(cb.and(termPredicates.toArray(Predicate[]::new)));
        }

        if (statusId != -1) {
            predicates.add(cb.equal(ticket.get("status")
                                          .get("id"),
                                    statusId));
        }

        cq.where(cb.and(predicates.toArray(Predicate[]::new)));

        logger.atInfo()
              .setMessage("Searching with criteria query")
              // This is a simplified logging of the query - in reality CriteriaQuery doesn't
              // expose its structure easily
              .addArgument(() -> "Terms: " + Arrays.toString(terms) + ", statusId: " + (statusId != -1 ? statusId : "any"))
              .log();

        return em.createQuery(cq)
                 .getResultStream();
    }

    public Stream<Ticket> findByStatusId(long statusId) {
        return em.createQuery("FROM Ticket WHERE deleted = false AND status.id = :id", Ticket.class)
                 .setParameter("id", statusId)
                 .getResultStream();
    }

    public Stream<Ticket> findByWorkflowIdAndStatusIdIncludingDeleted(long workflowId, long statusId) {
        return em.createQuery("""
                              FROM Ticket t
                              WHERE t.status.id = :statusId
                                AND t.project.workflow.id = :workflowId
                              """, Ticket.class)
                 .setParameter("statusId", statusId)
                 .setParameter("workflowId", workflowId)
                 .getResultStream();
    }

    public long countByWorkflowIdAndStatusIdIncludingDeleted(long workflowId, long statusId) {
        return em.createQuery("""
                              SELECT COUNT(t) FROM Ticket t
                              WHERE t.status.id = :statusId
                                AND t.project.workflow.id = :workflowId
                              """, Long.class)
                 .setParameter("statusId", statusId)
                 .setParameter("workflowId", workflowId)
                 .getSingleResult();
    }

    public Optional<Ticket> findById(long id) {
        return em.createQuery("FROM Ticket WHERE deleted = false AND id = :id", Ticket.class)
                 .setParameter("id", id)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<Ticket> findByIdIncludingDeleted(long id) {
        return em.createQuery("FROM Ticket WHERE id = :id", Ticket.class)
                 .setParameter("id", id)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<Ticket> findByIdentifier(String id) {
        return em.createQuery("FROM Ticket WHERE deleted = false AND identifier = :identifier", Ticket.class)
                 .setParameter("identifier", id)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<Ticket> findByIdentifierIncludingDeleted(String identifier) {
        return em.createQuery("FROM Ticket WHERE identifier = :identifier", Ticket.class)
                 .setParameter("identifier", identifier)
                 .getResultStream()
                 .findFirst();
    }

    public Stream<Ticket> findByStatusName(String status) {
        return em.createQuery("FROM Ticket WHERE deleted = false AND status.name = :name", Ticket.class)
                 .setParameter("name", status)
                 .getResultStream();
    }

    public Stream<Ticket> findAll() {
        return em.createQuery("FROM Ticket WHERE deleted = false", Ticket.class)
                 .getResultStream();
    }

    public Ticket save(Ticket ticket) {
        em.persist(ticket);
        return ticket;
    }

    public void delete(long id) {
        int deletedItems = em.createQuery("UPDATE Ticket SET deleted = true WHERE id = :id")
                             .setParameter("id", id)
                             .executeUpdate();
        logger.warn("Deleted tickets! count={}", deletedItems);
    }

    public void restore(long id) {
        int restoredItems = em.createQuery("UPDATE Ticket SET deleted = false WHERE id = :id AND deleted = true")
                              .setParameter("id", id)
                              .executeUpdate();
        logger.info("Restored tickets! count={}", restoredItems);
    }

    public Stream<Ticket> findByProjectId(long id) {
        return em.createQuery("FROM Ticket WHERE deleted = false AND project.id = :id", Ticket.class)
                 .setParameter("id", id)
                 .getResultStream();
    }

    public Stream<TicketHistory> findHistoryByTicketId(Long id) {
        return em.createQuery("FROM TicketHistory h WHERE h.ticket.id = :id ORDER BY h.timestamp DESC, h.id DESC",
                              TicketHistory.class)
                 .setParameter("id", id)
                 .getResultStream();
    }

    public Stream<Comment> findCommentsByTicketId(Long id) {
        return em.createQuery("FROM Comment where ticket.id = :id", Comment.class)
                 .setParameter("id", id)
                 .getResultStream();
    }

    public int countProjectTickets(long projectId) {
        return (int) em.createQuery("SELECT id FROM Ticket WHERE project.id = :id", Long.class)
                       .setParameter("id", projectId)
                       .getResultStream()
                       .count();
    }

    public Comment saveComment(Comment comment) {
        em.persist(comment);
        return comment;
    }

    public Stream<Ticket> findByPhaseId(long phaseId) {
        return em.createQuery("""
                              FROM Ticket t
                              WHERE t.deleted = false AND t.phase.id = :phaseId
                              """, Ticket.class)
                 .setParameter("phaseId", phaseId)
                 .getResultStream();
    }

    public Stream<Ticket> findOpenTicketsInProjects(Collection<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Stream.empty();
        }
        return em.createQuery("""
                              SELECT t FROM Ticket t
                              WHERE t.deleted = false
                              AND t.project.id IN :projectIds
                              AND NOT EXISTS (
                                  SELECT 1 FROM WorkflowFinishStatus fs
                                  WHERE fs.id.workflowId = t.project.workflow.id
                                  AND fs.id.statusId = t.status.id
                              )
                              ORDER BY t.updatedAt DESC
                              """, Ticket.class)
                 .setParameter("projectIds", projectIds)
                 .getResultStream();
    }

    public Stream<Ticket> findOpenAssignedTicketsInProjects(long assigneeId, Collection<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Stream.empty();
        }
        return em.createQuery("""
                              SELECT t FROM Ticket t
                              WHERE t.deleted = false
                              AND t.assignee.id = :assigneeId
                              AND t.project.id IN :projectIds
                              AND NOT EXISTS (
                                  SELECT 1 FROM WorkflowFinishStatus fs
                                  WHERE fs.id.workflowId = t.project.workflow.id
                                  AND fs.id.statusId = t.status.id
                              )
                              ORDER BY t.updatedAt DESC
                              """, Ticket.class)
                 .setParameter("assigneeId", assigneeId)
                 .setParameter("projectIds", projectIds)
                 .getResultStream();
    }

    public long countByProjectIdAndStatusId(long projectId, long statusId) {
        return em.createQuery("""
                              SELECT COUNT(t) FROM Ticket t
                              WHERE t.deleted = false
                              AND t.project.id = :projectId
                              AND t.status.id = :statusId
                              """, Long.class)
                 .setParameter("projectId", projectId)
                 .setParameter("statusId", statusId)
                 .getSingleResult();
    }

    public long countOpenAssignedTickets(long projectId, long userId) {
        return em.createQuery("""
                              SELECT COUNT(t) FROM Ticket t
                              WHERE t.deleted = false
                              AND t.project.id = :projectId
                              AND t.assignee.id = :userId
                              AND NOT EXISTS (
                                  SELECT 1 FROM WorkflowFinishStatus fs
                                  WHERE fs.id.workflowId = t.project.workflow.id
                                  AND fs.id.statusId = t.status.id
                              )
                              """, Long.class)
                 .setParameter("projectId", projectId)
                 .setParameter("userId", userId)
                 .getSingleResult();
    }

    public Stream<Ticket> findOpenAssignedTickets(long projectId, long userId) {
        return findOpenAssignedTicketsInProjects(userId, Collections.singleton(projectId));
    }

    public Stream<TicketHistory> findRecentStatusChangesInProjects(Collection<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Stream.empty();
        }
        return em.createQuery("""
                              SELECT h FROM TicketHistory h
                              WHERE h.action = dev.vepo.issues.ticket.history.TicketHistoryAction.STATUS_CHANGED
                              AND h.ticket.deleted = false
                              AND h.ticket.project.id IN :projectIds
                              ORDER BY h.timestamp DESC, h.id DESC
                              """, TicketHistory.class)
                 .setParameter("projectIds", projectIds)
                 .getResultStream();
    }

    public Stream<Comment> findRecentCommentsInProjects(Collection<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Stream.empty();
        }
        return em.createQuery("""
                              SELECT c FROM Comment c
                              WHERE c.ticket.deleted = false
                              AND c.ticket.project.id IN :projectIds
                              ORDER BY c.createdAt DESC, c.id DESC
                              """, Comment.class)
                 .setParameter("projectIds", projectIds)
                 .getResultStream();
    }

    public List<Ticket> findBacklogPage(long projectId, int page, int size) {
        return em.createQuery("""
                              SELECT t FROM Ticket t
                              WHERE t.project.id = :projectId
                              AND t.deleted = false
                              AND t.finishedAt IS NULL
                              ORDER BY t.backlogRank ASC, t.id ASC
                              """, Ticket.class)
                 .setParameter("projectId", projectId)
                 .setFirstResult(page * size)
                 .setMaxResults(size)
                 .getResultList();
    }

    public long countBacklog(long projectId) {
        return em.createQuery("""
                              SELECT COUNT(t) FROM Ticket t
                              WHERE t.project.id = :projectId
                              AND t.deleted = false
                              AND t.finishedAt IS NULL
                              """, Long.class)
                 .setParameter("projectId", projectId)
                 .getSingleResult();
    }

    public int maxBacklogRank(long projectId) {
        var max = em.createQuery("""
                                 SELECT MAX(t.backlogRank) FROM Ticket t
                                 WHERE t.project.id = :projectId
                                 """, Integer.class)
                    .setParameter("projectId", projectId)
                    .getSingleResult();
        return max == null ? 0 : max;
    }

    public List<Ticket> findBacklogEligibleOrdered(long projectId) {
        return em.createQuery("""
                              SELECT t FROM Ticket t
                              WHERE t.project.id = :projectId
                              AND t.deleted = false
                              AND t.finishedAt IS NULL
                              ORDER BY t.backlogRank ASC, t.id ASC
                              """, Ticket.class)
                 .setParameter("projectId", projectId)
                 .getResultList();
    }

    public Optional<Ticket> findBacklogEligibleById(long projectId, long ticketId) {
        return em.createQuery("""
                              SELECT t FROM Ticket t
                              WHERE t.id = :ticketId
                              AND t.project.id = :projectId
                              AND t.deleted = false
                              AND t.finishedAt IS NULL
                              """, Ticket.class)
                 .setParameter("ticketId", ticketId)
                 .setParameter("projectId", projectId)
                 .getResultStream()
                 .findFirst();
    }

    public Stream<Ticket> findForVersionChangelog(long projectId, long versionId, ChangelogAssociation association) {
        if (association == ChangelogAssociation.PHASE_DELIVERABLE) {
            return em.createQuery("""
                                  SELECT t FROM Ticket t
                                  JOIN t.phase p
                                  WHERE t.deleted = false
                                  AND t.project.id = :projectId
                                  AND p.deliverableVersion.id = :versionId
                                  AND NOT EXISTS (
                                      SELECT 1 FROM WorkflowFinishStatus fs
                                      WHERE fs.id.workflowId = t.project.workflow.id
                                      AND fs.id.statusId = t.status.id
                                      AND fs.outcome = dev.vepo.issues.workflow.FinishOutcome.CANCELED
                                  )
                                  """, Ticket.class)
                     .setParameter("projectId", projectId)
                     .setParameter("versionId", versionId)
                     .getResultStream();
        }
        var versionPredicate = association == ChangelogAssociation.TARGET
                                                                          ? "t.targetVersion.id = :versionId"
                                                                          : "t.observedVersion.id = :versionId";
        return em.createQuery("""
                              SELECT t FROM Ticket t
                              WHERE t.deleted = false
                              AND t.project.id = :projectId
                              AND %s
                              AND NOT EXISTS (
                                  SELECT 1 FROM WorkflowFinishStatus fs
                                  WHERE fs.id.workflowId = t.project.workflow.id
                                  AND fs.id.statusId = t.status.id
                                  AND fs.outcome = dev.vepo.issues.workflow.FinishOutcome.CANCELED
                              )
                              """.formatted(versionPredicate), Ticket.class)
                 .setParameter("projectId", projectId)
                 .setParameter("versionId", versionId)
                 .getResultStream();
    }
}
package dev.vepo.issues.ticket.link;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TicketLinkRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public TicketLink save(TicketLink link) {
        em.persist(link);
        return link;
    }

    public Optional<TicketLink> findById(long id) {
        return Optional.ofNullable(em.find(TicketLink.class, id));
    }

    @Transactional
    public void delete(TicketLink link) {
        em.remove(em.contains(link) ? link : em.merge(link));
    }

    public Stream<TicketLink> findByTicketId(long ticketId) {
        return em.createQuery("""
                              FROM TicketLink l
                              WHERE l.source.id = :ticketId OR l.target.id = :ticketId
                              ORDER BY l.id
                              """, TicketLink.class)
                 .setParameter("ticketId", ticketId)
                 .getResultStream();
    }

    public boolean exists(long sourceId, long targetId, TicketLinkType linkType) {
        return em.createQuery("""
                              SELECT COUNT(l) FROM TicketLink l
                              WHERE l.source.id = :sourceId
                                AND l.target.id = :targetId
                                AND l.linkType = :linkType
                              """, Long.class)
                 .setParameter("sourceId", sourceId)
                 .setParameter("targetId", targetId)
                 .setParameter("linkType", linkType)
                 .getSingleResult() > 0;
    }

    public boolean existsEitherDirection(long ticketAId, long ticketBId, TicketLinkType linkType) {
        return em.createQuery("""
                              SELECT COUNT(l) FROM TicketLink l
                              WHERE l.linkType = :linkType
                                AND ((l.source.id = :a AND l.target.id = :b)
                                  OR (l.source.id = :b AND l.target.id = :a))
                              """, Long.class)
                 .setParameter("linkType", linkType)
                 .setParameter("a", ticketAId)
                 .setParameter("b", ticketBId)
                 .getSingleResult() > 0;
    }

    public Optional<TicketLink> findChildOf(long childTicketId) {
        return em.createQuery("""
                              FROM TicketLink l
                              WHERE l.source.id = :childId AND l.linkType = :linkType
                              """, TicketLink.class)
                 .setParameter("childId", childTicketId)
                 .setParameter("linkType", TicketLinkType.CHILD_OF)
                 .getResultStream()
                 .findFirst();
    }

    public boolean isParent(long ticketId) {
        return em.createQuery("""
                              SELECT COUNT(l) FROM TicketLink l
                              WHERE l.target.id = :ticketId AND l.linkType = :linkType
                              """, Long.class)
                 .setParameter("ticketId", ticketId)
                 .setParameter("linkType", TicketLinkType.CHILD_OF)
                 .getSingleResult() > 0;
    }

    public List<TicketLink> findChildren(long parentTicketId) {
        return em.createQuery("""
                              FROM TicketLink l
                              WHERE l.target.id = :parentId AND l.linkType = :linkType
                              ORDER BY l.id
                              """, TicketLink.class)
                 .setParameter("parentId", parentTicketId)
                 .setParameter("linkType", TicketLinkType.CHILD_OF)
                 .getResultList();
    }

    public long countChildren(long parentTicketId) {
        return em.createQuery("""
                              SELECT COUNT(l) FROM TicketLink l
                              WHERE l.target.id = :parentId
                                AND l.linkType = :linkType
                                AND l.source.deleted = false
                              """, Long.class)
                 .setParameter("parentId", parentTicketId)
                 .setParameter("linkType", TicketLinkType.CHILD_OF)
                 .getSingleResult();
    }

    public long countDoneChildren(long parentTicketId) {
        return em.createQuery("""
                              SELECT COUNT(l) FROM TicketLink l
                              WHERE l.target.id = :parentId
                                AND l.linkType = :linkType
                                AND l.source.deleted = false
                                AND l.source.finishedAt IS NOT NULL
                              """, Long.class)
                 .setParameter("parentId", parentTicketId)
                 .setParameter("linkType", TicketLinkType.CHILD_OF)
                 .getSingleResult();
    }
}

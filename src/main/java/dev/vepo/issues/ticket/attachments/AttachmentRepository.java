package dev.vepo.issues.ticket.attachments;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AttachmentRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Attachment save(Attachment attachment) {
        em.persist(attachment);
        return attachment;
    }

    public Optional<Attachment> findByIdAndTicketId(long attachmentId, long ticketId) {
        return em.createQuery("""
                              FROM Attachment a
                              WHERE a.id = :attachmentId AND a.ticket.id = :ticketId
                              """, Attachment.class)
                 .setParameter("attachmentId", attachmentId)
                 .setParameter("ticketId", ticketId)
                 .getResultStream()
                 .findFirst();
    }

    public List<Attachment> findByTicketId(long ticketId) {
        return em.createQuery("""
                              FROM Attachment a
                              WHERE a.ticket.id = :ticketId
                              ORDER BY a.uploadedAt ASC, a.id ASC
                              """, Attachment.class)
                 .setParameter("ticketId", ticketId)
                 .getResultList();
    }

    public long countByTicketId(long ticketId) {
        return em.createQuery("""
                              SELECT COUNT(a) FROM Attachment a
                              WHERE a.ticket.id = :ticketId
                              """, Long.class)
                 .setParameter("ticketId", ticketId)
                 .getSingleResult();
    }

    public long sumSizeBytesByTicketId(long ticketId) {
        var sum = em.createQuery("""
                                 SELECT COALESCE(SUM(a.sizeBytes), 0) FROM Attachment a
                                 WHERE a.ticket.id = :ticketId
                                 """, Long.class)
                    .setParameter("ticketId", ticketId)
                    .getSingleResult();
        return sum == null ? 0L : sum;
    }

    @Transactional
    public void delete(Attachment attachment) {
        em.remove(em.contains(attachment) ? attachment : em.merge(attachment));
    }
}

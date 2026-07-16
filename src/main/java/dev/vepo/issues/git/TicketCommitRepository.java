package dev.vepo.issues.git;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TicketCommitRepository {

    private final EntityManager em;

    @Inject
    public TicketCommitRepository(EntityManager em) {
        this.em = em;
    }

    public Optional<TicketCommit> findByTicketIdAndSha(long ticketId, String sha) {
        return em.createQuery("FROM TicketCommit c WHERE c.ticket.id = :ticketId AND c.sha = :sha", TicketCommit.class)
                 .setParameter("ticketId", ticketId)
                 .setParameter("sha", sha)
                 .getResultStream()
                 .findFirst();
    }

    public List<TicketCommit> findByTicketId(long ticketId) {
        return em.createQuery("FROM TicketCommit c WHERE c.ticket.id = :ticketId ORDER BY c.committedAt DESC NULLS LAST, c.createdAt DESC",
                              TicketCommit.class)
                 .setParameter("ticketId", ticketId)
                 .getResultList();
    }

    @Transactional
    public TicketCommit save(TicketCommit commit) {
        em.persist(commit);
        return commit;
    }
}

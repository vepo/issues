package dev.vepo.issues.ticket.search.query;

import java.util.List;
import java.util.Set;

import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

@ApplicationScoped
public class TicketQueryRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Ticket> search(ParsedTicketQuery parsedQuery,
                               User requestingUser,
                               Set<Long> readableProjectIds,
                               int limit) {
        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(Ticket.class);
        var ticket = cq.from(Ticket.class);
        var builder = new TicketQueryPredicateBuilder(cb, ticket, cq, requestingUser);
        var expressionPredicate = builder.visit(parsedQuery.expression());
        var readableProjectsPredicate = readableProjectsPredicate(cb,
                                                                  ticket.get("project").get("id"),
                                                                  readableProjectIds);
        cq.where(cb.and(cb.isFalse(ticket.get("deleted")), readableProjectsPredicate, expressionPredicate));
        parsedQuery.order()
                   .ifPresent(order -> cq.orderBy(TicketQueryLanguageService.buildOrder(cb, ticket, order)));
        return em.createQuery(cq)
                 .setMaxResults(limit)
                 .getResultList();
    }

    private Predicate readableProjectsPredicate(CriteriaBuilder cb,
                                                Path<Long> projectId,
                                                Set<Long> readableProjectIds) {
        return readableProjectIds.isEmpty() ? cb.disjunction() : projectId.in(readableProjectIds);
    }
}

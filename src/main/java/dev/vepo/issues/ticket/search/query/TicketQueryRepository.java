package dev.vepo.issues.ticket.search.query;

import java.util.ArrayList;
import java.util.List;

import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;

@ApplicationScoped
public class TicketQueryRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Ticket> search(ParsedTicketQuery parsed, User currentUser) {
        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(Ticket.class);
        var ticket = cq.from(Ticket.class);
        var builder = new TicketQueryPredicateBuilder(cb, ticket, cq, currentUser);
        var expressionPredicate = builder.visit(parsed.expression());
        var predicates = new ArrayList<Predicate>();
        predicates.add(cb.isFalse(ticket.get("deleted")));
        predicates.add(expressionPredicate);
        cq.where(cb.and(predicates.toArray(Predicate[]::new)));
        parsed.order()
              .ifPresent(order -> cq.orderBy(TicketQueryLanguageService.buildOrder(cb, ticket, order)));
        return em.createQuery(cq)
                 .getResultList();
    }
}

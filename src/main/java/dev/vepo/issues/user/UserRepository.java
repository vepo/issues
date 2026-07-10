package dev.vepo.issues.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@ApplicationScoped
public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    private EntityManager em;

    @Inject
    public UserRepository(EntityManager entityManager) {
        this.em = entityManager;
    }

    public Optional<User> findById(Long id) {
        return em.createQuery("FROM User WHERE id = :id AND deleted = false", User.class)
                 .setParameter("id", id)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<User> findByEmail(String email) {
        return em.createQuery("FROM User WHERE email = :email AND deleted = false", User.class)
                 .setParameter("email", email)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<User> findByUsername(String username) {
        return em.createQuery("FROM User WHERE username = :username AND deleted = false", User.class)
                 .setParameter("username", username)
                 .getResultStream()
                 .findFirst();
    }

    public long countBlockingAssignedTickets(long userId) {
        return em.createQuery("""
                              SELECT COUNT(t)
                              FROM Ticket t
                              JOIN t.project p
                              JOIN p.workflow w
                              WHERE t.deleted = false
                                AND t.assignee.id = :userId
                                AND t.status <> w.start
                                AND NOT EXISTS (
                                    SELECT 1
                                    FROM WorkflowFinishStatus fs
                                    WHERE fs.workflow = w
                                      AND fs.status = t.status
                                      AND fs.outcome IN (dev.vepo.issues.workflow.FinishOutcome.DONE,
                                                         dev.vepo.issues.workflow.FinishOutcome.CANCELED)
                                )
                              """, Long.class)
                 .setParameter("userId", userId)
                 .getSingleResult();
    }

    public void softDelete(User user) {
        user.setDeleted(true);
        em.merge(user);
    }

    public User save(User user) {
        em.persist(user);
        return user;
    }

    public Stream<User> search(String name, String email, List<Role> roles) {
        logger.info("Searching for users...");
        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(User.class);
        var user = cq.from(User.class);

        var predicates = new ArrayList<Predicate>();

        predicates.add(cb.isFalse(user.get("deleted")));

        if (Objects.nonNull(name) && !name.isBlank()) {
            predicates.add(cb.like(cb.lower(user.get("name")), "%%%s%%".formatted(name).toLowerCase()));
        }

        if (Objects.nonNull(email) && !email.isBlank()) {
            predicates.add(cb.like(cb.lower(user.get("email")), "%%%s%%".formatted(email).toLowerCase()));
        }

        if (!predicates.isEmpty()) {
            cq = cq.where(cb.and(predicates));
        }

        return em.createQuery(cq)
                 .getResultStream()
                 .filter(u -> roles.isEmpty() || roles.stream().allMatch(role -> u.getRoles().contains(role)));
    }

    public Optional<User> findByEmailOrUsername(String credential) {
        logger.debug("Searching for user: credential={}", credential);
        return em.createQuery("""
                              FROM User
                              WHERE deleted = false
                                AND (email = :credential OR username = :credential)
                              """, User.class)
                 .setParameter("credential", credential)
                 .getResultStream()
                 .findFirst();
    }
}

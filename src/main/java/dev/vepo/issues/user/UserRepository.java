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

    public Optional<User> findByEmail(String email) {
        return em.createQuery("FROM User WHERE email = :email", User.class)
                 .setParameter("email", email)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<User> findByUsername(String username) {
        return em.createQuery("FROM User WHERE username = :username", User.class)
                 .setParameter("username", username)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<User> findById(Long id) {
        return em.createQuery("FROM User WHERE id = :id", User.class)
                 .setParameter("id", id)
                 .getResultStream()
                 .findFirst();
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

        // TODO: Filter on query
        // if (Objects.nonNull(roles) && !roles.isEmpty()) {
        // predicates.addAll(roles.stream()
        // .map(role -> cb.like(user.get("roles").as(String.class),
        // String.format("%%%s%%", role.name())))
        // .toList());
        // }

        if (!predicates.isEmpty()) {
            cq = cq.where(cb.and(predicates));
        }

        return em.createQuery(cq)
                 .getResultStream()
                 .filter(u -> roles.isEmpty() || u.getRoles().containsAll(roles));
    }

    public Optional<User> findByEmailOrUsername(String credential) {
        logger.debug("Searching for user: credential={}", credential);
        return em.createQuery("FROM User WHERE email = :credential OR username = :credential", User.class)
                 .setParameter("credential", credential)
                 .getResultStream()
                 .findFirst();
    }
}

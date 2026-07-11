package dev.vepo.issues.auth.apitoken;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ApiTokenRepository {

    private final EntityManager em;

    @Inject
    public ApiTokenRepository(EntityManager em) {
        this.em = em;
    }

    public Optional<ApiToken> findById(long id) {
        return Optional.ofNullable(em.find(ApiToken.class, id));
    }

    public Optional<ApiToken> findActiveByTokenHash(String tokenHash) {
        return em.createQuery("""
                              FROM ApiToken t
                              JOIN FETCH t.user
                              WHERE t.tokenHash = :tokenHash
                                AND t.revokedAt IS NULL
                              """, ApiToken.class)
                 .setParameter("tokenHash", tokenHash)
                 .getResultStream()
                 .findFirst();
    }

    public List<ApiToken> listByUserId(long userId) {
        return em.createQuery("""
                              FROM ApiToken t
                              WHERE t.user.id = :userId
                              ORDER BY t.createdAt DESC
                              """, ApiToken.class)
                 .setParameter("userId", userId)
                 .getResultList();
    }

    @Transactional
    public void persist(ApiToken token) {
        em.persist(token);
    }
}

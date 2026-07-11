package dev.vepo.issues.auth;

import java.util.Optional;

import dev.vepo.issues.auth.apitoken.ApiTokenHasher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RefreshTokenRepository {

    @PersistenceContext
    EntityManager em;

    private final ApiTokenHasher tokenHasher;

    @Inject
    public RefreshTokenRepository(ApiTokenHasher tokenHasher) {
        this.tokenHasher = tokenHasher;
    }

    public Optional<RefreshToken> findByToken(String rawToken) {
        return findByTokenHash(tokenHasher.hash(rawToken));
    }

    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return em.createQuery("FROM RefreshToken WHERE token = :token", RefreshToken.class)
                 .setParameter("token", tokenHash)
                 .getResultStream()
                 .findFirst();
    }

    @Transactional
    public RefreshToken save(RefreshToken refreshToken) {
        em.persist(refreshToken);
        return refreshToken;
    }

    @Transactional
    public void revokeTokenHash(String tokenHash) {
        em.createQuery("UPDATE RefreshToken SET revoked = true WHERE token = :token")
          .setParameter("token", tokenHash)
          .executeUpdate();
    }

    @Transactional
    public void revokeRawToken(String rawToken) {
        revokeTokenHash(tokenHasher.hash(rawToken));
    }

    @Transactional
    public void revokeAllForUser(long userId) {
        em.createQuery("UPDATE RefreshToken SET revoked = true WHERE user.id = :userId")
          .setParameter("userId", userId)
          .executeUpdate();
    }
}

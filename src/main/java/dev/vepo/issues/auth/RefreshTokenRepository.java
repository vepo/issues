package dev.vepo.issues.auth;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RefreshTokenRepository {

    @PersistenceContext
    EntityManager em;

    public Optional<RefreshToken> findByToken(String token) {
        return em.createQuery("FROM RefreshToken WHERE token = :token", RefreshToken.class)
                 .setParameter("token", token)
                 .getResultStream()
                 .findFirst();
    }

    @Transactional
    public RefreshToken save(RefreshToken refreshToken) {
        em.persist(refreshToken);
        return refreshToken;
    }

    @Transactional
    public void revokeToken(String token) {
        em.createQuery("UPDATE RefreshToken SET revoked = true WHERE token = :token")
          .setParameter("token", token)
          .executeUpdate();
    }

    @Transactional
    public void revokeAllForUser(long userId) {
        em.createQuery("UPDATE RefreshToken SET revoked = true WHERE user.id = :userId")
          .setParameter("userId", userId)
          .executeUpdate();
    }
}

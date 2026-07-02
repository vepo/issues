package dev.vepo.issues.user;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class PasswordResetTokenRepository {
    private EntityManager em;

    @Inject
    public PasswordResetTokenRepository(EntityManager entityManager) {
        this.em = entityManager;
    }

    public Optional<PasswordResetToken> findByToken(String token) {
        return em.createQuery("FROM PasswordResetToken WHERE token = :token", PasswordResetToken.class)
                 .setParameter("token", token)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<PasswordResetToken> findTokenByEmailOrUsername(String emailOrUsername) {
        return em.createQuery("FROM PasswordResetToken WHERE user.email = :emailOrUsername OR user.username = :emailOrUsername", PasswordResetToken.class)
                 .setParameter("emailOrUsername", emailOrUsername)
                 .getResultStream()
                 .findFirst();
    }

    public void invalidateAllUserTokens(Long userId) {
        em.createQuery("UPDATE PasswordResetToken t SET t.used = true WHERE t.user.id = :userId")
          .setParameter("userId", userId)
          .executeUpdate();
    }

    public PasswordResetToken save(PasswordResetToken resetToken) {
        this.em.persist(resetToken);
        return resetToken;
    }
}
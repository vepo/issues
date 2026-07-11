package dev.vepo.issues.user;

import java.util.Optional;

import dev.vepo.issues.auth.apitoken.ApiTokenHasher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class PasswordResetTokenRepository {
    private final EntityManager em;
    private final ApiTokenHasher tokenHasher;

    @Inject
    public PasswordResetTokenRepository(EntityManager entityManager, ApiTokenHasher tokenHasher) {
        this.em = entityManager;
        this.tokenHasher = tokenHasher;
    }

    public Optional<PasswordResetToken> findByToken(String rawToken) {
        return findByTokenHash(tokenHasher.hash(rawToken));
    }

    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return em.createQuery("FROM PasswordResetToken WHERE token = :token", PasswordResetToken.class)
                 .setParameter("token", tokenHash)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<PasswordResetToken> findTokenByEmailOrUsername(String emailOrUsername) {
        return em.createQuery("FROM PasswordResetToken WHERE user.email = :emailOrUsername OR user.username = :emailOrUsername",
                              PasswordResetToken.class)
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

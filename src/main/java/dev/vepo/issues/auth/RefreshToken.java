package dev.vepo.issues.auth;

import java.time.LocalDateTime;
import java.util.UUID;

import dev.vepo.issues.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "tb_refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SHA-256 hex of the opaque refresh token (plaintext returned once at issue).
     */
    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Transient
    private String rawToken;

    protected RefreshToken() {}

    public RefreshToken(User user, long validDays, String tokenHash, String rawToken) {
        this.token = tokenHash;
        this.rawToken = rawToken;
        this.user = user;
        this.expiresAt = LocalDateTime.now().plusDays(validDays);
        this.revoked = false;
    }

    public static String generateRawToken() {
        return UUID.randomUUID().toString();
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public String getRawToken() {
        return rawToken;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}

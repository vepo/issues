package dev.vepo.issues.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.User;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JwtTokenIssuer {

    private final String issuer;
    private final int accessTokenMinutes;

    public JwtTokenIssuer(@ConfigProperty(name = "mp.jwt.verify.issuer") String issuer,
                          @ConfigProperty(name = "auth.access-token-minutes", defaultValue = "15") int accessTokenMinutes) {
        this.issuer = issuer;
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public String issueAccessToken(User user) {
        var now = Instant.now();
        return Jwt.issuer(issuer)
                  .upn(user.getUsername())
                  .claim("username", user.getUsername())
                  .claim("id", user.getId())
                  .claim("email", user.getEmail())
                  .groups(user.getRoles()
                              .stream()
                              .map(Role::role)
                              .collect(Collectors.toSet()))
                  .issuedAt(now)
                  .expiresAt(now.plus(accessTokenMinutes, ChronoUnit.MINUTES))
                  .sign();
    }

    public long accessTokenExpiresInSeconds() {
        return accessTokenMinutes * 60L;
    }
}

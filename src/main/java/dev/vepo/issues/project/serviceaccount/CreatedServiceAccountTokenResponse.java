package dev.vepo.issues.project.serviceaccount;

import java.time.Instant;

public record CreatedServiceAccountTokenResponse(long id, String name, String prefix, Instant createdAt, String token) {

    public static CreatedServiceAccountTokenResponse load(ServiceAccountToken token, String rawSecret) {
        return new CreatedServiceAccountTokenResponse(token.getId(),
                                                      token.getName(),
                                                      token.getTokenPrefix(),
                                                      token.getCreatedAt(),
                                                      rawSecret);
    }
}

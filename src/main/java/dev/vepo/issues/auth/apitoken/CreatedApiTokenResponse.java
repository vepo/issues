package dev.vepo.issues.auth.apitoken;

import java.time.Instant;

public record CreatedApiTokenResponse(long id, String name, String prefix, Instant createdAt, String token) {

    public static CreatedApiTokenResponse load(ApiToken apiToken, String rawSecret) {
        return new CreatedApiTokenResponse(apiToken.getId(),
                                           apiToken.getName(),
                                           apiToken.getTokenPrefix(),
                                           apiToken.getCreatedAt(),
                                           rawSecret);
    }
}

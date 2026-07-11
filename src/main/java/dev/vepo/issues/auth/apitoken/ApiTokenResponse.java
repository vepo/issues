package dev.vepo.issues.auth.apitoken;

import java.time.Instant;

public record ApiTokenResponse(long id,
                               String name,
                               String tokenPrefix,
                               Instant createdAt,
                               Instant lastUsedAt,
                               Instant revokedAt) {

    public static ApiTokenResponse load(ApiToken apiToken) {
        return new ApiTokenResponse(apiToken.getId(),
                                    apiToken.getName(),
                                    apiToken.getTokenPrefix(),
                                    apiToken.getCreatedAt(),
                                    apiToken.getLastUsedAt(),
                                    apiToken.getRevokedAt());
    }
}

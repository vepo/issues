package dev.vepo.issues.project.serviceaccount;

import java.time.Instant;

public record ServiceAccountResponse(long id, String name, Instant createdAt, boolean active) {

    public static ServiceAccountResponse load(ServiceAccount serviceAccount) {
        return new ServiceAccountResponse(serviceAccount.getId(),
                                          serviceAccount.getName(),
                                          serviceAccount.getCreatedAt(),
                                          serviceAccount.isActive());
    }
}

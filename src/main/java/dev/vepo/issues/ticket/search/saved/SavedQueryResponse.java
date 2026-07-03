package dev.vepo.issues.ticket.search.saved;

import java.time.LocalDateTime;

public record SavedQueryResponse(long id,
                                 String slug,
                                 String name,
                                 String query,
                                 boolean showAtHome,
                                 long ownerId,
                                 String ownerName,
                                 LocalDateTime createdAt,
                                 LocalDateTime updatedAt) {
    public static SavedQueryResponse load(SavedQuery savedQuery) {
        return new SavedQueryResponse(savedQuery.getId(),
                                      savedQuery.getSlug(),
                                      savedQuery.getName(),
                                      savedQuery.getQueryText(),
                                      savedQuery.isShowAtHome(),
                                      savedQuery.getOwner().getId(),
                                      savedQuery.getOwner().getName(),
                                      savedQuery.getCreatedAt(),
                                      savedQuery.getUpdatedAt());
    }
}

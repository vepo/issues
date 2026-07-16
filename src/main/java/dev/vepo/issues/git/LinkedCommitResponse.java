package dev.vepo.issues.git;

import java.time.Instant;

import dev.vepo.issues.user.User;

public record LinkedCommitResponse(long id,
                                   String sha,
                                   String message,
                                   String authorName,
                                   String authorEmail,
                                   Long matchedUserId,
                                   String matchedUserName,
                                   Instant committedAt,
                                   String commitUrl,
                                   Instant createdAt) {

    public static LinkedCommitResponse load(TicketCommit commit) {
        User matched = commit.getMatchedUser();
        return new LinkedCommitResponse(commit.getId(),
                                        commit.getSha(),
                                        commit.getMessage(),
                                        commit.getAuthorName(),
                                        commit.getAuthorEmail(),
                                        matched != null ? matched.getId() : null,
                                        matched != null ? matched.getName() : null,
                                        commit.getCommittedAt(),
                                        commit.getCommitUrl(),
                                        commit.getCreatedAt());
    }
}

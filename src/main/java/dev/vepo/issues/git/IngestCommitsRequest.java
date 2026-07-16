package dev.vepo.issues.git;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IngestCommitsRequest(@NotEmpty List<@Valid IngestCommitItem> commits) {

    public record IngestCommitItem(@NotBlank @Size(max = 64) String sha,
                                   @NotBlank String message,
                                   String authorName,
                                   String authorEmail,
                                   Instant committedAt,
                                   @Size(max = 1024) String commitUrl) {}
}

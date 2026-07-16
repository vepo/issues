package dev.vepo.issues.ticket.csvimport;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InitTicketImportUploadRequest(@NotBlank String fileName,
                                            @NotNull @Min(1) Long totalBytes,
                                            @NotNull @Min(1) Integer chunkCount) {}

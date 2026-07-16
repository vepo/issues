package dev.vepo.issues.git;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectGitRepositoryRequest(@NotBlank @Size(max = 512) String remoteUrl,
                                          GitProvider provider,
                                          @Size(max = 128) String defaultBranch) {}

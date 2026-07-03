package dev.vepo.issues.project;

import jakarta.validation.constraints.NotNull;

public record AddProjectMemberRequest(@NotNull Long userId) {}

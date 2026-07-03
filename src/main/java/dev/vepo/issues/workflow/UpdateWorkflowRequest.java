package dev.vepo.issues.workflow;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateWorkflowRequest(@NotBlank(message = "Workflow name cannot be empty!") @Size(min = 5, max = 64, message = "Workflow name should have at least 5 caracters and at most 64!") String name,
                                    @NotNull(message = "No start status is defined!") String start,
                                    @NotEmpty List<TransitionRequest> transitions,
                                    String phaseStart,
                                    List<FinishStatusRequest> finishStatuses) {}

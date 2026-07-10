package dev.vepo.issues.workflow;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@ValidTransitions
public record CreateWorkflowRequest(@NotBlank(message = "Workflow name cannot be empty!") @Size(min = 5, max = 64, message = "Workflow name should have at least 5 caracters and at most 64!") String name,
                                    @NotEmpty(message = "No status defined!") @Size(min = 2, message = "At least 2 statuses must be defined!") List<String> statuses,
                                    @NotNull(message = "No start status is defined!") String start,
                                    @NotEmpty List<TransitionRequest> transitions,
                                    String phaseStart,
                                    @Valid List<FinishStatusRequest> finishStatuses,
                                    @Valid List<StatusWipRequest> wipLimits) {}

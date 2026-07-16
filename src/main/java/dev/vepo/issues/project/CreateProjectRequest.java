package dev.vepo.issues.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(@NotBlank(message = "Project prefix cannot be empty") @Size(min = 2, max = 10) String prefix,
                                   @NotBlank(message = "Project name cannot be empty") String name,
                                   @NotBlank(message = "Project description cannot be empty") String description,
                                   @NotNull(message = "Workflow ID must be provided") Long workflowId,
                                   Long ownerId,
                                   SecurityLevel securityLevel,
                                   TicketTemplateRequest ticketTemplate,
                                   PhaseTemplateRequest phaseTemplate) {}

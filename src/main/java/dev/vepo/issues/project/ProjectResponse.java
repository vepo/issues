package dev.vepo.issues.project;

import java.util.List;

import dev.vepo.issues.customfield.CustomFieldValueResponse;

public record ProjectResponse(long id,
                              String name,
                              String prefix,
                              String description,
                              ProjectWorkflowResponse workflow,
                              ProjectOwnerResponse owner,
                              SecurityLevel securityLevel,
                              TicketTemplateResponse ticketTemplate,
                              PhaseTemplateResponse phaseTemplate,
                              boolean prefixLocked) {

    public static ProjectResponse load(Project project, boolean prefixLocked) {
        return load(project, prefixLocked, List.of());
    }

    public static ProjectResponse load(Project project,
                                       boolean prefixLocked,
                                       List<CustomFieldValueResponse> customFieldDefaults) {
        return new ProjectResponse(project.getId(),
                                   project.getName(),
                                   project.getPrefix(),
                                   project.getDescription(),
                                   new ProjectWorkflowResponse(project.getWorkflow().getId(),
                                                               project.getWorkflow().getName()),
                                   ProjectOwnerResponse.load(project.getOwner()),
                                   project.getSecurityLevel() == null ? SecurityLevel.INTERNAL : project.getSecurityLevel(),
                                   TicketTemplateResponse.load(project, customFieldDefaults),
                                   PhaseTemplateResponse.load(project),
                                   prefixLocked);
    }
}

package dev.vepo.issues.project;

public record ProjectResponse(long id,
                              String name,
                              String prefix,
                              String description,
                              ProjectWorkflowResponse workflow,
                              ProjectOwnerResponse owner,
                              TicketTemplateResponse ticketTemplate,
                              PhaseTemplateResponse phaseTemplate) {

    public static ProjectResponse load(Project project) {
        return new ProjectResponse(project.getId(),
                                   project.getName(),
                                   project.getPrefix(),
                                   project.getDescription(),
                                   new ProjectWorkflowResponse(project.getWorkflow().getId(),
                                                               project.getWorkflow().getName()),
                                   ProjectOwnerResponse.load(project.getOwner()),
                                   TicketTemplateResponse.load(project),
                                   PhaseTemplateResponse.load(project));
    }
}
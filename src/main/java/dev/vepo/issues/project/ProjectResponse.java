package dev.vepo.issues.project;

public record ProjectResponse(long id,
                              String name,
                              String prefix,
                              String description,
                              ProjectWorkflowResponse workflow,
                              TicketTemplateResponse ticketTemplate) {

    public static ProjectResponse load(Project project) {
        return new ProjectResponse(project.getId(),
                                   project.getName(),
                                   project.getPrefix(),
                                   project.getDescription(),
                                   new ProjectWorkflowResponse(project.getWorkflow().getId(),
                                                               project.getWorkflow().getName()),
                                   TicketTemplateResponse.load(project));
    }
}
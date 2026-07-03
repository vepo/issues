package dev.vepo.issues.project;

import dev.vepo.issues.ticket.TicketPriority;

public record TicketTemplateResponse(boolean enabled,
                                     String title,
                                     String description,
                                     Long categoryId,
                                     TicketPriority priority) {

    public static TicketTemplateResponse load(Project project) {
        return new TicketTemplateResponse(project.isTicketTemplateEnabled(),
                                          project.getTicketTemplateTitle(),
                                          project.getTicketTemplateDescription(),
                                          project.getTicketTemplateCategoryId(),
                                          project.getTicketTemplatePriority());
    }
}

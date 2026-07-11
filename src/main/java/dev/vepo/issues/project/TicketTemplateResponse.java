package dev.vepo.issues.project;

import java.util.List;

import dev.vepo.issues.customfield.CustomFieldValueResponse;
import dev.vepo.issues.ticket.TicketPriority;

public record TicketTemplateResponse(boolean enabled,
                                     String title,
                                     String description,
                                     Long categoryId,
                                     TicketPriority priority,
                                     List<CustomFieldValueResponse> customFieldDefaults) {

    public static TicketTemplateResponse load(Project project) {
        return load(project, List.of());
    }

    public static TicketTemplateResponse load(Project project, List<CustomFieldValueResponse> customFieldDefaults) {
        return new TicketTemplateResponse(project.isTicketTemplateEnabled(),
                                          project.getTicketTemplateTitle(),
                                          project.getTicketTemplateDescription(),
                                          project.getTicketTemplateCategoryId(),
                                          project.getTicketTemplatePriority(),
                                          customFieldDefaults == null ? List.of() : List.copyOf(customFieldDefaults));
    }
}

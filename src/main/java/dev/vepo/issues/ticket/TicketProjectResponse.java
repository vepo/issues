package dev.vepo.issues.ticket;

import dev.vepo.issues.project.Project;

public record TicketProjectResponse(long id,
                                    String name) {

    public static TicketProjectResponse load(Project project) {
        return new TicketProjectResponse(project.getId(), project.getName());
    }
}
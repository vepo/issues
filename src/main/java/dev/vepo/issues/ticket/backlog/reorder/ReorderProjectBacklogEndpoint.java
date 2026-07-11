package dev.vepo.issues.ticket.backlog.reorder;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.project.ProjectPaths;
import dev.vepo.issues.ticket.backlog.BacklogService;
import dev.vepo.issues.ticket.backlog.BacklogTicketResponse;
import dev.vepo.issues.ticket.backlog.ReorderBacklogRequest;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@DenyAll
@ApplicationScoped
@Path(ProjectPaths.BASE)
@Tag(name = "Backlog")
public class ReorderProjectBacklogEndpoint {

    private final BacklogService backlogService;

    @Inject
    public ReorderProjectBacklogEndpoint(BacklogService backlogService) {
        this.backlogService = backlogService;
    }

    @POST
    @Path("{projectId}/backlog/reorder")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "reorderProjectBacklog", summary = "Reorder a ticket in the project backlog")
    public BacklogTicketResponse reorder(@PathParam("projectId") long projectId,
                                         @Valid ReorderBacklogRequest request,
                                         @Context SecurityContext context) {
        return backlogService.reorder(projectId, request, context.getUserPrincipal().getName());
    }
}

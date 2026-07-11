package dev.vepo.issues.ticket.backlog.list;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.project.ProjectPaths;
import dev.vepo.issues.ticket.backlog.BacklogPageResponse;
import dev.vepo.issues.ticket.backlog.BacklogService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@DenyAll
@ApplicationScoped
@Path(ProjectPaths.BASE)
@Tag(name = "Backlog")
public class ListProjectBacklogEndpoint {

    private final BacklogService backlogService;

    @Inject
    public ListProjectBacklogEndpoint(BacklogService backlogService) {
        this.backlogService = backlogService;
    }

    @GET
    @Path("{projectId}/backlog")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "listProjectBacklog", summary = "List project backlog page")
    public BacklogPageResponse list(@PathParam("projectId") long projectId,
                                    @QueryParam("page") @DefaultValue("0") int page,
                                    @QueryParam("size") @DefaultValue("20") int size,
                                    @Context SecurityContext context) {
        return backlogService.list(projectId, page, size, context.getUserPrincipal().getName());
    }
}

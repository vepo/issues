package dev.vepo.issues.project.status;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.project.ProjectPaths;
import dev.vepo.issues.project.ProjectService;
import dev.vepo.issues.project.ProjectStatusResponse;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(ProjectPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Project")
public class ListProjectStatusesEndpoint {

    private final ProjectService projectService;

    @Inject
    public ListProjectStatusesEndpoint(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GET
    @Path("{projectId}/status")
    @RolesAllowed({ Role.PROJECT_MANAGER_ROLE, Role.ADMIN_ROLE, Role.USER_ROLE })
    @Operation(operationId = "listProjectStatuses", summary = "List project workflow statuses")
    public List<ProjectStatusResponse> listStatuses(@PathParam("projectId") long projectId) {
        return projectService.listStatuses(projectId);
    }
}

package dev.vepo.issues.project.list;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.project.ProjectPaths;
import dev.vepo.issues.project.ProjectResponse;
import dev.vepo.issues.project.ProjectService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(ProjectPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Project")
public class ListProjectsEndpoint {

    private final ProjectService projectService;

    @Inject
    public ListProjectsEndpoint(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GET
    @RolesAllowed({ Role.PROJECT_MANAGER_ROLE, Role.ADMIN_ROLE, Role.USER_ROLE })
    @Operation(operationId = "listProjects", summary = "List all projects")
    public List<ProjectResponse> listAll(@Context SecurityContext securityContext) {
        return projectService.listAll(securityContext.getUserPrincipal().getName());
    }
}

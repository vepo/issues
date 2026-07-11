package dev.vepo.issues.project.update;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.project.CreateProjectRequest;
import dev.vepo.issues.project.ProjectPaths;
import dev.vepo.issues.project.ProjectResponse;
import dev.vepo.issues.project.ProjectService;
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

@Path(ProjectPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Project")
public class UpdateProjectEndpoint {

    private final ProjectService projectService;

    @Inject
    public UpdateProjectEndpoint(ProjectService projectService) {
        this.projectService = projectService;
    }

    @POST
    @Path("{projectId}")
    @ResponseStatus(201)
    @RolesAllowed({ Role.PROJECT_MANAGER_ROLE, Role.ADMIN_ROLE })
    @Operation(operationId = "updateProject", summary = "Update a project")
    public ProjectResponse update(@PathParam("projectId") Long projectId,
                                  @Valid CreateProjectRequest request,
                                  @Context SecurityContext securityContext) {
        return projectService.update(projectId, request, securityContext.getUserPrincipal().getName());
    }
}

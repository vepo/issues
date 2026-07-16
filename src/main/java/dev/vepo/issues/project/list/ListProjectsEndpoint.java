package dev.vepo.issues.project.list;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.project.ProjectPaths;
import dev.vepo.issues.project.ProjectResponse;
import dev.vepo.issues.project.ProjectService;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
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
    @PermitAll
    @Operation(operationId = "listProjects", summary = "List projects visible to the caller")
    public List<ProjectResponse> listAll(@Context SecurityContext securityContext) {
        var principal = securityContext.getUserPrincipal();
        if (principal == null) {
            return projectService.listPublic();
        }
        return projectService.listAll(principal.getName());
    }
}

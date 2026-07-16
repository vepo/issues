package dev.vepo.issues.project.find;

import java.util.Optional;

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
public class FindProjectByIdEndpoint {

    private final ProjectService projectService;

    @Inject
    public FindProjectByIdEndpoint(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GET
    @Path("{projectId}")
    @PermitAll
    @Operation(operationId = "findProjectById", summary = "Find project by ID")
    public ProjectResponse findById(@PathParam("projectId") long projectId, @Context SecurityContext securityContext) {
        return projectService.findById(projectId, optionalUsername(securityContext));
    }

    private static Optional<String> optionalUsername(SecurityContext securityContext) {
        var principal = securityContext.getUserPrincipal();
        return principal == null ? Optional.empty() : Optional.of(principal.getName());
    }
}

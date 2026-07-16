package dev.vepo.issues.git.get;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.git.GitPaths;
import dev.vepo.issues.git.ProjectGitRepositoryResponse;
import dev.vepo.issues.git.ProjectGitRepositoryService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(GitPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Git")
public class GetProjectGitEndpoint {

    private final ProjectGitRepositoryService service;

    @Inject
    public GetProjectGitEndpoint(ProjectGitRepositoryService service) {
        this.service = service;
    }

    @GET
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "getProjectGitRepository", summary = "Get project git repository association")
    public ProjectGitRepositoryResponse get(@PathParam("projectId") long projectId,
                                            @Context SecurityContext securityContext) {
        return service.find(projectId, securityContext.getUserPrincipal().getName())
                      .orElseThrow(() -> new NotFoundException("Git repository is not configured for project %d".formatted(projectId)));
    }
}

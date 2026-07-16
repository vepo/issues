package dev.vepo.issues.git.put;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.git.GitPaths;
import dev.vepo.issues.git.ProjectGitRepositoryRequest;
import dev.vepo.issues.git.ProjectGitRepositoryResponse;
import dev.vepo.issues.git.ProjectGitRepositoryService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(GitPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Git")
public class PutProjectGitEndpoint {

    private final ProjectGitRepositoryService service;

    @Inject
    public PutProjectGitEndpoint(ProjectGitRepositoryService service) {
        this.service = service;
    }

    @PUT
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "putProjectGitRepository", summary = "Create or update project git repository association")
    public ProjectGitRepositoryResponse put(@PathParam("projectId") long projectId,
                                            @Valid ProjectGitRepositoryRequest request,
                                            @Context SecurityContext securityContext) {
        return service.upsert(projectId, request, securityContext.getUserPrincipal().getName());
    }
}

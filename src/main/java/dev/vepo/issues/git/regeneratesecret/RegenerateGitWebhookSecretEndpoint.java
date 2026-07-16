package dev.vepo.issues.git.regeneratesecret;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.git.GitPaths;
import dev.vepo.issues.git.ProjectGitRepositoryResponse;
import dev.vepo.issues.git.ProjectGitRepositoryService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(GitPaths.BASE + "/regenerate-secret")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Git")
public class RegenerateGitWebhookSecretEndpoint {

    private final ProjectGitRepositoryService service;

    @Inject
    public RegenerateGitWebhookSecretEndpoint(ProjectGitRepositoryService service) {
        this.service = service;
    }

    @POST
    @ResponseStatus(200)
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "regenerateGitWebhookSecret", summary = "Regenerate forge webhook secret (returned once)")
    public ProjectGitRepositoryResponse regenerate(@PathParam("projectId") long projectId,
                                                   @Context SecurityContext securityContext) {
        return service.regenerateSecret(projectId, securityContext.getUserPrincipal().getName());
    }
}

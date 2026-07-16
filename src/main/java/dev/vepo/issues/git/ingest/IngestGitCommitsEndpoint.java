package dev.vepo.issues.git.ingest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.git.GitCommitService;
import dev.vepo.issues.git.GitPaths;
import dev.vepo.issues.git.IngestCommitsRequest;
import dev.vepo.issues.git.IngestCommitsResponse;
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

@Path(GitPaths.BASE + "/commits")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Git")
public class IngestGitCommitsEndpoint {

    private final GitCommitService gitCommitService;

    @Inject
    public IngestGitCommitsEndpoint(GitCommitService gitCommitService) {
        this.gitCommitService = gitCommitService;
    }

    @POST
    @ResponseStatus(200)
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE, Role.USER_ROLE })
    @Operation(operationId = "ingestGitCommits", summary = "Ingest commits that mention ticket identifiers (PAT/SA Bearer)")
    public IngestCommitsResponse ingest(@PathParam("projectId") long projectId,
                                        @Valid IngestCommitsRequest request,
                                        @Context SecurityContext securityContext) {
        return gitCommitService.ingestAuthenticated(projectId, request, securityContext.getUserPrincipal().getName());
    }
}

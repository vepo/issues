package dev.vepo.issues.git.webhook;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.git.GitCommitService;
import dev.vepo.issues.git.GitPaths;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path(GitPaths.BASE + "/webhook")
@ApplicationScoped
@DenyAll
@Tag(name = "Git")
public class GitWebhookEndpoint {

    private final GitCommitService gitCommitService;

    @Inject
    public GitWebhookEndpoint(GitCommitService gitCommitService) {
        this.gitCommitService = gitCommitService;
    }

    @POST
    @PermitAll
    @ResponseStatus(204)
    @Operation(operationId = "gitWebhook", summary = "Forge push webhook (HMAC)")
    public Response webhook(@PathParam("projectId") long projectId,
                            InputStream body,
                            @HeaderParam("X-Hub-Signature-256") String hubSignature256,
                            @HeaderParam("X-Gitlab-Token") String gitlabToken)
            throws IOException {
        var bytes = body == null ? new byte[0] : body.readAllBytes();
        gitCommitService.ingestWebhook(projectId, bytes, hubSignature256, gitlabToken);
        return Response.noContent().build();
    }
}

package dev.vepo.issues.dashboards.burndown;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.project.ProjectPaths;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@DenyAll
@ApplicationScoped
@Path(ProjectPaths.BASE)
@Tag(name = "Burndown")
public class LoadBurndownEndpoint {

    private final BurndownService burndownService;

    @Inject
    public LoadBurndownEndpoint(BurndownService burndownService) {
        this.burndownService = burndownService;
    }

    @GET
    @Path("{projectId}/burndown")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(operationId = "loadProjectBurndown", summary = "Load phase burndown series for a project")
    public BurndownResponse load(@PathParam("projectId") long projectId,
                                 @QueryParam("phaseId") long phaseId,
                                 @Context SecurityContext context) {
        return burndownService.load(projectId, phaseId, context.getUserPrincipal().getName());
    }
}

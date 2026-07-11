package dev.vepo.issues.phase.version.update;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.phase.UpdateVersionRequest;
import dev.vepo.issues.phase.VersionPaths;
import dev.vepo.issues.phase.VersionResponse;
import dev.vepo.issues.phase.VersionService;
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

@Path(VersionPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Version")
public class UpdateVersionEndpoint {

    private final VersionService versionService;

    @Inject
    public UpdateVersionEndpoint(VersionService versionService) {
        this.versionService = versionService;
    }

    @POST
    @Path("{versionId}")
    @ResponseStatus(200)
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "updateVersion", summary = "Update a project version")
    public VersionResponse update(@PathParam("projectId") long projectId,
                                  @PathParam("versionId") long versionId,
                                  @Valid UpdateVersionRequest request,
                                  @Context SecurityContext securityContext) {
        return versionService.update(projectId, versionId, request, securityContext.getUserPrincipal().getName());
    }
}

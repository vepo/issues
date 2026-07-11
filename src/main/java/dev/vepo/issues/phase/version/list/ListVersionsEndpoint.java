package dev.vepo.issues.phase.version.list;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.phase.VersionPaths;
import dev.vepo.issues.phase.VersionResponse;
import dev.vepo.issues.phase.VersionService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
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

@Path(VersionPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Version")
public class ListVersionsEndpoint {

    private final VersionService versionService;

    @Inject
    public ListVersionsEndpoint(VersionService versionService) {
        this.versionService = versionService;
    }

    @GET
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "listVersions", summary = "List project versions")
    public List<VersionResponse> list(@PathParam("projectId") long projectId,
                                      @Context SecurityContext securityContext) {
        return versionService.listByProject(projectId, securityContext.getUserPrincipal().getName());
    }
}

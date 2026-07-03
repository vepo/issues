package dev.vepo.issues.phase.version.create;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.phase.CreateVersionRequest;
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
import jakarta.ws.rs.core.MediaType;

@Path(VersionPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Version")
public class CreateVersionEndpoint {

    private final VersionService versionService;

    @Inject
    public CreateVersionEndpoint(VersionService versionService) {
        this.versionService = versionService;
    }

    @POST
    @ResponseStatus(201)
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "createVersion", summary = "Create a project version")
    public VersionResponse create(@PathParam("projectId") long projectId, @Valid CreateVersionRequest request) {
        return versionService.create(projectId, request);
    }
}

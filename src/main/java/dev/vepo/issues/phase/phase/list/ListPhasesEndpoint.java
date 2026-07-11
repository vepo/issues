package dev.vepo.issues.phase.phase.list;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.phase.PhasePaths;
import dev.vepo.issues.phase.PhaseResponse;
import dev.vepo.issues.phase.PhaseService;
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

@Path(PhasePaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Phase")
public class ListPhasesEndpoint {

    private final PhaseService phaseService;

    @Inject
    public ListPhasesEndpoint(PhaseService phaseService) {
        this.phaseService = phaseService;
    }

    @GET
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "listPhases", summary = "List project phases")
    public List<PhaseResponse> list(@PathParam("projectId") long projectId,
                                    @Context SecurityContext securityContext) {
        return phaseService.listByProject(projectId, securityContext.getUserPrincipal().getName());
    }
}

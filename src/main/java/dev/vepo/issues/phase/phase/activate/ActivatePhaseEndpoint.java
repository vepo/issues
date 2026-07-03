package dev.vepo.issues.phase.phase.activate;

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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(PhasePaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Phase")
public class ActivatePhaseEndpoint {

    private final PhaseService phaseService;

    @Inject
    public ActivatePhaseEndpoint(PhaseService phaseService) {
        this.phaseService = phaseService;
    }

    @POST
    @Path("{phaseId}/activate")
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "activatePhase", summary = "Activate a planned phase")
    public PhaseResponse activate(@PathParam("projectId") long projectId,
                                  @PathParam("phaseId") long phaseId,
                                  @Context SecurityContext securityContext) {
        return phaseService.activate(projectId, phaseId, securityContext.getUserPrincipal().getName());
    }
}

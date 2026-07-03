package dev.vepo.issues.phase.phase.update;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.phase.PhasePaths;
import dev.vepo.issues.phase.PhaseResponse;
import dev.vepo.issues.phase.PhaseService;
import dev.vepo.issues.phase.UpdatePhaseRequest;
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

@Path(PhasePaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Phase")
public class UpdatePhaseEndpoint {

    private final PhaseService phaseService;

    @Inject
    public UpdatePhaseEndpoint(PhaseService phaseService) {
        this.phaseService = phaseService;
    }

    @POST
    @Path("{phaseId}")
    @ResponseStatus(200)
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "updatePhase", summary = "Update a project phase")
    public PhaseResponse update(@PathParam("projectId") long projectId,
                                @PathParam("phaseId") long phaseId,
                                @Valid UpdatePhaseRequest request) {
        return phaseService.update(projectId, phaseId, request);
    }
}

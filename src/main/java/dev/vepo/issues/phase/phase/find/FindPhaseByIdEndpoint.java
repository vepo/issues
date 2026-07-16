package dev.vepo.issues.phase.phase.find;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.phase.PhasePaths;
import dev.vepo.issues.phase.PhaseResponse;
import dev.vepo.issues.phase.PhaseService;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
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
public class FindPhaseByIdEndpoint {

    private final PhaseService phaseService;

    @Inject
    public FindPhaseByIdEndpoint(PhaseService phaseService) {
        this.phaseService = phaseService;
    }

    @GET
    @Path("{phaseId}")
    @PermitAll
    @Operation(operationId = "findPhaseById", summary = "Find project phase by ID")
    public PhaseResponse findById(@PathParam("projectId") long projectId,
                                  @PathParam("phaseId") long phaseId,
                                  @Context SecurityContext securityContext) {
        return phaseService.findById(projectId, phaseId, optionalUsername(securityContext).orElse(null));
    }

    private static java.util.Optional<String> optionalUsername(SecurityContext securityContext) {
        var principal = securityContext.getUserPrincipal();
        return principal == null ? java.util.Optional.empty() : java.util.Optional.of(principal.getName());
    }

}

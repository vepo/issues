package dev.vepo.issues.phase.phase.list;

import java.util.List;

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
public class ListPhasesEndpoint {

    private final PhaseService phaseService;

    @Inject
    public ListPhasesEndpoint(PhaseService phaseService) {
        this.phaseService = phaseService;
    }

    @GET
    @PermitAll
    @Operation(operationId = "listPhases", summary = "List project phases")
    public List<PhaseResponse> list(@PathParam("projectId") long projectId,
                                    @Context SecurityContext securityContext) {
        return phaseService.listByProject(projectId, optionalUsername(securityContext).orElse(null));
    }

    private static java.util.Optional<String> optionalUsername(SecurityContext securityContext) {
        var principal = securityContext.getUserPrincipal();
        return principal == null ? java.util.Optional.empty() : java.util.Optional.of(principal.getName());
    }

}

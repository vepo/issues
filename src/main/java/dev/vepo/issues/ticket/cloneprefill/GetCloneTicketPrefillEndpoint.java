package dev.vepo.issues.ticket.cloneprefill;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.ticket.CloneTicketPrefillResponse;
import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
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

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class GetCloneTicketPrefillEndpoint {

    private final CloneTicketPrefillService cloneTicketPrefillService;

    @Inject
    public GetCloneTicketPrefillEndpoint(CloneTicketPrefillService cloneTicketPrefillService) {
        this.cloneTicketPrefillService = cloneTicketPrefillService;
    }

    @GET
    @Path("/{sourceId:[0-9]+}/clone-prefill")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "getCloneTicketPrefill", summary = "Get target-aware ticket clone defaults")
    public CloneTicketPrefillResponse getPrefill(@PathParam("sourceId") long sourceId,
                                                 @QueryParam("targetProjectId") long targetProjectId,
                                                 @Context SecurityContext securityContext) {
        return cloneTicketPrefillService.getPrefill(sourceId,
                                                    targetProjectId,
                                                    securityContext.getUserPrincipal().getName());
    }
}

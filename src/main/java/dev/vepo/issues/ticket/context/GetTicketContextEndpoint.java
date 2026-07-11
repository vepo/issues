package dev.vepo.issues.ticket.context;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class GetTicketContextEndpoint {

    private final TicketContextService ticketContextService;

    @Inject
    public GetTicketContextEndpoint(TicketContextService ticketContextService) {
        this.ticketContextService = ticketContextService;
    }

    @GET
    @Path("/{id:[0-9]+}/context")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "getTicketContext", summary = "Get composite ticket context for agents")
    public TicketContextResponse getContext(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        return ticketContextService.getContext(id, securityContext.getUserPrincipal().getName());
    }
}

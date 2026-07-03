package dev.vepo.issues.ticket.restore;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.ticket.TicketService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class RestoreTicketEndpoint {

    private final TicketService ticketService;

    @Inject
    public RestoreTicketEndpoint(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @POST
    @Path("/{id}/restore")
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "restoreTicket", summary = "Restore a soft-deleted ticket")
    public TicketResponse restore(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        return ticketService.restore(id, securityContext.getUserPrincipal().getName());
    }
}

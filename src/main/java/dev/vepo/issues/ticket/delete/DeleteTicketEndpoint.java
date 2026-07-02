package dev.vepo.issues.ticket.delete;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.ticket.TicketService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class DeleteTicketEndpoint {

    private final TicketService ticketService;

    @Inject
    public DeleteTicketEndpoint(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "deleteTicket", summary = "Delete a ticket")
    public void delete(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        ticketService.delete(id, securityContext.getUserPrincipal().getName());
    }
}

package dev.vepo.issues.ticket.subscribe;

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
import dev.vepo.issues.ticket.SubscribeTicketRequest;
import dev.vepo.issues.ticket.TicketExpandedResponse;

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class SubscribeTicketEndpoint {

    private final TicketService ticketService;

    @Inject
    public SubscribeTicketEndpoint(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PUT
    @Path("/{id}/subscribe")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "subscribeTicket", summary = "Subscribe to ticket")
    public TicketExpandedResponse subscribe(@PathParam("id") Long id, SubscribeTicketRequest request) {
        return ticketService.subscribe(id, request);
    }
}

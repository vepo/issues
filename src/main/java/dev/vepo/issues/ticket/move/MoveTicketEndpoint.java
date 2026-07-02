package dev.vepo.issues.ticket.move;

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
import dev.vepo.issues.ticket.MoveTicketRequest;
import dev.vepo.issues.ticket.TicketResponse;
import jakarta.validation.Valid;

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class MoveTicketEndpoint {

    private final TicketService ticketService;

    @Inject
    public MoveTicketEndpoint(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @POST
    @Path("/{id}/move")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "moveTicket", summary = "Move ticket to new status")
    public TicketResponse moveTicket(@PathParam("id") Long id, @Valid MoveTicketRequest request, @Context SecurityContext securityContext) {
        return ticketService.moveTicket(id, request, securityContext.getUserPrincipal().getName());
    }
}

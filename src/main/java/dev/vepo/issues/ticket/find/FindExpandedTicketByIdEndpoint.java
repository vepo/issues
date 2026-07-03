package dev.vepo.issues.ticket.find;

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
import dev.vepo.issues.ticket.TicketExpandedResponse;

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class FindExpandedTicketByIdEndpoint {

    private final TicketService ticketService;

    @Inject
    public FindExpandedTicketByIdEndpoint(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GET
    @Path("/{id:[0-9]+}/expanded")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "findExpandedTicketById", summary = "Find expanded ticket by numeric ID")
    public TicketExpandedResponse findExpandedById(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        return ticketService.findExpandedById(id, securityContext.getUserPrincipal().getName());
    }
}

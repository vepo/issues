package dev.vepo.issues.ticket.search;

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
import java.util.List;
import dev.vepo.issues.ticket.TicketResponse;

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class SearchTicketsEndpoint {

    private final TicketService ticketService;

    @Inject
    public SearchTicketsEndpoint(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GET
    @Path("search")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "searchTickets", summary = "Search tickets")
    public List<TicketResponse> search(@QueryParam("term") String term,
                                       @QueryParam("statusId") @DefaultValue("-1") long statusId) {
        return ticketService.search(term, statusId);
    }
}

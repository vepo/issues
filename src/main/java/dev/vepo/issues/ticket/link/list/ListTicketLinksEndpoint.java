package dev.vepo.issues.ticket.link.list;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.ticket.link.TicketLinkResponse;
import dev.vepo.issues.ticket.link.TicketLinkService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
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

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class ListTicketLinksEndpoint {

    private final TicketLinkService ticketLinkService;

    @Inject
    public ListTicketLinksEndpoint(TicketLinkService ticketLinkService) {
        this.ticketLinkService = ticketLinkService;
    }

    @GET
    @Path("/{id}/links")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "listTicketLinks", summary = "List ticket links")
    public List<TicketLinkResponse> listLinks(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        return ticketLinkService.listLinks(id, securityContext.getUserPrincipal().getName());
    }
}

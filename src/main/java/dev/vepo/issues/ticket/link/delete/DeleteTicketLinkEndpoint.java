package dev.vepo.issues.ticket.link.delete;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.ticket.link.TicketLinkService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
public class DeleteTicketLinkEndpoint {

    private final TicketLinkService ticketLinkService;

    @Inject
    public DeleteTicketLinkEndpoint(TicketLinkService ticketLinkService) {
        this.ticketLinkService = ticketLinkService;
    }

    @DELETE
    @Path("/{id}/links/{linkId}")
    @ResponseStatus(204)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "deleteTicketLink", summary = "Delete a ticket link")
    public void deleteLink(@PathParam("id") Long id,
                           @PathParam("linkId") Long linkId,
                           @Context SecurityContext securityContext) {
        ticketLinkService.deleteLink(id, linkId, securityContext.getUserPrincipal().getName());
    }
}

package dev.vepo.issues.ticket.link.create;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.ticket.link.CreateTicketLinkRequest;
import dev.vepo.issues.ticket.link.TicketLinkResponse;
import dev.vepo.issues.ticket.link.TicketLinkService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
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
public class CreateTicketLinkEndpoint {

    private final TicketLinkService ticketLinkService;

    @Inject
    public CreateTicketLinkEndpoint(TicketLinkService ticketLinkService) {
        this.ticketLinkService = ticketLinkService;
    }

    @POST
    @Path("/{id}/links")
    @ResponseStatus(201)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "createTicketLink", summary = "Create a ticket link")
    public TicketLinkResponse createLink(@PathParam("id") Long id,
                                         @Valid CreateTicketLinkRequest request,
                                         @Context SecurityContext securityContext) {
        return ticketLinkService.createLink(id, request, securityContext.getUserPrincipal().getName());
    }
}

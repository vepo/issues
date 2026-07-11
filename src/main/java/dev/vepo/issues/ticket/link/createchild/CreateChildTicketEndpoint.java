package dev.vepo.issues.ticket.link.createchild;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.ticket.link.CreateChildTicketRequest;
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
public class CreateChildTicketEndpoint {

    private final TicketLinkService ticketLinkService;

    @Inject
    public CreateChildTicketEndpoint(TicketLinkService ticketLinkService) {
        this.ticketLinkService = ticketLinkService;
    }

    @POST
    @Path("/{id}/children")
    @ResponseStatus(201)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "createChildTicket", summary = "Create a child ticket under an Epic")
    public TicketResponse createChild(@PathParam("id") Long id,
                                      @Valid CreateChildTicketRequest request,
                                      @Context SecurityContext securityContext) {
        return ticketLinkService.createChild(id, request, securityContext.getUserPrincipal().getName());
    }
}

package dev.vepo.issues.ticket.comments.add;

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
import dev.vepo.issues.ticket.comments.CommentRequest;
import dev.vepo.issues.ticket.comments.CommentResponse;
import org.jboss.resteasy.reactive.ResponseStatus;

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class AddCommentEndpoint {

    private final TicketService ticketService;

    @Inject
    public AddCommentEndpoint(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @POST
    @Path("/{id}/comments")
    @ResponseStatus(201)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "addComment", summary = "Add ticket comment")
    public CommentResponse addComment(@PathParam("id") Long id, CommentRequest request, @Context SecurityContext securityContext) {
        return ticketService.addComment(id, request, securityContext.getUserPrincipal().getName());
    }
}

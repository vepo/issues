package dev.vepo.issues.ticket.comments.list;

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
import dev.vepo.issues.ticket.comments.CommentResponse;

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class ListCommentsEndpoint {

    private final TicketService ticketService;

    @Inject
    public ListCommentsEndpoint(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GET
    @Path("/{id}/comments")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "listComments", summary = "List ticket comments")
    public List<CommentResponse> listComments(@PathParam("id") Long id) {
        return ticketService.listComments(id);
    }
}

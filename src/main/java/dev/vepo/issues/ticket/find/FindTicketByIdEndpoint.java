package dev.vepo.issues.ticket.find;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.ticket.TicketService;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import dev.vepo.issues.ticket.TicketResponse;

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class FindTicketByIdEndpoint {

    private final TicketService ticketService;

    @Inject
    public FindTicketByIdEndpoint(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GET
    @Path("/{id}")
    @PermitAll
    @Operation(operationId = "findTicketById", summary = "Find ticket by ID")
    public TicketResponse findById(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        return ticketService.findById(id, optionalUsername(securityContext));
    }

    private static java.util.Optional<String> optionalUsername(SecurityContext securityContext) {
        var principal = securityContext.getUserPrincipal();
        return principal == null ? java.util.Optional.empty() : java.util.Optional.of(principal.getName());
    }
}

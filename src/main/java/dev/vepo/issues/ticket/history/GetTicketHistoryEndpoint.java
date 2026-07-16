package dev.vepo.issues.ticket.history;

import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.ticket.TicketHistoryResponse;
import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.ticket.TicketService;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
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
public class GetTicketHistoryEndpoint {

    private final TicketService ticketService;

    @Inject
    public GetTicketHistoryEndpoint(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GET
    @Path("/{id}/history")
    @PermitAll
    @Operation(operationId = "getTicketHistory", summary = "Get ticket history")
    public List<TicketHistoryResponse> getHistory(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        return ticketService.getHistory(id, optionalUsername(securityContext));
    }

    private static Optional<String> optionalUsername(SecurityContext securityContext) {
        var principal = securityContext.getUserPrincipal();
        return principal == null ? Optional.empty() : Optional.of(principal.getName());
    }
}

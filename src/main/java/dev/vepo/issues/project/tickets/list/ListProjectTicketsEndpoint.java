package dev.vepo.issues.project.tickets.list;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.project.ProjectPaths;
import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.ticket.TicketService;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(ProjectPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Project")
public class ListProjectTicketsEndpoint {

    private final TicketService ticketService;

    @Inject
    public ListProjectTicketsEndpoint(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GET
    @Path("{projectId}/tickets")
    @PermitAll
    @Operation(operationId = "listProjectTickets", summary = "List tickets by project")
    public List<TicketResponse> findByProjectId(@PathParam("projectId") long projectId,
                                                @Context SecurityContext securityContext) {
        return ticketService.findByProjectId(projectId, optionalUsername(securityContext));
    }

    private static java.util.Optional<String> optionalUsername(SecurityContext securityContext) {
        var principal = securityContext.getUserPrincipal();
        return principal == null ? java.util.Optional.empty() : java.util.Optional.of(principal.getName());
    }

}

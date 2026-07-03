package dev.vepo.issues.ticket.csvimport.execute;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.ticket.csvimport.ImportTicketsResponse;
import dev.vepo.issues.ticket.csvimport.TicketImportService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
@DenyAll
@Tag(name = "Ticket")
public class ExecuteGlobalTicketImportEndpoint {

    private final TicketImportService ticketImportService;

    @Inject
    public ExecuteGlobalTicketImportEndpoint(TicketImportService ticketImportService) {
        this.ticketImportService = ticketImportService;
    }

    @POST
    @Path("/import/{importId}/execute")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "executeGlobalTicketImport", summary = "Execute global ticket import from stored CSV rows")
    public ImportTicketsResponse execute(@PathParam("importId") long importId,
                                         @Context SecurityContext securityContext) {
        return ticketImportService.execute(null, importId, securityContext.getUserPrincipal().getName());
    }
}

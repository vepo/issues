package dev.vepo.issues.ticket.csvimport.correct;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.ticket.csvimport.CorrectImportRowRequest;
import dev.vepo.issues.ticket.csvimport.ImportRowValidationResponse;
import dev.vepo.issues.ticket.csvimport.TicketImportService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class CorrectGlobalTicketImportRowEndpoint {

    private final TicketImportService ticketImportService;

    @Inject
    public CorrectGlobalTicketImportRowEndpoint(TicketImportService ticketImportService) {
        this.ticketImportService = ticketImportService;
    }

    @PUT
    @Path("/import/{importId}/rows/{rowId}")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "correctGlobalTicketImportRow", summary = "Correct project or status on a global import preview row")
    public ImportRowValidationResponse correctRow(@PathParam("importId") long importId,
                                                  @PathParam("rowId") long rowId,
                                                  @Valid CorrectImportRowRequest request) {
        return ticketImportService.correctRow(null, importId, rowId, request);
    }
}

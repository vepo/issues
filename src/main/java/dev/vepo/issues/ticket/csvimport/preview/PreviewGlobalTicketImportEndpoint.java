package dev.vepo.issues.ticket.csvimport.preview;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.ticket.csvimport.PreviewTicketImportResponse;
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
import jakarta.ws.rs.core.MediaType;

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class PreviewGlobalTicketImportEndpoint {

    private final TicketImportService ticketImportService;

    @Inject
    public PreviewGlobalTicketImportEndpoint(TicketImportService ticketImportService) {
        this.ticketImportService = ticketImportService;
    }

    @POST
    @Path("/import/{importId}/preview")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "previewGlobalTicketImport", summary = "Preview and validate global ticket CSV import")
    public PreviewTicketImportResponse preview(@PathParam("importId") long importId) {
        return ticketImportService.preview(null, importId);
    }
}

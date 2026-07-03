package dev.vepo.issues.ticket.csvimport.upload;

import java.io.InputStream;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.ticket.csvimport.TicketImportService;
import dev.vepo.issues.ticket.csvimport.TicketImportUploadResponse;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class UploadGlobalTicketImportEndpoint {

    private final TicketImportService ticketImportService;

    @Inject
    public UploadGlobalTicketImportEndpoint(TicketImportService ticketImportService) {
        this.ticketImportService = ticketImportService;
    }

    @POST
    @Path("/import/upload")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "uploadGlobalTicketImport", summary = "Upload CSV file for global ticket import")
    public TicketImportUploadResponse upload(@HeaderParam("X-File-Name") String fileName,
                                             InputStream content,
                                             @Context SecurityContext securityContext) {
        var resolvedName = fileName == null || fileName.isBlank() ? "import.csv" : fileName;
        return ticketImportService.upload(null, resolvedName, content, securityContext.getUserPrincipal().getName());
    }
}

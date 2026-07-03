package dev.vepo.issues.ticket.csvimport.upload;

import java.io.InputStream;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.project.ProjectPaths;
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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(ProjectPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class UploadTicketImportEndpoint {

    private final TicketImportService ticketImportService;

    @Inject
    public UploadTicketImportEndpoint(TicketImportService ticketImportService) {
        this.ticketImportService = ticketImportService;
    }

    @POST
    @Path("{projectId}/tickets/import/upload")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "uploadTicketImport", summary = "Upload CSV file for ticket import")
    public TicketImportUploadResponse upload(@PathParam("projectId") long projectId,
                                             @HeaderParam("X-File-Name") String fileName,
                                             InputStream content,
                                             @Context SecurityContext securityContext) {
        var resolvedName = fileName == null || fileName.isBlank() ? "import.csv" : fileName;
        return ticketImportService.upload(projectId, resolvedName, content, securityContext.getUserPrincipal().getName());
    }
}

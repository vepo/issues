package dev.vepo.issues.ticket.csvimport.upload;

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
public class CompleteTicketImportUploadEndpoint {

    private final TicketImportService ticketImportService;

    @Inject
    public CompleteTicketImportUploadEndpoint(TicketImportService ticketImportService) {
        this.ticketImportService = ticketImportService;
    }

    @POST
    @Path("{projectId}/tickets/import/{importId}/upload/complete")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "completeTicketImportUpload", summary = "Complete chunked CSV upload and parse for ticket import")
    public TicketImportUploadResponse complete(@PathParam("projectId") long projectId,
                                               @PathParam("importId") long importId,
                                               @Context SecurityContext securityContext) {
        return ticketImportService.completeUpload(projectId, importId, securityContext.getUserPrincipal().getName());
    }
}

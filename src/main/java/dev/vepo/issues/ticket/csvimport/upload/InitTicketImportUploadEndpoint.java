package dev.vepo.issues.ticket.csvimport.upload;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.project.ProjectPaths;
import dev.vepo.issues.ticket.csvimport.InitTicketImportUploadRequest;
import dev.vepo.issues.ticket.csvimport.InitTicketImportUploadResponse;
import dev.vepo.issues.ticket.csvimport.TicketImportService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
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
public class InitTicketImportUploadEndpoint {

    private final TicketImportService ticketImportService;

    @Inject
    public InitTicketImportUploadEndpoint(TicketImportService ticketImportService) {
        this.ticketImportService = ticketImportService;
    }

    @POST
    @Path("{projectId}/tickets/import/upload/init")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "initTicketImportUpload", summary = "Start chunked CSV upload for ticket import")
    public InitTicketImportUploadResponse init(@PathParam("projectId") long projectId,
                                               @Valid InitTicketImportUploadRequest request,
                                               @Context SecurityContext securityContext) {
        return ticketImportService.initUpload(projectId, request, securityContext.getUserPrincipal().getName());
    }
}

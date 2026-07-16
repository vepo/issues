package dev.vepo.issues.ticket.csvimport.upload;

import java.io.InputStream;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.project.ProjectPaths;
import dev.vepo.issues.ticket.csvimport.TicketImportService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path(ProjectPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class UploadTicketImportPartEndpoint {

    private final TicketImportService ticketImportService;

    @Inject
    public UploadTicketImportPartEndpoint(TicketImportService ticketImportService) {
        this.ticketImportService = ticketImportService;
    }

    @PUT
    @Path("{projectId}/tickets/import/{importId}/upload/parts/{partIndex}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "uploadTicketImportPart", summary = "Upload one CSV chunk for ticket import")
    public Response uploadPart(@PathParam("projectId") long projectId,
                               @PathParam("importId") long importId,
                               @PathParam("partIndex") int partIndex,
                               InputStream content,
                               @Context SecurityContext securityContext) {
        ticketImportService.acceptPart(projectId, importId, partIndex, content, securityContext.getUserPrincipal().getName());
        return Response.noContent().build();
    }
}

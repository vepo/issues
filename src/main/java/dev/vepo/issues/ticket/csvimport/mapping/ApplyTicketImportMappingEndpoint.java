package dev.vepo.issues.ticket.csvimport.mapping;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.project.ProjectPaths;
import dev.vepo.issues.ticket.csvimport.ApplyColumnMappingRequest;
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
import jakarta.ws.rs.core.Response;

@Path(ProjectPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class ApplyTicketImportMappingEndpoint {

    private final TicketImportService ticketImportService;

    @Inject
    public ApplyTicketImportMappingEndpoint(TicketImportService ticketImportService) {
        this.ticketImportService = ticketImportService;
    }

    @PUT
    @Path("{projectId}/tickets/import/{importId}/mapping")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "applyTicketImportMapping", summary = "Apply column mapping to a stored CSV import")
    public Response applyMapping(@PathParam("projectId") long projectId,
                                 @PathParam("importId") long importId,
                                 @Valid ApplyColumnMappingRequest request) {
        ticketImportService.applyMapping(projectId, importId, request.mapping());
        return Response.noContent().build();
    }
}

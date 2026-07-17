package dev.vepo.issues.ticket.export;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path(TicketPaths.BASE)
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class ExportTicketsEndpoint {

    private final TicketExportService exportService;
    private final TicketExportResponseFactory responseFactory;
    private final ProjectAccessService projectAccessService;

    @Inject
    public ExportTicketsEndpoint(TicketExportService exportService,
                                 TicketExportResponseFactory responseFactory,
                                 ProjectAccessService projectAccessService) {
        this.exportService = exportService;
        this.responseFactory = responseFactory;
        this.projectAccessService = projectAccessService;
    }

    @POST
    @Path("/export")
    @RolesAllowed({ Role.USER_ROLE, Role.PROJECT_MANAGER_ROLE, Role.ADMIN_ROLE })
    @Operation(operationId = "exportTickets", summary = "Export tickets")
    public Response export(ExportTicketsRequest request, @Context SecurityContext securityContext) {
        var requestingUser = projectAccessService.requireUser(securityContext.getUserPrincipal().getName());
        try {
            var rows = exportService.prepare(request, requestingUser);
            return responseFactory.create(request, rows);
        } catch (TicketExportLimitExceededException exception) {
            throw new ClientErrorException(exception.getMessage(), 413);
        }
    }
}

package dev.vepo.issues.ticket.attachments.list;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.ticket.attachments.AttachmentResponse;
import dev.vepo.issues.ticket.attachments.AttachmentService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class ListTicketAttachmentsEndpoint {

    private final AttachmentService attachmentService;

    @Inject
    public ListTicketAttachmentsEndpoint(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GET
    @Path("/{id}/attachments")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "listTicketAttachments", summary = "List ticket attachments")
    public List<AttachmentResponse> list(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        return attachmentService.list(id, securityContext.getUserPrincipal().getName());
    }
}

package dev.vepo.issues.ticket.attachments.download;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.ticket.attachments.AttachmentService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path(TicketPaths.BASE)
@ApplicationScoped
@DenyAll
@Tag(name = "Ticket")
public class DownloadTicketAttachmentEndpoint {

    private final AttachmentService attachmentService;

    @Inject
    public DownloadTicketAttachmentEndpoint(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GET
    @Path("/{id}/attachments/{attachmentId}")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "downloadTicketAttachment", summary = "Download a ticket attachment")
    public Response download(@PathParam("id") Long id,
                             @PathParam("attachmentId") Long attachmentId,
                             @Context SecurityContext securityContext) {
        return attachmentService.download(id, attachmentId, securityContext.getUserPrincipal().getName());
    }
}

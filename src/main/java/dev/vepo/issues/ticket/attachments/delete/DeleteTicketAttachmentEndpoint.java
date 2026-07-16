package dev.vepo.issues.ticket.attachments.delete;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.ticket.attachments.AttachmentService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
public class DeleteTicketAttachmentEndpoint {

    private final AttachmentService attachmentService;

    @Inject
    public DeleteTicketAttachmentEndpoint(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @DELETE
    @Path("/{id}/attachments/{attachmentId}")
    @ResponseStatus(204)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "deleteTicketAttachment", summary = "Delete a ticket attachment")
    public void delete(@PathParam("id") Long id,
                       @PathParam("attachmentId") Long attachmentId,
                       @Context SecurityContext securityContext) {
        attachmentService.delete(id, attachmentId, securityContext.getUserPrincipal().getName());
    }
}

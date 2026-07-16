package dev.vepo.issues.ticket.attachments.upload;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.ticket.attachments.AttachmentResponse;
import dev.vepo.issues.ticket.attachments.AttachmentService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class UploadTicketAttachmentEndpoint {

    private final AttachmentService attachmentService;

    @Inject
    public UploadTicketAttachmentEndpoint(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @POST
    @Path("/{id}/attachments")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @ResponseStatus(201)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "uploadTicketAttachment", summary = "Upload a ticket attachment")
    public AttachmentResponse upload(@PathParam("id") Long id,
                                     @RestForm("file") FileUpload file,
                                     @Context SecurityContext securityContext) {
        return attachmentService.upload(id, file, securityContext.getUserPrincipal().getName());
    }
}

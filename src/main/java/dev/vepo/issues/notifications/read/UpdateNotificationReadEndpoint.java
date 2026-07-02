package dev.vepo.issues.notifications.read;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.notifications.NotificationPaths;
import dev.vepo.issues.notifications.NotificationService;
import dev.vepo.issues.notifications.UpdateNotificationStatusReadRequest;
import dev.vepo.issues.notifications.UserNotificationEvent;
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
import jakarta.ws.rs.core.MediaType;

@DenyAll
@ApplicationScoped
@Path(NotificationPaths.BASE)
@Tag(name = "Notification")
public class UpdateNotificationReadEndpoint {

    private final NotificationService notificationService;

    @Inject
    public UpdateNotificationReadEndpoint(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @POST
    @Path("{id}/read")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "updateNotificationRead", summary = "Mark notification as read")
    public UserNotificationEvent updateReadStatus(@PathParam("id") long notificationId,
                                                  UpdateNotificationStatusReadRequest request) {
        return notificationService.markAsRead(notificationId, request);
    }
}

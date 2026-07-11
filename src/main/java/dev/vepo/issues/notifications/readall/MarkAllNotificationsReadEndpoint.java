package dev.vepo.issues.notifications.readall;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.notifications.MarkAllNotificationsReadResponse;
import dev.vepo.issues.notifications.NotificationPaths;
import dev.vepo.issues.notifications.NotificationService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@DenyAll
@ApplicationScoped
@Path(NotificationPaths.BASE)
@Tag(name = "Notification")
public class MarkAllNotificationsReadEndpoint {

    private final NotificationService notificationService;

    @Inject
    public MarkAllNotificationsReadEndpoint(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @POST
    @Path("read-all")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "markAllNotificationsRead", summary = "Mark all notifications as read for the current user")
    public MarkAllNotificationsReadResponse markAllRead(@Context SecurityContext context) {
        return notificationService.markAllAsRead(context.getUserPrincipal().getName());
    }
}

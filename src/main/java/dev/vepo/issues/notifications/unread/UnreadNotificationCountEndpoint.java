package dev.vepo.issues.notifications.unread;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.notifications.NotificationPaths;
import dev.vepo.issues.notifications.NotificationService;
import dev.vepo.issues.notifications.UnreadNotificationCountResponse;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@DenyAll
@ApplicationScoped
@Path(NotificationPaths.BASE)
@Tag(name = "Notification")
public class UnreadNotificationCountEndpoint {

    private final NotificationService notificationService;

    @Inject
    public UnreadNotificationCountEndpoint(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GET
    @Path("unread-count")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "getUnreadNotificationCount", summary = "Count unread notifications for the current user")
    public UnreadNotificationCountResponse unreadCount(@Context SecurityContext context) {
        return notificationService.countUnread(context.getUserPrincipal().getName());
    }
}

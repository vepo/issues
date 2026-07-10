package dev.vepo.issues.notifications.list;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.notifications.NotificationPageResponse;
import dev.vepo.issues.notifications.NotificationPaths;
import dev.vepo.issues.notifications.NotificationService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@DenyAll
@ApplicationScoped
@Path(NotificationPaths.BASE)
@Tag(name = "Notification")
public class ListNotificationsEndpoint {

    private final NotificationService notificationService;

    @Inject
    public ListNotificationsEndpoint(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "listNotifications", summary = "List notifications for the current user")
    public NotificationPageResponse list(@Context SecurityContext context,
                                         @QueryParam("page") @DefaultValue("0") int page,
                                         @QueryParam("size") @DefaultValue("20") int size) {
        return notificationService.list(context.getUserPrincipal().getName(), page, size);
    }
}

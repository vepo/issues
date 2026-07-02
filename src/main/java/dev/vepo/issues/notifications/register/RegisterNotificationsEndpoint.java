package dev.vepo.issues.notifications.register;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.notifications.NotificationChannelRegistry;
import dev.vepo.issues.notifications.NotificationPaths;
import dev.vepo.issues.notifications.NotificationRepository;
import dev.vepo.issues.notifications.UserNotificationEvent;
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
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

@DenyAll
@ApplicationScoped
@Path(NotificationPaths.BASE)
@Tag(name = "Notification")
public class RegisterNotificationsEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(RegisterNotificationsEndpoint.class);

    private final NotificationChannelRegistry channelRegistry;
    private final NotificationRepository notificationRepository;
    private final Sse sse;

    @Inject
    public RegisterNotificationsEndpoint(NotificationChannelRegistry channelRegistry,
                                         NotificationRepository notificationRepository,
                                         Sse sse) {
        this.channelRegistry = channelRegistry;
        this.notificationRepository = notificationRepository;
        this.sse = sse;
    }

    @GET
    @Path("register")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "registerNotifications", summary = "Register SSE notification channel")
    public void register(@Context SseEventSink eventSink, @Context SecurityContext context) {
        logger.info("Register channel! principal={}", context.getUserPrincipal().getName());
        channelRegistry.register(context.getUserPrincipal().getName(), eventSink);
        notificationRepository.findAll(context.getUserPrincipal().getName())
                              .forEach(notification -> eventSink.send(sse.newEventBuilder()
                                                                         .id("ticket-change")
                                                                         .mediaType(MediaType.APPLICATION_JSON_TYPE)
                                                                         .data(UserNotificationEvent.load(notification))
                                                                         .build()));
    }
}

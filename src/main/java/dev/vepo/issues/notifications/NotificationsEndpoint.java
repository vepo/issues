package dev.vepo.issues.notifications;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

@DenyAll
@ApplicationScoped
@Path("notifications")
public class NotificationsEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsEndpoint.class);

    private final Map<String, SseEventSink> openChannels;

    private final TicketRepository ticketRepository;
    private final NotificationRepository notificationRepository;
    private final Sse sse;

    @Inject
    public NotificationsEndpoint(@Context Sse sse, TicketRepository ticketRepository,
                                 NotificationRepository notificationRepository) {
        openChannels = new HashMap<>();
        this.sse = sse;
        this.ticketRepository = ticketRepository;
        this.notificationRepository = notificationRepository;
    }

    @GET
    @Path("register")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    public void register(@Context SseEventSink eventSink, @Context SecurityContext context) {
        logger.info("Register channel! principal={}", context.getUserPrincipal().getName());
        openChannels.put(context.getUserPrincipal().getName(), eventSink);
        this.notificationRepository.findAll(context.getUserPrincipal().getName())
                                   .forEach(notification -> eventSink.send(sse.newEventBuilder()
                                                                              .id("ticket-change")
                                                                              .mediaType(MediaType.APPLICATION_JSON_TYPE)
                                                                              .data(UserNotificationEvent.load(notification))
                                                                              .build()));
    }

    @POST
    @Transactional
    @Path("{id}/read")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    public UserNotificationEvent updateReadStatus(@PathParam("id") long notificationId, UpdateNotificationStatusReadRequest request) {
        var noti = this.notificationRepository.findById(notificationId).orElseThrow(() -> new NotFoundException());
        noti.setRead(request.read());
        return UserNotificationEvent.load(notificationRepository.save(noti));
    }

    @Transactional
    @PermitAll
    public void listenNotifications(@ObservesAsync NotificationEvent event) {
        logger.info("Processing CDI Event! event={}", event);
        this.ticketRepository.findById(event.ticketId())
                             .ifPresentOrElse(ticket -> ticket.getSubscribers()
                                                              .forEach(sucriber -> {
                                                                  var notification = new Notification(event.type(), sucriber, ticket, event.content());
                                                                  this.notificationRepository.save(notification);
                                                                  this.openChannels.computeIfPresent(sucriber.getUsername(),
                                                                                                     (username, sink) -> {
                                                                                                         if (!sink.isClosed()) {
                                                                                                             sink.send(sse.newEventBuilder()
                                                                                                                          .id("ticket-change")
                                                                                                                          .mediaType(MediaType.APPLICATION_JSON_TYPE)
                                                                                                                          .data(UserNotificationEvent.load(notification))
                                                                                                                          .build());
                                                                                                             return sink;
                                                                                                         }
                                                                                                         return null;
                                                                                                     });
                                                              }),
                                              () -> logger.error("Ticket not found!!! notification={}", event));
    }
}

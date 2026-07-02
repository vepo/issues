package dev.vepo.issues.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.ticket.TicketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;

@ApplicationScoped
public class NotificationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);

    private final TicketRepository ticketRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationChannelRegistry channelRegistry;
    private final Sse sse;

    @Inject
    public NotificationEventListener(TicketRepository ticketRepository,
                                     NotificationRepository notificationRepository,
                                     NotificationChannelRegistry channelRegistry,
                                     Sse sse) {
        this.ticketRepository = ticketRepository;
        this.notificationRepository = notificationRepository;
        this.channelRegistry = channelRegistry;
        this.sse = sse;
    }

    @Transactional
    public void listenNotifications(@ObservesAsync NotificationEvent event) {
        logger.info("Processing CDI Event! event={}", event);
        ticketRepository.findById(event.ticketId())
                        .ifPresentOrElse(ticket -> ticket.getSubscribers()
                                                         .forEach(subscriber -> {
                                                             var notification = new Notification(event.type(),
                                                                                                 subscriber,
                                                                                                 ticket,
                                                                                                 event.content());
                                                             notificationRepository.save(notification);
                                                             channelRegistry.computeIfPresent(subscriber.getUsername(),
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

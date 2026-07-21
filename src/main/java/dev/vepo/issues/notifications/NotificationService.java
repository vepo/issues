package dev.vepo.issues.notifications;

import java.util.Set;

import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;

@ApplicationScoped
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationChannelRegistry channelRegistry;
    private final Sse sse;

    @Inject
    public NotificationService(NotificationRepository notificationRepository,
                               NotificationChannelRegistry channelRegistry,
                               Sse sse) {
        this.notificationRepository = notificationRepository;
        this.channelRegistry = channelRegistry;
        this.sse = sse;
    }

    @Transactional
    public void notifyMentions(Ticket ticket, User author, Set<User> mentionedUsers, String content) {
        mentionedUsers.stream()
                      .filter(mentioned -> !mentioned.getId().equals(author.getId()))
                      .forEach(mentioned -> deliverDirect("comment-mention", mentioned, ticket, content));
    }

    @Transactional
    public void notifyDueDateReminder(Ticket ticket, User assignee, String content) {
        deliverDirect("due-date-reminder", assignee, ticket, content);
    }

    private void deliverDirect(String type, User recipient, Ticket ticket, String content) {
        var notification = notificationRepository.save(new Notification(type, recipient, ticket, content));
        channelRegistry.computeIfPresent(recipient.getUsername(), (username, sink) -> {
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
    }

    public NotificationPageResponse list(String username, int page, int size) {
        var safePage = Math.max(page, 0);
        var safeSize = size < 1 || size > 50 ? 20 : size;
        var total = notificationRepository.countByUsername(username);
        var items = notificationRepository.findPage(username, safePage, safeSize)
                                          .stream()
                                          .map(UserNotificationEvent::load)
                                          .toList();
        var hasMore = (long) (safePage + 1) * safeSize < total;
        return new NotificationPageResponse(items, total, safePage, safeSize, hasMore);
    }

    @Transactional
    public UserNotificationEvent markAsRead(long notificationId, UpdateNotificationStatusReadRequest request,
                                            String username) {
        var notification = notificationRepository.findByIdAndUsername(notificationId, username)
                                                 .orElseThrow(NotFoundException::new);
        notification.setRead(request.read());
        return UserNotificationEvent.load(notificationRepository.save(notification));
    }

    public UnreadNotificationCountResponse countUnread(String username) {
        return new UnreadNotificationCountResponse(notificationRepository.countUnreadByUsername(username));
    }

    @Transactional
    public MarkAllNotificationsReadResponse markAllAsRead(String username) {
        var updated = notificationRepository.markAllReadByUsername(username);
        return new MarkAllNotificationsReadResponse(updated, 0);
    }
}

package dev.vepo.issues.notifications;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Inject
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
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
    public UserNotificationEvent markAsRead(long notificationId, UpdateNotificationStatusReadRequest request) {
        var notification = notificationRepository.findById(notificationId)
                                                 .orElseThrow(NotFoundException::new);
        notification.setRead(request.read());
        return UserNotificationEvent.load(notificationRepository.save(notification));
    }
}

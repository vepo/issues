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

    @Transactional
    public UserNotificationEvent markAsRead(long notificationId, UpdateNotificationStatusReadRequest request) {
        var notification = notificationRepository.findById(notificationId)
                                                 .orElseThrow(NotFoundException::new);
        notification.setRead(request.read());
        return UserNotificationEvent.load(notificationRepository.save(notification));
    }
}

package dev.vepo.issues.notifications;

public record UserNotificationEvent(long id, String type, boolean read, String content, long ticketId, long timestamp) {

    public static UserNotificationEvent load(Notification notification) {
        return new UserNotificationEvent(notification.getId(),
                                         notification.getType(),
                                         notification.isRead(),
                                         notification.getContent(),
                                         notification.getReffer().getId(),
                                         notification.getCreatedAt()
                                                     .toEpochMilli());
    }

}

package dev.vepo.issues.notifications;

import java.util.List;

public record NotificationPageResponse(List<UserNotificationEvent> items, long total, int page, int size,
                                       boolean hasMore) {}

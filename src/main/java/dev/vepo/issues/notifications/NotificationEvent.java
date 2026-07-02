package dev.vepo.issues.notifications;

public record NotificationEvent(long ticketId, String type, String content) {

}

package dev.vepo.issues.notifications.readall;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.categories.Category;
import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.notifications.Notification;
import dev.vepo.issues.notifications.NotificationRepository;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class MarkAllNotificationsReadEndpointTest {

    @Test
    void shouldRejectUnauthenticatedMarkAll() {
        given().accept(ContentType.JSON)
               .when()
               .post("/api/notifications/read-all")
               .then()
               .statusCode(401);
    }

    @Test
    void shouldMarkAllUnreadNotificationsForCurrentUserAndReturnUpdatedCount() {
        var recipient = Given.randomUser();
        var auth = authenticate(recipient);
        var ticket = createTicket();

        seedNotification(recipient, ticket.id(), "UNREAD_A", "Unread A", Instant.parse("2026-07-10T10:00:00Z"), false);
        seedNotification(recipient, ticket.id(), "UNREAD_B", "Unread B", Instant.parse("2026-07-10T11:00:00Z"), false);
        seedNotification(recipient, ticket.id(), "READ", "Already read", Instant.parse("2026-07-10T12:00:00Z"), true);

        given().header(auth)
               .accept(ContentType.JSON)
               .when()
               .post("/api/notifications/read-all")
               .then()
               .statusCode(200)
               .body("updated", equalTo(2))
               .body("unread", equalTo(0));
    }

    @Test
    void shouldReturnZeroUpdatedWhenNoUnreadRemain() {
        var recipient = Given.randomUser();
        var auth = authenticate(recipient);
        var ticket = createTicket();

        seedNotification(recipient, ticket.id(), "UNREAD", "Unread", Instant.parse("2026-07-10T10:00:00Z"), false);

        given().header(auth)
               .accept(ContentType.JSON)
               .when()
               .post("/api/notifications/read-all")
               .then()
               .statusCode(200)
               .body("updated", equalTo(1))
               .body("unread", equalTo(0));

        given().header(auth)
               .accept(ContentType.JSON)
               .when()
               .post("/api/notifications/read-all")
               .then()
               .statusCode(200)
               .body("updated", equalTo(0))
               .body("unread", equalTo(0));
    }

    @Test
    void shouldNotMarkNotificationsBelongingToAnotherUser() {
        var recipient = Given.randomUser();
        var otherUser = Given.randomUser();
        var auth = authenticate(recipient);
        var ticket = createTicket();

        seedNotification(recipient, ticket.id(), "MINE", "Mine unread", Instant.parse("2026-07-10T10:00:00Z"), false);
        var otherNotificationId = seedNotification(otherUser, ticket.id(), "THEIRS", "Theirs unread",
                                                   Instant.parse("2026-07-10T11:00:00Z"), false);

        given().header(auth)
               .accept(ContentType.JSON)
               .when()
               .post("/api/notifications/read-all")
               .then()
               .statusCode(200)
               .body("updated", equalTo(1))
               .body("unread", equalTo(0));

        var otherStillUnread = Given.transaction(() -> {
            var notification = Given.inject(NotificationRepository.class)
                                    .findById(otherNotificationId)
                                    .orElseThrow();
            return !notification.isRead();
        });
        assertThat(otherStillUnread).isTrue();
    }

    private static Header authenticate(User user) {
        var response = given().contentType(ContentType.JSON)
                              .body("""
                                    {
                                        "email": "%s",
                                        "password": "password"
                                    }
                                    """.formatted(user.getEmail()))
                              .when()
                              .post("/api/auth/login")
                              .then()
                              .statusCode(200)
                              .extract()
                              .jsonPath();
        return new Header("Authorization", "Bearer " + response.getString("token"));
    }

    private static TicketResponse createTicket() {
        var category = Given.transaction(() -> Given.inject(CategoryRepository.class)
                                                    .save(new Category("Notify" + UUID.randomUUID(), "blue")));
        var project = Given.simpleProject();
        return given().contentType(ContentType.JSON)
                      .header(Given.authenticatedProjectManager())
                      .body("""
                            {
                                "title": "Notification ticket %s",
                                "description": "Seed ticket for mark-all read tests.",
                                "projectId": %d,
                                "categoryId": %d
                            }
                            """.formatted(UUID.randomUUID(), project.id(), category.getId()))
                      .when()
                      .post("/api/tickets")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(TicketResponse.class);
    }

    private static long seedNotification(User recipient, long ticketId, String type, String content, Instant createdAt,
                                         boolean read) {
        return Given.transaction(() -> {
            var ticket = Given.inject(TicketRepository.class)
                              .findById(ticketId)
                              .orElseThrow();
            var user = Given.inject(UserRepository.class)
                            .findById(recipient.getId())
                            .orElseThrow();
            var notification = new Notification(type, user, ticket, content);
            notification.setCreatedAt(createdAt);
            notification.setRead(read);
            return Given.inject(NotificationRepository.class)
                        .save(notification)
                        .getId();
        });
    }
}

package dev.vepo.issues.notifications.list;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

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
class ListNotificationsEndpointTest {

    @Test
    void shouldRejectUnauthenticatedListNotifications() {
        given().accept(ContentType.JSON)
               .when()
               .get("/api/notifications?page=0&size=20")
               .then()
               .statusCode(401);
    }

    @Test
    void shouldReturnEmptyNotificationPageWhenUserHasNoNotifications() {
        var recipient = Given.randomUser();
        var auth = authenticate(recipient);

        given().header(auth)
               .accept(ContentType.JSON)
               .when()
               .get("/api/notifications?page=0&size=20")
               .then()
               .statusCode(200)
               .body("items", hasSize(0))
               .body("total", equalTo(0))
               .body("page", equalTo(0))
               .body("size", equalTo(20))
               .body("hasMore", equalTo(false));
    }

    @Test
    void shouldReturnNotificationPageWithItemFieldsWhenUserHasNotifications() {
        var recipient = Given.randomUser();
        var auth = authenticate(recipient);
        var ticket = createTicket();
        var notificationId = seedNotification(recipient, ticket.id(), "TICKET_UPDATED", "Ticket was updated",
                                              Instant.parse("2026-07-10T12:00:00Z"));

        var json = given().header(auth)
                          .accept(ContentType.JSON)
                          .when()
                          .get("/api/notifications?page=0&size=20")
                          .then()
                          .statusCode(200)
                          .extract()
                          .jsonPath();

        assertThat(json.getInt("total")).isEqualTo(1);
        assertThat(json.getInt("page")).isEqualTo(0);
        assertThat(json.getInt("size")).isEqualTo(20);
        assertThat(json.getBoolean("hasMore")).isFalse();
        assertThat(json.getList("items")).hasSize(1);
        assertThat(json.getLong("items[0].id")).isEqualTo(notificationId);
        assertThat(json.getString("items[0].type")).isEqualTo("TICKET_UPDATED");
        assertThat(json.getBoolean("items[0].read")).isFalse();
        assertThat(json.getString("items[0].content")).isEqualTo("Ticket was updated");
        assertThat(json.getLong("items[0].ticketId")).isEqualTo(ticket.id());
        assertThat(json.getLong("items[0].timestamp")).isEqualTo(Instant.parse("2026-07-10T12:00:00Z")
                                                                        .toEpochMilli());
    }

    @Test
    void shouldOrderNotificationsNewestFirstByCreatedAtThenId() {
        var recipient = Given.randomUser();
        var auth = authenticate(recipient);
        var ticket = createTicket();

        var olderId = seedNotification(recipient, ticket.id(), "OLDER", "Older notification",
                                       Instant.parse("2026-07-10T10:00:00Z"));
        var newerId = seedNotification(recipient, ticket.id(), "NEWER", "Newer notification",
                                       Instant.parse("2026-07-10T11:00:00Z"));
        var newestSameSecondId = seedNotification(recipient, ticket.id(), "NEWEST", "Newest same second",
                                                  Instant.parse("2026-07-10T11:00:00Z"));

        var ids = given().header(auth)
                         .accept(ContentType.JSON)
                         .when()
                         .get("/api/notifications?page=0&size=20")
                         .then()
                         .statusCode(200)
                         .extract()
                         .jsonPath()
                         .getList("items.id", Long.class);

        assertThat(ids).containsExactly(newestSameSecondId, newerId, olderId);
    }

    @Test
    void shouldPaginateNotificationsAndReportHasMore() {
        var recipient = Given.randomUser();
        var auth = authenticate(recipient);
        var ticket = createTicket();

        seedNotification(recipient, ticket.id(), "N1", "First", Instant.parse("2026-07-10T10:00:00Z"));
        seedNotification(recipient, ticket.id(), "N2", "Second", Instant.parse("2026-07-10T11:00:00Z"));
        seedNotification(recipient, ticket.id(), "N3", "Third", Instant.parse("2026-07-10T12:00:00Z"));

        var page0 = given().header(auth)
                           .accept(ContentType.JSON)
                           .when()
                           .get("/api/notifications?page=0&size=2")
                           .then()
                           .statusCode(200)
                           .extract()
                           .jsonPath();

        assertThat(page0.getList("items")).hasSize(2);
        assertThat(page0.getLong("total")).isEqualTo(3);
        assertThat(page0.getInt("page")).isEqualTo(0);
        assertThat(page0.getInt("size")).isEqualTo(2);
        assertThat(page0.getBoolean("hasMore")).isTrue();
        assertThat(page0.getList("items.content", String.class)).containsExactly("Third", "Second");

        var page1 = given().header(auth)
                           .accept(ContentType.JSON)
                           .when()
                           .get("/api/notifications?page=1&size=2")
                           .then()
                           .statusCode(200)
                           .extract()
                           .jsonPath();

        assertThat(page1.getList("items")).hasSize(1);
        assertThat(page1.getLong("total")).isEqualTo(3);
        assertThat(page1.getInt("page")).isEqualTo(1);
        assertThat(page1.getInt("size")).isEqualTo(2);
        assertThat(page1.getBoolean("hasMore")).isFalse();
        assertThat(page1.getList("items.content", String.class)).containsExactly("First");
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
                                "description": "Seed ticket for notification list tests.",
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

    private static long seedNotification(User recipient, long ticketId, String type, String content, Instant createdAt) {
        return Given.transaction(() -> {
            var ticket = Given.inject(TicketRepository.class)
                              .findById(ticketId)
                              .orElseThrow();
            var user = Given.inject(UserRepository.class)
                            .findById(recipient.getId())
                            .orElseThrow();
            var notification = new Notification(type, user, ticket, content);
            notification.setCreatedAt(createdAt);
            return Given.inject(NotificationRepository.class)
                        .save(notification)
                        .getId();
        });
    }
}

package dev.vepo.issues.ticket.comments.add;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.auth.AuthResponse;
import dev.vepo.issues.ticket.TicketTestFixtures;
import dev.vepo.issues.user.User;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class AddCommentEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("It should be possible to add a comment to a ticket")
    void shouldAddCommentTest() {
        var loggedUser = given().header(fixtures.userAuthenticatedHeader())
                                .accept(ContentType.JSON)
                                .when()
                                .get("/api/auth/me")
                                .then()
                                .statusCode(200)
                                .extract()
                                .as(AuthResponse.class);
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "content": "Another test comment"
                     }""")
               .post("/api/tickets/" + fixtures.ticket().id() + "/comments")
               .then()
               .statusCode(201)
               .body("content", equalTo("Another test comment"))
               .body("author.id", equalTo((int) loggedUser.id()))
               .body("author.name", equalTo(loggedUser.name()))
               .body("author.email", equalTo(loggedUser.email()));
    }

    @Test
    @DisplayName("It should not be possible to add a comment to a non-existent ticket")
    void shouldNotAddCommentToInvalidTicketTest() {
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "content": "Invalid ticket comment"
                     }""")
               .post("/api/tickets/9999/comments")
               .then()
               .statusCode(404)
               .body("message", equalTo("Ticket does not found! ticketId=9999"));
    }

    @Test
    @DisplayName("It should notify a mentioned project member even when they are not a ticket subscriber")
    void shouldNotifyMentionedProjectMemberTest() {
        var mentioned = Given.randomUser();
        Given.addProjectMember(fixtures.project().id(), mentioned.getId());

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "content": "Thanks @%s for reviewing!"
                     }""".formatted(mentioned.getUsername()))
               .post("/api/tickets/" + fixtures.ticket().id() + "/comments")
               .then()
               .statusCode(201);

        assertThat(mentionNotificationsFor(mentioned)).hasSize(1);
    }

    @Test
    @DisplayName("It should not notify the author when they mention themselves")
    void shouldNotNotifySelfMentionTest() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "content": "Note to self @project-manager"
                     }""")
               .post("/api/tickets/" + fixtures.ticket().id() + "/comments")
               .then()
               .statusCode(201);

        assertThat(mentionNotificationsFor(fixtures.pmAuthenticatedHeader())).isEmpty();
    }

    @Test
    @DisplayName("It should ignore unknown usernames and email-like text when parsing mentions")
    void shouldIgnoreUnknownMentionsAndEmailLikeTextTest() {
        var bystander = Given.randomUser();
        Given.addProjectMember(fixtures.project().id(), bystander.getId());

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "content": "Contact pm@issues.vepo.dev or ping @doesnotexist999"
                     }""")
               .post("/api/tickets/" + fixtures.ticket().id() + "/comments")
               .then()
               .statusCode(201);

        assertThat(mentionNotificationsFor(bystander)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private java.util.List<Map<String, Object>> mentionNotificationsFor(User user) {
        return mentionNotificationsFor(authenticate(user));
    }

    @SuppressWarnings("unchecked")
    private java.util.List<Map<String, Object>> mentionNotificationsFor(Header authentication) {
        var items = (java.util.List<Map<String, Object>>) (java.util.List<?>) given().header(authentication)
                                                                                     .accept(ContentType.JSON)
                                                                                     .when()
                                                                                     .get("/api/notifications?page=0&size=20")
                                                                                     .then()
                                                                                     .statusCode(200)
                                                                                     .extract()
                                                                                     .jsonPath()
                                                                                     .getList("items", Map.class);
        return items.stream()
                    .filter(item -> "comment-mention".equals(item.get("type"))
                            && ((Number) item.get("ticketId")).longValue() == fixtures.ticket().id())
                    .toList();
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
}

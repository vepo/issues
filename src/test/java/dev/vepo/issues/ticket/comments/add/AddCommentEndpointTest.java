package dev.vepo.issues.ticket.comments.add;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.auth.AuthResponse;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

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
}

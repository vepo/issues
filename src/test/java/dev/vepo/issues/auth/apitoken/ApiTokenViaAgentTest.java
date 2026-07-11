package dev.vepo.issues.auth.apitoken;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class ApiTokenViaAgentTest {

    @Test
    @DisplayName("Should set viaAgent on comment when using personal API token")
    void shouldSetViaAgentOnCommentWhenUsingPersonalApiToken() {
        var fixtures = TicketTestFixtures.create();
        var secret = createPersonalApiToken(fixtures.userAuthenticatedHeader(), "via-agent-comment");

        given().header(new Header("Authorization", "Bearer " + secret))
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "content": "Comment from agent"
                     }
                     """)
               .when()
               .post("/api/tickets/{id}/comments", fixtures.ticket().id())
               .then()
               .statusCode(201)
               .body("content", is("Comment from agent"))
               .body("viaAgent", is(true));
    }

    @Test
    @DisplayName("Should set viaAgent on history when moving ticket with personal API token")
    void shouldSetViaAgentOnHistoryWhenMovingTicketWithPersonalApiToken() {
        var fixtures = TicketTestFixtures.create();
        var secret = createPersonalApiToken(fixtures.userAuthenticatedHeader(), "via-agent-move");
        var inProgress = Given.status("In Progress");

        given().header(new Header("Authorization", "Bearer " + secret))
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "to": %d
                     }
                     """.formatted(inProgress.getId()))
               .when()
               .post("/api/tickets/{id}/move", fixtures.ticket().id())
               .then()
               .statusCode(200);

        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/{id}/history", fixtures.ticket().id())
               .then()
               .statusCode(200)
               .body("find { it.action == 'STATUS_CHANGED' }.viaAgent", is(true));
    }

    @Test
    @DisplayName("Should leave viaAgent false when commenting with JWT")
    void shouldLeaveViaAgentFalseWhenCommentingWithJwt() {
        var fixtures = TicketTestFixtures.create();

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "content": "Human comment"
                     }
                     """)
               .when()
               .post("/api/tickets/{id}/comments", fixtures.ticket().id())
               .then()
               .statusCode(201)
               .body("viaAgent", is(false));
    }

    private static String createPersonalApiToken(Header auth, String name) {
        return given().header(auth)
                      .contentType(ContentType.JSON)
                      .body("""
                            {
                                "name": "%s"
                            }
                            """.formatted(name))
                      .when()
                      .post("/api/account/api-tokens")
                      .then()
                      .statusCode(201)
                      .extract()
                      .path("token");
    }
}

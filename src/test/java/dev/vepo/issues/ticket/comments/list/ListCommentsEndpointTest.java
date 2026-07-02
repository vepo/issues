package dev.vepo.issues.ticket.comments.list;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ListCommentsEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("It should be possible to list comments for a ticket")
    void shouldListCommentsTest() {
        // First add a comment
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "content": "This is a test comment"
                     }""")
               .post("/api/tickets/" + fixtures.ticket().id() + "/comments")
               .then()
               .statusCode(201);

        // Then list comments
        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/" + fixtures.ticket().id() + "/comments")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0))
               .body("[0].content", equalTo("This is a test comment"));
    }
}

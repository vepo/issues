package dev.vepo.issues.ticket.move;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class MoveTicketEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("It should be psosible to move ticket to a new status")
    void moveTicketTest() {
        var inProgress = Given.status("In Progress");
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "to": %d
                     }""".formatted(inProgress.getId()))
               .post("/api/tickets/" + fixtures.ticket().id() + "/move")
               .then()
               .statusCode(200)
               .body("status", is(inProgress.getId().intValue()));
    }

    @Test
    @DisplayName("It should not be possible to move ticket to invalid status")
    void shouldNotMoveToInvalidStatusTest() {
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "to": 9999
                     }""")
               .post("/api/tickets/" + fixtures.ticket().id() + "/move")
               .then()
               .statusCode(400)
               .body("message", equalTo("Stage not defined in project! stageId=9999"));
    }
}

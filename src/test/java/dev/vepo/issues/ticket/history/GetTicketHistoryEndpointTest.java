package dev.vepo.issues.ticket.history;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class GetTicketHistoryEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("It should be possible to get ticket history")
    void shouldGetTicketHistoryTest() {
        // First make some changes to generate history
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "Updated Title for History",
                         "description": "Updated description for history",
                         "categoryId": %d
                     }""".formatted(fixtures.feature().getId()))
               .post("/api/tickets/" + fixtures.ticket().id())
               .then()
               .statusCode(200);

        // Get history
        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/" + fixtures.ticket().id() + "/history")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0));
    }
}

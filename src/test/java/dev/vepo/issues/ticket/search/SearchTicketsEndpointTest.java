package dev.vepo.issues.ticket.search;

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
class SearchTicketsEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("It should be possible to search tickets by term on title or description")
    void shouldSearchTicketsByTermTest() {
        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/search?term=" + fixtures.ticket().title())
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0))
               .body("[0].title", equalTo(fixtures.ticket().title()));
        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/search?term=" + fixtures.ticket().description())
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0))
               .body("[0].description", equalTo(fixtures.ticket().description()));
    }
}

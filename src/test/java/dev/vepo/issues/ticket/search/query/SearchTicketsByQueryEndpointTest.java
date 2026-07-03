package dev.vepo.issues.ticket.search.query;

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
class SearchTicketsByQueryEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("Should search tickets using query language")
    void shouldSearchTicketsByQuery() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                         "query": "title ~ \\"%s\\""
                     }""".formatted(fixtures.ticket().title()))
               .when()
               .post("/api/tickets/search/query")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0))
               .body("[0].title", equalTo(fixtures.ticket().title()));
    }

    @Test
    @DisplayName("Should return bad request for invalid query")
    void shouldRejectInvalidQuery() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .body("""
                     {
                         "query": "project ="
                     }""")
               .when()
               .post("/api/tickets/search/query")
               .then()
               .statusCode(400);
    }
}

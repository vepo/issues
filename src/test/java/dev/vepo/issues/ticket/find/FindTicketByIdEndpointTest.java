package dev.vepo.issues.ticket.find;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class FindTicketByIdEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("It should be possible to retrieve a ticket by its ID")
    void shouldRetrieveTicketByIdTest() {
        var ticket = fixtures.ticket();
        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/{id}", ticket.id())
               .then()
               .statusCode(200)
               .body("id", equalTo((int) ticket.id()))
               .body("title", equalTo(ticket.title()))
               .body("description", equalTo(ticket.description()))
               .body("status", equalTo((int) ticket.status()))
               .body("project", equalTo((int) ticket.project()))
               .body("category", equalTo((int) ticket.category()))
               .body("author", equalTo((int) ticket.author()));
    }
}

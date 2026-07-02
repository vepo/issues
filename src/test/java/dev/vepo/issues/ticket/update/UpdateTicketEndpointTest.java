package dev.vepo.issues.ticket.update;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class UpdateTicketEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("It should be possible to update a ticket")
    void updateTicketTest() {
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "New Ticket Title",
                         "description": "New Ticket description",
                         "categoryId": %d
                     }""".formatted(fixtures.feature().getId()))
               .post("/api/tickets/" + fixtures.ticket().id())
               .then()
               .statusCode(200)
               .body("title", equalTo("New Ticket Title"))
               .body("description", equalTo("New Ticket description"))
               .body("category", is(fixtures.feature().getId().intValue()));
    }

    @Test
    @DisplayName("It should not be possible to update ticket with invalid category ID")
    void shouldNotUpdateTicketWithInvalidCategoryIdTest() {
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "Invalid Category Ticket",
                         "description": "This ticket has an invalid category ID.",
                         "categoryId": 9999
                     }""")
               .post("/api/tickets/" + fixtures.ticket().id())
               .then()
               .statusCode(404)
               .body("message", equalTo("Category does not found! categoryId=9999"));
    }
}

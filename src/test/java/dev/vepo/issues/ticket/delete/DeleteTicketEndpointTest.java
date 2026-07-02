package dev.vepo.issues.ticket.delete;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class DeleteTicketEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("Only admin or project manager should be able to delete a ticket")
    void shouldDeleteTicketTest() {
        // First create a ticket to delete
        var ticketToDelete = given().header(fixtures.pmAuthenticatedHeader())
                                    .contentType(ContentType.JSON)
                                    .accept(ContentType.JSON)
                                    .when()
                                    .body("""
                                          {
                                              "title": "Ticket to delete",
                                              "description": "This ticket will be deleted.",
                                              "projectId": %d,
                                              "categoryId": %d
                                          }""".formatted(fixtures.project().id(), fixtures.bug().getId()))
                                    .post("/api/tickets")
                                    .then()
                                    .statusCode(201)
                                    .extract()
                                    .as(TicketResponse.class);

        // Delete the ticket as PM
        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .delete("/api/tickets/" + ticketToDelete.id())
               .then()
               .statusCode(204);

        // Verify ticket is deleted
        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/" + ticketToDelete.id())
               .then()
               .statusCode(404);
    }

    @Test
    @DisplayName("Regular user should not be able to delete a ticket")
    void regularUserShouldNotDeleteTicketTest() {
        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .delete("/api/tickets/" + fixtures.ticket().id())
               .then()
               .statusCode(403);
    }
}

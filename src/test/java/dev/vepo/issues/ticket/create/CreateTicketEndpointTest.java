package dev.vepo.issues.ticket.create;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class CreateTicketEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("It should be possible to create a new ticket")
    void shouldCreateNewTicketTest() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "New Ticket",
                         "description": "This is a new ticket.",
                         "projectId": %d,
                         "categoryId": %d
                     }""".formatted(fixtures.project().id(), fixtures.bug().getId()))
               .post("/api/tickets")
               .then()
               .statusCode(201)
               .body("title", equalTo("New Ticket"))
               .body("description", equalTo("This is a new ticket."))
               .body("project", equalTo((int) fixtures.project().id()))
               .body("category", equalTo(fixtures.bug().getId().intValue()))
               .body("author", equalTo((int) Given.userIdByEmail("pm@issues.vepo.dev")))
               .body("status", equalTo((int) fixtures.allStatuses().stream()
                                                     .filter(status -> status.name().equals("TODO"))
                                                     .findFirst()
                                                     .orElseThrow(() -> new IllegalStateException("TODO status not found")).id()));
    }

    @Test
    @DisplayName("It should not be possible to create a ticket with an invalid project ID")
    void shouldNotCreateTicketWithInvalidProjectIdTest() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "Invalid Project Ticket",
                         "description": "This ticket has an invalid project ID.",
                         "projectId": 9999,
                         "categoryId": %d
                     }""".formatted(fixtures.bug().getId()))
               .post("/api/tickets")
               .then()
               .statusCode(404)
               .body("message", equalTo("Project does not found! projectId=9999"));
    }
}

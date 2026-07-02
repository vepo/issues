package dev.vepo.issues.ticket.find;

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
class FindExpandedTicketByIdEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("It should return a expanded entity with ")
    void shouldReturnExpandedTicketInformationTest() {
        var ticket = fixtures.ticket();
        var project = fixtures.project();
        var category = Given.category(ticket.category());
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/{id}/expanded", ticket.id())
               .then()
               .statusCode(200)
               .body("id", equalTo((int) ticket.id()))
               .body("title", equalTo(ticket.title()))
               .body("description", equalTo(ticket.description()))
               .body("category", equalTo(category.getName()))
               .body("project.id", equalTo((int) project.id()))
               .body("project.name", equalTo(project.name()));
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/{id}/expanded", ticket.identifier())
               .then()
               .statusCode(200)
               .body("id", equalTo((int) ticket.id()))
               .body("title", equalTo(ticket.title()))
               .body("description", equalTo(ticket.description()))
               .body("category", equalTo(category.getName()))
               .body("project.id", equalTo((int) project.id()))
               .body("project.name", equalTo(project.name()));
    }
}

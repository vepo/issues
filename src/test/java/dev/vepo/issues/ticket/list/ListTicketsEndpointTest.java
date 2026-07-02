package dev.vepo.issues.ticket.list;

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
class ListTicketsEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("No authenticated user should be able to list tickets")
    void noAuthenticatedUserShouldListTicketsTest() {
        given().when()
               .accept(ContentType.JSON)
               .get("/api/tickets")
               .then()
               .statusCode(401);
    }

    @Test
    @DisplayName("Only authenticated users should be able to list tickets")
    void onlyAuthenticatedUsersShouldListTicketsTest() {
        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0));
    }

    @Test
    @DisplayName("It should be possible to list tickets by status")
    void shouldListTicketsByStatusTest() {
        var todo = fixtures.allStatuses().stream()
                           .filter(status -> status.name().equals("TODO"))
                           .findFirst()
                           .orElseThrow(() -> new IllegalStateException("TODO status not found"));
        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets?status=TODO")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0))
               .body("[0].status", equalTo((int) todo.id()));
        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets?status=" + todo.id())
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0))
               .body("[0].status", equalTo((int) todo.id()));
    }
}

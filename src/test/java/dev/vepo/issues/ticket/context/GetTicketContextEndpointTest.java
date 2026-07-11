package dev.vepo.issues.ticket.context;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class GetTicketContextEndpointTest {

    @Test
    @DisplayName("Should return ticket context with ticket, transitions and custom fields")
    void shouldReturnTicketContextWhenAuthenticated() {
        var fixtures = TicketTestFixtures.create();

        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/{id}/context", fixtures.ticket().id())
               .then()
               .statusCode(200)
               .body("$", hasKey("ticket"))
               .body("$", hasKey("availableTransitions"))
               .body("$", hasKey("customFields"))
               .body("ticket.id", is((int) fixtures.ticket().id()))
               .body("ticket.identifier", is(fixtures.ticket().identifier()))
               .body("availableTransitions", notNullValue())
               .body("customFields", notNullValue());
    }

    @Test
    @DisplayName("Should reject unauthenticated ticket context request")
    void shouldRejectUnauthenticatedTicketContextRequest() {
        var fixtures = TicketTestFixtures.create();

        given().accept(ContentType.JSON)
               .when()
               .get("/api/tickets/{id}/context", fixtures.ticket().id())
               .then()
               .statusCode(401);
    }
}

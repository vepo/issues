package dev.vepo.issues.ticket.restore;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class RestoreTicketEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("Project manager should restore a soft-deleted ticket")
    void shouldRestoreDeletedTicketWhenProjectManager() {
        var ticket = createTicketToDelete();

        given().header(fixtures.pmAuthenticatedHeader())
               .when()
               .delete("/api/tickets/" + ticket.id())
               .then()
               .statusCode(204);

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .post("/api/tickets/" + ticket.id() + "/restore")
               .then()
               .statusCode(200)
               .body("id", equalTo((int) ticket.id()))
               .body("deleted", equalTo(false));

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/" + ticket.id())
               .then()
               .statusCode(200)
               .body("deleted", equalTo(false));
    }

    @Test
    @DisplayName("Regular user should not restore a deleted ticket")
    void regularUserShouldNotRestoreDeletedTicket() {
        var ticket = createTicketToDelete();

        given().header(fixtures.pmAuthenticatedHeader())
               .when()
               .delete("/api/tickets/" + ticket.id())
               .then()
               .statusCode(204);

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .post("/api/tickets/" + ticket.id() + "/restore")
               .then()
               .statusCode(403);
    }

    @Test
    @DisplayName("Restore should fail when ticket is not deleted")
    void shouldRejectRestoreWhenTicketIsNotDeleted() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .post("/api/tickets/" + fixtures.ticket().id() + "/restore")
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Restore should log RESTORED history entry")
    void shouldLogRestoredHistoryEntry() {
        var ticket = createTicketToDelete();

        given().header(fixtures.pmAuthenticatedHeader())
               .when()
               .delete("/api/tickets/" + ticket.id())
               .then()
               .statusCode(204);

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .post("/api/tickets/" + ticket.id() + "/restore")
               .then()
               .statusCode(200);

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/tickets/" + ticket.id() + "/history")
               .then()
               .statusCode(200)
               .body("find { it.action == 'RESTORED' }", notNullValue());
    }

    private TicketResponse createTicketToDelete() {
        return given().header(fixtures.pmAuthenticatedHeader())
                      .contentType(ContentType.JSON)
                      .accept(ContentType.JSON)
                      .when()
                      .body("""
                            {
                                "title": "Ticket to restore",
                                "description": "This ticket will be deleted and restored.",
                                "projectId": %d,
                                "categoryId": %d
                            }""".formatted(fixtures.project().id(), fixtures.bug().getId()))
                      .post("/api/tickets")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(TicketResponse.class);
    }
}

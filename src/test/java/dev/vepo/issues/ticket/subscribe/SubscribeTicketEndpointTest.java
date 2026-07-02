package dev.vepo.issues.ticket.subscribe;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.stream.IntStream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.ticket.SubscribeTicketRequest;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class SubscribeTicketEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("It should subscribe/unsubscribe to tickets")
    void subsrcibeTest() {
        var authUser = Given.me(fixtures.userAuthenticatedHeader());
        // subscribe to ticket
        // execute the operation again to verify its idempotent
        IntStream.range(0, 10)
                 .forEach(count -> {
                     given().header(fixtures.userAuthenticatedHeader())
                            .accept(ContentType.JSON)
                            .contentType(ContentType.JSON)
                            .when()
                            .body(new SubscribeTicketRequest(authUser.id()))
                            .put("/api/tickets/" + fixtures.ticket().id() + "/subscribe")
                            .then()
                            .statusCode(200);
                     // check if user is subscribed
                     given().header(fixtures.userAuthenticatedHeader())
                            .contentType(ContentType.JSON)
                            .accept(ContentType.JSON)
                            .when()
                            .get("/api/tickets/{id}/expanded", fixtures.ticket().id())
                            .then()
                            .statusCode(200)
                            .body("id", equalTo((int) fixtures.ticket().id()))
                            .body("subscribers.size()", Matchers.equalTo(1))
                            .body("subscribers[0].username", equalTo(authUser.username()));
                 });
        // unsubscribe
        // execute the operation again to verify its idempotent
        IntStream.range(0, 10)
                 .forEach(count -> {
                     given().header(fixtures.userAuthenticatedHeader())
                            .accept(ContentType.JSON)
                            .contentType(ContentType.JSON)
                            .when()
                            .delete("/api/tickets/" + fixtures.ticket().id() + "/subscribe/" + authUser.id())
                            .then()
                            .statusCode(200);
                     given().header(fixtures.userAuthenticatedHeader())
                            .contentType(ContentType.JSON)
                            .accept(ContentType.JSON)
                            .when()
                            .get("/api/tickets/{id}/expanded", fixtures.ticket().id())
                            .then()
                            .statusCode(200)
                            .body("id", equalTo((int) fixtures.ticket().id()))
                            .body("subscribers.size()", Matchers.equalTo(0));
                 });
    }
}

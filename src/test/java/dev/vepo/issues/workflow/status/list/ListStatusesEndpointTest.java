package dev.vepo.issues.workflow.status.list;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ListStatusesEndpointTest {

    @Test
    void shouldListStatusesWhenAuthenticated() {
        given().header(Given.authenticatedUser())
               .accept(ContentType.JSON)
               .when()
               .get("/api/status")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThanOrEqualTo(0));
    }

    @Test
    void shouldRejectUnauthenticatedListStatuses() {
        given().accept(ContentType.JSON)
               .when()
               .get("/api/status")
               .then()
               .statusCode(401);
    }
}

package dev.vepo.issues.workflow.update;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class UpdateWorkflowEndpointTest {

    @Test
    void shouldUpdateWorkflowNameAndTransitions() {
        var created = given().header(Given.authenticatedProjectManager())
                             .when()
                             .contentType("application/json")
                             .body("""
                                   {
                                       "name": "Update Target Flow",
                                       "statuses": ["Open", "Closed"],
                                       "start": "Open",
                                       "transitions": [{"from": "Open", "to": "Closed"}]
                                   }""")
                             .post("/api/workflows")
                             .then()
                             .statusCode(201)
                             .extract()
                             .path("id");

        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "Updated Flow Name",
                         "start": "Closed",
                         "transitions": [{"from": "Closed", "to": "Open"}, {"from": "Open", "to": "Closed"}]
                     }""")
               .put("/api/workflows/" + created)
               .then()
               .statusCode(200)
               .body("name", is("Updated Flow Name"))
               .body("start", is("Closed"))
               .body("transitions.size()", is(2));
    }

    @Test
    @DisplayName("Should reject update when transition references unknown status")
    void shouldRejectUpdateWhenTransitionReferencesUnknownStatus() {
        var created = given().header(Given.authenticatedProjectManager())
                             .when()
                             .contentType("application/json")
                             .body("""
                                   {
                                       "name": "Invalid Update Flow",
                                       "statuses": ["A", "B"],
                                       "start": "A",
                                       "transitions": [{"from": "A", "to": "B"}]
                                   }""")
                             .post("/api/workflows")
                             .then()
                             .statusCode(201)
                             .extract()
                             .path("id");

        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "Invalid Update Flow",
                         "start": "A",
                         "transitions": [{"from": "A", "to": "Unknown"}]
                     }""")
               .put("/api/workflows/" + created)
               .then()
               .statusCode(400);
    }

    @Test
    void shouldRejectUpdateForUserRole() {
        given().header(Given.authenticatedUser())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "Denied Update",
                         "start": "Open",
                         "transitions": [{"from": "Open", "to": "Closed"}]
                     }""")
               .put("/api/workflows/1")
               .then()
               .statusCode(403);
    }
}

package dev.vepo.issues.workflow;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import dev.vepo.issues.Given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
class WorkflowEndpointTest {

    @Test
    @Order(1)
    void listEmptyWorkflowTest() {
        given().header(Given.authenticatedUser())
               .when()
               .get("/api/workflows")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThanOrEqualTo(0));
    }

    @Test
    @Order(2)
    void createWorkflowTest() {
        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "Test Workflow",
                         "statuses": ["Status 1", "Status 2"],
                         "start": "Status 1",
                         "transitions": [{"from": "Status 1", "to": "Status 2"}]
                     }""")
               .post("/api/workflows")
               .then()
               .statusCode(201)
               .body("name", is("Test Workflow"))
               .body("statuses.size()", is(2))
               .body("statuses[0]", is("Status 1"))
               .body("statuses[1]", is("Status 2"))
               .body("start", is("Status 1"))
               .body("transitions.size()", is(1))
               .body("transitions[0].from", is("Status 1"))
               .body("transitions[0].to", is("Status 2"));
    }

    @Test
    @Order(3)
    void listWorkflowTest() {
        given().header(Given.authenticatedUser())
               .when()
               .get("/api/workflows")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThanOrEqualTo(1)) // Check that there is one workflow
               .body("find { it.name == 'Test Workflow' }.name", is("Test Workflow"))
               .body("find { it.name == 'Test Workflow' }.statuses.size()", is(2))
               .body("find { it.name == 'Test Workflow' }.statuses[0]", is("Status 1"))
               .body("find { it.name == 'Test Workflow' }.statuses[1]", is("Status 2"))
               .body("find { it.name == 'Test Workflow' }.start", is("Status 1"))
               .body("find { it.name == 'Test Workflow' }.transitions.size()", is(1))
               .body("find { it.name == 'Test Workflow' }.transitions[0].from", is("Status 1"))
               .body("find { it.name == 'Test Workflow' }.transitions[0].to", is("Status 2"));
    }

    @Test
    @Order(4)
    @DisplayName("It should validate create workflow request")
    void validateCreateWorkflowRequestTest() {
        given().header(Given.authenticatedUser())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "Test Workflow",
                         "statuses": ["Status 1", "Status 2"],
                         "start": "Status 1",
                         "transitions": [{"from": "Status 1", "to": "Status 2"}]
                     }""")
               .post("/api/workflows")
               .then()
               .statusCode(403);
        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "statuses": ["Status 1", "Status 2"],
                         "start": "Status 1",
                         "transitions": [{"from": "Status 1", "to": "Status 2"}]
                     }""")
               .post("/api/workflows")
               .then()
               .statusCode(400)
               .body("violations[0].field", is("create.request.name"))
               .body("violations[0].message", is("Workflow name cannot be empty!"));
        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "name",
                         "statuses": ["Status 1", "Status 2"],
                         "start": "Status 1",
                         "transitions": [{"from": "Status 1", "to": "Status 2"}]
                     }""")
               .post("/api/workflows")
               .then()
               .statusCode(400)
               .body("violations[0].field", is("create.request.name"))
               .body("violations[0].message", is("Workflow name should have at least 5 caracters and at most 64!"));
        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "nameWithMoreThan64CharactersAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                         "statuses": ["Status 1", "Status 2"],
                         "start": "Status 1",
                         "transitions": [{"from": "Status 1", "to": "Status 2"}]
                     }""")
               .post("/api/workflows")
               .then()
               .statusCode(400)
               .body("violations[0].field", is("create.request.name"))
               .body("violations[0].message", is("Workflow name should have at least 5 caracters and at most 64!"));
        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "ValidName",
                         "start": "Status 1",
                         "transitions": [{"from": "Status 1", "to": "Status 2"}]
                     }""")
               .post("/api/workflows")
               .then()
               .statusCode(400)
               .body("violations[0].field", is("create.request.statuses"))
               .body("violations[0].message", is("No status defined!"));
        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "ValidName",
                         "statuses": [],
                         "start": "Status 1",
                         "transitions": [{"from": "Status 1", "to": "Status 2"}]
                     }""")
               .post("/api/workflows")
               .then()
               .statusCode(400)
               .body("violations.find { it.field == 'create.request.statuses' & it.message == 'No status defined!' }", notNullValue());
        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "ValidName",
                         "statuses": ["Status 1"],
                         "start": "Status 1",
                         "transitions": [{"from": "Status 1", "to": "Status 2"}]
                     }""")
               .post("/api/workflows")
               .then()
               .statusCode(400)
               .body("violations.find { it.field == 'create.request.statuses' & it.message == 'At least 2 statuses must be defined!' }", notNullValue());
        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "ValidName",
                         "statuses": ["Status 1", "Status 2"],
                         "transitions": [{"from": "Status 1", "to": "Status 2"}]
                     }""")
               .post("/api/workflows")
               .then()
               .statusCode(400)
               .body("violations.find { it.field == 'create.request.start' & it.message == 'No start status is defined!' }", notNullValue());
        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "ValidName",
                         "statuses": ["Status 1", "Status 2"],
                         "start": "Status 3",
                         "transitions": [{"from": "Status 1", "to": "Status 2"}]
                     }""")
               .post("/api/workflows")
               .then()
               .statusCode(400)
               .body("violations.find { it.field == 'create.request' }.message", is("Workflow should define all status used on start and transitions!"));
    }
}
package dev.vepo.issues.workflow.create;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class CreateWorkflowEndpointTest {

    @Test
    void shouldCreateWorkflowWhenRequestIsValid() {
        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "Created Workflow",
                         "statuses": ["Status 1", "Status 2"],
                         "start": "Status 1",
                         "transitions": [{"from": "Status 1", "to": "Status 2"}]
                     }""")
               .post("/api/workflows")
               .then()
               .statusCode(201)
               .body("name", is("Created Workflow"))
               .body("statuses.size()", is(2))
               .body("statuses[0]", is("Status 1"))
               .body("statuses[1]", is("Status 2"))
               .body("start", is("Status 1"))
               .body("transitions.size()", is(1))
               .body("transitions[0].from", is("Status 1"))
               .body("transitions[0].to", is("Status 2"));
    }

    @Test
    void shouldPersistFinishStatusesWhenCreatingWorkflow() {
        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "Finish Status Flow",
                         "statuses": ["Open", "Done", "Canceled"],
                         "start": "Open",
                         "transitions": [
                             {"from": "Open", "to": "Done"},
                             {"from": "Open", "to": "Canceled"},
                             {"from": "Done", "to": "Open"}
                         ],
                         "finishStatuses": [
                             {"status": "Done", "outcome": "DONE"},
                             {"status": "Canceled", "outcome": "CANCELED"}
                         ],
                         "phaseStart": "Open"
                     }""")
               .post("/api/workflows")
               .then()
               .statusCode(201)
               .body("finishStatuses.size()", is(2))
               .body("finishStatuses[0].status", is("Canceled"))
               .body("finishStatuses[0].outcome", is("CANCELED"))
               .body("finishStatuses[1].status", is("Done"))
               .body("finishStatuses[1].outcome", is("DONE"))
               .body("phaseStart", is("Open"));
    }

    @Test
    void shouldPersistWipLimitsWhenCreatingWorkflow() {
        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "WIP Limit Flow",
                         "statuses": ["Backlog", "Doing", "Done"],
                         "start": "Backlog",
                         "transitions": [
                             {"from": "Backlog", "to": "Doing"},
                             {"from": "Doing", "to": "Done"},
                             {"from": "Done", "to": "Doing"}
                         ],
                         "wipLimits": [
                             {"status": "Doing", "wipLimit": 3}
                         ]
                     }""")
               .post("/api/workflows")
               .then()
               .statusCode(201)
               .body("wipLimits.size()", is(1))
               .body("wipLimits[0].status", is("Doing"))
               .body("wipLimits[0].wipLimit", is(3));
    }

    @Test
    void shouldRejectWipLimitStatusNotInWorkflow() {
        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "Invalid WIP Flow",
                         "statuses": ["Open", "Done"],
                         "start": "Open",
                         "transitions": [{"from": "Open", "to": "Done"}],
                         "wipLimits": [{"status": "Missing", "wipLimit": 2}]
                     }""")
               .post("/api/workflows")
               .then()
               .statusCode(400)
               .body("message", is("WIP limit status is not part of this workflow: Missing"));
    }

    @Test
    void shouldRejectFinishStatusNotInWorkflow() {
        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "Invalid Finish Flow",
                         "statuses": ["Open", "Done"],
                         "start": "Open",
                         "transitions": [{"from": "Open", "to": "Done"}],
                         "finishStatuses": [{"status": "Missing", "outcome": "DONE"}]
                     }""")
               .post("/api/workflows")
               .then()
               .statusCode(400)
               .body("message", is("Finish status is not part of this workflow: Missing"));
    }

    @Test
    @DisplayName("It should validate create workflow request")
    void shouldValidateCreateWorkflowRequest() {
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

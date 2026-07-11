package dev.vepo.issues.ticket.move;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class MoveTicketEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("It should be psosible to move ticket to a new status")
    void moveTicketTest() {
        var inProgress = Given.status("In Progress");
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "to": %d
                     }""".formatted(inProgress.getId()))
               .post("/api/tickets/" + fixtures.ticket().id() + "/move")
               .then()
               .statusCode(200)
               .body("status", is(inProgress.getId().intValue()));

        List<?> history = given().header(fixtures.userAuthenticatedHeader())
                                 .accept(ContentType.JSON)
                                 .when()
                                 .get("/api/tickets/" + fixtures.ticket().id() + "/history")
                                 .then()
                                 .statusCode(200)
                                 .extract()
                                 .jsonPath()
                                 .getList("");

        @SuppressWarnings("unchecked")
        var statusChange = history.stream()
                                  .map(entry -> (Map<String, Object>) entry)
                                  .filter(e -> "STATUS_CHANGED".equals(e.get("action")))
                                  .findFirst()
                                  .orElseThrow();
        assertEquals("status", statusChange.get("field"));
        assertEquals("In progress", statusChange.get("newValue"));
    }

    @Test
    void shouldSetFinishDateWhenMovingToDoneFinishStatus() {
        var inProgress = Given.status("In Progress");
        var done = Given.status("Done");
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .when()
               .body("""
                     {
                         "to": %d
                     }""".formatted(inProgress.getId()))
               .post("/api/tickets/" + fixtures.ticket().id() + "/move")
               .then()
               .statusCode(200);

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .when()
               .body("""
                     {
                         "to": %d
                     }""".formatted(done.getId()))
               .post("/api/tickets/" + fixtures.ticket().id() + "/move")
               .then()
               .statusCode(200)
               .body("finishedAt", notNullValue());

        given().header(fixtures.userAuthenticatedHeader())
               .when()
               .get("/api/tickets/" + fixtures.ticket().id())
               .then()
               .statusCode(200)
               .body("finishedAt", notNullValue());
    }

    @Test
    void shouldClearFinishDateWhenLeavingDoneFinishStatus() {
        var inProgress = Given.status("In Progress");
        var done = Given.status("Done");
        var ticketId = fixtures.ticket().id();

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .when()
               .body("""
                     {
                         "to": %d
                     }""".formatted(inProgress.getId()))
               .post("/api/tickets/" + ticketId + "/move")
               .then()
               .statusCode(200);
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .when()
               .body("""
                     {
                         "to": %d
                     }""".formatted(done.getId()))
               .post("/api/tickets/" + ticketId + "/move")
               .then()
               .statusCode(200)
               .body("finishedAt", notNullValue());

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .when()
               .body("""
                     {
                         "to": %d
                     }""".formatted(inProgress.getId()))
               .post("/api/tickets/" + ticketId + "/move")
               .then()
               .statusCode(200)
               .body("finishedAt", nullValue());

        given().header(fixtures.userAuthenticatedHeader())
               .when()
               .get("/api/tickets/" + ticketId)
               .then()
               .statusCode(200)
               .body("finishedAt", nullValue());
    }

    @Test
    void shouldNotSetFinishDateWhenMovingToCanceledFinishStatus() {
        var workflowId = given().header(Given.authenticatedProjectManager())
                                .when()
                                .contentType("application/json")
                                .body("""
                                      {
                                          "name": "Canceled Finish Flow",
                                          "statuses": ["Open", "Canceled"],
                                          "start": "Open",
                                          "transitions": [{"from": "Open", "to": "Canceled"}],
                                          "finishStatuses": [{"status": "Canceled", "outcome": "CANCELED"}]
                                      }""")
                                .post("/api/workflows")
                                .then()
                                .statusCode(201)
                                .extract()
                                .path("id");

        var project = given().header(Given.authenticatedProjectManager())
                             .when()
                             .contentType("application/json")
                             .body("""
                                   {
                                       "prefix": "CNF",
                                       "name": "Canceled Finish Project %s",
                                       "description": "Project for canceled finish test",
                                       "workflowId": %d
                                   }""".formatted(java.util.UUID.randomUUID(), workflowId))
                             .post("/api/projects")
                             .then()
                             .statusCode(201)
                             .extract()
                             .as(dev.vepo.issues.project.ProjectResponse.class);

        var canceled = Given.allStatuses()
                            .stream()
                            .filter(status -> "Canceled".equals(status.name()))
                            .findFirst()
                            .orElseThrow();

        var ticket = given().header(fixtures.pmAuthenticatedHeader())
                            .when()
                            .contentType(ContentType.JSON)
                            .body("""
                                  {
                                      "title": "Canceled finish ticket",
                                      "description": "Canceled finish ticket description",
                                      "projectId": %d,
                                      "categoryId": %d
                                  }""".formatted(project.id(), fixtures.bug().getId()))
                            .post("/api/tickets")
                            .then()
                            .statusCode(201)
                            .extract()
                            .as(dev.vepo.issues.ticket.TicketResponse.class);

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .when()
               .body("""
                     {
                         "to": %d
                     }""".formatted(canceled.id()))
               .post("/api/tickets/" + ticket.id() + "/move")
               .then()
               .statusCode(200)
               .body("finishedAt", nullValue())
               .body("canceledAt", notNullValue());
    }

    @Test
    void shouldRejectMoveWhenTargetStatusWipLimitReached() {
        var workflowId = given().header(Given.authenticatedProjectManager())
                                .when()
                                .contentType("application/json")
                                .body("""
                                      {
                                          "name": "WIP Move Flow",
                                          "statuses": ["Open", "Doing", "Done"],
                                          "start": "Open",
                                          "transitions": [
                                              {"from": "Open", "to": "Doing"},
                                              {"from": "Doing", "to": "Done"}
                                          ],
                                          "wipLimits": [{"status": "Doing", "wipLimit": 1}]
                                      }""")
                                .post("/api/workflows")
                                .then()
                                .statusCode(201)
                                .extract()
                                .path("id");

        var project = given().header(Given.authenticatedProjectManager())
                             .when()
                             .contentType("application/json")
                             .body("""
                                   {
                                       "prefix": "WIP",
                                       "name": "WIP Move Project %s",
                                       "description": "Project for WIP move test",
                                       "workflowId": %d
                                   }""".formatted(java.util.UUID.randomUUID(), workflowId))
                             .post("/api/projects")
                             .then()
                             .statusCode(201)
                             .extract()
                             .as(dev.vepo.issues.project.ProjectResponse.class);

        Given.addProjectMember(project.id(), "user@issues.vepo.dev");

        var doingId = given().header(fixtures.pmAuthenticatedHeader())
                             .accept(ContentType.JSON)
                             .when()
                             .get("/api/projects/" + project.id() + "/status")
                             .then()
                             .statusCode(200)
                             .extract()
                             .jsonPath()
                             .getList("", java.util.Map.class)
                             .stream()
                             .filter(s -> "Doing".equals(s.get("name")))
                             .map(s -> ((Number) s.get("id")).longValue())
                             .findFirst()
                             .orElseThrow();

        var firstTicket = given().header(fixtures.pmAuthenticatedHeader())
                                 .when()
                                 .contentType(ContentType.JSON)
                                 .body("""
                                       {
                                           "title": "First WIP ticket",
                                           "description": "Occupies Doing",
                                           "projectId": %d,
                                           "categoryId": %d
                                       }""".formatted(project.id(), fixtures.bug().getId()))
                                 .post("/api/tickets")
                                 .then()
                                 .statusCode(201)
                                 .extract()
                                 .as(dev.vepo.issues.ticket.TicketResponse.class);

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .when()
               .body("""
                     {
                         "to": %d
                     }""".formatted(doingId))
               .post("/api/tickets/" + firstTicket.id() + "/move")
               .then()
               .statusCode(200);

        var secondTicket = given().header(fixtures.pmAuthenticatedHeader())
                                  .when()
                                  .contentType(ContentType.JSON)
                                  .body("""
                                        {
                                            "title": "Second WIP ticket",
                                            "description": "Should be blocked",
                                            "projectId": %d,
                                            "categoryId": %d
                                        }""".formatted(project.id(), fixtures.bug().getId()))
                                  .post("/api/tickets")
                                  .then()
                                  .statusCode(201)
                                  .extract()
                                  .as(dev.vepo.issues.ticket.TicketResponse.class);

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .when()
               .body("""
                     {
                         "to": %d
                     }""".formatted(doingId))
               .post("/api/tickets/" + secondTicket.id() + "/move")
               .then()
               .statusCode(400)
               .body("message", equalTo("WIP limit reached for status Doing (limit 1)"));
    }

    @Test
    @DisplayName("It should not be possible to move ticket to invalid status")
    void shouldNotMoveToInvalidStatusTest() {
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "to": 9999
                     }""")
               .post("/api/tickets/" + fixtures.ticket().id() + "/move")
               .then()
               .statusCode(400)
               .body("message", equalTo("Stage not defined in project! stageId=9999"));
    }

    @Test
    @DisplayName("Should reject move when status-required custom field is missing")
    void shouldRejectMoveWhenStatusRequiredCustomFieldMissing() {
        var suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        var workflow = given().header(fixtures.pmAuthenticatedHeader())
                              .contentType(ContentType.JSON)
                              .body("""
                                    {
                                      "name": "CF Move Flow %s",
                                      "statuses": ["TODO", "In Progress", "Done"],
                                      "start": "TODO",
                                      "transitions": [
                                        {"from": "TODO", "to": "In Progress"},
                                        {"from": "In Progress", "to": "Done"}
                                      ],
                                      "finishStatuses": [{"status": "Done", "outcome": "DONE"}]
                                    }
                                    """.formatted(suffix))
                              .post("/api/workflows")
                              .then()
                              .statusCode(201)
                              .extract()
                              .as(dev.vepo.issues.workflow.WorkflowResponse.class);

        var project = given().header(fixtures.pmAuthenticatedHeader())
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                     "name": "CF Move Project %s",
                                     "description": "Isolated project for status-required move.",
                                     "prefix": "MV%s",
                                     "workflowId": %d
                                   }
                                   """.formatted(suffix, suffix.substring(0, 4).toUpperCase(), workflow.id()))
                             .post("/api/projects")
                             .then()
                             .statusCode(201)
                             .extract()
                             .as(dev.vepo.issues.project.ProjectResponse.class);

        var key = "done_" + suffix;
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "key": "%s",
                       "label": "Done notes",
                       "type": "TEXT",
                       "required": false,
                       "statusRequired": ["Done"]
                     }
                     """.formatted(key))
               .post("/api/workflows/%d/custom-fields".formatted(workflow.id()))
               .then()
               .statusCode(201);

        var ticketId = given().header(fixtures.pmAuthenticatedHeader())
                              .contentType(ContentType.JSON)
                              .body("""
                                    {
                                      "title": "Move CF ticket %s",
                                      "description": "Ticket for status-required custom field.",
                                      "projectId": %d,
                                      "categoryId": %d
                                    }
                                    """.formatted(suffix, project.id(), fixtures.bug().getId()))
                              .post("/api/tickets")
                              .then()
                              .statusCode(201)
                              .extract()
                              .path("id");

        var inProgressId = Given.status("In Progress").getId();
        var doneId = Given.status("Done").getId();

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {"to": %d}
                     """.formatted(inProgressId))
               .post("/api/tickets/" + ticketId + "/move")
               .then()
               .statusCode(200);

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {"to": %d}
                     """.formatted(doneId))
               .post("/api/tickets/" + ticketId + "/move")
               .then()
               .statusCode(400);

        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "title": "Move CF ticket %s",
                       "description": "Ticket for status-required custom field.",
                       "categoryId": %d,
                       "priority": "MEDIUM",
                       "customFields": [{"key": "%s", "value": "Completed work"}]
                     }
                     """.formatted(suffix, fixtures.bug().getId(), key))
               .post("/api/tickets/" + ticketId)
               .then()
               .statusCode(200);

        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {"to": %d}
                     """.formatted(doneId))
               .post("/api/tickets/" + ticketId + "/move")
               .then()
               .statusCode(200)
               .body("status", equalTo(doneId.intValue()));
    }
}

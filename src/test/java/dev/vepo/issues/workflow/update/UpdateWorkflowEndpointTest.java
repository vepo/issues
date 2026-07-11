package dev.vepo.issues.workflow.update;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.categories.Category;
import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.ticket.history.TicketHistoryAction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class UpdateWorkflowEndpointTest {

    @Inject
    TicketRepository ticketRepository;

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
                         "statuses": ["Open", "Closed"],
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
                         "statuses": ["A", "B"],
                         "start": "A",
                         "transitions": [{"from": "A", "to": "Unknown"}]
                     }""")
               .put("/api/workflows/" + created)
               .then()
               .statusCode(400);
    }

    @Test
    void shouldUpdateWipLimitsOnWorkflow() {
        var created = given().header(Given.authenticatedProjectManager())
                             .when()
                             .contentType("application/json")
                             .body("""
                                   {
                                       "name": "WIP Update Flow",
                                       "statuses": ["Open", "Doing", "Done"],
                                       "start": "Open",
                                       "transitions": [
                                           {"from": "Open", "to": "Doing"},
                                           {"from": "Doing", "to": "Done"}
                                       ],
                                       "wipLimits": [{"status": "Doing", "wipLimit": 2}]
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
                         "name": "WIP Update Flow",
                         "statuses": ["Open", "Doing", "Done"],
                         "start": "Open",
                         "transitions": [
                             {"from": "Open", "to": "Doing"},
                             {"from": "Doing", "to": "Done"}
                         ],
                         "wipLimits": [{"status": "Doing", "wipLimit": 5}]
                     }""")
               .put("/api/workflows/" + created)
               .then()
               .statusCode(200)
               .body("wipLimits.size()", is(1))
               .body("wipLimits[0].status", is("Doing"))
               .body("wipLimits[0].wipLimit", is(5));

        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "WIP Update Flow",
                         "statuses": ["Open", "Doing", "Done"],
                         "start": "Open",
                         "transitions": [
                             {"from": "Open", "to": "Doing"},
                             {"from": "Doing", "to": "Done"}
                         ],
                         "wipLimits": []
                     }""")
               .put("/api/workflows/" + created)
               .then()
               .statusCode(200)
               .body("wipLimits.size()", is(0));
    }

    @Test
    void shouldRejectUpdateForUserRole() {
        given().header(Given.authenticatedUser())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "Denied Update",
                         "statuses": ["Open", "Closed"],
                         "start": "Open",
                         "transitions": [{"from": "Open", "to": "Closed"}]
                     }""")
               .put("/api/workflows/1")
               .then()
               .statusCode(403);
    }

    @Test
    @DisplayName("Should add and remove statuses when no tickets need remapping")
    void shouldAddAndRemoveStatusesWithoutTickets() {
        var created = given().header(Given.authenticatedProjectManager())
                             .when()
                             .contentType("application/json")
                             .body("""
                                   {
                                       "name": "Editable Status Flow",
                                       "statuses": ["Open", "Doing", "Done"],
                                       "start": "Open",
                                       "transitions": [
                                           {"from": "Open", "to": "Doing"},
                                           {"from": "Doing", "to": "Done"}
                                       ]
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
                         "name": "Editable Status Flow",
                         "statuses": ["Open", "Review", "Done"],
                         "start": "Open",
                         "transitions": [
                             {"from": "Open", "to": "Review"},
                             {"from": "Review", "to": "Done"}
                         ]
                     }""")
               .put("/api/workflows/" + created)
               .then()
               .statusCode(200)
               .body("statuses", hasItem("Review"))
               .body("statuses", not(hasItem("Doing")))
               .body("statuses.size()", is(3));
    }

    @Test
    @DisplayName("Should reject removing a status that still has tickets without replacement")
    void shouldRejectRemoveStatusWithTicketsWithoutReplacement() {
        var workflowId = createWorkflow("Remap Required Flow",
                                        """
                                        ["Backlog", "Active", "Done"]""",
                                        "Backlog",
                                        """
                                        [{"from": "Backlog", "to": "Active"}, {"from": "Active", "to": "Done"}]""");
        var projectId = createProject(workflowId, "Remap Project");
        var categoryId = createCategory();
        var ticketId = createTicket(projectId, categoryId, "Ticket in Active");
        var activeId = statusId(projectId, "Active");
        moveTicket(ticketId, activeId);

        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "Remap Required Flow",
                         "statuses": ["Backlog", "Done"],
                         "start": "Backlog",
                         "transitions": [{"from": "Backlog", "to": "Done"}]
                     }""")
               .put("/api/workflows/" + workflowId)
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Should remap tickets when removing a status with replacement")
    void shouldRemapTicketsWhenRemovingStatusWithReplacement() {
        var workflowId = createWorkflow("Remap Success Flow",
                                        """
                                        ["Todo", "Doing", "Done"]""",
                                        "Todo",
                                        """
                                        [{"from": "Todo", "to": "Doing"}, {"from": "Doing", "to": "Done"}]""",
                                        """
                                        [{"status": "Done", "outcome": "DONE"}]""");
        var projectId = createProject(workflowId, "Remap Success Project");
        var categoryId = createCategory();
        var ticketId = createTicket(projectId, categoryId, "Remap me");
        moveTicket(ticketId, statusId(projectId, "Doing"));

        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "Remap Success Flow",
                         "statuses": ["Todo", "Done"],
                         "start": "Todo",
                         "transitions": [{"from": "Todo", "to": "Done"}],
                         "finishStatuses": [{"status": "Done", "outcome": "DONE"}],
                         "statusReplacements": [{"from": "Doing", "to": "Done"}]
                     }""")
               .put("/api/workflows/" + workflowId)
               .then()
               .statusCode(200)
               .body("statuses", not(hasItem("Doing")))
               .body("statuses", hasItem("Done"));

        given().header(Given.authenticatedUser())
               .when()
               .get("/api/tickets/" + ticketId)
               .then()
               .statusCode(200)
               .body("status", is((int) statusId(projectId, "Done")));

        var historyActions = ticketRepository.findHistoryByTicketId(ticketId)
                                             .map(h -> h.action)
                                             .toList();
        assertTrue(historyActions.contains(TicketHistoryAction.STATUS_CHANGED));
    }

    @Test
    @DisplayName("Should rename status without affecting another workflow sharing the old name")
    void shouldRenameStatusWithoutAffectingOtherWorkflow() {
        createWorkflow("Shared Name Flow A",
                       """
                       ["Alpha", "Omega"]""",
                       "Alpha",
                       """
                       [{"from": "Alpha", "to": "Omega"}]""");
        var workflowB = createWorkflow("Shared Name Flow B",
                                       """
                                       ["Alpha", "Omega"]""",
                                       "Alpha",
                                       """
                                       [{"from": "Alpha", "to": "Omega"}]""");

        given().header(Given.authenticatedProjectManager())
               .when()
               .contentType("application/json")
               .body("""
                     {
                         "name": "Shared Name Flow B",
                         "statuses": ["Alpha Renamed", "Omega"],
                         "start": "Alpha Renamed",
                         "transitions": [{"from": "Alpha Renamed", "to": "Omega"}]
                     }""")
               .put("/api/workflows/" + workflowB)
               .then()
               .statusCode(200)
               .body("statuses", hasItem("Alpha Renamed"))
               .body("statuses", not(hasItem("Alpha")));

        var workflows = given().header(Given.authenticatedUser())
                               .when()
                               .get("/api/workflows")
                               .then()
                               .statusCode(200)
                               .extract()
                               .jsonPath()
                               .getList("", java.util.Map.class);
        var flowA = workflows.stream()
                             .filter(w -> "Shared Name Flow A".equals(w.get("name")))
                             .findFirst()
                             .orElseThrow();
        @SuppressWarnings("unchecked")
        var statuses = (java.util.List<String>) flowA.get("statuses");
        assertTrue(statuses.contains("Alpha"));
    }

    private long createWorkflow(String name, String statusesJson, String start, String transitionsJson) {
        return createWorkflow(name, statusesJson, start, transitionsJson, "[]");
    }

    private long createWorkflow(String name, String statusesJson, String start, String transitionsJson, String finishJson) {
        return ((Number) given().header(Given.authenticatedProjectManager())
                                .when()
                                .contentType("application/json")
                                .body("""
                                      {
                                          "name": "%s",
                                          "statuses": %s,
                                          "start": "%s",
                                          "transitions": %s,
                                          "finishStatuses": %s
                                      }""".formatted(name, statusesJson, start, transitionsJson, finishJson))
                                .post("/api/workflows")
                                .then()
                                .statusCode(201)
                                .extract()
                                .path("id")).longValue();
    }

    private long createProject(long workflowId, String namePrefix) {
        var projectId = ((Number) given().header(Given.authenticatedProjectManager())
                                         .when()
                                         .contentType("application/json")
                                         .body("""
                                               {
                                                   "name": "%s %d",
                                                   "description": "Project for workflow status edit tests",
                                                   "prefix": "W%d",
                                                   "workflowId": %d
                                               }""".formatted(namePrefix, workflowId, workflowId % 10000, workflowId))
                                         .post("/api/projects")
                                         .then()
                                         .statusCode(201)
                                         .extract()
                                         .path("id")).longValue();
        Given.authenticatedUser();
        Given.addProjectMember(projectId, "user@issues.vepo.dev");
        return projectId;
    }

    private long createCategory() {
        return Given.transaction(() -> Given.inject(CategoryRepository.class)
                                            .save(new Category("Cat" + UUID.randomUUID(), "blue"))
                                            .getId());
    }

    private long createTicket(long projectId, long categoryId, String title) {
        return ((Number) given().header(Given.authenticatedUser())
                                .when()
                                .contentType("application/json")
                                .body("""
                                      {
                                          "title": "%s",
                                          "description": "Ticket body for workflow remap",
                                          "projectId": %d,
                                          "categoryId": %d
                                      }""".formatted(title, projectId, categoryId))
                                .post("/api/tickets")
                                .then()
                                .statusCode(201)
                                .extract()
                                .path("id")).longValue();
    }

    private long statusId(long projectId, String name) {
        return given().header(Given.authenticatedUser())
                      .when()
                      .get("/api/projects/%d/status".formatted(projectId))
                      .then()
                      .statusCode(200)
                      .extract()
                      .jsonPath()
                      .getList("", java.util.Map.class)
                      .stream()
                      .filter(s -> name.equals(s.get("name")))
                      .map(s -> ((Number) s.get("id")).longValue())
                      .findFirst()
                      .orElseThrow();
    }

    private void moveTicket(long ticketId, long statusId) {
        given().header(Given.authenticatedUser())
               .when()
               .contentType("application/json")
               .body("{\"to\": %d}".formatted(statusId))
               .post("/api/tickets/%d/move".formatted(ticketId))
               .then()
               .statusCode(200);
    }
}

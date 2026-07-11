package dev.vepo.issues.dashboards.burndown;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class LoadBurndownEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("Should return empty series when phase dates are incomplete")
    void shouldReturnEmptySeriesWhenPhaseDatesIncomplete() {
        var phaseId = createPhase(fixtures.project().id(), "Burndown Incomplete", null, null);

        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/{projectId}/burndown?phaseId={phaseId}", fixtures.project().id(), phaseId)
               .then()
               .statusCode(200)
               .body("datesComplete", is(false))
               .body("series", hasSize(0))
               .body("phaseId", equalTo(phaseId.intValue()));
    }

    @Test
    @DisplayName("Should build burndown series with ideal and remaining points")
    void shouldBuildBurndownSeriesWithIdealAndRemaining() {
        var start = LocalDate.now().minusDays(2);
        var end = LocalDate.now().plusDays(2);
        var phaseId = createPhase(fixtures.project().id(), "Burndown Complete", start.toString(), end.toString());

        createTicket(fixtures.project().id(), "Open points ticket", 5, phaseId);
        var missingId = createTicket(fixtures.project().id(), "Missing points ticket", null, phaseId);
        var doneTicket = createTicket(fixtures.project().id(), "Done points ticket", 3, phaseId);
        moveThroughInProgressToDone(doneTicket);

        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/{projectId}/burndown?phaseId={phaseId}", fixtures.project().id(), phaseId)
               .then()
               .statusCode(200)
               .body("datesComplete", is(true))
               .body("commitmentPoints", equalTo(8))
               .body("remainingPoints", equalTo(5))
               .body("series.size()", greaterThanOrEqualTo(3))
               .body("series[0].ideal", equalTo(8.0f))
               .body("series[0].remaining", equalTo(8))
               .body("warnings", hasSize(1))
               .body("warnings[0].code", equalTo("MISSING_STORY_POINTS"))
               .body("warnings.ticketId", hasItem((int) missingId));
    }

    @Test
    @DisplayName("Should treat canceled tickets as burned on burndown")
    void shouldTreatCanceledTicketsAsBurned() {
        var workflowId = given().header(Given.authenticatedProjectManager())
                                .contentType(ContentType.JSON)
                                .body("""
                                      {
                                          "name": "Burndown Cancel Flow %s",
                                          "statuses": ["Open", "Canceled"],
                                          "start": "Open",
                                          "transitions": [{"from": "Open", "to": "Canceled"}],
                                          "finishStatuses": [{"status": "Canceled", "outcome": "CANCELED"}]
                                      }""".formatted(UUID.randomUUID()))
                                .post("/api/workflows")
                                .then()
                                .statusCode(201)
                                .extract()
                                .path("id");

        var project = given().header(Given.authenticatedProjectManager())
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                       "prefix": "BDN",
                                       "name": "Burndown Cancel Project %s",
                                       "description": "Project for burndown cancel burn",
                                       "workflowId": %d
                                   }""".formatted(UUID.randomUUID(), workflowId))
                             .post("/api/projects")
                             .then()
                             .statusCode(201)
                             .extract()
                             .jsonPath();

        var projectId = project.getLong("id");
        var start = LocalDate.now().minusDays(1);
        var end = LocalDate.now().plusDays(5);
        var phaseId = createPhase(projectId, "Burndown Canceled", start.toString(), end.toString());
        var ticketId = createTicket(projectId, "Cancel burn ticket", 8, phaseId);

        var canceledId = Given.allStatuses()
                              .stream()
                              .filter(status -> "Canceled".equals(status.name()))
                              .findFirst()
                              .orElseThrow()
                              .id();
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "to": %d
                     }""".formatted(canceledId))
               .post("/api/tickets/" + ticketId + "/move")
               .then()
               .statusCode(200)
               .body("canceledAt", notNullValue())
               .body("finishedAt", nullValue());

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/{projectId}/burndown?phaseId={phaseId}", projectId, phaseId)
               .then()
               .statusCode(200)
               .body("datesComplete", is(true))
               .body("commitmentPoints", equalTo(8))
               .body("remainingPoints", equalTo(0));
    }

    private Long createPhase(long projectId, String name, String startDate, String endDate) {
        var body = startDate == null
                                     ? """
                                       {
                                           "name": "%s"
                                       }""".formatted(name)
                                     : """
                                       {
                                           "name": "%s",
                                           "startDate": "%s",
                                           "endDate": "%s"
                                       }""".formatted(name, startDate, endDate);
        return given().header(fixtures.pmAuthenticatedHeader())
                      .contentType(ContentType.JSON)
                      .accept(ContentType.JSON)
                      .body(body)
                      .post("/api/projects/%d/phases".formatted(projectId))
                      .then()
                      .statusCode(201)
                      .extract()
                      .jsonPath()
                      .getLong("id");
    }

    private long createTicket(long projectId, String title, Integer storyPoints, long phaseId) {
        var pointsJson = storyPoints == null ? "null" : storyPoints.toString();
        return given().header(fixtures.pmAuthenticatedHeader())
                      .contentType(ContentType.JSON)
                      .accept(ContentType.JSON)
                      .body("""
                            {
                                "title": "%s",
                                "description": "Burndown ticket description text",
                                "projectId": %d,
                                "categoryId": %d,
                                "phaseId": %d,
                                "storyPoints": %s
                            }""".formatted(title, projectId, fixtures.bug().getId(), phaseId, pointsJson))
                      .post("/api/tickets")
                      .then()
                      .statusCode(201)
                      .body("storyPoints", storyPoints == null ? nullValue() : equalTo(storyPoints))
                      .extract()
                      .jsonPath()
                      .getLong("id");
    }

    private void moveThroughInProgressToDone(long ticketId) {
        var inProgress = Given.status("In Progress");
        var done = Given.status("Done");
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "to": %d
                     }""".formatted(inProgress.getId()))
               .post("/api/tickets/" + ticketId + "/move")
               .then()
               .statusCode(200);
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "to": %d
                     }""".formatted(done.getId()))
               .post("/api/tickets/" + ticketId + "/move")
               .then()
               .statusCode(200)
               .body("finishedAt", notNullValue());
    }
}

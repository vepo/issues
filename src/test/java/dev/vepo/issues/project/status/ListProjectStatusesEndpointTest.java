package dev.vepo.issues.project.status;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.project.ProjectResponse;
import dev.vepo.issues.project.ProjectTestFixtures;
import dev.vepo.issues.workflow.WorkflowResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class ListProjectStatusesEndpointTest {

    private WorkflowResponse workflow;
    private Header userAuthenticatedHeader;
    private Header pmAuthenticatedHeader;

    @BeforeEach
    void setup() {
        var fixtures = ProjectTestFixtures.create();
        this.workflow = fixtures.workflow();
        this.userAuthenticatedHeader = fixtures.userAuthenticatedHeader();
        this.pmAuthenticatedHeader = fixtures.pmAuthenticatedHeader();
    }

    @Test
    @DisplayName("Non-authenticated user should not be able to get project statuses")
    void nonAuthenticatedUserShouldNotGetProjectStatusesTest() {
        given().when()
               .accept(ContentType.JSON)
               .get("/api/projects/1/status")
               .then()
               .statusCode(403);
    }

    @Test
    @DisplayName("Authenticated users should be able to get project statuses")
    void authenticatedUsersShouldGetProjectStatusesTest() {
        // First create a project
        var createdProject = given().header(pmAuthenticatedHeader)
                                    .accept(ContentType.JSON)
                                    .when()
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                              "name": "Test Project For Statuses",
                                              "description": "This is a test project for statuses.",
                                              "prefix": "PRJ",
                                              "workflowId": %d
                                          }""".formatted(workflow.id()))
                                    .post("/api/projects")
                                    .then()
                                    .statusCode(201)
                                    .extract()
                                    .as(ProjectResponse.class);

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + createdProject.id() + "/status")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0))
               .body("[0].name", is(workflow.start()));

        dev.vepo.issues.Given.addProjectMember(createdProject.id(), "user@issues.vepo.dev");

        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + createdProject.id() + "/status")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0))
               .body("[0].name", is(workflow.start()));
    }

    @Test
    void shouldExposeWipLimitOnProjectStatuses() {
        var workflowId = given().header(pmAuthenticatedHeader)
                                .when()
                                .contentType(ContentType.JSON)
                                .body("""
                                      {
                                          "name": "Status WIP Flow",
                                          "statuses": ["Open", "Doing", "Done"],
                                          "start": "Open",
                                          "transitions": [
                                              {"from": "Open", "to": "Doing"},
                                              {"from": "Doing", "to": "Done"}
                                          ],
                                          "wipLimits": [{"status": "Doing", "wipLimit": 4}]
                                      }""")
                                .post("/api/workflows")
                                .then()
                                .statusCode(201)
                                .extract()
                                .path("id");

        var project = given().header(pmAuthenticatedHeader)
                             .when()
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                       "prefix": "SWP",
                                       "name": "Status WIP Project %s",
                                       "description": "Expose WIP on statuses",
                                       "workflowId": %d
                                   }""".formatted(java.util.UUID.randomUUID(), workflowId))
                             .post("/api/projects")
                             .then()
                             .statusCode(201)
                             .extract()
                             .as(ProjectResponse.class);

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + project.id() + "/status")
               .then()
               .statusCode(200)
               .body("find { it.name == 'Doing' }.wipLimit", is(4))
               .body("find { it.name == 'Open' }.wipLimit", org.hamcrest.Matchers.nullValue());
    }

    @Test
    @DisplayName("Getting statuses for non-existent project should return 404")
    void getStatusesForNonExistentProjectShouldReturn404Test() {
        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/9999/status")
               .then()
               .statusCode(404)
               .body("message", is("Project with ID 9999 does not exist"));
    }
}

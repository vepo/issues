package dev.vepo.issues.project.workflow;

import static io.restassured.RestAssured.given;
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
class FindProjectWorkflowEndpointTest {

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
    @DisplayName("Non-authenticated user should not be able to get project workflow")
    void nonAuthenticatedUserShouldNotGetProjectWorkflowTest() {
        given().when()
               .accept(ContentType.JSON)
               .get("/api/projects/1/workflow")
               .then()
               .statusCode(401);
    }

    @Test
    @DisplayName("Authenticated users should be able to get project workflow")
    void authenticatedUsersShouldGetProjectWorkflowTest() {
        // First create a project
        var createdProject = given().header(pmAuthenticatedHeader)
                                    .accept(ContentType.JSON)
                                    .when()
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                              "name": "Test Project For Workflow",
                                              "description": "This is a test project for workflow.",
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
               .get("/api/projects/" + createdProject.id() + "/workflow")
               .then()
               .statusCode(200)
               .body("id", is((int) workflow.id()))
               .body("name", is(workflow.name()));

        dev.vepo.issues.Given.addProjectMember(createdProject.id(), "user@issues.vepo.dev");

        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + createdProject.id() + "/workflow")
               .then()
               .statusCode(200)
               .body("id", is((int) workflow.id()))
               .body("name", is(workflow.name()));
    }

    @Test
    @DisplayName("Getting workflow for non-existent project should return 404")
    void getWorkflowForNonExistentProjectShouldReturn404Test() {
        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/9999/workflow")
               .then()
               .statusCode(404)
               .body("message", is("Project with ID 9999 does not exist"));
    }
}

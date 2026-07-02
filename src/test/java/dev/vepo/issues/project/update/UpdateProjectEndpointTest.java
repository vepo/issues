package dev.vepo.issues.project.update;

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
class UpdateProjectEndpointTest {

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
    @DisplayName("Only project managers should be allowed to update projects")
    void onlyProjectManagerShouldBeAllowedToUpdateProjectsTest() {
        // First create a project
        var createdProject = given().header(pmAuthenticatedHeader)
                                    .accept(ContentType.JSON)
                                    .when()
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                              "name": "Original Project",
                                              "description": "Original description.",
                                              "prefix": "ORG",
                                              "workflowId": %d
                                          }""".formatted(workflow.id()))
                                    .post("/api/projects")
                                    .then()
                                    .statusCode(201)
                                    .extract()
                                    .as(ProjectResponse.class);

        // Test that regular user cannot update
        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Updated Project",
                         "description": "Updated description.",
                         "prefix": "UPD",
                         "workflowId": %d
                     }""".formatted(workflow.id()))
               .post("/api/projects/" + createdProject.id())
               .then()
               .statusCode(403);

        // Test that PM can update
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Updated Project",
                         "description": "Updated description.",
                         "prefix": "UPD",
                         "workflowId": %d
                     }""".formatted(workflow.id()))
               .post("/api/projects/" + createdProject.id())
               .then()
               .statusCode(201)
               .body("id", is((int) createdProject.id()))
               .body("name", is("Updated Project"))
               .body("description", is("Updated description."))
               .body("prefix", is("UPD"))
               .body("workflow.id", is((int) workflow.id()));
    }

    @Test
    @DisplayName("Updating non-existent project should return 404")
    void updateNonExistentProjectShouldReturn404Test() {
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Non-existent Project",
                         "description": "This project doesn't exist.",
                         "prefix": "NEX",
                         "workflowId": %d
                     }""".formatted(workflow.id()))
               .post("/api/projects/9999")
               .then()
               .statusCode(404)
               .body("message", is("Project with ID 9999 does not exist"));
    }
}

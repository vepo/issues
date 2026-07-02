package dev.vepo.issues.project.create;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.project.ProjectTestFixtures;
import dev.vepo.issues.workflow.WorkflowResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
class CreateProjectEndpointTest {

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
    @DisplayName("No user should be allowed to create a project")
    void noUserShouldBeAllowedToCreateProjectTest() {
        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Test Project",
                         "description": "This is a test project.",
                         "workflowId": %d
                     }""".formatted(workflow.id()))
               .post("/api/projects")
               .then()
               .statusCode(403);
    }

    @Test
    @DisplayName("Only project managers should be allowed to create projects")
    void onlyProjectManagerShouldBeAllowedToCreateProjectsTest() {
        var suffix = java.util.UUID.randomUUID().toString().substring(0, 6);
        var prefix = "P" + suffix.substring(0, 4);
        var projectName = "Test Project " + suffix;
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "%s",
                         "description": "This is a test project.",
                         "prefix": "%s",
                         "workflowId": %d
                     }""".formatted(projectName, prefix, workflow.id()))
               .post("/api/projects")
               .then()
               .statusCode(201)
               .body("name", is(projectName))
               .body("description", is("This is a test project."))
               .body("workflow.id", is((int) workflow.id()))
               .body("workflow.name", is(workflow.name()));
    }

    @Test
    @DisplayName("Project name is a required field")
    void projectNameIsRequiredTest() {
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "description": "This is a test project.",
                         "workflowId": 1,
                         "prefix": "PRJ"
                     }""".formatted(workflow.id()))
               .post("/api/projects")
               .then()
               .statusCode(400)
               .body("violations[0].field", is("create.request.name"))
               .body("violations[0].message", is("Project name cannot be empty"));
    }

    @Test
    @DisplayName("Workflow ID must be provided and must exist")
    void workflowIdMustExistTest() {
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Test Project",
                         "description": "This is a test project.",
                         "prefix": "PRJJJJ"
                     }""")
               .post("/api/projects")
               .then()
               .statusCode(400)
               .body("violations[0].field", is("create.request.workflowId"))
               .body("violations[0].message", is("Workflow ID must be provided"));

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Test Project",
                         "description": "This is a test project.",
                         "workflowId": 9999,
                         "prefix": "PRJJJJ"
                     }""")
               .post("/api/projects")
               .then()
               .statusCode(404)
               .body("message", is("Workflow with ID 9999 does not exist"));
    }

    @Test
    @DisplayName("Project prefix validation")
    void projectPrefixValidationTest() {
        // Test empty prefix
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Test Project with no prefix",
                         "description": "This is a test project.",
                         "prefix": "",
                         "workflowId": %d
                     }""".formatted(workflow.id()))
               .post("/api/projects")
               .then()
               .statusCode(400);

        // Test prefix that's too long
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Test Project with prefix too long",
                         "description": "This is a test project.",
                         "prefix": "TOOLONGPREFIX",
                         "workflowId": %d
                     }""".formatted(workflow.id()))
               .post("/api/projects")
               .then()
               .statusCode(400);
    }

    @Test
    @DisplayName("Project description can be optional")
    void projectDescriptionCanBeOptionalTest() {
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Project Without Description",
                         "prefix": "PWD",
                         "workflowId": %d
                     }""".formatted(workflow.id()))
               .post("/api/projects")
               .then()
               .statusCode(201)
               .body("name", is("Project Without Description"))
               .body("description", equalTo(null))
               .body("prefix", is("PWD"))
               .body("workflow.id", is((int) workflow.id()));
    }
}

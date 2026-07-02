package dev.vepo.issues.project;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import dev.vepo.issues.Given;
import dev.vepo.issues.workflow.WorkflowResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
class ProjectEndpointTest {

    private WorkflowResponse workflow;
    private Header userAuthenticatedHeader;
    private Header pmAuthenticatedHeader;

    @BeforeEach
    void setup() {
        this.workflow = Given.simpleWorkflow();
        this.userAuthenticatedHeader = Given.authenticatedUser();
        this.pmAuthenticatedHeader = Given.authenticatedProjectManager();
    }

    @Test
    @Order(1)
    @DisplayName("Non authenticated user should not be able to list projects")
    void nonAuthenticatedUserShouldNotListProjectsTest() {
        given().when()
               .accept(ContentType.JSON)
               .get("/api/projects")
               .then()
               .statusCode(401);
    }

    @Test
    @Order(2)
    @DisplayName("Only authenticated users should be able to list projects")
    void onlyAuthenticatedUsersShouldListProjectsTest() {
        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThan(0));
    }

    @Test
    @Order(3)
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
    @Order(4)
    @DisplayName("Only project managers should be allowed to create projects")
    void onlyProjectManagerShouldBeAllowedToCreateProjectsTest() {
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Test Project",
                         "description": "This is a test project.",
                         "prefix": "PRJ",
                         "workflowId": %d
                     }""".formatted(workflow.id()))
               .post("/api/projects")
               .then()
               .statusCode(201)
               .body("name", is("Test Project"))
               .body("description", is("This is a test project."))
               .body("workflow.id", is((int) workflow.id()))
               .body("workflow.name", is(workflow.name()));
    }

    @Test
    @Order(5)
    @DisplayName("Project listing should return the created project")
    void anyoneCanListProjects() {
        Stream.of(userAuthenticatedHeader, pmAuthenticatedHeader)
              .forEach(header -> given().header(header)
                                        .accept(ContentType.JSON)
                                        .when()
                                        .get("/api/projects")
                                        .then()
                                        .statusCode(200)
                                        .body("$.size()", greaterThan(1))
                                        .body("find { it.name == 'Test Project' }.name", is("Test Project"))
                                        .body("find { it.name == 'Test Project' }.description", is("This is a test project."))
                                        .body("find { it.name == 'Test Project' }.workflow.id", is((int) workflow.id()))
                                        .body("find { it.name == 'Test Project' }.workflow.name", is(workflow.name())));
    }

    @Test
    @Order(6)
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
    @Order(7)
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
    @Order(8)
    @DisplayName("Non-authenticated user should not be able to get project by ID")
    void nonAuthenticatedUserShouldNotGetProjectByIdTest() {
        given().when()
               .accept(ContentType.JSON)
               .get("/api/projects/1")
               .then()
               .statusCode(401);
    }

    @Test
    @Order(9)
    @DisplayName("Authenticated users should be able to get project by ID")
    void authenticatedUsersShouldGetProjectByIdTest() {
        // First create a project
        var createdProject = given().header(pmAuthenticatedHeader)
                                    .accept(ContentType.JSON)
                                    .when()
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                              "name": "Test Project For Get",
                                              "description": "This is a test project for get by ID.",
                                              "prefix": "PRJ",
                                              "workflowId": %d
                                          }""".formatted(workflow.id()))
                                    .post("/api/projects")
                                    .then()
                                    .statusCode(201)
                                    .extract()
                                    .as(ProjectResponse.class);

        // Test that both user and PM can access it
        Stream.of(userAuthenticatedHeader, pmAuthenticatedHeader)
              .forEach(header -> given().header(header)
                                        .accept(ContentType.JSON)
                                        .when()
                                        .get("/api/projects/" + createdProject.id())
                                        .then()
                                        .statusCode(200)
                                        .body("id", is((int) createdProject.id()))
                                        .body("name", is("Test Project For Get"))
                                        .body("description", is("This is a test project for get by ID."))
                                        .body("workflow.id", is((int) workflow.id())));
    }

    @Test
    @Order(10)
    @DisplayName("Getting non-existent project should return 404")
    void getNonExistentProjectShouldReturn404Test() {
        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/9999")
               .then()
               .statusCode(404)
               .body("message", is("Project with ID 9999 does not exist"));
    }

    @Test
    @Order(11)
    @DisplayName("Non-authenticated user should not be able to get project workflow")
    void nonAuthenticatedUserShouldNotGetProjectWorkflowTest() {
        given().when()
               .accept(ContentType.JSON)
               .get("/api/projects/1/workflow")
               .then()
               .statusCode(401);
    }

    @Test
    @Order(12)
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

        // Test that both user and PM can access the workflow
        Stream.of(userAuthenticatedHeader, pmAuthenticatedHeader)
              .forEach(header -> given().header(header)
                                        .accept(ContentType.JSON)
                                        .when()
                                        .get("/api/projects/" + createdProject.id() + "/workflow")
                                        .then()
                                        .statusCode(200)
                                        .body("id", is((int) workflow.id()))
                                        .body("name", is(workflow.name())));
    }

    @Test
    @Order(13)
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

    @Test
    @Order(14)
    @DisplayName("Non-authenticated user should not be able to get project statuses")
    void nonAuthenticatedUserShouldNotGetProjectStatusesTest() {
        given().when()
               .accept(ContentType.JSON)
               .get("/api/projects/1/status")
               .then()
               .statusCode(401);
    }

    @Test
    @Order(15)
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

        // Test that both user and PM can access the statuses
        Stream.of(userAuthenticatedHeader, pmAuthenticatedHeader)
              .forEach(header -> given().header(header)
                                        .accept(ContentType.JSON)
                                        .when()
                                        .get("/api/projects/" + createdProject.id() + "/status")
                                        .then()
                                        .statusCode(200)
                                        .body("$.size()", greaterThan(0))
                                        .body("[0].name", is(workflow.start())));
    }

    @Test
    @Order(16)
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

    @Test
    @Order(17)
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
    @Order(18)
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

    @Test
    @Order(19)
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
    @Order(20)
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

package dev.vepo.issues.project.find;

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
class FindProjectByIdEndpointTest {

    private WorkflowResponse workflow;
    private Header userAuthenticatedHeader;
    private Header pmAuthenticatedHeader;
    private ProjectTestFixtures fixtures;

    @BeforeEach
    void setup() {
        this.fixtures = ProjectTestFixtures.create();
        this.workflow = fixtures.workflow();
        this.userAuthenticatedHeader = fixtures.userAuthenticatedHeader();
        this.pmAuthenticatedHeader = fixtures.pmAuthenticatedHeader();
    }

    @Test
    @DisplayName("Anonymous user cannot read Internal project by ID")
    void anonymousUserShouldNotGetInternalProjectByIdTest() {
        var createdProject = given().header(pmAuthenticatedHeader)
                                    .accept(ContentType.JSON)
                                    .when()
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                              "name": "Anon Deny Internal",
                                              "description": "Anonymous cannot read internal.",
                                              "prefix": "ADI",
                                              "workflowId": %d
                                          }""".formatted(workflow.id()))
                                    .post("/api/projects")
                                    .then()
                                    .statusCode(201)
                                    .extract()
                                    .as(ProjectResponse.class);

        given().when()
               .accept(ContentType.JSON)
               .get("/api/projects/" + createdProject.id())
               .then()
               .statusCode(403);
    }

    @Test
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

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + createdProject.id())
               .then()
               .statusCode(200)
               .body("id", is((int) createdProject.id()))
               .body("name", is("Test Project For Get"))
               .body("securityLevel", is("INTERNAL"));

        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + createdProject.id())
               .then()
               .statusCode(200)
               .body("id", is((int) createdProject.id()))
               .body("name", is("Test Project For Get"))
               .body("description", is("This is a test project for get by ID."))
               .body("workflow.id", is((int) workflow.id()));
    }

    @Test
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
    @DisplayName("Find project should expose prefixLocked false without tickets and true with tickets")
    void shouldExposePrefixLockedOnFindProject() {
        var suffix = java.util.UUID.randomUUID().toString().substring(0, 6);
        var emptyProject = given().header(pmAuthenticatedHeader)
                                  .accept(ContentType.JSON)
                                  .when()
                                  .contentType(ContentType.JSON)
                                  .body("""
                                        {
                                            "name": "Find Prefix Empty %s",
                                            "description": "No tickets yet.",
                                            "prefix": "FE%s",
                                            "workflowId": %d
                                        }""".formatted(suffix, suffix.substring(0, 2), workflow.id()))
                                  .post("/api/projects")
                                  .then()
                                  .statusCode(201)
                                  .extract()
                                  .as(ProjectResponse.class);

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + emptyProject.id())
               .then()
               .statusCode(200)
               .body("prefixLocked", is(false));

        var lockedSuffix = java.util.UUID.randomUUID().toString().substring(0, 6);
        var lockedProject = given().header(pmAuthenticatedHeader)
                                   .accept(ContentType.JSON)
                                   .when()
                                   .contentType(ContentType.JSON)
                                   .body("""
                                         {
                                             "name": "Find Prefix Locked %s",
                                             "description": "Has a ticket.",
                                             "prefix": "FL%s",
                                             "workflowId": %d
                                         }""".formatted(lockedSuffix, lockedSuffix.substring(0, 2), workflow.id()))
                                   .post("/api/projects")
                                   .then()
                                   .statusCode(201)
                                   .extract()
                                   .as(ProjectResponse.class);

        var category = fixtures.category();
        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "title": "Locks find prefixLocked",
                         "description": "Creates prefix lock for find response.",
                         "projectId": %d,
                         "categoryId": %d
                     }""".formatted(lockedProject.id(), category.getId()))
               .post("/api/tickets")
               .then()
               .statusCode(201);

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects/" + lockedProject.id())
               .then()
               .statusCode(200)
               .body("prefixLocked", is(true));
    }
}

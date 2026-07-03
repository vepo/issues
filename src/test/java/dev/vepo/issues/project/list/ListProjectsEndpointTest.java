package dev.vepo.issues.project.list;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
class ListProjectsEndpointTest {

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
    @DisplayName("Non authenticated user should not be able to list projects")
    void nonAuthenticatedUserShouldNotListProjectsTest() {
        given().when()
               .accept(ContentType.JSON)
               .get("/api/projects")
               .then()
               .statusCode(401);
    }

    @Test
    @DisplayName("Only authenticated users should be able to list projects")
    void onlyAuthenticatedUsersShouldListProjectsTest() {
        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("Project listing is scoped to membership for users and ownership for PMs")
    void projectListingRespectsMembershipScope() {
        var suffix = java.util.UUID.randomUUID().toString().substring(0, 6);
        var projectName = "Scoped Project " + suffix;
        var createdProject = given().header(pmAuthenticatedHeader)
                                    .accept(ContentType.JSON)
                                    .when()
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                              "name": "%s",
                                              "description": "This is a test project.",
                                              "prefix": "SP%s",
                                              "workflowId": %d
                                          }""".formatted(projectName, suffix.substring(0, 2), workflow.id()))
                                    .post("/api/projects")
                                    .then()
                                    .statusCode(201)
                                    .extract()
                                    .jsonPath();

        var projectId = createdProject.getLong("id");

        given().header(pmAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects")
               .then()
               .statusCode(200)
               .body("find { it.id == %d }.name".formatted(projectId), is(projectName));

        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects")
               .then()
               .statusCode(200)
               .body("find { it.id == %d }".formatted(projectId), org.hamcrest.Matchers.nullValue());

        dev.vepo.issues.Given.addProjectMember(projectId, "user@issues.vepo.dev");

        given().header(userAuthenticatedHeader)
               .accept(ContentType.JSON)
               .when()
               .get("/api/projects")
               .then()
               .statusCode(200)
               .body("find { it.id == %d }.name".formatted(projectId), is(projectName));
    }
}
